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

import org.zoxweb.shared.data.FileInfo;

import java.io.InputStream;

/**
 * Value holder that pairs a {@link FileInfo} (the file's metadata) with the
 * InputStream supplying the file's content, so both can be handed around as a
 * single source object.
 */
public class FileInfoStreamSource {

    private FileInfo fileInfo;
    private InputStream srcInputStream;

    /**
     *
     * @param fileInfo
     * @param is
     */
    public FileInfoStreamSource(FileInfo fileInfo, InputStream is) {
        this.fileInfo = fileInfo;
        srcInputStream = is;
    }

    /**
     * Returns the input stream.
     * @return input stream
     */
    public InputStream getSourceInputStream() {
        return srcInputStream;
    }

    /**
     * Returns FileInfo object.
     * @return FileInfo
     */
    public FileInfo getFileInfo() {
        return fileInfo;
    }


}