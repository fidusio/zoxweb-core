package org.zoxweb.shared.util;

public interface ExceptionReason {

    enum Reason {
        NOT_FOUND,
        UNAUTHORIZED,
        ACCESS_DENIED,
        LOGIN_FAILED,
        INCOMPLETE
    }


    Reason getReason();

    void setReason(Reason reason);

    int getStatusCode();

    void setStatusCode(int code);

    String getMessage();

}
