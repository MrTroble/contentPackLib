package com.troblecodings.contentpacklib;

public class ContentPackException extends RuntimeException {

    private static final long serialVersionUID = -8606204926182331234L;

    public ContentPackException(final String message) {
        super(message);
    }

    public ContentPackException(final Throwable throwable) {
        super(throwable);
    }

    public ContentPackException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

}
