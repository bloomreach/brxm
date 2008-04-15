package org.hippoecm.frontend.core;

import java.util.Hashtable;

public class PluginConfig extends Hashtable<String, String> {
    private static final long serialVersionUID = 1L;

    private Hashtable<String, String> reverse;

    public PluginConfig() {
        reverse = new Hashtable<String, String>();
    }

    @Override
    public String put(String key, String value) {
        String old = super.put(key, value);
        if (old != null) {
            reverse.put(old, null);
        }
        if (value != null) {
            reverse.put(value, key);
        }
        return old;
    }

    public String resolve(String name) {
        return reverse.get(name);
    }
}
