package org.hippoecm.frontend.plugins.yui.javascript;

public class StringArrayValue extends Value<String[]> {
    private static final long serialVersionUID = 1L;

    private boolean escaped;
    
    public StringArrayValue(String[] value, boolean escaped) {
        super(value);
        this.escaped = escaped;
    }

    public String getScriptValue() {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        if (value != null) {
            for (int i = 0; i < value.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                if (escaped) {
                    buf.append(StringValue.escapeString(value[i]));
                } else {
                    buf.append(value[i]);
                }
            }
        }
        buf.append(']');
        return buf.toString();
    }
}
