package org.hippoecm.frontend.plugins.yui.javascript;

public class IntValue extends Value<Integer> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    public IntValue(Integer value) {
        super(value);
    }

    public String getScriptValue() {
        return Integer.toString(value);
    }

}
