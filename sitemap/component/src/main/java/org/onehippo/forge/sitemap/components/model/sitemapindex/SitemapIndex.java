/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.sitemap.components.model.sitemapindex;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sitemap" type="{http://www.sitemaps.org/schemas/sitemap/0.9}tSitemap"
 *         maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "sitemap"
})
@XmlRootElement(name = "sitemapindex")
@Generated(value = "com.sun.tools.internal.xjc.Driver",
        date = "2012-02-11T11:25:35+01:00",
        comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
public class SitemapIndex {

    @XmlElement(required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver",
            date = "2012-02-11T11:25:35+01:00",
            comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    private List<TSitemap> sitemap;

    /**
     * Gets the value of the sitemap property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sitemap property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSitemap().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TSitemap }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver",
            date = "2012-02-11T11:25:35+01:00",
            comments = "JAXB RI vJAXB 2.1.10 in JDK 6")
    public List<TSitemap> getSitemap() {
        if (sitemap == null) {
            sitemap = new ArrayList<TSitemap>();
        }
        return this.sitemap;
    }


}
