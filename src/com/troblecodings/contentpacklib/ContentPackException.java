package com.troblecodings.contentpacklib;

public class ContentPackException extends RuntimeException {

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
