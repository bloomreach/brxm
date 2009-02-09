package org.hippoecm.hst.core.container;

public class ContainerException extends Exception {

    private static final long serialVersionUID = 1L;

    public ContainerException() {
        super();
    }

    public ContainerException(String message) {
        super(message);
    }

    public ContainerException(Throwable nested) {
        super(nested);
    }

    public ContainerException(String msg, Throwable nested) {
        super(msg, nested);
    }
    
}
