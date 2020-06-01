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

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 *                 Container for the data needed to describe a sitemap.
 *             
 * 
 * <p>Java class for tSitemap complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tSitemap">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="loc" type="{http://www.sitemaps.org/schemas/sitemap/0.9}tLocSitemap"/>
 *         &lt;element name="lastmod" type="{http://www.sitemaps.org/schemas/sitemap/0.9}tLastmodSitemap"
 *         minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlType(name = "tSitemap", propOrder = {

})
@Generated(value = TSitemap.GENERATOR_ENGINE,
        date = TSitemap.GENERATION_MOMENT,
        comments = TSitemap.GENERATION_COMMENT)
public class TSitemap {

    @XmlTransient
    public static final String GENERATOR_ENGINE = "com.sun.tools.internal.xjc.Driver";
    @XmlTransient
    public static final String GENERATION_MOMENT = "2012-02-11T11:25:35+01:00";
    @XmlTransient
    public static final String GENERATION_COMMENT = "JAXB RI vJAXB 2.1.10 in JDK 6";

    @Generated(value = GENERATOR_ENGINE,
            date = GENERATION_MOMENT,
            comments = GENERATION_COMMENT)
    private String loc;
    @Generated(value = GENERATOR_ENGINE,
            date = GENERATION_MOMENT,
            comments = GENERATION_COMMENT)
    private String lastmod;

    /**
     * Gets the value of the loc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = GENERATOR_ENGINE,
            date = GENERATION_MOMENT,
            comments = GENERATION_COMMENT)
    public String getLoc() {
        return loc;
    }

    /**
     * Sets the value of the loc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = GENERATOR_ENGINE,
            date = GENERATION_MOMENT,
            comments = GENERATION_COMMENT)
    @XmlElement(required = true)
    public void setLoc(final String value) {
        this.loc = value;
    }

    /**
     * Gets the value of the lastmod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = GENERATOR_ENGINE,
            date = GENERATION_MOMENT,
            comments = GENERATION_COMMENT)
    public String getLastmod() {
        return lastmod;
    }

    /**
     * Sets the value of the lastmod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = GENERATOR_ENGINE,
            date = GENERATION_MOMENT,
            comments = GENERATION_COMMENT)
    public void setLastmod(final String value) {
        this.lastmod = value;
    }

}
