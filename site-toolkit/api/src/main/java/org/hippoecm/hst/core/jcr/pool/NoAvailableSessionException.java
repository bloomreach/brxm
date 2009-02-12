package org.hippoecm.hst.core.jcr.pool;

import java.util.NoSuchElementException;

public class NoAvailableSessionException extends NoSuchElementException {

    private static final long serialVersionUID = 1L;

    public NoAvailableSessionException() {
        super();
    }

    /**
     * Constructs a <code>NoAvailableSessionException</code>, saving a reference
     * to the error message string <tt>s</tt> for later retrieval by the
     * <tt>getMessage</tt> method.
     *
     * @param   s   the detail message.
     */
    public NoAvailableSessionException(String s) {
        super(s);
    }
    
}
