/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.provider.jcr;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRValueProviderImpl implements JCRValueProvider{
  
    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(JCRValueProviderImpl.class);
    
    // transient node because ValueProvider implements Serializable
    private transient Node jcrNode;
    private transient Node canonicalNode;
    private transient Node handle;
    
    private String nodePath;
    private String canonicalPath;
    private String handlePath;
    
    private String nodeName;
    private boolean detached = false;
    private boolean isLoaded = false;
    private List<Integer> supportedPropertyTypes = new ArrayList<Integer>();
    private Map<String, Object> properties = new HashMap<String, Object>();
    
    {
        supportedPropertyTypes.add(PropertyType.STRING);
        supportedPropertyTypes.add(PropertyType.BOOLEAN);
        supportedPropertyTypes.add(PropertyType.DATE);
        supportedPropertyTypes.add(PropertyType.DOUBLE);
        supportedPropertyTypes.add(PropertyType.LONG);
    }
    
    public JCRValueProviderImpl(Node jcrNode) {
        this.jcrNode = jcrNode;
        if(jcrNode != null) {
            try {
                this.nodePath = jcrNode.getPath();
                this.nodeName = jcrNode.getName();
                this.canonicalNode = JCRUtilities.getCanonical(jcrNode);
                if(canonicalNode == null) {
                    log.debug("Node does not have a canonical representation");
                } else {
                    // if canonical node is a handle, set the handle. If canonical node is null, node cannot be a hippo:document
                    this.canonicalPath = canonicalNode.getPath();
                    if(canonicalNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                        this.handle = canonicalNode;
                        this.handlePath = handle.getPath();
                    } else if(canonicalNode.isNodeType(HippoNodeType.NT_DOCUMENT) && canonicalNode.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                       this.handle = canonicalNode.getParent();
                       this.handlePath = handle.getPath();
                    }
                    
                }
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
    
    public Node getCanonical(){
        if(isDetached()) {
            log.warn("Node '{}' is detached. Return null", nodePath);
            return null;
        }
        return this.canonicalNode;
    }
    
    public String getCanonicalPath(){
        return this.canonicalPath;
    }
    
    public Node getHandle(){
        if(isDetached()) {
            log.warn("Node '{}' is detached. Return null", nodePath);
            return null;
        }
        return this.handle;
    }
    
    public String getHandlePath(){
        return this.handlePath;
    }
    
    public void detach(){
        log.debug("Detaching node '{}'", this.nodePath);
        this.detached = true;
        this.jcrNode = null;
        this.canonicalNode = null;
        this.handle = null;
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
        Object o = this.properties.get(propertyName);
        if(o != null) {
            return true;
        }
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
        Object o = this.properties.get(propertyName);
        if(o != null && o instanceof String) {
            return (String)o;
        }
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.STRING, false);
        if(prop!= null) {
            this.properties.put(propertyName, prop.getObject());
            return (String)prop.getObject();
        }
        return null;
    }
    
    public String[] getStrings(String propertyName){
        Object o = this.properties.get(propertyName);
        if(o != null && o instanceof String[]) {
            return (String[])o;
        }
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.STRING, true);
        if(prop!= null) {
            this.properties.put(propertyName, prop.getObject());
            return (String[])prop.getObject();
        }
        return null;
    }

    public Double getDouble(String propertyName) {
        Object o = this.properties.get(propertyName);
        if(o != null && o instanceof Double) {
            return (Double)o;
        }
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.DOUBLE, false);
        if(prop!= null) {
            this.properties.put(propertyName, prop.getObject());
            return (Double)prop.getObject();
        }
        return null;
    }

    public Double[] getDoubles(String propertyName) {
        Object o = this.properties.get(propertyName);
        if(o != null && o instanceof Double[]) {
            return (Double[])o;
        }
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.DOUBLE, true);
        if(prop!= null) {
            this.properties.put(propertyName, prop.getObject());
            return (Double[])prop.getObject();
        }
        return null;
    }

    public Long getLong(String propertyName) {
        Object o = this.properties.get(propertyName);
        if(o != null && o instanceof Long) {
            return (Long)o;
        }
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.LONG, false);
        if(prop!= null) {
            this.properties.put(propertyName, prop.getObject());
            return (Long)prop.getObject();
        }
        return null;
    }

    public Long[] getLongs(String propertyName) {
        Object o = this.properties.get(propertyName);
        if(o != null && o instanceof Long[]) {
            return (Long[])o;
        }
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.LONG, true);
        if(prop!= null) {
            this.properties.put(propertyName, prop.getObject());
            return (Long[])prop.getObject();
        }
        return null;
    }

    public Calendar getDate(String propertyName) {
        Object o = this.properties.get(propertyName);
        if(o != null && o instanceof Calendar) {
            return (Calendar)o;
        }
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.DATE, false);
        if(prop!= null) {
            this.properties.put(propertyName, prop.getObject());
            return (Calendar)prop.getObject();
        }
        return null;
    }

    public Calendar[] getDates(String propertyName) {
        Object o = this.properties.get(propertyName);
        if(o != null && o instanceof Calendar[]) {
            return (Calendar[])o;
        }
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.DATE, true);
        if(prop!= null) {
            this.properties.put(propertyName, prop.getObject());
            return (Calendar[])prop.getObject();
        }
        return null;
    }

    public Boolean getBoolean(String propertyName) {
        Object o = this.properties.get(propertyName);
        if(o != null && o instanceof Boolean) {
            return (Boolean)o;
        }
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.BOOLEAN, false);
        if(prop!= null) {
            this.properties.put(propertyName, prop.getObject());
            return (Boolean)prop.getObject();
        }
        return null;
    }

    public Boolean[] getBooleans(String propertyName) {
        Object o = this.properties.get(propertyName);
        if(o != null && o instanceof Boolean[]) {
            return (Boolean[])o;
        }
        WrappedProp prop = this.getWrappedProp(propertyName,PropertyType.BOOLEAN, true);
        if(prop!= null) {
            this.properties.put(propertyName, prop.getObject());
            return (Boolean[])prop.getObject();
        }
        return null;
    }

    public Map<String, Object> getProperties() {
        if(this.isLoaded) {
            return this.properties;
        }
        
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
            return this.properties;
        }
        try {
            for(PropertyIterator allProps = jcrNode.getProperties(); allProps.hasNext();) {
                Property p = allProps.nextProperty();
                if(properties.containsKey(p.getName())) {
                    continue;
                }
                if(supportedPropertyTypes.contains(p.getType())) {
                    WrappedProp prop = getWrappedProp(p);
                    if(prop!= null) {
                        properties.put(p.getName(), prop.getObject());
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception: {}", e.getMessage());
        }
        this.isLoaded = true;
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
