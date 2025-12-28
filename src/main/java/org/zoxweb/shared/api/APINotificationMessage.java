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

import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.NVPair;

import java.util.List;

/**
 * The API notification message interface.
 *
 * @author mzebib
 *
 */
public interface APINotificationMessage
        extends APIMessage {
    /**
     * @return the sender ID.
     */
    public String getSenderID();

    /**
     * This method sets the sender ID.
     *
     * @param senderID
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public void setSenderID(String senderID)
            throws NullPointerException, IllegalArgumentException;


    /**
     * @return the recipient IDs.
     */
    public String[] getRecipientIDs();

    /**
     * This method sets the recipient IDs.
     *
     * @param recipients
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public void setRecipientIDs(String... recipients)
            throws NullPointerException, IllegalArgumentException;


    /**
     * @return the title/subject of the message.
     */
    public String getTitle();

    /**
     * This method sets the subject of the message.
     *
     * @param title
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public void setTitle(String title)
            throws NullPointerException, IllegalArgumentException;


    /**
     * @return the message body.
     */
    public String getBodyTemplate();

    /**
     * This method sets the message body.
     *
     * @param messageBody
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public void setBodyTemplate(String messageBody)
            throws NullPointerException, IllegalArgumentException;


    /**
     * @return the message body tags.
     */
    public ArrayValues<NVPair> getBodyTags();

    /**
     * This method sets the message body tags.
     *
     * @param tags
     */
    public void setBodyTags(ArrayValues<NVPair> tags);

    /**
     * This method sets the message body tags.
     *
     * @param tags
     */
    public void setBodyTags(List<NVPair> tags);


    /**
     * @return the extra attributes.
     */
    public ArrayValues<NVPair> getExtraAttribues();

    /**
     * This method sets extra attributes.
     *
     * @param attributes
     */
    public void setExtraAttributes(ArrayValues<NVPair> attributes);

    /**
     * This method sets extra attributes.
     *
     * @param attributes
     */
    public void setExtraAttributes(List<NVPair> attributes);


    /**
     * @return the pre-token tag.
     */
    public String getPreTag();

    /**
     * This method sets the pre-token tag.
     *
     * @param preTokenTag
     */
    public void setPreTag(String preTokenTag);


    /**
     * @return the post-token tag.
     */
    public String getPostTag();

    /**
     * This method sets the post-token tag.
     *
     * @param postTokenTag
     */
    public void setPostTag(String postTokenTag);

}
