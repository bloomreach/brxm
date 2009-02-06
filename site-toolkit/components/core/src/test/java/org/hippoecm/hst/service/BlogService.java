package org.hippoecm.hst.service;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.provider.ValueProvider;

public class BlogService implements Service, ValueProvider {
    
    private Map<String, Object> properties = new HashMap<String, Object>();

    public void setProperty(String name, Object value) {
        this.properties.put(name, value);
    }
    
    public Object getProperty(String name) {
        return this.properties.get(name);
    }

    public void closeValueProvider(boolean closeChildServices) {
    }

    public void dump(StringBuffer buf, String indent) {
    }

    public Service[] getChildServices() {
        return null;
    }

    public ValueProvider getValueProvider() {
        return this;
    }

    public Boolean getBoolean(String propertyName) {
        return (Boolean) getProperty(propertyName);
    }

    public Boolean[] getBooleans(String propertyName) {
        return (Boolean []) getProperty(propertyName);
    }

    public Calendar getDate(String propertyName) {
        return (Calendar) getProperty(propertyName);
    }

    public Calendar[] getDates(String propertyName) {
        return (Calendar []) getProperty(propertyName);
    }

    public Double getDouble(String propertyName) {
        return (Double) getProperty(propertyName);
    }

    public Double[] getDoubles(String propertyName) {
        return (Double []) getProperty(propertyName);
    }

    public Long getLong(String propertyName) {
        return (Long) getProperty(propertyName);
    }

    public Long[] getLongs(String propertyName) {
        return (Long []) getProperty(propertyName);
    }

    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public String getString(String propertyName) {
        return (String) this.properties.get(propertyName);
    }

    public String[] getStrings(String propertyName) {
        return (String []) this.properties.get(propertyName);
    }

    public boolean hasProperty(String propertyName) {
        return this.properties.containsKey(propertyName);
    }
    
}
