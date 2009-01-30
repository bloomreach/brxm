package org.hippoecm.hst.core.domain;

public class DomainMappingException extends Exception{

    private static final long serialVersionUID = 1L;

    public DomainMappingException(String msg) {
        super(msg);
    }

    public DomainMappingException(String msg, Throwable cause) {
        super(msg, cause);
    }
 
}