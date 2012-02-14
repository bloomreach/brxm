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

import java.util.Calendar;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.provider.PropertyMap;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRValueProviderImpl implements JCRValueProvider{
  
    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(JCRValueProviderImpl.class);
    private static final Calendar[] EMPTY_CALENDAR_ARRAY = new Calendar[0];

    // transient node because ValueProvider implements Serializable
    private transient Node jcrNode;

    private String nodePath;
    private String canonicalPath;
    private String identifier;
    private String nodeName;
    private String localizedName;
    
    private boolean detached = false;
    private boolean isLoaded = false;
   
    private PropertyMapImpl propertyMap = new PropertyMapImpl();
    
    public JCRValueProviderImpl(Node jcrNode) {
        this(jcrNode, true);
    }
    
    /**
     * if <code>lazyLoading</code> is false, we'll actively fill all the properties of the jcr node in the properties map
     * and fetch the canonical path
     * @param jcrNode
     * @param lazyLoading
     */
    public JCRValueProviderImpl(Node jcrNode, boolean lazyLoading) {
        this.jcrNode = jcrNode;
        if(jcrNode == null) {
            return;
        }
        try {
            this.nodeName = jcrNode.getName();
            this.nodePath = jcrNode.getPath();
            if(!lazyLoading) {
                populate();
                populateCanonicalPath();
                populateIdentifier();
            }
        } catch (RepositoryException e) {
            log.error("RepositoryException ", e);
        }
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
            log.error("Repository Exception", e);
            return null;
        }
    }
   
    public void detach(){
        if(this.nodePath != null) {
            log.debug("Detaching node '{}'", this.nodePath);
        }
        this.detached = true;
        this.jcrNode = null;
        // AFTER a valueprovider is detached, properties cannot be populated any more. This means 
        // we can optimize the PropertyMapImpl now to not use all unique empty hashmaps. 
        propertyMap.providerDetached();
    }
    
    public boolean isDetached(){
        return this.detached;
    }
    
    public String getName() {
       return this.nodeName;
    }
    
    public String getLocalizedName(){
        if(localizedName != null) {
            return localizedName;
        }
        Node node = this.getJcrNode();
        if(!(node instanceof HippoNode)){
            localizedName =  getName();
            return localizedName;
        }
        try {
            localizedName = ((HippoNode)node).getLocalizedName();
            return localizedName;
        } catch (RepositoryException e) {
           log.error("RepositoryException during fetching localizedName. Return null", e);
           return null;
        } 
    }
    
    public String getPath() {
      return this.nodePath;
    }
    
    public String getCanonicalPath() {
        if(canonicalPath != null) {
            return canonicalPath;
        }
        
        populateCanonicalPath();
        
        return canonicalPath;
    }
    
    /**
     * We return the uuid of the jcr node. If we cannot get the uuid, we return the canonical path
     */
    public String getIdentifier() {
        if(identifier != null) {
            return identifier;
        }
        
        populateIdentifier();
        
        return identifier;
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
        if(isLoaded) {
            // all properties are already loaded, but propertyName was not one of them.
            return false;
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
                Property prop = jcrNode.getProperty(propertyName);
                PropertyDefinition propDef = prop.getDefinition();
                loadProperty(prop, propDef, propertyName);
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
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        
        loadProperty(propertyName,PropertyType.STRING, true);
        
        String[] val =  this.propertyMap.getStringArrays().get(propertyName);
        if(val == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return val;
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
            return ArrayUtils.EMPTY_DOUBLE_OBJECT_ARRAY;
        }
        
        loadProperty(propertyName,PropertyType.DOUBLE, true);
        Double[] val = this.propertyMap.getDoubleArrays().get(propertyName);
        if(val == null) {
            return ArrayUtils.EMPTY_DOUBLE_OBJECT_ARRAY;
        }
        return val;
    }

    public Long getLong(String propertyName) {
        Long o = this.propertyMap.getLongs().get(propertyName);
        if(o != null) {
            return (Long)o;
        } 
        
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return Long.valueOf(0);
        }
        
        loadProperty(propertyName,PropertyType.LONG, false);
        
        o = this.propertyMap.getLongs().get(propertyName);
        if(o == null) {
           return Long.valueOf(0); 
        } 
        return o;
    }

    public Long[] getLongs(String propertyName) {
        Long[] o = this.propertyMap.getLongArrays().get(propertyName);
        if(o != null) {
            return o;
        }
        if(this.propertyMap.isUnAvailableProperty(propertyName)) {
            return ArrayUtils.EMPTY_LONG_OBJECT_ARRAY;
        }
        
        loadProperty(propertyName,PropertyType.LONG, true);
        Long[] val = this.propertyMap.getLongArrays().get(propertyName);
        if(val == null) {
            return ArrayUtils.EMPTY_LONG_OBJECT_ARRAY;
        }
        return val;
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
            return EMPTY_CALENDAR_ARRAY;
        }
        
        loadProperty(propertyName,PropertyType.DATE, true);
        Calendar[] val = this.propertyMap.getCalendarArrays().get(propertyName);
        if(val == null) {
            return EMPTY_CALENDAR_ARRAY;
        }
        return val;
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
            return ArrayUtils.EMPTY_BOOLEAN_OBJECT_ARRAY;
        }
        loadProperty(propertyName,PropertyType.BOOLEAN, true);
        Boolean[] val  = this.propertyMap.getBooleanArrays().get(propertyName);
        if(val == null) {
           return ArrayUtils.EMPTY_BOOLEAN_OBJECT_ARRAY;
        } 
        return val;
    }

    public Map<String, Object> getProperties() {
        PropertyMap p = getPropertyMap();
        return p.getAllMapsCombined();
    }
    
    public PropertyMap getPropertyMap() {
        if(this.isLoaded) {
           return propertyMap;
        }
        populate();
        
        return propertyMap;
    }
    
    private void loadProperty(String propertyName, int propertyType, boolean isMultiple){
        if(isLoaded) {
          return; 
        }
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
            return;
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


    private void populate() {
        if(isDetached()){
            log.warn("Jcr Node is detatched. Return already loaded properties ");
            return;
        }
        try {
            for(PropertyIterator allProps = jcrNode.getProperties(); allProps.hasNext();) {
                Property p = allProps.nextProperty();
                if(this.propertyMap.hasProperty(p.getName())) {
                    // already loaded
                    continue;
                }
                if(PropertyType.STRING == p.getType() ||
                        PropertyType.BOOLEAN  == p.getType() || 
                        PropertyType.DATE  == p.getType() ||
                        PropertyType.DOUBLE  == p.getType() ||
                        PropertyType.LONG == p.getType()) {
                  loadProperty(p, p.getDefinition(), p.getName());
                }
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception: {}", e.getMessage());
        }
        this.isLoaded = true;
    }
    
    private void populateCanonicalPath(){
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot get canonical path");
            return;
        } 
        this.canonicalPath = this.nodePath;
        
        if(jcrNode instanceof HippoNode) {
            try {
                Node canonical = ((HippoNode)jcrNode).getCanonicalNode();
                if(canonical != null) {
                    this.canonicalPath = canonical.getPath();
                } else {
                    log.info("The canonical path of a virtual only node is the path of the virtual node");
                    this.canonicalPath = jcrNode.getPath();
                }
            } catch (RepositoryException e) {
                log.warn("Repository Exception during fetching canonical path: ", e);
            }
            
        }
    }
    
    private void populateIdentifier(){
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot get identifier");
            return;
        } 
        if(jcrNode instanceof HippoNode) {
            try {
                Node canonical = ((HippoNode)jcrNode).getCanonicalNode();
                if(canonical != null) {
                    this.identifier = canonical.getIdentifier();
                } else {
                    log.warn("Cannot get an identifier for a virtual only node '{}'", jcrNode.getPath());
                }
            } catch (RepositoryException e) {
                log.warn("Repository Exception during fetching canonical path: ", e);
            }
            
        }
    }

}
