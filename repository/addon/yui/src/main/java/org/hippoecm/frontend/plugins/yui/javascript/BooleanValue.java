package org.hippoecm.frontend.plugins.yui.javascript;

public class BooleanValue extends Value<Boolean> {
    private static final long serialVersionUID = 1L;
    
    public BooleanValue(Boolean defaultValue) {
        super(defaultValue);
    }

    public String getScriptValue() {
        return get().toString();
    }

}
