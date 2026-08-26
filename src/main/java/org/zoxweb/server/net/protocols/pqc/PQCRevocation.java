package org.zoxweb.server.net.protocols.pqc;

import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.NVGenericMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;

/**
 * Best-effort leaf revocation check of the PQC auditor (META-TCP-PQC.md §7), resolved fastest
 * first: a handshake-stapled OCSP response (zero network), one active OCSP POST against the
 * certificate's AIA responder, then one CRL download from its distribution point. Soft-fail
 * throughout: any failure yields {@code status: not_checked} with the reason — never a thrown
 * exception, never an unbounded network wait.
 * <p>
 * Lives in the {@code pqc} package because OCSP request/response handling is Bouncy Castle
 * (bcpkix) — the package is the only place in the tree with BC imports.
 */
public final class PQCRevocation {

    public static final long DEFAULT_TIMEOUT_MILLIS = 5000;

    /** CRLs can run to megabytes; anything past this is not worth the audit's time. */
    public static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;

    private PQCRevocation() {
    }

    /**
     * @param leaf         the certificate to check
     * @param issuer       its issuer (needed for the active OCSP CertificateID), null when absent
     * @param stapledOCSP  the handshake-stapled response, null when the server stapled none
     * @param timeoutMillis hard bound per network exchange; {@code <= 0} uses the default
     * @return a {@code revocation} sub-map: {@code status} ({@code good} / {@code revoked} /
     * {@code unknown} / {@code not_checked}), {@code source} ({@code stapled_ocsp} /
     * {@code ocsp} / {@code crl}), and {@code reason} when not checked
     */
    public static NVGenericMap check(X509Certificate leaf, X509Certificate issuer,
                                     byte[] stapledOCSP, long timeoutMillis) {
        NVGenericMap revocation = new NVGenericMap("revocation");
        long timeout = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MILLIS;

        // 1) stapled: provided in the handshake, no network
        if (stapledOCSP != null) {
            String status = ocspStatus(stapledOCSP, leaf);
            if (status != null)
                return revocation.build("status", status).build("source", "stapled_ocsp");
        }

        // 2) active OCSP: one POST to the AIA responder
        String ocspFailure;
        String ocspUrl = ocspURL(leaf);
        if (ocspUrl != null && issuer != null) {
            try {
                byte[] request = buildOCSPRequest(leaf, issuer);
                byte[] response = httpFetch(ocspUrl, request, "application/ocsp-request", timeout);
                String status = ocspStatus(response, leaf);
                if (status != null)
                    return revocation.build("status", status).build("source", "ocsp");
                ocspFailure = "unparseable responder answer";
            } catch (Exception e) {
                ocspFailure = e.getMessage() != null ? e.getMessage() : e.toString();
            }
        } else {
            ocspFailure = ocspUrl == null ? "no responder in AIA" : "issuer certificate unavailable";
        }

        // 3) CRL fallback: one bounded download
        String crlFailure;
        String crlUrl = crlURL(leaf);
        if (crlUrl != null) {
            try {
                byte[] crlBytes = httpFetch(crlUrl, null, null, timeout);
                X509CRL crl = (X509CRL) CertificateFactory.getInstance("X.509")
                        .generateCRL(new ByteArrayInputStream(crlBytes));
                return revocation.build("status", crl.isRevoked(leaf) ? "revoked" : "good")
                        .build("source", "crl");
            } catch (Exception e) {
                crlFailure = e.getMessage() != null ? e.getMessage() : e.toString();
            }
        } else {
            crlFailure = "no distribution point";
        }

        return revocation.build("status", "not_checked")
                .build("reason", "ocsp: " + ocspFailure + "; crl: " + crlFailure);
    }

    // ---- OCSP ----

    /** @return good / revoked / unknown from a DER OCSP response, or null when unusable */
    private static String ocspStatus(byte[] der, X509Certificate leaf) {
        try {
            OCSPResp response = new OCSPResp(der);
            if (response.getStatus() != OCSPResp.SUCCESSFUL)
                return null;
            BasicOCSPResp basic = (BasicOCSPResp) response.getResponseObject();
            if (basic == null)
                return null;
            SingleResp[] singles = basic.getResponses();
            SingleResp match = null;
            for (SingleResp single : singles) {
                if (single.getCertID() != null
                        && leaf.getSerialNumber().equals(single.getCertID().getSerialNumber())) {
                    match = single;
                    break;
                }
            }
            // a lone entry that doesn't advertise the serial still refers to the leaf
            if (match == null && singles.length == 1)
                match = singles[0];
            if (match == null)
                return null;
            Object status = match.getCertStatus();
            if (status == CertificateStatus.GOOD)
                return "good";
            return status instanceof RevokedStatus ? "revoked" : "unknown";
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] buildOCSPRequest(X509Certificate leaf, X509Certificate issuer) throws Exception {
        DigestCalculatorProvider digests = new JcaDigestCalculatorProviderBuilder().build();
        CertificateID id = new CertificateID(digests.get(CertificateID.HASH_SHA1),
                new JcaX509CertificateHolder(issuer), leaf.getSerialNumber());
        OCSPReqBuilder builder = new OCSPReqBuilder();
        builder.addRequest(id);
        return builder.build().getEncoded();
    }

    // ---- certificate extension URLs ----

    /** @return the certificate's AIA OCSP responder URL, or null */
    public static String ocspURL(X509Certificate cert) {
        try {
            AuthorityInformationAccess aia = AuthorityInformationAccess.fromExtensions(
                    new JcaX509CertificateHolder(cert).getExtensions());
            if (aia != null) {
                for (AccessDescription access : aia.getAccessDescriptions()) {
                    if (AccessDescription.id_ad_ocsp.equals(access.getAccessMethod())
                            && access.getAccessLocation().getTagNo() == GeneralName.uniformResourceIdentifier)
                        return access.getAccessLocation().getName().toString();
                }
            }
        } catch (Exception e) {
            // absent
        }
        return null;
    }

    /** @return the certificate's first CRL distribution point URL, or null */
    public static String crlURL(X509Certificate cert) {
        try {
            CRLDistPoint distPoints = CRLDistPoint.fromExtensions(
                    new JcaX509CertificateHolder(cert).getExtensions());
            if (distPoints != null) {
                for (DistributionPoint point : distPoints.getDistributionPoints()) {
                    DistributionPointName name = point.getDistributionPoint();
                    if (name != null && name.getType() == DistributionPointName.FULL_NAME) {
                        for (GeneralName general : GeneralNames.getInstance(name.getName()).getNames()) {
                            if (general.getTagNo() == GeneralName.uniformResourceIdentifier)
                                return general.getName().toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // absent
        }
        return null;
    }

    // ---- bounded HTTP ----

    /** One bounded exchange: POST when a body is given, GET otherwise; hard timeouts, size cap. */
    private static byte[] httpFetch(String url, byte[] body, String contentType, long timeoutMillis)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            int timeout = (int) Math.min(Integer.MAX_VALUE, Math.max(1000, timeoutMillis / 2));
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setInstanceFollowRedirects(true);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", contentType);
                connection.setFixedLengthStreamingMode(body.length);
                OutputStream out = connection.getOutputStream();
                out.write(body);
                out.flush();
                SharedIOUtil.close(out);
            }
            if (connection.getResponseCode() != 200)
                throw new IOException("HTTP " + connection.getResponseCode() + " from " + url);
            InputStream in = connection.getInputStream();
            try {
                ByteArrayOutputStream collected = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int total = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_RESPONSE_BYTES)
                        throw new IOException("response exceeds " + MAX_RESPONSE_BYTES + " bytes");
                    collected.write(buffer, 0, read);
                }
                return collected.toByteArray();
            } finally {
                SharedIOUtil.close(in);
            }
        } finally {
            connection.disconnect();
        }
    }
}
