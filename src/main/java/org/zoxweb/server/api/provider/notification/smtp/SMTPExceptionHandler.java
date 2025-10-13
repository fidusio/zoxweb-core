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
package org.zoxweb.server.api.provider.notification.smtp;

import org.zoxweb.shared.util.GetNameValue;

import org.zoxweb.shared.api.APIException;
import org.zoxweb.shared.api.APIExceptionHandler;
import org.zoxweb.shared.api.APIException.Category;
import org.zoxweb.shared.api.APIException.Code;

/**
 *
 */
public class SMTPExceptionHandler
        implements APIExceptionHandler {

    public enum SMTPError
            implements GetNameValue<Integer> {
        CONNECTION_FAILED("connection_failed", 101, Category.CONNECTION, Code.CONNECTION_FAILED);

        private final String name;
        private final Integer value;
        private final Category category;
        private final Code code;

        SMTPError(String name, Integer value, Category category, Code code) {
            this.name = name;
            this.value = value;
            this.category = category;
            this.code = code;
        }


        public String getName() {
            return name;
        }


        public Integer getValue() {
            return value;
        }

        public Category getCategory() {
            return category;
        }

        public Code getCode() {
            return code;
        }
    }


    /**
     * This variable declares that only one instance of this class can be created.
     */
    public static final SMTPExceptionHandler SINGLETON = new SMTPExceptionHandler();

    /**
     * The default constructor is declared private to prevent
     * outside instantiation of this class.
     */
    private SMTPExceptionHandler() {

    }

    /**
     *
     * @param e
     * @throws APIException
     */
    public void throwException(Exception e)
            throws APIException {
        APIException apiException = mapException(e);

        if (apiException != null) {
            throw apiException;
        }
    }

    /**
     *
     * @param e
     * @return
     */
    public APIException mapException(Exception e) {
        return null;
    }

}
