package org.zoxweb.server.net;

import org.zoxweb.server.http.OkHTTPCall;
import org.zoxweb.server.io.UByteArrayInputStream;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.net.MACAddressFilter;
import org.zoxweb.shared.net.OUIVendor;
import org.zoxweb.shared.util.SUS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * In-memory MAC vendor (OUI) lookup table.
 * <p>
 * Every entry is an {@link OUIVendor}: a registered MAC prefix, its mask length in bits and the
 * short and long manufacturer names. Entries are indexed by mask length and normalized hex prefix,
 * and {@link #lookup(String)} performs a longest-prefix match (/36 before /28 before /24).
 * <p>
 * The table can be built from two sources:
 * <ul>
 *   <li>the Wireshark manuf file, through {@link #load(String)} (download),
 *       {@link #load(InputStream)} or {@link #loadResource(String)}; the file is tab-separated:
 *       {@code prefix[/mask] <tab> short name <tab> long name};</li>
 *   <li>entries already persisted in a datastore, through {@link #load(Iterable)} or
 *       {@link #add(OUIVendor)}.</li>
 * </ul>
 * {@link #getVendors()} returns the indexed entries, so a file-built table can be handed to a
 * datastore and later rebuilt from it.
 * <p>
 * Usage model: build the table first, then share it for lookups. {@link #add(OUIVendor)} and
 * {@link #getVendors()} are synchronized; {@link #lookup(String)} is lock-free and must not run
 * concurrently with adds.
 * Java 8 compatible.
 */
public final class OUIDB {

    /** Download location of the Wireshark manuf file. */
    public static final String WIRESHARK_MANUF_URL =
            "https://www.wireshark.org/download/automated/data/manuf";

    // mask bits (descending) -> hex prefix (bits/4 chars) -> vendor
    private final NavigableMap<Integer, Map<String, OUIVendor>> byMask =
            new TreeMap<Integer, Map<String, OUIVendor>>(Collections.<Integer>reverseOrder());

    private int entries;

    /** Creates an empty table; fill it with one of the {@code load} methods or {@link #add(OUIVendor)}. */
    public OUIDB() {
    }

    // ---------------------------------------------------------------- loading

    /**
     * Downloads a manuf file over HTTP(S) and indexes its entries.
     *
     * @param url the file location, e.g. {@link #WIRESHARK_MANUF_URL}
     * @return this table, for chaining
     * @throws IOException if the request fails or returns an error status
     */
    public OUIDB load(String url) throws IOException {
        return load(new UByteArrayInputStream(getRawContent(url)));
    }


    public static byte[] getRawContent(String url) throws IOException {
        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, HTTPMethod.GET);
        hmci.getHeaders().build("User-Agent", "OuiDB/1.0");
        return OkHTTPCall.send(hmci).getData();
    }

    /**
     * Indexes a manuf file bundled on the classpath.
     *
     * @param resource the absolute resource path, e.g. {@code "/manuf"}
     * @return this table, for chaining
     * @throws IOException if the resource is missing or cannot be read
     */
    public OUIDB loadResource(String resource) throws IOException {
        InputStream in = OUIDB.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("Resource not found: " + resource);
        }
        try {
            return load(in);
        } finally {
            in.close();
        }
    }

    public OUIDB load(byte[] data) throws IOException {
        return load(new UByteArrayInputStream(data));
    }


    /**
     * Parses manuf-format data and indexes one {@link OUIVendor} per usable line.
     * <p>
     * Parsing rules: the stream is read as UTF-8; blank lines and lines starting with {@code #}
     * are skipped; a prefix without {@code /mask} is a /24; lines with fewer than two columns,
     * an unparsable mask or a prefix shorter than the mask are skipped; when the long name column
     * is missing or empty the short name is used for both.
     * The caller owns and closes the stream.
     *
     * @param in the manuf data
     * @return this table, for chaining
     * @throws IOException if reading the stream fails
     */
    public OUIDB load(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#' || line.trim().isEmpty()) continue;

            String[] f = line.split("\t");
            if (f.length < 2) continue;

            String prefix = f[0].trim();
            int bits = 24;
            int slash = prefix.indexOf('/');
            if (slash > 0) {
                try {
                    bits = Integer.parseInt(prefix.substring(slash + 1).trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                prefix = prefix.substring(0, slash);
            }

            String hex = MACAddressFilter.normalize(prefix);
            int len = bits / 4;
            if (hex.length() < len) continue;
            hex = hex.substring(0, len);

            String shortName = f[1].trim();
            String longName = f.length >= 3 ? f[2].trim() : "";
            if (longName.isEmpty()) longName = shortName;

            add(new OUIVendor(hex, bits, shortName, longName));
        }
        return this;
    }

    /**
     * Indexes existing entries, e.g. the result of a datastore search.
     * Entries that {@link #add(OUIVendor)} rejects are silently skipped.
     *
     * @param vendors the entries to index, may be null
     * @return this table, for chaining
     */
    public OUIDB load(Iterable<OUIVendor> vendors) {
        if (vendors != null) {
            for (OUIVendor v : vendors) {
                add(v);
            }
        }
        return this;
    }

    /**
     * Indexes one entry under its mask length and prefix. The index key is the prefix as
     * returned by {@link MACAddressFilter#normalize(String)}, truncated to the mask length; the
     * entry itself is stored as given and not modified. A later entry for the same prefix and
     * mask replaces the earlier one.
     * <p>
     * Rejected and not indexed: null, entries flagged locally administered, a mask length of
     * zero or less, a null prefix, and a prefix shorter than the mask length.
     *
     * @param vendor the entry to index
     * @return true if the entry was indexed, false if it was rejected
     */
    public synchronized boolean add(OUIVendor vendor) {
        if (vendor == null || vendor.isLocallyAdministered()) return false;
        int bits = vendor.getMaskBits();
        String hex = MACAddressFilter.normalize(vendor.getPrefix());
        if (bits <= 0 || hex == null || hex.length() < bits / 4) return false;
        hex = hex.substring(0, bits / 4);

        Map<String, OUIVendor> m = byMask.get(bits);
        if (m == null) {
            m = new HashMap<String, OUIVendor>();
            byMask.put(bits, m);
        }
        if (m.put(hex, vendor) == null) entries++;
        return true;
    }

    /**
     * Returns every indexed entry, longest mask first, e.g. to persist a file-built table.
     * The list is a new one the caller owns, but the entries are the live {@link OUIVendor}
     * instances held by the index, by design: changing one changes what
     * {@link #lookup(String)} returns.
     *
     * @return the indexed entries
     */
    public synchronized List<OUIVendor> getVendors() {
        List<OUIVendor> ret = new ArrayList<OUIVendor>(entries);
        for (Map<String, OUIVendor> m : byMask.values()) {
            ret.addAll(m.values());
        }
        return ret;
    }

    // ---------------------------------------------------------------- lookup

    /**
     * Resolves a MAC address to its vendor by longest-prefix match: the /36 index is consulted
     * first, then /28, then /24, and the first hit is returned.
     * <p>
     * The input is normalized with {@link MACAddressFilter#normalize(String)}, so any separator
     * style, case and surrounding whitespace are accepted; a bare hex prefix of at least six
     * digits works too. If the first octet has the locally administered (U/L) bit set, the index
     * is not consulted and a fresh {@link OUIVendor#newLocallyAdministered()} entry is returned.
     *
     * @param mac the MAC address, or a hex prefix of at least six digits
     * @return the matching entry (shared with the index), a locally administered entry, or null
     *         if the address is null, too short, not hex, or not registered
     */
    public OUIVendor lookup(String mac) {
        if (mac == null) return null;
        String hex = MACAddressFilter.normalize(mac);
        if (hex.length() < 6) return null;

        int firstOctet;
        try {
            firstOctet = Integer.parseInt(hex.substring(0, 2), 16);
        } catch (NumberFormatException e) {
            return null;
        }
        if ((firstOctet & 0x02) != 0) return OUIVendor.newLocallyAdministered();

        for (Map.Entry<Integer, Map<String, OUIVendor>> e : byMask.entrySet()) {
            int n = e.getKey() / 4;
            if (hex.length() >= n) {
                OUIVendor v = e.getValue().get(hex.substring(0, n));
                if (v != null) return v;
            }
        }
        return null;
    }

    /**
     * Byte-array form of {@link #lookup(String)}.
     *
     * @param mac the MAC address bytes, normally six
     * @return the matching entry, a locally administered entry, or null if not found or mac is null
     */
    public OUIVendor lookup(byte[] mac) {
        if (mac == null) return null;
        return lookup(SUS.bytesToHex(mac));
    }

    /**
     * Convenience over {@link #lookup(String)}.
     *
     * @param mac the MAC address, in any form {@link #lookup(String)} accepts
     * @return the short manufacturer name, or null if not found
     */
    public String lookupName(String mac) {
        OUIVendor v = lookup(mac);
        return v != null ? v.getName() : null;
    }

    /**
     * Convenience over {@link #lookup(String)}.
     *
     * @param mac the MAC address, in any form {@link #lookup(String)} accepts
     * @return the long manufacturer name, or null if not found
     */
    public String lookupDescription(String mac) {
        OUIVendor v = lookup(mac);
        return v != null ? v.getDescription() : null;
    }

    /**
     * Returns the number of indexed entries, counting each prefix/mask pair once.
     *
     * @return the entry count
     */
    public int size() {
        return entries;
    }

    // ---------------------------------------------------------------- demo

    /**
     * Demo: downloads the Wireshark manuf file and prints the vendor of each MAC address given
     * on the command line, or of a built-in sample set when none is given.
     *
     * @param args optional MAC addresses to resolve
     * @throws IOException if the download fails
     */
    public static void main(String[] args) throws IOException {
        OUIDB db = new OUIDB().load(WIRESHARK_MANUF_URL);
        System.out.println("Loaded " + db.size() + " prefixes");

        String[] macAddresses = {
                "00:00:0C:12:34:56",
                "00-1B-C5-00-00-01",
                "DA:A1:19:00:00:01",
                "24:A1:0D:6A:3C:7E",
                "24:A1:0D:5A:3C:7E"
        };
        if (args.length != 0) {
            macAddresses = args;
        }

        for (String mac : macAddresses) {
            byte[] inBytes = MACAddressFilter.macAddressToBytes(mac);
            OUIVendor v = db.lookup(inBytes);
            System.out.println(SUS.bytesToHex(inBytes) + " Vendor: "
                    + (v != null ? v.getName() + " / " + v.getDescription() : "unknown"));
        }
    }
}
