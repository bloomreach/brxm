/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;


@XmlRootElement(name = "node", namespace = EssentialConst.URI_JCR_NAMESPACE)
public final class XmlNode implements NodeOrProperty {

    public static final String SV_TYPE_STRING = "String";
    public static final String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype";
    private static final Logger log = LoggerFactory.getLogger(XmlNode.class);
    private static final long serialVersionUID = 1L;
    public static final String HIPPOSYSEDIT_SUPERTYPE = "hipposysedit:supertype";
    private Collection<NodeOrProperty> nodeOrProperty;
    private Collection<XmlNode> childNodes;
    private Collection<XmlProperty> properties;
    private String name;
    private String merge;


    public XmlNode() {
    }

    public XmlNode(final String name) {
        this.name = name;
    }

    public XmlNode(final String name, final String primaryType) {
        this.name = name;
        final XmlProperty property = new XmlProperty(EssentialConst.NS_JCR_PRIMARY_TYPE, SV_TYPE_STRING);
        property.addValue(primaryType);
        addProperty(property);

    }

    public void addNode(final XmlNode node) {
        getXmlNodeOrXmlProperty().add(node);
    }

    public void addProperty(final XmlProperty property) {
        getXmlNodeOrXmlProperty().add(property);
    }

    public Collection<XmlNode> getTemplates() {
        final XmlNode editType = getEditType();
        if (editType != null) {
            return editType.getChildNodes();
        }
        return Collections.emptyList();
    }

    public XmlProperty getSupertypeProperty() {

        XmlNode editNode = getEditNode();
        if (editNode == null) {
            return null;
        }
        final Collection<XmlNode> editNodes = editNode.getChildNodesByName(HIPPOSYSEDIT_NODETYPE);
        if (editNodes.isEmpty()) {
            return null;
        }
        editNode = editNodes.iterator().next();
        if (editNode == null) {
            return null;
        }
        return editNode.getXmlPropertyByName(HIPPOSYSEDIT_SUPERTYPE);

    }

    private XmlNode getEditType() {

        final XmlNode myNode = getEditNode();
        final Collection<XmlNode> editTypes = myNode.getChildNodesByName(HIPPOSYSEDIT_NODETYPE);
        if (editTypes.size() == 1) {
            return editTypes.iterator().next();
        }
        return null;
    }

    private XmlNode getEditNode() {
        final Collection<XmlNode> myNodes = getChildNodesByName(HIPPOSYSEDIT_NODETYPE);
        final int size = myNodes.size();
        if (size != 1) {
            log.error("Expected one node but got:  {}", size);
            return null;
        }
        return myNodes.iterator().next();
    }

    @Override
    public String getType() {
        return null;
    }


    @Override
    @XmlElements({
            @XmlElement(name = "node", type = XmlNode.class),
            @XmlElement(name = "property", type = XmlProperty.class)
    })
    public Collection<NodeOrProperty> getXmlNodeOrXmlProperty() {
        if (nodeOrProperty == null) {
            nodeOrProperty = new LinkedList<>();
        }
        return nodeOrProperty;
    }


    @XmlTransient
    public Collection<XmlNode> getChildNodes() {
        if (childNodes == null) {
            childNodes = new LinkedList<>();
            final Collection<NodeOrProperty> xmlNodes = getXmlNodeOrXmlProperty();
            for (NodeOrProperty object : xmlNodes) {
                if (object.isNode()) {
                    childNodes.add((XmlNode) object);
                }
            }
        }
        return childNodes;
    }

    @XmlTransient
    public Collection<XmlProperty> getProperties() {
        if (properties == null) {
            properties = new LinkedList<>();
            final Collection<NodeOrProperty> nodesOrProperties = getXmlNodeOrXmlProperty();
            for (NodeOrProperty object : nodesOrProperties) {
                if (object.isProperty()) {
                    properties.add((XmlProperty) object);
                }
            }
        }
        return properties;
    }

    @Override
    @XmlAttribute(name = "name", namespace = EssentialConst.URI_JCR_NAMESPACE, required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NMTOKEN")
    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public Collection<XmlNode> getChildNodesByName(final String nodeName) {
        final Predicate<XmlNode> sameName = new Predicate<XmlNode>() {
            @Override
            public boolean apply(XmlNode node) {
                return nodeName.equals(node.getName());
            }
        };
        return Collections2.filter(getChildNodes(), sameName);
    }

    public Collection<XmlNode> getChildNodesByType(final String nodeType) {
        final Predicate<XmlNode> primaryType = new Predicate<XmlNode>() {
            @Override
            public boolean apply(final XmlNode node) {
                final XmlProperty property = node.getXmlPropertyByName(EssentialConst.NS_JCR_PRIMARY_TYPE);
                return property.getMultiple() && nodeType.equals(property.getSingleValue());
            }
        };
        return Collections2.filter(getChildNodes(), primaryType);

    }

    public XmlProperty getXmlPropertyByName(String propertyName) {
        final Collection<XmlProperty> myProperties = getProperties();
        for (XmlProperty property : myProperties) {
            if (propertyName.equals(property.getName())) {
                return property;
            }
        }
        return null;
    }

    @XmlAttribute(name = "merge", namespace = EssentialConst.URI_AUTOEXPORT_NAMESPACE, required = false)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public String getMerge() {
        return merge;
    }

    public void setMerge(final String merge) {
        this.merge = merge;
    }


    @Override
    public boolean isNode() {
        return true;
    }

    @Override
    public boolean isProperty() {
        return false;
    }

    @Override
    public Boolean getMultiple() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("XmlNode{");
        sb.append("nodeOrProperty=").append(nodeOrProperty);
        sb.append(", childNodes=").append(childNodes);
        sb.append(", properties=").append(properties);
        sb.append(", name='").append(name).append('\'');
        sb.append(", merge='").append(merge).append('\'');
        sb.append('}');
        return sb.toString();
    }
}