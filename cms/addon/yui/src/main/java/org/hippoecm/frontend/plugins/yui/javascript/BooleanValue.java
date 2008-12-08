package org.hippoecm.frontend.plugins.yui.javascript;

public class BooleanValue extends Value<Boolean> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    public BooleanValue(Boolean defaultValue) {
        super(defaultValue);
    }

    public String getScriptValue() {
        return get().toString();
    }

}
