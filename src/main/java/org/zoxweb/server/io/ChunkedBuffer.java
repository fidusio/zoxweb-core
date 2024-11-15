/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import org.zoxweb.shared.util.ReferenceID;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class ChunkedBuffer
	implements ReferenceID<String>
{

	private final int chunkSize;
	private final byte content[];
	private int currentChunk = 0;
	private final String referenceID;

	/**
	 * This constructor instantiates ChunkedBuffer with given content and chunk size.
	 * @param content
	 * @param chunkSize
	 */
	public ChunkedBuffer(byte content[], int chunkSize)
	{
		this(content, chunkSize, null);
	}

	/**
	 * This constructor instantiates ChunkedBuffer with given content, chuck size, and reference ID.
	 * @param content
	 * @param chunkSize
	 * @param referenceID
	 */
	public ChunkedBuffer(byte content[], int chunkSize, String referenceID)
	{
		SUS.checkIfNulls("Null content", content);

		if (chunkSize < 1)
		{
			throw new IllegalArgumentException("Chunk too small");
		}

		this.chunkSize = chunkSize;
		this.content = content;
		this.referenceID = referenceID == null ? UUID.randomUUID().toString() : referenceID;		
	}

	/**
	 * Writes to given output stream.
	 * @param os
	 * @return
	 * @throws IOException
	 */
	public synchronized int write(OutputStream os) throws IOException
	{
		int length = currentChunkSize();

		if (currentChunk() * chunkSize+length <= content.length)
		{
			os.write(content, currentChunk()*chunkSize, length);
			currentChunk++;
			return length;
		}

		return -1;
	}

	/**
	 * Returns the current chuck.
	 * @return
	 */
	public int currentChunk()
	{
		return currentChunk;
	}

	/**
	 * Returns the current chunk size.
	 * @return
	 */
	public synchronized int currentChunkSize()
	{
		int index = content.length - ((currentChunk+1) * chunkSize);
		
		if (index >= 0)
		{
			return chunkSize;
		}
		else
		{
			return content.length % chunkSize;
		}
	}

	/**
	 * Returns the content.
	 * @return
	 */
	public final byte[] getContent()
	{
		return content;
	}

	/**
	 * Returns the reference ID.
	 * @return
	 */
	@Override
	public String getReferenceID()
	{
		return referenceID;
	}

	@Override
	public void setReferenceID(String referenceID)
	{
		throw new UnsupportedOperationException("Reference ID (final) is initialized in constructor.");
	}

}