package org.hippoecm.repository.export;

import org.dom4j.Namespace;
import org.dom4j.QName;

final class Constants {

	private Constants() {}
	
    static final Namespace JCR_NAMESPACE = new Namespace("sv", "http://www.jcp.org/jcr/sv/1.0");
    static final QName NAME_QNAME = new QName("name", JCR_NAMESPACE);
    static final QName TYPE_QNAME = new QName("type", JCR_NAMESPACE);
    static final QName NODE_QNAME = new QName("node", JCR_NAMESPACE);
    static final QName PROPERTY_QNAME = new QName("property", JCR_NAMESPACE);
    static final QName VALUE_QNAME = new QName("value", JCR_NAMESPACE);

}
