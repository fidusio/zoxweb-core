/*
 * Copyright (c) 2012-May 27, 2014 ZoxWeb.com LLC.
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

import org.zoxweb.shared.util.ExceptionReason;
import org.zoxweb.shared.util.GetValue;

/**
 * The API exception class which extends RuntimeException class.
 *
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class APIException
        extends RuntimeException
        implements ExceptionReason {


    private Reason reason = Reason.INCOMPLETE;

    /**
     * This enum includes error categories.
     *
     * @author mzebib
     *
     */
    public enum Category {
        CONNECTION,
        INITIALIZATON,
        OPERATION,
        GENERIC
    }

    /**
     * This enum includes error codes.
     *
     * @author mzebib
     *
     */
    public enum Code
            implements GetValue<String> {
        GENERIC("Unknown error."),
        MISSING_PARAMETERS("Invalid entries."),
        CREATE_FAILED("Create failed."),
        READ_FAILED("Read failed."),
        UPDATE_FAILED("Updated failed."),
        DELETE_FAILED("Deleted failed."),
        SUBMISSION_FAILED("Submission failed."),
        VIRUS_DETECTED("Vrius has been detected."),
        INVALID_CREDENTIALS("Invalid credentials"),
        RETRY("Retry later."),
        DUPLICATE_ENTRY_NOT_ALLOWED("Entry already exists."),
        CONNECTION_FAILED("Connection to remote service provider failed."),
        PROVIDER_EXCEPTION("Provider exception."),
        EXPIRED("Message delivery expired."),
        MISSING_CONFIGURATION("Missing configuration.");

        private final String value;

        Code(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }


    }

    /**
     * Set default category to generic.
     */
    private Category category = Category.GENERIC;
    private int statusCode = -1;

    /**
     * Set default code to generic.
     */
    private Code code = Code.GENERIC;

    /**
     * This method returns the error category.
     *
     * @return the category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * This method returns the error code.
     *
     * @return the Code
     */
    public Code getCode() {
        return code;
    }

    /**
     * This is the default constructor.
     */
    public APIException() {
        super();
    }

    /**
     * This constructor instantiates APIStoreException based given reason.
     *
     * @param reason
     */
    public APIException(String reason) {
        super(reason);
    }


    public APIException(String message, int statusCode) {
        super(message);
        setStatusCode(statusCode);
    }

    public APIException(String message, Reason reason) {
        super(message);
        setReason(reason);
    }

    /**
     * This constructor instantiates APIStoreException based on given reason,
     * set category and set code.
     *
     * @param message
     * @param category defined
     * @param code     defined
     */
    public APIException(String message, Category category, Code code) {
        this(message, category, code, -1);
    }

    /**
     * This constructor instantiates APIStoreException based on given message,
     * set category and set code.
     *
     * @param message
     * @param category
     * @param code
     * @param statusCode domain specific
     */
    public APIException(String message, Category category, Code code, int statusCode) {
        super(message);
        this.category = category;
        this.code = code;
        setStatusCode(statusCode);
    }


    public Reason getReason() {
        // TODO Auto-generated method stub
        return reason;
    }


    public void setReason(Reason reason) {
        // TODO Auto-generated method stub
        this.reason = reason;
    }

    @Override
    public int getStatusCode() {
        // TODO Auto-generated method stub
        return statusCode;
    }

    @Override
    public void setStatusCode(int code) {
        // TODO Auto-generated method stub
        statusCode = code;
    }


}
