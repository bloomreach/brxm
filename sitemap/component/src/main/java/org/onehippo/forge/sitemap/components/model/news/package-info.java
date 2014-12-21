@XmlSchema(
        namespace = "http://www.sitemaps.org/schemas/sitemap/0.9",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix = "", namespaceURI = "http://www.sitemaps.org/schemas/sitemap/0.9"),
                @XmlNs(prefix = "news", namespaceURI = "http://www.google.com/schemas/sitemap-news/0.9")
        }
)
package org.onehippo.forge.sitemap.components.model.news;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;