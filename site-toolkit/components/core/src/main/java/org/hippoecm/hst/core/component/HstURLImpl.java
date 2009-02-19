package org.hippoecm.hst.core.component;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class HstURLImpl implements HstURL {
    
    protected String type = TYPE_RENDER;
    protected Map<String, String[]> parameterMap = new HashMap<String, String[]>();
    protected String resourceID;
    protected HstURLProvider urlProvider;
    
    public HstURLImpl(HstURLProvider urlProvider) {
        this.urlProvider = urlProvider;
    }

    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }

    public String getType() {
        return this.type;
    }

    public void setParameter(String name, String value) {
        this.parameterMap.put(name, new String [] { value });
    }

    public void setParameter(String name, String[] values) {
        this.parameterMap.put(name, values);
    }

    public void setParameters(Map<String, String[]> parameters) {
        this.parameterMap.putAll(parameters);
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void write(Writer out) throws IOException {
        out.write(toString());
    }

    public void write(Writer out, boolean escapeXML) throws IOException {
        write(out);
    }
    
    public String toString() {
        this.urlProvider.setType(this.type);
        this.urlProvider.setParameters(this.parameterMap);
        return this.urlProvider.toString();
    }

}
