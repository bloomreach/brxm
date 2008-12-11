package org.hippoecm.hst.core.exception;

public class ContextBaseException  extends Exception {
  
    private static final long serialVersionUID = 1L;

    public ContextBaseException() {
         super();
     }
     
     public ContextBaseException(String s) {
         super(s);
     }
     
     public ContextBaseException(Exception e) {
         super(e);
     }
}
