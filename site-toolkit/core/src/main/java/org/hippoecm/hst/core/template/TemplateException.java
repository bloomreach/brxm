package org.hippoecm.hst.core.template;

public class TemplateException extends Exception {
     public TemplateException() {
    	 super();
     }
     
     public TemplateException(String s) {
    	 super(s);
     }
     
     public TemplateException(Exception e) {
    	 super(e);
     }
}
