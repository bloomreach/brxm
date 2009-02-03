package org.hippoecm.hst.provider.jcr;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.provider.jcr.JCRValueProvider;
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
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
            return null;
        }
        try {
            if(jcrNode.hasProperty(propertyName)) {
                Property p = jcrNode.getProperty(propertyName);
                if(p.getType() == PropertyType.STRING && !p.getDefinition().isMultiple()) {
                    return p.getString();
                } else {
                    log.warn("Property '{}' is not of type String or single-valued for '{}'.", propertyName, nodePath);
                }
            } else {
                log.debug("Property '{}' not found at '{}'. Return null", propertyName, nodePath);
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception: {}", e.getMessage());
        }
        return null;
    }
    
    public String[] getStrings(String propertyName){
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
            return null;
        }
        try {
            if(jcrNode.hasProperty(propertyName)) {
                Property p = jcrNode.getProperty(propertyName);
                if(p.getType() == PropertyType.STRING  && p.getDefinition().isMultiple()) {
                    Value[] values = p.getValues();
                    String[] strings = new String[values.length];
                    int i = 0;
                    for(Value val : values) {
                        strings[i] = val.getString();
                        i++;
                    }
                    return strings;
                } else {
                    log.warn("Property '{}' is not multi-valued or does not hold strings '{}'. Return null.", propertyName, nodePath);
                }
            } else {
                log.debug("Property '{}' not found at '{}'.Return null", propertyName, nodePath);
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception: {}", e.getMessage());
        }
        return null;
    }

    public long getDouble(String propertyName) {
        return 0;
    }

    public long[] getDoubles(String propertyName) {
        return null;
    }

    public int getInt(String propertyName) {
        return 0;
    }

    public int[] getInts(String propertyName) {
        return null;
    }

    public long getLong(String propertyName) {
        return 0;
    }

    public long[] getLongs(String propertyName) {
        return null;
    }

    public Calendar getDate(String propertyName) {
        return null;
    }

    public Calendar[] getDates(String propertyName) {
        return null;
    }

    public boolean getBoolean(String propertyName) {
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method. Return false");
            return false;
        }
        try {
            if(jcrNode.hasProperty(propertyName)) {
                Property p = jcrNode.getProperty(propertyName);
                if(p.getType() == PropertyType.BOOLEAN && !p.getDefinition().isMultiple()) {
                    return p.getBoolean();
                } else {
                    log.warn("Property '{}' is not of type Boolean or single-valued for '{}'.", propertyName, nodePath);
                }
            } else {
                log.debug("Property '{}' not found at '{}'. Return null", propertyName, nodePath);
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception: {}", e.getMessage());
        }
        return false;
    }

    public boolean[] getBooleans(String propertyName) {
        if(isDetached()){
            log.warn("Jcr Node is detatched. Cannot execute method");
            return null;
        }
        try {
            if(jcrNode.hasProperty(propertyName)) {
                Property p = jcrNode.getProperty(propertyName);
                if(p.getType() == PropertyType.BOOLEAN  && p.getDefinition().isMultiple()) {
                    Value[] values = p.getValues();
                    boolean[] bools = new boolean[values.length];
                    int i = 0;
                    for(Value val : values) {
                        bools[i] = val.getBoolean();
                        i++;
                    }
                    return bools;
                } else {
                    log.warn("Property '{}' is not multi-valued or does not hold booleans '{}'. Return null.", propertyName, nodePath);
                }
            } else {
                log.debug("Property '{}' not found at '{}'.Return null", propertyName, nodePath);
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception: {}", e.getMessage());
        }
        return null;
    }



}
