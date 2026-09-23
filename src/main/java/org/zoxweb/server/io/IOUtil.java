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
import org.zoxweb.shared.util.RegistrarMapDefault;
import org.zoxweb.shared.util.SUS;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.FileSystem;
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

/**
 * Server-side (full JVM) I/O helpers: slurping streams, files, readers, classpath
 * resources and URLs into strings or byte buffers; relaying one stream into another
 * with optional hashing; file and directory lookup, validation, creation and recursive
 * deletion; path matching; delimiter-based framing of a {@link UByteArrayOutputStream};
 * and {@link Proxy} conversion.
 * <p>
 * Conventions used throughout:
 * <ul>
 *   <li>Scratch buffers come from {@link ByteBufferUtil} and are returned to its pool
 *       in a {@code finally} block.</li>
 *   <li>Every {@code close} / {@code closeIS} / {@code closeOS} flag decides whether the
 *       caller-supplied stream is closed on return (including on error). Streams the method
 *       opened itself (from a file name, {@link File} or {@link Path}) are always closed.</li>
 *   <li>A {@link CloseEnabledInputStream} / {@link CloseEnabledOutputStream} is always
 *       handed to {@link SharedIOUtil#close} by the relay methods; the wrapper itself
 *       decides whether the underlying stream really closes.</li>
 *   <li>Text is decoded as UTF-8 unless a charset is passed explicitly.</li>
 * </ul>
 * The class is stateless and cannot be instantiated.
 */
public class IOUtil {



    private IOUtil() {
    }


    /**
     * Reads a classpath resource resolved relative to {@code IOUtil} itself
     * (see {@link Class#getResourceAsStream(String)} for the absolute/relative rules)
     * and returns its content as a UTF-8 string.
     * @param resourceID resource name
     * @return resource content
     * @throws IOException in case of IO error
     * @throws NullPointerException if the resource does not exist
     */
    public static String resourceToString(String resourceID) throws IOException {
        return resourceToString(IOUtil.class, resourceID);
    }

    /**
     * Reads a classpath resource resolved relative to {@code clazz}
     * (see {@link Class#getResourceAsStream(String)} for the absolute/relative rules)
     * and returns its content as a UTF-8 string. The resource stream is closed.
     * @param clazz whose class loader and package resolve the resource
     * @param resourceID resource name
     * @return resource content
     * @throws IOException in case of IO error
     * @throws NullPointerException if the resource does not exist
     */
    public static String resourceToString(Class<?> clazz, String resourceID) throws IOException {
        return inputStreamToString(clazz.getResourceAsStream(resourceID), true);
    }

    /**
     * Trims the file name and returns it as a {@link File} if it denotes an existing
     * regular file.
     * @param filename path to check
     * @return the file, or null if it does not exist or is not a regular file
     * @throws NullPointerException if {@code filename} is null or blank
     */
    public static File validateAsFile(String filename) {
        filename = SUS.trimOrNull(filename);
        SUS.checkIfNulls("Filename can't be null.", filename);

        return validateAsFile(new File(filename));
    }

    /**
     * Returns {@code file} if it denotes an existing regular file.
     * @param file to check
     * @return the same file, or null if it does not exist or is not a regular file
     * @throws NullPointerException if {@code file} is null
     */
    public static File validateAsFile(File file) {
        SUS.checkIfNulls("File can't be null.", file);
        return (file.exists() && file.isFile()) ? file : null;
    }


    /**
     * Positions a random access file at its end (append position).
     * @param br file to seek
     * @return the same file, for chaining
     * @throws IOException in case of IO error
     */
    public static RandomAccessFile endOfFile(RandomAccessFile br)
            throws IOException {
        br.seek(br.length());
        return br;
    }


    /**
     * Walks every root of a {@link FileSystem} and prints each entry (relative to its
     * root, or the root itself for the root entry) to stdout. Entries are printed with
     * {@code print}, i.e. without separators; see {@link #toStringFileSystem(FileSystem)}
     * for a newline-separated listing.
     * @param fs file system to walk
     * @throws IOException in case of IO error
     */
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

    /**
     * Walks every root of a {@link FileSystem} and returns a newline-separated listing
     * of every entry (relative to its root, or the root itself for the root entry).
     * Useful for dumping zip/jar file systems.
     * @param fs file system to walk
     * @return one entry per line, each line terminated by {@code \n}
     * @throws IOException in case of IO error
     */
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
     * This method will read all the response part of the url connection.
     * <p>
     * Opens a {@link URLConnection} with caching disabled, reads the response body to
     * completion and, for HTTP(S), disconnects. The connection is opened with
     * {@code doOutput=true}; no request body is written.
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

    /**
     * Locates a file first on the file system and, failing that, as a resource of the
     * system class loader (see {@link #locateFile(ClassLoader, String)}).
     * @param filename path or resource name
     * @return the file, or null if not found either way
     * @throws NullPointerException if {@code filename} is null or blank
     */
    public static File locateFile(String filename) {
        File ret = validateAsFile(filename);
        if (ret != null)
            return ret;
        return locateFile(ClassLoader.getSystemClassLoader(), filename);
    }



    /**
     * {@link Path} flavour of {@link #locateFile(String)}.
     * @param filename path or resource name
     * @return the path, or null if not found
     * @throws NullPointerException if {@code filename} is null or blank
     */
    public static Path locatePath(String filename) {
        File ret = locateFile(filename);
        return ret != null ? ret.toPath() : null;
    }


    /**
     * Locates a file on the file system and, failing that, as a resource of the given
     * class loader.
     * <p>
     * The resource fallback converts the resource URL with {@link URL#getFile()}, which
     * only yields a usable {@link File} for {@code file:} URLs (resources on disk, not
     * inside a jar). For a jar-packed resource the returned file will not exist.
     * @param cl class loader used for the resource fallback
     * @param filename path or resource name
     * @return the file, or null if no such resource exists
     */
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
     * Delete a directory recursively (children first, then the directory itself).
     * A non-directory path is simply deleted. Symbolic links are deleted, not followed.
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

    /**
     * Recursively lists the regular files under {@code rootDir} whose full path string
     * matches {@code filterPattern} and, when given, does not match {@code filterExclusion}.
     * Both patterns are Java regular expressions applied with {@link String#matches(String)}
     * (whole-string match) to {@code path.toString()}, so they must account for the
     * platform separator.
     * @param rootDir directory to walk
     * @param filterPattern inclusion regex, must match the whole path
     * @param filterExclusion exclusion regex, may be null or empty
     * @return matching files as {@code file:} URLs, in walk order
     * @throws IOException in case of IO error
     */
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
     * Reads an input stream to completion and decodes it with the given charset.
     * @param is to be read
     * @param charsetEncoding charset encoding
     * @param close if true the stream is closed on return
     * @return decoded content
     * @throws IOException in case of IO error
     * @throws UnsupportedEncodingException if the charset is not supported
     */
    public static String inputStreamToString(InputStream is, String charsetEncoding, boolean close)
            throws IOException {
        UByteArrayOutputStream baos = inputStreamToByteArray(is, close);
        return new String(baos.getInternalBuffer(), 0, baos.size(), charsetEncoding);
    }

    /**
     * Reads a classpath resource resolved relative to {@code clazz} as a UTF-8 string.
     * <p>
     * Unlike {@link #resourceToString(Class, String)}, every failure (missing resource,
     * IO error) is printed to stderr and swallowed, and {@code null} is returned. The
     * declared exceptions are never actually propagated.
     * @param clazz for resource access
     * @param resource resource name looking for
     * @return String object of the content, or null on any failure
     * @throws NullPointerException declared, never thrown
     * @throws IOException declared, never thrown
     */
    public static String inputStreamToString(Class<?> clazz, String resource) throws NullPointerException, IOException {

        String content = null;
        try {
            content = (inputStreamToString(clazz.getResourceAsStream(resource), true));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return content;
    }

    /**
     * Drains an input stream to end of stream, discarding the data, and returns how many
     * bytes it contained.
     * @param is to be drained
     * @param close if true the stream is closed on return
     * @return number of bytes read
     * @throws IOException in case of IO error
     */
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
     * Load path file as a string (UTF-8). The file is always closed.
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
     * Load path file as a string (UTF-8). The file is always closed.
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
     * Load path file as a string (UTF-8). The file is always closed.
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
     * Reads an input stream to completion and decodes it as UTF-8, using pooled
     * scratch buffers.
     * <p>
     * Reading stops at end of stream or at the first {@code read} that returns 0 bytes,
     * which a well-behaved blocking stream never does.
     * @param is to be completely read and converted to string
     * @param close if true will close the stream at the end
     * @return file content as string
     * @throws IOException in case of io errors
     */
    public static String inputStreamToString(InputStream is, boolean close)
            throws IOException {
        byte[] buffer = ByteBufferUtil.allocateByteArray(SharedIOUtil.K_8);
        UByteArrayOutputStream ubaos = ByteBufferUtil.allocateUBAOS(256);
        int read;

        try {
            while ((read = is.read(buffer, 0, buffer.length)) > 0) {
               ubaos.write(buffer, 0, read);
            }
            return ubaos.getString(0, ubaos.size());

        } finally {
            ByteBufferUtil.cache(ubaos);
            ByteBufferUtil.cache(buffer);
            if (close) {
                SharedIOUtil.close(is);
            }
        }
    }

    /**
     * Reads a character reader to completion.
     * <p>
     * Reading stops at end of stream or at the first {@code read} that returns 0 chars,
     * which a well-behaved blocking reader never does.
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
     * Read the all the content of an input stream and return it as a byte array.
     * <p>
     * The returned {@link UByteArrayOutputStream} is freshly allocated (not pooled) and
     * belongs to the caller; read the bytes via {@code getInternalBuffer()} / {@code size()}
     * or {@code toByteArray()}.
     *
     * @param is    to be read
     * @param close if true it will be closed after reading
     * @return byte array
     * @throws IOException in case of IO errors
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
     * Read the all the content of a file and return it as a byte array; see
     * {@link #inputStreamToByteArray(File, boolean)}.
     *
     * @param filename to be read
     * @param close    accepted for symmetry; the file stream is always closed
     * @return byte array
     * @throws IOException in case of IO errors
     */
    public static UByteArrayOutputStream inputStreamToByteArray(String filename, boolean close)
            throws IOException {
        return inputStreamToByteArray(new File(filename), close);
    }

    /**
     * Read the all the content of a file and return it as UByteArrayOutputStream.
     * The file stream is opened here and is <b>always closed</b>, whatever the value
     * of {@code close}.
     *
     * @param file  to be read
     * @param close accepted for symmetry; the file stream is always closed
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
     * Write a string to file as UTF-8, creating or <b>truncating</b> the file.
     *
     * @param file to be created and written to
     * @param toWrite string content
     * @throws IOException in case of IO errors
     */
    public static void writeToFile(File file, String toWrite)
            throws IOException {
        writeToFile(file, SUS.getBytes(toWrite));
    }

    /**
     * Write a byte[] to file, creating or <b>truncating</b> the file. The file is
     * always closed.
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
     * @throws FileNotFoundException if the file does not exist
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
     * Return the path creation in millis, read from {@link BasicFileAttributes#creationTime()}.
     * On file systems that do not record creation time the value is implementation
     * defined (typically the last-modified time or the epoch).
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
     * Creates a directory, including any missing parents, if it does not already exist.
     * @param dirName to be created
     * @return the directory, or null if it could not be created or the path exists but is
     *         not a directory
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
     * Copies {@code is} into {@code os} until end of stream; see
     * {@link #relayStreams(InputStream, OutputStream, boolean, boolean)}.
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

    /**
     * Copies {@code is} into {@code os} while hashing the data; see
     * {@link #relayStreams(CryptoConst.HashType, InputStream, OutputStream, boolean, boolean)}.
     * @param hashType digest algorithm
     * @param is to be read
     * @param os to be written to
     * @param both if true close both streams
     * @return the digest, algorithm and byte count
     * @throws IOException in case of IO errors, or if the algorithm is unavailable
     */
    public static HashResult relayStreams(CryptoConst.HashType hashType, InputStream is, OutputStream os, boolean both)
            throws IOException {
        return relayStreams(hashType, is, os, both, both);
    }

    /**
     * relays streams and update message digest if not null. Neither stream is closed.
     * @param md to be updated, may be null
     * @param is input stream
     * @param os output stream
     * @return total data copied between is and os
     * @throws IOException in case of IO errors
     */
    public static long relayStreams(MessageDigest md, InputStream is, OutputStream os)
            throws IOException {
        return relayStreams(md, is, os, false, false);
    }

    /**
     * Copies {@code is} into {@code os} until end of stream, feeding every chunk to
     * {@code md} when it is not null, then flushes {@code os}. Uses a pooled 8K buffer.
     * @param md digest to update, may be null
     * @param is input stream
     * @param os output stream
     * @param closeIS if true close {@code is} on return
     * @param closeOS if true close {@code os} on return
     * @return total bytes copied
     * @throws IOException in case of IO errors
     */
    public static long relayStreams(MessageDigest md, InputStream is, OutputStream os, boolean closeIS, boolean closeOS)
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
            if (closeIS) {
                SharedIOUtil.close(is);
            }
            if (closeOS) {
                SharedIOUtil.close(os);
            }
            ByteBufferUtil.cache(buffer);
        }
        return totalCopied;
    }

    /**
     * hash and input  the call must invoke md.digest()
     * <p>
     * Drains the stream to end, feeding every chunk to the digest; the data itself is
     * discarded. The digest is <b>not</b> finalized here.
     * @param messageDigest to be updated
     * @param inputStream input stream
     * @param closeIS if true the input stream will be closed when is.read() == -1
     * @return total data copied between is and os
     * @throws IOException in case of IO errors
     * @throws NullPointerException if messageDigest or inputStream are null
     */
    public static long hashInputStream(MessageDigest messageDigest, InputStream inputStream, boolean closeIS)
            throws IOException {
        SUS.checkIfNulls("message digest and input stream must not be null", messageDigest, inputStream);
        long totalCopied = 0;
        int read;
        byte[] buffer = ByteBufferUtil.allocateByteArray(SharedIOUtil.K_8);
        try {
            while ((read = inputStream.read(buffer)) != -1) {
                totalCopied += read;
                if (messageDigest != null)
                    messageDigest.update(buffer, 0, read);
            }
        } finally {
            ByteBufferUtil.cache(buffer);
            if (closeIS) {
                SharedIOUtil.close(inputStream);
            }
        }
        return totalCopied;
    }


    /**
     * Copies {@code is} into {@code os} until end of stream while computing a digest of
     * the data with the given algorithm.
     * <p>
     * A stream is closed when its flag is true <b>or</b> when it is a
     * {@link CloseEnabledInputStream} / {@link CloseEnabledOutputStream} (the wrapper then
     * decides whether the underlying stream really closes).
     * @param hashType digest algorithm
     * @param is to be read
     * @param os to be written to
     * @param closeIS if true close {@code is} on return
     * @param closeOS if true close {@code os} on return
     * @return the digest, algorithm and byte count
     * @throws IOException in case of IO errors, or wrapping {@link NoSuchAlgorithmException}
     *         if the JVM has no provider for {@code hashType}
     */
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
     * Copies {@code is} into {@code os} until end of stream and flushes {@code os}.
     * <p>
     * A stream is closed when its flag is true <b>or</b> when it is a
     * {@link CloseEnabledInputStream} / {@link CloseEnabledOutputStream} (the wrapper then
     * decides whether the underlying stream really closes).
     * @param is to be read
     * @param os to be written to
     * @param closeIS if true close {@code is} on return
     * @param closeOS if true close {@code os} on return
     * @return total bytes copied
     * @throws IOException in case of IO errors
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

    /**
     * Splits the buffered bytes of {@code uboas} on {@code delim} and returns every
     * complete, delimiter-terminated token as its own byte array.
     * <p>
     * The consumed bytes (all tokens and their delimiters) are removed from the front of
     * the buffer; any trailing partial token after the last delimiter is left in place for
     * the next call. This makes the method usable as an incremental frame splitter over a
     * stream. The buffer is locked for the duration of the call.
     * @param uboas buffer to split and consume from
     * @param delim delimiter byte sequence
     * @param noEmpty if true zero-length tokens (consecutive delimiters) are dropped
     * @return the extracted tokens in order, without their delimiters
     * @throws NullPointerException if {@code uboas} or {@code delim} is null
     * @throws IllegalArgumentException if {@code delim} is empty
     */
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


    /**
     * Maps the shared {@link ProxyType} enum to {@link Proxy.Type}.
     * @param pt shared proxy type
     * @return the JVM proxy type, or null for a value without a JVM equivalent
     * @throws NullPointerException if {@code pt} is null
     */
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


    /**
     * Builds a JVM {@link Proxy} from the type, host and port of a shared {@link IPAddress}.
     * The host is resolved eagerly by {@link InetSocketAddress}.
     * @param proxyInfo proxy address and type
     * @return the proxy
     * @throws NullPointerException if {@code proxyInfo} or its proxy type is null
     */
    public static Proxy toProxy(IPAddress proxyInfo) {
        return new Proxy(toProxyType(proxyInfo.getProxyType()), new InetSocketAddress(proxyInfo.getInetAddress(), proxyInfo.getPort()));
    }

    /**
     * Whether {@code filePathStr} lies inside {@code directoryPathStr}, compared on real
     * paths (symbolic links resolved). Both paths must exist; if either does not, or
     * cannot be resolved, the result is false. A path equal to the directory itself
     * counts as inside.
     * @param directoryPathStr containing directory
     * @param filePathStr path to test
     * @return true if inside
     */
    public static boolean isFileInDirectory(String directoryPathStr, String filePathStr) {
        try {
            Path filePath = Paths.get(filePathStr).toRealPath();
            Path directoryPath = Paths.get(directoryPathStr).toRealPath();

            return filePath.startsWith(directoryPath);
        } catch (IOException e) {
        }
        return false;
    }

    /**
     * Whether {@code file} lies strictly inside {@code directory}, compared on canonical
     * paths. Neither path needs to exist. The directory itself does <b>not</b> count as
     * inside (a trailing separator is required after the directory prefix). Returns false
     * if either canonical path cannot be computed.
     * @param directory containing directory
     * @param file file to test
     * @return true if strictly inside
     */
    public static boolean isFileInDirectory(File directory, File file) {
        try {
            String fileCanonicalPath = file.getCanonicalPath();
            String directoryCanonicalPath = directory.getCanonicalPath();

            return fileCanonicalPath.startsWith(directoryCanonicalPath + File.separator);
        } catch (IOException e) {
        }

        return false;
    }

    /**
     * Renames the file on disk to {@code <absolute path>.<ext>} and returns the renamed file.
     * @param file to rename
     * @param ext extension to append, without the dot
     * @return a {@link File} for the new name, or null if {@link File#renameTo(File)} failed
     *         (the original file is then untouched)
     */
    public static File addFileExtension(File file, String ext) {
        File renamed = new File(file.getAbsolutePath() + "." + ext);
        return file.renameTo(renamed) ? renamed : null;
    }

    /**
     * Renames the file on disk by stripping a trailing {@code .<ext>} from its absolute
     * path and returns the renamed file. A file whose name does not end with that
     * extension is left untouched and returned as is.
     * @param file to rename
     * @param ext extension to strip, without the dot
     * @return a {@link File} for the new name, the same {@code file} if it had no such
     *         extension, or null if {@link File#renameTo(File)} failed
     */
    public static File removeFileExtension(File file, String ext) {
        String absPath = file.getAbsolutePath();
        if (absPath.endsWith("." + ext)) {
            File renamed = new File(absPath.substring(0, absPath.length() - (ext.length() + 1)));
            return file.renameTo(renamed) ? renamed : null;
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

    /**
     * Returns {@code pFile} if {@link #isRegularFile(Path)} holds, otherwise null.
     * @param pFile to check
     * @return the same path or null
     */
    public static Path regularFileOrNull(Path pFile) {
        return isRegularFile(pFile) ? pFile : null;
    }

    /**
     * Find the first match in the parentPath the match is case-insensitive.
     * <p>
     * Walks {@code parentPath} recursively and returns the first readable regular file
     * whose trailing path elements equal those of {@code toFind}, compared element by
     * element ignoring case (see {@link #endsWithIgnoreCase(Path, Path)}), so
     * {@code toFind} may be a bare file name or a relative suffix such as
     * {@code conf/app.json}. When a cache is supplied it is consulted first and the
     * match is stored under {@code toFind} on success.
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

    /**
     * Whether the last {@code n} name elements of {@code fullPath} equal the {@code n}
     * name elements of {@code suffix}, compared element by element ignoring case.
     * A suffix with more elements than the full path never matches.
     * @param fullPath path to test
     * @param suffix trailing elements to look for
     * @return true if {@code fullPath} ends with {@code suffix}
     */
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
