package org.hippoecm.frontend.plugins.yui.header;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.IClusterable;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.plugins.yui.util.HeaderContributorHelper.JsConfig;

public class JavascriptSettings implements IClusterable {
    private static final long serialVersionUID = 1L;

    private static final String SINGLE_QUOTE = "'";
    private static final String SINGLE_QUOTE_ESCAPED = "\\'";
    private MiniMap map;

    public JavascriptSettings() {
        this(10);
    }

    public JavascriptSettings(int initialSize) {
        map = new MiniMap(initialSize);
    }

    private void store(String key, Object value) {
        ensureCapacity();
        map.put(key, value);
    }

    private void ensureCapacity() {
        if (map.isFull()) {
            MiniMap newMap = new MiniMap(map.size() * 2);
            newMap.putAll(map);
            map = newMap;
        }
    }

    /**
     * Store boolean value
     */
    public void put(String key, boolean value) {
        store(key, Boolean.toString(value));
    }

    /**
     * Store int value
     */
    public void put(String key, int value) {
        store(key, Integer.toString(value));
    }

    /**
     * Store double value
     * @param key
     * @param value
     */
    public void put(String key, double value) {
        store(key, Double.toString(value));
    }

    /**
     * Convenience method, auto wraps and escapes String value
     * @param key
     * @param value
     */
    public void put(String key, String value) {
        put(key, value, true);
    }

    /**
     * 
     * @param key
     * @param value
     * @param escapeAndWrap
     */
    public void put(String key, String value, boolean escapeAndWrap) {
        //escape single quotes and wrap
        if (escapeAndWrap) {
            value = escapeAndWrap(value);
        }
        store(key, value);
    }

    public void put(String key, String[] values) {
        put(key, values, true);
    }

    public void put(String key, String[] values, boolean escapeAndWrap) {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                if (escapeAndWrap) {
                    buf.append(escapeAndWrap(values[i]));
                } else {
                    buf.append(values[i]);
                }
            }
        }
        buf.append(']');
        store(key, buf.toString());
    }

    public void put(String key, Map<String, String> map) {
        put(key, map, true);
    }

    public void put(String key, Map<String, String> schemaMetaFields, boolean escapeAndWrap) {
        String value = null;
        if (schemaMetaFields != null) {
            StringBuilder buf = new StringBuilder();
            boolean first = true;
            for (Entry<String, String> e : schemaMetaFields.entrySet()) {
                //TODO: A IPluginConfig map can be passed into this method, which will results in a jcr:primaryType key-value entry, which breaks 
                //the js-object and shouldn't be present. We could just try and ignore it by wrapping the js-object key's with quotes as well.
                if (e.getKey().startsWith("jcr:"))
                    continue;
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(e.getKey()).append(':');
                if (escapeAndWrap) {
                    buf.append(escapeAndWrap(e.getValue()));
                } else {
                    buf.append(e.getValue());
                }
            }
            if (buf.length() > 0) {
                buf.insert(0, '{').append('}');
                value = buf.toString();
            }
        }
        store(key, value);
    }

    public void put(String key, JsConfig values) {
        store(key, values);
    }

    private String escapeAndWrap(String value) {
        //TODO: backslash should be escaped as well
        if (value != null)
            value = SINGLE_QUOTE + value.replace(SINGLE_QUOTE, SINGLE_QUOTE_ESCAPED) + SINGLE_QUOTE;
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;

        for (Object o : map.entrySet()) {
            Entry e = (Entry) o;
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

}
