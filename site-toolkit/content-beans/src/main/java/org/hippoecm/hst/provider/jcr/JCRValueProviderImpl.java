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
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

import org.hippoecm.hst.provider.PropertyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRValueProviderImpl implements JCRValueProvider{
  
    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(JCRValueProviderImpl.class);
    
    // transient node because ValueProvider implements Serializable
    private transient Node jcrNode;
    
    private String nodePath;
    private String nodeName;
    
    private boolean detached = false;
    private boolean isLoaded = false;
    private List<Integer> supportedPropertyTypes = new ArrayList<Integer>();
   
    //private Map<String, Object> properties = new HashMap<String, Object>();
    
    private PropertyMapImpl propertyMap = new PropertyMapImpl();
    
    {
        supportedPropertyTypes.add(PropertyType.STRING);
        supportedPropertyTypes.add(PropertyType.BOOLEAN);
        supportedPropertyTypes.add(PropertyType.DATE);
        supportedPropertyTypes.add(PropertyType.DOUBLE);
        supportedPropertyTypes.add(PropertyType.LONG);
    }
    
    public JCRValueProviderImpl(Node jcrNode) {
        this.jcrNode = jcrNode;
    }

    public Node getJcrNode(){
        if(isDetached()) {
            log.warn("Node '{}' is detached. Return null", nodePath);
            return null;
        }
        return this.jcrNode;
    }
    
    public Node getParentJcrNode(){
        if(this.jcrNode == null) {
            log.warn("Cannot get parent node when node is detached");
            return null;
        }
        try {
            if(this.jcrNode.isSame(jcrNode.getSession().getRootNode())) {
                log.warn("Cannot get parent node for the jcr rootNode");
                return null;
            } 
            return this.jcrNode.getParent();
        } catch(RepositoryException e ) {
            log.error("Repository Exception {}", e);
            return null;
        }
    }
   
    public void detach(){
        if(this.nodePath != null) {
            log.debug("Detaching node '{}'", this.nodePath);
        }
        this.detached = true;
        this.jcrNode = null;
    }
    
    public boolean isDetached(){
        return this.detached;
    }
    
    public String getName() {
        if(this.nodeName != null) {
            return this.nodeName;
        }
        if(isDetached()) {
            return null;
        } else if(this.jcrNode != null) {
            try {
                this.nodeName = jcrNode.getName();
            } catch (RepositoryException e) {
                log.error("Error while retrieving jcr node name {}", e);
            }
        }
        return this.nodeName;
    }

    public String getPath() {
        if(this.nodePath != null) {
            return this.nodePath;
        }
        if(isDetached()) {
            return null;
        } else if(this.jcrNode != null) {
            try {
                this.nodePath = jcrNode.getPath();
            } catch (RepositoryException e) {
                log.error("Error while retrieving jcr node path {}", e);
            }
        }
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
        boolean b = this.propertyMap.hasProperty(propertyName);
        if(b) {
            return true;
        }
        b = this.propertyMap.isUnAvailableProperty(propertyName);
        if(b) {
            return false;
        }
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
            return false;
        }
        try {
            boolean bool =  jcrNode.hasProperty(propertyName);
            if(bool) {
                this.propertyMap.addAvailableProperty(propertyName);
            } else {
                this.propertyMap.addUnAvailableProperty(propertyName);
            }
            return bool;
        } catch (RepositoryException e) {
            log.warn("Repository Exception during check if property exists: ", e);
        }
        return false;
    }
    
    public String getString(String propertyName){
        String o = this.propertyMap.getStrings().get(propertyName);
        if(o != null) {
            return o;
        }
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return null;
        }
        loadProperty(propertyName,PropertyType.STRING, false);
        return this.propertyMap.getStrings().get(propertyName);
    }
    
    public String[] getStrings(String propertyName){
        String[] o = this.propertyMap.getStringArrays().get(propertyName);
        if(o != null ) {
            return o;
        }
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return new String[0];
        }
        
        loadProperty(propertyName,PropertyType.STRING, true);
        
        return this.propertyMap.getStringArrays().get(propertyName);
    }

    public Double getDouble(String propertyName) {
        Double o = this.propertyMap.getDoubles().get(propertyName);
        if(o != null) {
            return o;
        }
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return new Double(0);
        }
        
        loadProperty(propertyName,PropertyType.DOUBLE, false);
        
        o = this.propertyMap.getDoubles().get(propertyName);
        if(o == null) {
           return new Double(0); 
        } 
        return o;
    }

    public Double[] getDoubles(String propertyName) {
        Double[] o = this.propertyMap.getDoubleArrays().get(propertyName);
        if(o != null) {
            return o;
        }
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return new Double[0];
        }
        
        loadProperty(propertyName,PropertyType.DOUBLE, true);
       
        return this.propertyMap.getDoubleArrays().get(propertyName);
    }

    public Long getLong(String propertyName) {
        Long o = this.propertyMap.getLongs().get(propertyName);
        if(o != null) {
            return (Long)o;
        } 
        
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return new Long(0);
        }
        
        loadProperty(propertyName,PropertyType.LONG, false);
        
        o = this.propertyMap.getLongs().get(propertyName);
        if(o == null) {
           return new Long(0); 
        } 
        return o;
    }

    public Long[] getLongs(String propertyName) {
        Long[] o = this.propertyMap.getLongArrays().get(propertyName);
        if(o != null) {
            return o;
        }
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return new Long[0];
        }
        
        loadProperty(propertyName,PropertyType.LONG, true);
       
        return this.propertyMap.getLongArrays().get(propertyName);
    }
  
    // As a Calendar object is modifiable, return a cloned instance such that the underlying Calendar object cannot be changed
    public Calendar getDate(String propertyName) {
        Calendar o = this.propertyMap.getCalendars().get(propertyName);
        if(o != null) {
            return (Calendar)o.clone();
        }
        
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return null;
        }
        
        loadProperty(propertyName,PropertyType.DATE, false);
        return this.propertyMap.getCalendars().get(propertyName);
    }

    // As a Calendar object is modifiable, return a new instance such that the underlying Calendar object cannot be changed
    public Calendar[] getDates(String propertyName) {
        Calendar[] o = this.propertyMap.getCalendarArrays().get(propertyName);
        if(o != null) {
            return (Calendar[])o.clone();
        }
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return null;
        }
        
        loadProperty(propertyName,PropertyType.DATE, true);
      
        return this.propertyMap.getCalendarArrays().get(propertyName);
    }

    public Boolean getBoolean(String propertyName) {
        Boolean o = this.propertyMap.getBooleans().get(propertyName);
        if(o != null ) {
            return o;
        }
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return false;
        }
        
        loadProperty(propertyName,PropertyType.BOOLEAN, false);
        
        o = this.propertyMap.getBooleans().get(propertyName);
        if(o == null) {
           return false; 
        } 
        return o;
    }

    public Boolean[] getBooleans(String propertyName) {
        Boolean[] o = this.propertyMap.getBooleanArrays().get(propertyName);
        if(o != null ) {
            return o;
        }
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return new Boolean[0];
        }
        loadProperty(propertyName,PropertyType.BOOLEAN, true);
        return this.propertyMap.getBooleanArrays().get(propertyName);
    }

    public Map<String, Object> getProperties() {
        PropertyMap p = getPropertyMap();
        return p.getAllMapsCombined();
    }
    
    public PropertyMap getPropertyMap() {
        if(this.isLoaded) {
           return propertyMap;
        }
        
        
        
        if(isDetached()){
            log.warn("Jcr Node is detatched. Return already loaded properties ");
            return propertyMap;
        }
        try {
            for(PropertyIterator allProps = jcrNode.getProperties(); allProps.hasNext();) {
                Property p = allProps.nextProperty();
                if(this.propertyMap.hasProperty(p.getName())) {
                    // already loaded
                    continue;
                }
                if(supportedPropertyTypes.contains(p.getType())) {
                   loadProperty(p, p.getDefinition(), p.getName());
                }
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception: {}", e.getMessage());
        }
        this.isLoaded = true;
        return propertyMap;
    }
    
    


    private void loadProperty(String propertyName, int propertyType, boolean isMultiple){
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
        }
        try {
            if(jcrNode.hasProperty(propertyName)) {
                
                Property prop = jcrNode.getProperty(propertyName);
                if(prop.getType() != propertyType) {
                    if (log.isWarnEnabled()) log.warn("Cannot return property '{}' for node '{}' because it is of the wrong type. Return null", propertyName, this.nodePath);
                    return;
                }
                PropertyDefinition propDef = prop.getDefinition();
                if( (propDef.isMultiple() && isMultiple) ||  (!propDef.isMultiple() && !isMultiple)) {
                    loadProperty(jcrNode.getProperty(propertyName), propDef, propertyName);
                    return;
                }
                
                else {
                    if (log.isWarnEnabled()) log.warn("Cannot return property '{}' for node '{}'. Return null", propertyName, this.nodePath);
                    return;
                }
            }
            else {
                this.propertyMap.addUnAvailableProperty(propertyName);
                log.debug("Property '{}' not found at '{}'.Return null", propertyName, nodePath);
                return;
            }
        } catch (RepositoryException e) {
            if (log.isWarnEnabled()) log.warn("RepositoryException: Exception for fetching property '{}' from '{}'", propertyName, this.nodePath);
        }
        return;
    }
    
    private void loadProperty(Property p, PropertyDefinition propDef, String propertyName){
       
        try {
            switch (p.getType()) {
            case PropertyType.BOOLEAN : 
                if(propDef.isMultiple()) {
                   
                    Value[] values = p.getValues();
                    Boolean[] bools = new Boolean[values.length];
                    int i = 0;
                    for(Value val : values) {
                        bools[i] = val.getBoolean();
                        i++;
                    }

                    this.propertyMap.getBooleanArrays().put(propertyName, bools);
                    this.propertyMap.addAvailableProperty(propertyName);
                    return;
                }
                else {
                    this.propertyMap.getBooleans().put(propertyName, p.getBoolean());
                    this.propertyMap.addAvailableProperty(propertyName);
                    return;
                }
            case PropertyType.STRING :
                if(propDef.isMultiple()) {
                    Value[] values = p.getValues();
                    String[] strings = new String[values.length];
                    int i = 0;
                    for(Value val : values) {
                        strings[i] = val.getString();
                        i++;
                    }
                    this.propertyMap.getStringArrays().put(propertyName, strings);
                    this.propertyMap.addAvailableProperty(propertyName);
                    return;
                } else {
                    this.propertyMap.getStrings().put(propertyName, p.getString());
                    this.propertyMap.addAvailableProperty(propertyName);
                    return;
                }
            case PropertyType.LONG :
                if(propDef.isMultiple()) {
                    Value[] values = p.getValues();
                    Long[] longs = new Long[values.length];
                    int i = 0;
                    for(Value val : values) {
                        longs[i] = val.getLong();
                        i++;
                    }
                    this.propertyMap.getLongArrays().put(propertyName, longs);
                    this.propertyMap.addAvailableProperty(propertyName);
                    return;
                } else {
                    this.propertyMap.getLongs().put(propertyName, p.getLong());
                    this.propertyMap.addAvailableProperty(propertyName);
                    return;
                }
            case PropertyType.DOUBLE :
                if(propDef.isMultiple()) {
                    Value[] values = p.getValues();
                    Double[] doubles = new Double[values.length];
                    int i = 0;
                    for(Value val : values) {
                        doubles[i] = val.getDouble();
                        i++;
                    }
                    this.propertyMap.getDoubleArrays().put(propertyName, doubles);
                    this.propertyMap.addAvailableProperty(propertyName);
                    return;
                } else {
                    this.propertyMap.getDoubles().put(propertyName, p.getDouble());
                    this.propertyMap.addAvailableProperty(propertyName);
                    return;
                }    
            case PropertyType.DATE :
                if(propDef.isMultiple()) {
                    Value[] values = p.getValues();
                    Calendar[] dates = new Calendar[values.length];
                    int i = 0;
                    for(Value val : values) {
                        dates[i] = val.getDate();
                        i++;
                    }
                    this.propertyMap.getCalendarArrays().put(propertyName, dates);
                    this.propertyMap.addAvailableProperty(propertyName);
                    return;
                } else {
                    this.propertyMap.getCalendars().put(propertyName, p.getDate());
                    this.propertyMap.addAvailableProperty(propertyName);
                    return;
                }    
                
            default: 
                if (log.isWarnEnabled()) log.warn("getPropObject is only support for boolean, long, double, date and strings. Return null");
                return ;
            }
        } catch (ValueFormatException e) {
            if (log.isWarnEnabled()) log.warn("ValueFormatException: Exception for fetching property from '{}'", this.nodePath);
        } catch (IllegalStateException e) {
            if (log.isWarnEnabled()) log.warn("IllegalStateException: Exception for fetching property from '{}'", this.nodePath);
        } catch (RepositoryException e) {
            if (log.isWarnEnabled()) log.warn("RepositoryException: Exception for fetching property from '{}'", this.nodePath);
        }
        return ;
    }

    public void flush() {
        this.propertyMap.flush();
    }


}
