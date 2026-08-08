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
package org.zoxweb.shared.api;

import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.NVGenericMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * The API document store interface which extends API service provider interface.
 *
 * @param <P> Protocol specific connection
 * @param <S> System specific connection
 */
public interface APIDocumentStore<P, S>
        extends APIServiceProvider<P, S> {

    /**
     * This method creates a file.
     *
     * @param folderID
     * @param file
     * @param is
     * @param closeStream
     * @return APIFileInfoMap
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws NullPointerException
     * @throws AccessException
     * @throws APIException
     */
    APIFileInfoMap createFile(String folderID, APIFileInfoMap file, InputStream is, boolean closeStream)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;

    /**
     *
     * Create a folder
     *
     * @param folderFullPath
     * @return APIFileInfoMap
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws AccessException
     * @throws APIException
     */
    APIFileInfoMap createFolder(String folderFullPath)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;

    /**
     * This method reads a file.
     *
     * @param map
     * @param os
     * @param closeStream
     * @return APIFileInfoMap
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws NullPointerException
     * @throws AccessException
     * @throws APIException
     */
    APIFileInfoMap readFile(APIFileInfoMap map, OutputStream os, boolean closeStream)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;

    /**
     * This method updates a file.
     *
     * @param map
     * @param is
     * @param closeStream
     * @return APIFileInfoMap
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws NullPointerException
     * @throws AccessException
     * @throws APIException
     */
    APIFileInfoMap updateFile(APIFileInfoMap map, InputStream is, boolean closeStream)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;

    /**
     * This method deletes a file.
     *
     * @param map
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws NullPointerException
     * @throws AccessException
     * @throws APIException
     */
    void deleteFile(APIFileInfoMap map)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;

    /**
     * This method returns a list of files.
     *
     * @return the map
     * @throws IOException
     * @throws AccessException
     * @throws APIException
     */
    Map<String, APIFileInfoMap> discover()
            throws IOException, AccessException, APIException;


    /**
     * This method is used to search for list of files given arguments.
     *
     * @param args search arguments
     * @return List of APIFileInfoMap
     * @throws NullPointerException if args is null
     * @throws IllegalArgumentException if args are invalid
     * @throws IOException if an I/O error occurs
     * @throws AccessException if access is denied
     * @throws APIException if an API error occurs
     */
    List<APIFileInfoMap> search(String... args)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;

    /**
     * Lists the stored versions of a file, newest first. Optional capability — implementations
     * with version control override this; others throw {@link UnsupportedOperationException}.
     *
     * @param map the file whose versions to list
     * @return one NVGenericMap per stored version (version number, length, creation time,
     *         and whether it is the current version)
     * @throws NullPointerException if map is null
     * @throws IllegalArgumentException if the file is unknown
     * @throws IOException if an I/O error occurs
     * @throws AccessException if access is denied
     * @throws APIException if an API error occurs
     */
    default List<NVGenericMap> fileVersions(APIFileInfoMap map)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException {
        throw new UnsupportedOperationException("file versioning not supported");
    }

    /**
     * Reads a specific stored version of a file. Optional capability — implementations
     * with version control override this; others throw {@link UnsupportedOperationException}.
     *
     * @param map the file to read
     * @param version the version to read
     * @param os the destination stream
     * @param closeStream if true os is closed when done
     * @return APIFileInfoMap
     * @throws NullPointerException if map or os is null
     * @throws IllegalArgumentException if the file or version is unknown
     * @throws IOException if an I/O error occurs
     * @throws AccessException if access is denied
     * @throws APIException if an API error occurs
     */
    default APIFileInfoMap readFile(APIFileInfoMap map, long version, OutputStream os, boolean closeStream)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException {
        throw new UnsupportedOperationException("file versioning not supported");
    }

    /**
     * Rolls a file back so the given stored version becomes its current content. Optional
     * capability — implementations with version control override this; others throw
     * {@link UnsupportedOperationException}.
     *
     * @param map the file to roll back
     * @param version the version to restore as current
     * @return APIFileInfoMap
     * @throws NullPointerException if map is null
     * @throws IllegalArgumentException if the file or version is unknown
     * @throws IOException if an I/O error occurs
     * @throws AccessException if access is denied
     * @throws APIException if an API error occurs
     */
    default APIFileInfoMap rollbackFile(APIFileInfoMap map, long version)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException {
        throw new UnsupportedOperationException("file versioning not supported");
    }

}