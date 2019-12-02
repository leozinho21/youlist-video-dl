package com.thesis.home.youlist.exceptions;

/**
 * Created by HOME on 11/5/2016.
 */
public class ExtractionException extends Exception {
    public ExtractionException(String message) {
        super(message);
    }

    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
