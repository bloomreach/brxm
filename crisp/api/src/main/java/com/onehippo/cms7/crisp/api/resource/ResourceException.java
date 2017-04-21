/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

/**
 * The <CODE>ResourceException</CODE> class defines a general exception that any CRISP <CODE>Resource</CODE>
 * operations can throw when it is unable to perform its operation successfully.
 */
public class ResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new <CODE>Resource</CODE> exception.
     */
    public ResourceException() {
        super();
    }

    /**
     * Constructs a new <CODE>Resource</CODE> exception with the given message.
     * @param message the exception message
     */
    public ResourceException(String message) {
        super(message);
    }

    /**
     * Constructs a new <CODE>Resource</CODE> exception with the nested exception.
     * @param nested the nested exception
     */
    public ResourceException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructs a new <CODE>Resource</CODE> exception with the given message and a nested exception.
     * @param message the exception message
     * @param nested the nested exception
     */
    public ResourceException(String message, Throwable nested) {
        super(message, nested);
    }
}
