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
package org.zoxweb.shared.util;

/**
 * This class contains methods that set and retrieve the values for
 * token, sourcePostIndex, and sourcePreIndex.
 */
public class StringToken {

    private String token;
    private int sourcePostIndex;
    private int sourcePreIndex;

    /**
     * This constructor can be accessed outside the class.
     */
    public StringToken() {
    }

    /**
     * This constructor builds StringToken object based
     * on token, sourcePreIndex, and sourcePostIndex values.
     * @param token
     * @param sourcePreIndex
     * @param sourcePostIndex
     */
    public StringToken(String token, int sourcePreIndex, int sourcePostIndex) {
        setToken(token);
        setSourcePreIndex(sourcePreIndex);
        setSourcePostIndex(sourcePostIndex);
    }

    /**
     * Returns the token.
     * @return token
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the token.
     * @param token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Returns the source post index.
     * @return index
     */
    public int getSourcePostIndex() {
        return sourcePostIndex;
    }

    /**
     * Sets the source post index.
     * @param sourcePostIndex
     */
    public void setSourcePostIndex(int sourcePostIndex) {
        this.sourcePostIndex = sourcePostIndex;
    }

    /**
     * Returns the source pre index.
     * @return index
     */
    public int getSourcePreIndex() {
        return sourcePreIndex;
    }

    /**
     * Sets the source pre index.
     * @param sourcePreIndex
     */
    public void setSourcePreIndex(int sourcePreIndex) {
        this.sourcePreIndex = sourcePreIndex;
    }

}