@XmlSchema(
        namespace = EssentialConst.URI_JCR_NAMESPACE,
        xmlns = {
                @XmlNs(namespaceURI = EssentialConst.URI_JCR_NAMESPACE, prefix = "sv"),
                @XmlNs(namespaceURI = EssentialConst.URI_AUTOEXPORT_NAMESPACE, prefix = "h")
        },
        elementFormDefault = XmlNsForm.QUALIFIED) package org.onehippo.cms7.essentials.dashboard.utils.xml;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;

import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;


