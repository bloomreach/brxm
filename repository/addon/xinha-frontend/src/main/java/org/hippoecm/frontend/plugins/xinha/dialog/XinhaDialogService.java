package org.hippoecm.frontend.plugins.xinha.dialog;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;

public abstract class XinhaDialogService<K extends Enum<K>> implements IClusterable {
    private static final long serialVersionUID = 1L; 
    
    protected EnumMap<K, String> values;
    
    public XinhaDialogService() {
        values = createEnumMap();
    }
    
    protected abstract EnumMap<K, String> createEnumMap();

    public abstract void update(Map<String, String> parameters);
    
    public String getJavascriptValue() {
        StringBuilder sb = new StringBuilder(values.size() * 20);
        sb.append('{');

        Iterator<K> it = values.keySet().iterator();
        while (it.hasNext()) {
            K key = it.next();
            sb.append(getXinhaParameterName(key)).append(": ").append(JavascriptUtil.serialize2JS(values.get(key)));
            if (it.hasNext())
                sb.append(',');
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Return the Xinha parameter name encoded in enumeration K
     * @param k
     * @return
     */
    abstract protected String getXinhaParameterName(K k);
    
    protected EnumModel<K> newEnumModel(Enum<K> e) {
        return new EnumModel<K>(values, e);
    }
}

class EnumModel<K extends Enum<K>> implements IModel {
    private static final long serialVersionUID = 1L;

    private EnumMap<K, String> values;
    private Enum<K> e;

    public EnumModel(EnumMap<K, String> values, Enum<K> e) {
        this.e = e;
        this.values = values;
    }

    public Object getObject() {
        return values.get(e);
    }

    @SuppressWarnings("unchecked")
    public void setObject(Object object) {
        if (object == null)
            return;
        values.put((K) e, (String) object);
    }

    public void detach() {
    }
}

