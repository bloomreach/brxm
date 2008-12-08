package org.hippoecm.frontend.plugins.yui.javascript;

public abstract class SettingsArraySetting<K extends Settings> extends Setting<K[]> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    public SettingsArraySetting(String javascriptKey, K[] defaultValue) {
        super(javascriptKey, defaultValue);
    }

    public Value<K[]> newValue() {
        return new SettingsArrayValue<K>(defaultValue);
    }

}
