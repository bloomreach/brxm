package org.hippoecm.frontend.plugins.yui.javascript;

public class IntValue extends Value<Integer> {
    private static final long serialVersionUID = 1L;
    
    public IntValue(Integer value) {
        super(value);
    }

    public String getScriptValue() {
        return Integer.toString(value);
    }

}
