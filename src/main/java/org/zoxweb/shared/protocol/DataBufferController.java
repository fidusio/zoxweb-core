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
package org.zoxweb.shared.protocol;

/**
 * The internal buffer access interface.
 */
public interface DataBufferController
{

	/**
	 * Returns the internal buffer.
	 * @return
	 */
	byte[] getInternalBuffer();

	/**
	 * Returns the internal buffer size.
	 * @return
	 */
	int size();


	/**
	 * @param copy if true will return a copy of the data if false direct access to the internal buff in this mode use it with extreme care
	 * @return BytesArray object of actual data
	 */
	BytesArray toBytesArray(boolean copy);


	/**
	 *
	 * @param copy if true will return a copy of the data if false direct access to the internal buff in this mode use it with extreme care
	 * @param offset offset
	 * @param length length of data
	 * @return BytesArray object of actual data, if copy is true the BytesArray.offset = 0 and BytesArray.length = length
	 */
	BytesArray toBytesArray(boolean copy, int offset, int length);


	/**
	 * @param index of the char we need to look at
	 * @return the char at index.
	 */
	char charAt(int index);

	/**
	 * @param index of byte looking for
	 * @return the byte at index.
	 */
	byte byteAt(int index);

	/**
	 * Return the first index of matching bytes in contained within the stream
	 * @param match
	 * @return index of the match, -1 no match found
	 */
	int indexOf(byte[] match);

	/**
	 *
	 * @param startAt
	 * @param match
	 * @param offset
	 * @param length
	 * @return index
	 */
	int indexOf(int startAt, byte[] match, int offset, int length);

	int indexOf(int startAt, byte[] match);

	int indexOf(int startAt, String str);



	int indexOf(String str);

	int indexOfIgnoreCase(String str);

	/**
	 * @param index
	 * @return the integer at index.
	 */
	int intAt(int index);


	byte[] copyBytes(int from);
	byte[] copyBytes(int from, int to);

	String getString(int startIndex);


	String getString(int startIndex, int length);
}
