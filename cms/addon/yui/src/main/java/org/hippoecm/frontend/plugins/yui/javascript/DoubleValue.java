package org.hippoecm.frontend.plugins.yui.javascript;

public class DoubleValue extends Value<Double> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    public DoubleValue(Double value) {
        super(value);
    }

    public String getScriptValue() {
        return Double.toString(value);
    }

}
