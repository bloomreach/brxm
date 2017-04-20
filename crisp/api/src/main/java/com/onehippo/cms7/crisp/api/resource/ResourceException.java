package com.onehippo.cms7.crisp.api.resource;

public class ResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceException() {
        super();
    }

    public ResourceException(String message) {
        super(message);
    }

    public ResourceException(Throwable nested) {
        super(nested);
    }

    public ResourceException(String msg, Throwable nested) {
        super(msg, nested);
    }

}
