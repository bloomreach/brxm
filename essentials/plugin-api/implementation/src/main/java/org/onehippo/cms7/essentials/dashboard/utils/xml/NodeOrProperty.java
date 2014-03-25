package org.onehippo.cms7.essentials.dashboard.utils.xml;

import java.io.Serializable;
import java.util.Collection;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @version "$Id: NodeOrProperty.java 172944 2013-08-06 16:37:37Z mmilicevic $"
 */
public interface NodeOrProperty extends Serializable {


    @XmlTransient
    String getType();

    @XmlTransient
    Collection<NodeOrProperty> getXmlNodeOrXmlProperty();

    String getName();

    boolean isNode();

    boolean isProperty();

    @XmlTransient
    Boolean getMultiple();

    XmlProperty getPropertyForName(String propertyName);
}
