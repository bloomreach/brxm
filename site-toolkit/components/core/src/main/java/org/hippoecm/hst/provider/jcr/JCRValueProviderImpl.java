package org.hippoecm.hst.provider.jcr;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRValueProviderImpl implements JCRValueProvider{
    
    private static final Logger log = LoggerFactory.getLogger(JCRValueProviderImpl.class);
    private transient Node jcrNode;
    private String nodePath;
    private String nodeName;
    private boolean detached = false;
    
    public JCRValueProviderImpl(Node jcrNode) {
        this.jcrNode = jcrNode;
        if(jcrNode != null) {
            try {
                this.nodePath = jcrNode.getPath();
                this.nodeName = jcrNode.getName();
            } catch (RepositoryException e) {
                log.error("Repository Exception: {}", e.getMessage());
            }
        }
    }

    public Node getJcrNode(){
        if(isDetached()) {
            log.warn("Node '{}' is detached. Return null", nodePath);
            return null;
        }
        return this.jcrNode;
    }
    
    public void detach(){
        log.debug("Detaching node '{}'", this.nodePath);
        this.detached = true;
        this.jcrNode = null;
    }
    
    public boolean isDetached(){
        return this.detached;
    }
    
    public String getName() {
        return this.nodeName;
    }

    public String getPath() {
        return this.nodePath;
    }
    
    public boolean isNodeType(String nodeType) {
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
            return false;
        } 
        try {
            return jcrNode.isNodeType(nodeType);
        } catch (RepositoryException e) {
            log.warn("Repository Exception during nodetype check: ", e);
        }
        return false;
    }
    
    public boolean hasProperty(String propertyName){
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
            return false;
        }
        try {
            return jcrNode.hasProperty(propertyName);
        } catch (RepositoryException e) {
            log.warn("Repository Exception during check if property exists: ", e);
        }
        return false;
    }
    
    public String getString(String propertyName){
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.STRING, false);
        if(prop!= null) {
            return (String)prop.getObject();
        }
        return null;
    }
    
    public String[] getStrings(String propertyName){
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.STRING, true);
        if(prop!= null) {
            return (String[])prop.getObject();
        }
        return null;
    }

    public Double getDouble(String propertyName) {
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.DOUBLE, false);
        if(prop!= null) {
            return (Double)prop.getObject();
        }
        return null;
    }

    public Double[] getDoubles(String propertyName) {
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.DOUBLE, true);
        if(prop!= null) {
            return (Double[])prop.getObject();
        }
        return null;
    }

    public Long getLong(String propertyName) {
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.LONG, false);
        if(prop!= null) {
            return (Long)prop.getObject();
        }
        return null;
    }

    public Long[] getLongs(String propertyName) {
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.LONG, true);
        if(prop!= null) {
            return (Long[])prop.getObject();
        }
        return null;
    }

    public Calendar getDate(String propertyName) {
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.DATE, false);
        if(prop!= null) {
            return (Calendar)prop.getObject();
        }
        return null;
    }

    public Calendar[] getDates(String propertyName) {
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.DATE, true);
        if(prop!= null) {
            return (Calendar[])prop.getObject();
        }
        return null;
    }

    public Boolean getBoolean(String propertyName) {
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.BOOLEAN, false);
        if(prop!= null) {
            return (Boolean)prop.getObject();
        }
        return null;
    }

    public Boolean[] getBooleans(String propertyName) {
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.BOOLEAN, true);
        if(prop!= null) {
            return (Boolean[])prop.getObject();
        }
        return null;
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
            return properties;
        }
        try {
            for(PropertyIterator allProps = jcrNode.getProperties(); allProps.hasNext();) {
                Property p = allProps.nextProperty();
                properties.put(p.getName(), this.getWrappedProp(p).getObject());
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception: {}", e.getMessage());
        }
        return properties;
        
    }


    private WrappedProp getWrappedProp(String propertyName, int propertyType, boolean isMultiple){
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
            return null;
        }
        try {
            if(jcrNode.hasProperty(propertyName)) {
                Property prop = jcrNode.getProperty(propertyName);
                if(prop.getType() != propertyType) {
                    log.warn("Cannot return property '{}' for node '{}' because it is of the wrong type. Return null", propertyName, this.nodePath);
                    return null;
                }
                if( (prop.getDefinition().isMultiple() && isMultiple) ||  (!prop.getDefinition().isMultiple() && !isMultiple)) {
                    return getWrappedProp(jcrNode.getProperty(propertyName));
                }
                else {
                    log.warn("Cannot return property '{}' for node '{}'. Return null", propertyName, this.nodePath);
                    return null;
                }
            }
            else {
                log.debug("Property '{}' not found at '{}'.Return null", propertyName, nodePath);
            }
        } catch (RepositoryException e) {
            log.warn("RepositoryException: Exception for fetching property '{}' from '{}'", propertyName, this.nodePath);
        }
        return null;
    }
    
    private WrappedProp getWrappedProp(Property p){
       
        try {
            switch (p.getType()) {
            case PropertyType.BOOLEAN : 
                if(p.getDefinition().isMultiple()) {
                    Value[] values = p.getValues();
                    boolean[] bools = new boolean[values.length];
                    int i = 0;
                    for(Value val : values) {
                        bools[i] = val.getBoolean();
                        i++;
                    }
                    return new WrappedProp(bools, true, PropertyType.BOOLEAN); 
                }
                else {
                    return new WrappedProp(p.getBoolean(), true, PropertyType.BOOLEAN);  
                }
            case PropertyType.STRING :
                if(p.getDefinition().isMultiple()) {
                    Value[] values = p.getValues();
                    String[] strings = new String[values.length];
                    int i = 0;
                    for(Value val : values) {
                        strings[i] = val.getString();
                        i++;
                    }
                    return new WrappedProp(strings, true, PropertyType.STRING); 
                } else {
                    return new WrappedProp(p.getString(), true, PropertyType.STRING); 
                }
            case PropertyType.LONG :
                if(p.getDefinition().isMultiple()) {
                    Value[] values = p.getValues();
                    Long[] longs = new Long[values.length];
                    int i = 0;
                    for(Value val : values) {
                        longs[i] = val.getLong();
                        i++;
                    }
                    return new WrappedProp(longs, true, PropertyType.LONG); 
                } else {
                    return new WrappedProp(p.getLong(), true, PropertyType.LONG); 
                }
            case PropertyType.DOUBLE :
                if(p.getDefinition().isMultiple()) {
                    Value[] values = p.getValues();
                    Double[] doubles = new Double[values.length];
                    int i = 0;
                    for(Value val : values) {
                        doubles[i] = val.getDouble();
                        i++;
                    }
                    return new WrappedProp(doubles, true, PropertyType.DOUBLE); 
                } else {
                    return new WrappedProp(p.getDouble(), true, PropertyType.DOUBLE); 
                }    
            case PropertyType.DATE :
                if(p.getDefinition().isMultiple()) {
                    Value[] values = p.getValues();
                    Calendar[] dates = new Calendar[values.length];
                    int i = 0;
                    for(Value val : values) {
                        dates[i] = val.getDate();
                        i++;
                    }
                    return new WrappedProp(dates, true, PropertyType.DATE); 
                } else {
                    return new WrappedProp(p.getDate(), true, PropertyType.DATE); 
                }    
                
            default: 
                log.warn("getPropObject is only support for boolean, long, double, date and strings. Return null");
                return null;
            }
        } catch (ValueFormatException e) {
            log.warn("ValueFormatException: Exception for fetching property from '{}'", this.nodePath);
        } catch (IllegalStateException e) {
            log.warn("IllegalStateException: Exception for fetching property from '{}'", this.nodePath);
        } catch (RepositoryException e) {
            log.warn("RepositoryException: Exception for fetching property from '{}'", this.nodePath);
        }
        return null;
    }

    
    public class WrappedProp {
        private Object object;
        private boolean isMultiple;
        private int type;
        
        private WrappedProp(Object o, boolean isMultiple, int type){
            this.object = o;
            this.isMultiple = isMultiple;
            this.type = type;
        }
        
        public Object getObject() {
            return object;
        }
        
        public int getType() {
            return type;
        }
        
        public boolean isMultiple() {
            return isMultiple;
        }
        
    }
}
