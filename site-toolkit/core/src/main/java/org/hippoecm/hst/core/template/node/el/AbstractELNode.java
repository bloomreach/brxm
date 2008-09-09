package org.hippoecm.hst.core.template.node.el;

import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractELNode implements ELNode {

    private Logger log = LoggerFactory.getLogger(AbstractELNode.class);
    protected Node jcrNode;

    public AbstractELNode(Node node) {
        this.jcrNode = node;
    }

    public AbstractELNode(ContextBase contextBase, String relativePath) throws RepositoryException {
        this.jcrNode = contextBase.getRelativeNode(relativePath);
    }

    public Node getJcrNode() {
        return jcrNode;
    }

    public Map getHasProperty() {
        if (jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            public Object get(Object propertyName) {
                try {
                    return jcrNode.hasProperty((String) propertyName);
                } catch (RepositoryException e) {
                    log.error("RepositoryException " + e.getMessage());
                    return false;
                }

            }
        };
    }

    public Map getProperty() {
        if (jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            public Object get(Object propertyName) {
                try {
                    Value val = jcrNode.getProperty((String) propertyName).getValue();
                    switch (val.getType()) {
                    case PropertyType.BINARY:
                        break;
                    case PropertyType.BOOLEAN:
                        return val.getBoolean();
                    case PropertyType.DATE:
                        return val.getDate();
                    case PropertyType.DOUBLE:
                        return val.getDouble();
                    case PropertyType.LONG:
                        return val.getLong();
                    case PropertyType.REFERENCE:
                        // TODO return path of referenced node?
                        break;
                    case PropertyType.PATH:
                        // TODO return what?
                        break;
                    case PropertyType.STRING:
                        return val.getString();
                    case PropertyType.NAME:
                        // TODO what to return
                        break;
                    default:
                        log.error("Illegal type for Value");
                        return "";
                    }
                } catch (ValueFormatException e) {
                    log.warn("Property is multivalued: not applicable for AbstractELNode. Return null");
                } catch (PathNotFoundException e) {
                    log.warn("Property " + propertyName + " not found. Return null");
                } catch (RepositoryException e) {
                    log.warn("RepositoryException " + e.getMessage());
                }
                return null;
            }
        };
    }

    public String getDecodedName() {
        try {
            return ISO9075Helper.decodeLocalName(jcrNode.getName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }

    public String getName() {
        try {
            return jcrNode.getName();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }

    public String getPath() {
        try {
            return jcrNode.getPath();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }

}
