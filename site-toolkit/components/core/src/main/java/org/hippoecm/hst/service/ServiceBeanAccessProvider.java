package org.hippoecm.hst.service;

import java.util.Calendar;
import java.util.Map;

import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.proxy.BeanAccessProvider;

public class ServiceBeanAccessProvider implements BeanAccessProvider {
    
    public static final String HST_SERVICE_NAMESPACE_SEPARATOR = ":";
    
    private static Class STRING_ARRAY_CLASS = new String[0].getClass();
    private static Class PRIMITIVE_BOOLEAN_ARRAY_CLASS = new boolean[0].getClass();
    private static Class BOOLEAN_ARRAY_CLASS = new Boolean[0].getClass();
    private static Class PRIMITIVE_DOUBLE_ARRAY_CLASS = new double[0].getClass();
    private static Class DOUBLE_ARRAY_CLASS = new Double[0].getClass();
    private static Class PRIMITIVE_LONG_ARRAY_CLASS = new long[0].getClass();
    private static Class LONG_ARRAY_CLASS = new Long[0].getClass();
    private static Class CALENDAR_ARRAY_CLASS = new Calendar[0].getClass();
    
    protected String namespace;
    protected Service service;
    protected ValueProvider valueProvider;
    
    public ServiceBeanAccessProvider(Service service, String namespace) throws IllegalAccessException, NoSuchFieldException {
        this.service = service;
        this.valueProvider = service.getValueProvider();
        this.namespace = namespace;
    }

    public Object getProperty(String name, Class returnType) {
        String nodePropName = this.namespace + HST_SERVICE_NAMESPACE_SEPARATOR + name;
        
        if (returnType == null) {
            return this.valueProvider.getProperties().get(nodePropName);
        }
        
        if (!returnType.isArray()) {
            if (returnType == String.class) {
                return this.valueProvider.getString(nodePropName);
            } else if (returnType == boolean.class || returnType == Boolean.class) {
                return this.valueProvider.getBoolean(nodePropName);
            } else if (returnType == long.class || returnType == Long.class) {
                return this.valueProvider.getLong(nodePropName);
            } else if (returnType == double.class || returnType == Double.class) {
                return this.valueProvider.getDouble(nodePropName);
            } else if (returnType == Calendar.class) {
                return this.valueProvider.getDate(nodePropName);
            }
        } else {
            if (returnType == STRING_ARRAY_CLASS) {
                return this.valueProvider.getStrings(nodePropName);
            } else if (returnType == PRIMITIVE_BOOLEAN_ARRAY_CLASS || returnType == BOOLEAN_ARRAY_CLASS) {
                return this.valueProvider.getBooleans(nodePropName);
            } else if (returnType == PRIMITIVE_LONG_ARRAY_CLASS || returnType == LONG_ARRAY_CLASS) {
                return this.valueProvider.getLongs(nodePropName);
            } else if (returnType == PRIMITIVE_DOUBLE_ARRAY_CLASS || returnType == DOUBLE_ARRAY_CLASS) {
                return this.valueProvider.getDoubles(nodePropName);
            } else if (returnType == CALENDAR_ARRAY_CLASS) {
                return this.valueProvider.getDates(nodePropName);
            }
        }
        
        return this.valueProvider.getProperties().get(nodePropName);
    }

    public Object setProperty(String name, Object value, Class returnType) {
        this.valueProvider.getProperties().put(this.namespace + HST_SERVICE_NAMESPACE_SEPARATOR + name, value);
        return null;
    }

    public Object invoke(String name, Object[] args, Class returnType) {
        throw new UnsupportedOperationException("Service value provider does not support invocation on operations.");
    }

}
