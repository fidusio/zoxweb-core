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

import java.util.ArrayList;
import java.util.List;

/**
 * This class extends RuntimeException and is used to handle store and manage
 * multiple exceptions.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class ExceptionCollection
        extends RuntimeException {

    private List<Exception> list = new ArrayList<Exception>();
    private String message;

    /**
     * The default constructor.
     */
    public ExceptionCollection() {

    }

    /**
     * This constructor instantiates ExceptionCollection with
     * message set based on reason input.
     * @param reason
     */
    public ExceptionCollection(String reason) {
        super();
        setMessage(reason);
    }

    /**
     * Returns the exception list.
     * @return list of exceptions
     */
    public List<Exception> getExceptionList() {
        return list;
    }

    /**
     * Sets the exception list.
     * @param list
     */
    public void setExceptionList(List<Exception> list) {
        this.list = list;
    }

    /**
     * Returns the message;
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Adds an existing exception collection to the current exception collection.
     * @param ec
     */
    public void add(ExceptionCollection ec) {
        list.addAll(ec.getExceptionList());
    }

}