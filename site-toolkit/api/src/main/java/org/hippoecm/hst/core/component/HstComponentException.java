package org.hippoecm.hst.core.component;

public class HstComponentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HstComponentException() {
        super();
    }

    public HstComponentException(String message) {
        super(message);
    }

    public HstComponentException(Throwable nested) {
        super(nested);
    }

    public HstComponentException(String msg, Throwable nested) {
        super(msg, nested);
    }
    
}
