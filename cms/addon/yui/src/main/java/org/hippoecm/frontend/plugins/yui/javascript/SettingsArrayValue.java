package org.hippoecm.frontend.plugins.yui.javascript;

public class SettingsArrayValue<K extends Settings> extends Value<K[]> {
    private static final long serialVersionUID = 1L;

    public SettingsArrayValue(K[] value) {
        super(value);
    }

    public String getScriptValue() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for(K k : value) {
            if(!k.isValid()) {
                continue;
            }
            if(first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(k.toScript());
        }
        sb.append(']');
        return sb.toString();
    }

}
