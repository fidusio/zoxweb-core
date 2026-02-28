/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.io;

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.HashResult;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.net.ProxyType;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.RegistrarMapDefault;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.*;
import java.net.*;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IOUtil {



    private IOUtil() {
    }


    public static String resourceToString(String resourceID) throws IOException {
        return resourceToString(IOUtil.class, resourceID);
    }

    public static String resourceToString(Class<?> clazz, String resourceID) throws IOException {
        return inputStreamToString(clazz.getResourceAsStream(resourceID), true);
    }

    public static File validateAsFile(String filename) {
        filename = SUS.trimOrNull(filename);
        SUS.checkIfNulls("Filename can't be null.", filename);

        return validateAsFile(new File(filename));
    }

    public static File validateAsFile(File file) {
        SUS.checkIfNulls("File can't be null.", file);
        return (file.exists() && file.isFile()) ? file : null;
    }


    public static RandomAccessFile endOfFile(RandomAccessFile br)
            throws IOException {
        br.seek(br.length());
        return br;
    }


    public static void printFileSystem(FileSystem fs) throws IOException {
        for (Path root : fs.getRootDirectories()) {
            Files.walk(root)
                    .forEach(path -> {
                        Path relPath = root.relativize(path);
                        String display = relPath.toString().isEmpty() ? root.toString() : relPath.toString();
                        System.out.print(display);
                    });
        }
    }

    public static String toStringFileSystem(FileSystem fs) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Path root : fs.getRootDirectories()) {
            Files.walk(root)
                    .forEach(path -> {
                        Path relPath = root.relativize(path);
                        sb.append(relPath.toString().isEmpty() ? root.toString() : relPath.toString());
                        sb.append("\n");

                    });
        }
        return sb.toString();
    }

    /**
     * This method will read all the response part of the url connection
     *
     * @param url to be read
     * @return ByteArrayOutputStream.
     * @throws IOException in case of an io exception
     */
    public static ByteArrayOutputStream readAllURLResponse(URL url)
            throws IOException {
        URLConnection con = url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);

        con.setUseCaches(false);
        con.connect();
        try (InputStream is = con.getInputStream()) {
            return inputStreamToByteArray(is, false);
        } finally {
            if (con instanceof HttpURLConnection) {
                ((HttpURLConnection) con).disconnect();
            }
        }
    }

    public static File locateFile(String filename) {
        File ret = validateAsFile(filename);
        if (ret != null)
            return ret;
        return locateFile(ClassLoader.getSystemClassLoader(), filename);
    }


    public static File locateFile(ClassLoader cl, String filename) {
        File ret = new File(filename);
        if (!ret.exists() || !ret.isFile()) {
            java.net.URL resource = cl.getResource(filename);
            if (resource == null) {
                return null;
            }
            ret = new File(resource.getFile());
        }

        return ret;
    }

    /**
     * Delete a directory recursively
     *
     * @param path to be deleted
     * @throws IOException in case of an error encountered
     */
    public static void deleteDirectoryRecursively(Path path)
            throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursively(entry);
                }
            }
        }

        Files.delete(path);
    }

    public static List<URL> listMatches(Path rootDir, String filterPattern, String filterExclusion)
            throws IOException {

        List<URL> ret = new ArrayList<>();

        Predicate<Path> composition = null;
        Predicate<Path> pattern = p -> p.toString().matches(filterPattern);
        if (!SUS.isEmpty(filterExclusion)) {
            Predicate<Path> exclusion = p -> p.toString().matches(filterExclusion);
            composition = pattern.and(exclusion.negate());
        } else {
            composition = pattern;
        }

        try (Stream<Path> paths = Files.walk(rootDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(composition)
                    .forEach(p ->
                    {
                        try {
                            ret.add(p.toUri().toURL());
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    });
        }

        return ret;
    }


    /**
     * @param is to be read
     * @param charsetEncoding charset encoding
     * @param close if true
     * @return
     * @throws IOException
     */
    public static String inputStreamToString(InputStream is, String charsetEncoding, boolean close)
            throws IOException {
        UByteArrayOutputStream baos = inputStreamToByteArray(is, close);
        return new String(baos.getInternalBuffer(), 0, baos.size(), charsetEncoding);
    }

    public static long countInputStreamBytes(InputStream is, boolean close)
            throws IOException {
        byte[] buffer = ByteBufferUtil.allocateByteArray(SharedIOUtil.K_8);
        int read;
        long counter = 0;

        try {
            while ((read = is.read(buffer, 0, buffer.length)) != -1) {
                counter += read;
            }

        } finally {
            ByteBufferUtil.cache(buffer);
            if (close) {
                SharedIOUtil.close(is);
            }
        }


        return counter;

    }


    /**
     * Load path file as a string
     *
     * @param filename to read
     * @return file content as string
     * @throws IOException in case of error
     */
    public static String inputStreamToString(String filename)
            throws IOException {
        return inputStreamToString(new FileInputStream(filename), true);
    }

    /**
     * Load path file as a string
     *
     * @param file to read
     * @return file content as string
     * @throws IOException in case of error
     */
    public static String inputStreamToString(File file)
            throws IOException {
        return inputStreamToString(new FileInputStream(file), true);
    }


    /**
     * Load path file as a string
     *
     * @param path to read
     * @return file content as string
     * @throws IOException in case of error
     */
    public static String pathToString(Path path)
            throws IOException {
        return inputStreamToString(Files.newInputStream(path), true);
    }

    /**
     * @param is to be completely read and converted to string
     * @param close if true will close the stream at the end
     * @return file content as string
     * @throws IOException in case of io errors
     */
    public static String inputStreamToString(InputStream is, boolean close)
            throws IOException {
        byte[] buffer = ByteBufferUtil.allocateByteArray(SharedIOUtil.K_8);
        StringBuilder sb = new StringBuilder();
        int read;
        try {
            while ((read = is.read(buffer, 0, buffer.length)) > 0) {
                sb.append(new String(buffer, 0, read, Const.UTF_8));
            }

        } finally {
            ByteBufferUtil.cache(buffer);
            if (close) {
                SharedIOUtil.close(is);
            }
        }

        return sb.toString();
    }

    /**
     *
     * @param reader to be completely read and converted to string
     * @param close if true will close the stream at the end
     * @return reader content as string
     * @throws IOException in case of io errors
     */
    public static String readerToString(Reader reader, boolean close)
            throws IOException {
        char[] buffer = new char[SharedIOUtil.K_4];
        StringBuilder sb = new StringBuilder();
        int read;

        try {
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                sb.append(buffer, 0, read);
            }
        } finally {
            if (close) {
                SharedIOUtil.close(reader);
            }
        }

        return sb.toString();
    }


    /**
     * Read the all the content of an input stream and return it as a byte array
     *
     * @param is    to be read
     * @param close if true it will be closed after reading
     * @return byte array
     * @throws IOException
     */
    public static UByteArrayOutputStream inputStreamToByteArray(InputStream is, boolean close)
            throws IOException {
        UByteArrayOutputStream baos = new UByteArrayOutputStream();
        byte[] buffer = ByteBufferUtil.allocateByteArray(SharedIOUtil.K_8);
        try {
            int read;

            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
        } finally {
            ByteBufferUtil.cache(buffer);
            if (close) {
                SharedIOUtil.close(is);
            }

        }

        return baos;
    }


    /**
     * Read the all the content of an input stream and return it as a byte array
     *
     * @param filename to be read
     * @param close    if true is will closed after reading
     * @return byte array
     * @throws IOException
     */
    public static UByteArrayOutputStream inputStreamToByteArray(String filename, boolean close)
            throws IOException {
        return inputStreamToByteArray(new File(filename), close);
    }

    /**
     * Read the all the content of an input stream and return it as UByteArrayOutputStream
     *
     * @param file  to be read
     * @param close if true is will be closed after reading
     * @return UByteArrayOutputStream
     * @throws IOException in case of IO errors
     */
    public static UByteArrayOutputStream inputStreamToByteArray(File file, boolean close)
            throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return inputStreamToByteArray(is, close);
        } finally {
            SharedIOUtil.close(is);
        }
    }


    /**
     * Write a string to file
     *
     * @param file to be created and written to
     * @param toWrite string content
     * @throws IOException in case of IO errors
     */
    public static void writeToFile(File file, String toWrite)
            throws IOException {
        writeToFile(file, SharedStringUtil.getBytes(toWrite));
    }

    /**
     * Write a byte[] to file
     *
     * @param file to be created and written to
     * @param buffer byte[] content
     * @throws IOException in case of IO errors
     */
    public static void writeToFile(File file, byte[] buffer)
            throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(buffer);
        } finally {
            SharedIOUtil.close(fos);
        }

    }

    /**
     * Return the file creation in millis
     *
     * @param file to check
     * @return file creation time in millis
     * @throws IOException in case of IO errors
     */
    public static long fileCreationTime(File file)
            throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException();
        }

        return fileCreationTime(file.toPath());
    }

    /**
     * Return the path creation in millis
     *
     * @param path to be checked
     * @return file creation time in millis
     * @throws IOException in case of IO errors
     */
    public static long fileCreationTime(Path path)
            throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        return attributes.creationTime().to(TimeUnit.MILLISECONDS);
    }

    /**
     * @param dirName to be created
     * @return file representing the directory if it failed
     */
    public static File createDirectory(String dirName) {
        File dir = new File(dirName);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (!dir.isDirectory())
            return null;

        return dir;
    }

    /**
     * @param is to be read
     * @param os to be written to
     * @param closeBoth if true close both stream
     * @return how many bytes where successfully copied
     * @throws IOException in case of IO errors
     */
    public static long relayStreams(InputStream is, OutputStream os, boolean closeBoth)
            throws IOException {
        return relayStreams(is, os, closeBoth, closeBoth);
    }

    public static HashResult relayStreams(CryptoConst.HashType hashType, InputStream is, OutputStream os, boolean both)
            throws IOException {
        return relayStreams(hashType, is, os, both, both);
    }

    /**
     * relays streams and update message digest if not null
     * @param md to be updated
     * @param is input stream
     * @param os output stream
     * @return total data copied between is and os
     * @throws IOException in case of IO errors
     */
    public static long relayStreams(MessageDigest md, InputStream is, OutputStream os)
            throws IOException {

        long totalCopied = 0;
        int read;
        byte[] buffer = ByteBufferUtil.allocateByteArray(SharedIOUtil.K_8);
        try {
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                totalCopied += read;
                if (md != null)
                    md.update(buffer, 0, read);
            }
            os.flush();
        } finally {
            ByteBufferUtil.cache(buffer);
        }
        return totalCopied;
    }


    public static HashResult relayStreams(CryptoConst.HashType hashType, InputStream is, OutputStream os, boolean closeIS, boolean closeOS)
            throws IOException {
        long totalCopied = 0;
        try {
            MessageDigest md = MessageDigest.getInstance(hashType.getName());
            try {
                totalCopied = relayStreams(md, is, os);
            } finally {
                if (closeIS || is instanceof CloseEnabledInputStream) {
                    SharedIOUtil.close(is);
                }

                if (closeOS || os instanceof CloseEnabledOutputStream) {
                    SharedIOUtil.close(os);
                }
            }

            return new HashResult(hashType, md.digest(), totalCopied);
        } catch (NoSuchAlgorithmException he) {
            throw new IOException(he);
        }
    }

    /**
     * @param is
     * @param os
     * @param closeIS
     * @param closeOS
     * @return
     * @throws IOException
     */
    public static long relayStreams(InputStream is, OutputStream os, boolean closeIS, boolean closeOS)
            throws IOException {

        try {
            return relayStreams(null, is, os);
        } finally {
            if (closeIS || is instanceof CloseEnabledInputStream) {
                SharedIOUtil.close(is);
            }

            if (closeOS || os instanceof CloseEnabledOutputStream) {
                SharedIOUtil.close(os);
            }
        }
    }

    /**
     * This utility method will check if the output stream != null then invoke flush
     *
     * @param os output stream
     * @throws IOException if os.flush() throw an exception
     */
    public static void flush(OutputStream os)
            throws IOException {
        if (os != null) {
            os.flush();
        }
    }

    public static List<byte[]> parse(UByteArrayOutputStream uboas, byte[] delim, boolean noEmpty) {
        if (uboas == null || delim == null) {
            throw new NullPointerException("uboas and/or delimiter cannot be null.");
        }

        if (delim.length == 0) {
            throw new IllegalArgumentException("Empty delimiter is not accepted.");
        }

        int match = 0;
        ArrayList<byte[]> matchedTokens = new ArrayList<byte[]>();
        int index = 0;
        synchronized (uboas) {
            while ((match = uboas.indexOf(index, delim)) != -1) {
                byte[] message = Arrays.copyOfRange(uboas.getInternalBuffer(), index, match);
                //uboas.removeAt(0, match + delim.length);

                if (!noEmpty || message.length > 0)
                    matchedTokens.add(message);
                index = match + delim.length;
            }
            if (index > 0) {
                uboas.removeAt(0, index);
            }
        }

        return matchedTokens;
    }


    public static Proxy.Type toProxyType(ProxyType pt) {
        switch (pt) {
            case DIRECT:
                return Proxy.Type.DIRECT;
            case HTTP:
                return Proxy.Type.HTTP;
            case SOCKS:
                return Proxy.Type.SOCKS;
        }

        return null;
    }


    public static Proxy toProxy(IPAddress proxyInfo) {
        return new Proxy(toProxyType(proxyInfo.getProxyType()), new InetSocketAddress(proxyInfo.getInetAddress(), proxyInfo.getPort()));
    }

    public static boolean isFileInDirectory(String directoryPathStr, String filePathStr) {
        try {
            Path filePath = Paths.get(filePathStr).toRealPath();
            Path directoryPath = Paths.get(directoryPathStr).toRealPath();

            return filePath.startsWith(directoryPath);
        } catch (IOException e) {
        }
        return false;
    }

    public static boolean isFileInDirectory(File directory, File file) {
        try {
            String fileCanonicalPath = file.getCanonicalPath();
            String directoryCanonicalPath = directory.getCanonicalPath();

            return fileCanonicalPath.startsWith(directoryCanonicalPath + File.separator);
        } catch (IOException e) {
        }

        return false;
    }

    public static File addFileExtension(File file, String ext) {
        String newName = file.getAbsolutePath() + "." + ext;
        file.renameTo(new File(newName));
        return file;
    }

    public static File removeFileExtension(File file, String ext) {
        String absPath = file.getAbsolutePath();
        if (absPath.endsWith("." + ext)) {
            String newPath = absPath.substring(0, absPath.length() - (ext.length() + 1));
            file.renameTo(new File(newPath));
        }
        return file;
    }

    /**
     * True if file exists and regular file and readable
     * @param pFile to check
     * @return status
     */
    public static boolean isRegularFile(Path pFile) {
        return Files.exists(pFile) && Files.isRegularFile(pFile) && Files.isReadable(pFile);
    }

    public static Path regularFileOrNull(Path pFile) {
        return isRegularFile(pFile) ? pFile : null;
    }

    /**
     * Find the first match in the parentPath the match is case-insensitive
     * @param parentPath entry path
     * @param toFind file name looking for
     * @param cache if not null the match in cached
     * @return matching path or null
     * @throws IOException in case of io error
     */
    public static Path findFirstMatchingInPath(Path parentPath, String toFind, RegistrarMapDefault<String, Path> cache) throws IOException {
        Path searchPathObj = Paths.get(toFind);

        Path match = cache != null ? cache.lookup(toFind) : null;

        if (match == null) {
            try (Stream<Path> stream = Files.walk(parentPath)) {
                Optional<Path> search = stream
                        .filter(IOUtil::isRegularFile)
                        .filter(p -> endsWithIgnoreCase(p, searchPathObj))
                        .findFirst();

                if (search.isPresent()) {
                    match = search.get();
                    if (cache != null)
                        cache.map(match, toFind);
                }
            }
        }

        return match;
    }

    public static boolean endsWithIgnoreCase(Path fullPath, Path suffix) {
        int suffixCount = suffix.getNameCount();
        int fullCount = fullPath.getNameCount();

        if (suffixCount > fullCount) return false;

        for (int i = 0; i < suffixCount; i++) {
            String suffixPart = suffix.getName(suffixCount - 1 - i).toString();
            String fullPart = fullPath.getName(fullCount - 1 - i).toString();
            if (!suffixPart.equalsIgnoreCase(fullPart)) {
                return false;
            }
        }
        return true;
    }
}
