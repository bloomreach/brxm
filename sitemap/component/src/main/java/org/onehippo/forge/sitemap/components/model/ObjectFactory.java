/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.05.05 at 01:46:58 PM CEST 
//


package org.onehippo.forge.sitemap.components.model;

import java.math.BigDecimal;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.hippoecm.hst.components.sitemap.model package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final String SITEMAP_SCHEMA_URI = "http://www.sitemaps.org/schemas/sitemap/0.9";

    private static final QName _Lastmod_QNAME = new QName(SITEMAP_SCHEMA_URI, "lastmod");
    private static final QName _Loc_QNAME = new QName(SITEMAP_SCHEMA_URI, "loc");
    private static final QName _Changefreq_QNAME = new QName(SITEMAP_SCHEMA_URI, "changefreq");
    private static final QName _Priority_QNAME = new QName(SITEMAP_SCHEMA_URI, "priority");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.hippoecm.hst.components.sitemap.model
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Urlset }
     * 
     */
    public Urlset createUrlset() {
        return new Urlset();
    }

    /**
     * Create an instance of {@link Url }
     * 
     */
    public Url createUrl() {
        return new Url();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = SITEMAP_SCHEMA_URI, name = "lastmod")
    public JAXBElement<String> createLastmod(String value) {
        return new JAXBElement<String>(_Lastmod_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = SITEMAP_SCHEMA_URI, name = "loc")
    public JAXBElement<String> createLoc(String value) {
        return new JAXBElement<String>(_Loc_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = SITEMAP_SCHEMA_URI, name = "changefreq")
    public JAXBElement<String> createChangefreq(String value) {
        return new JAXBElement<String>(_Changefreq_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = SITEMAP_SCHEMA_URI, name = "priority")
    public JAXBElement<BigDecimal> createPriority(BigDecimal value) {
        return new JAXBElement<BigDecimal>(_Priority_QNAME, BigDecimal.class, null, value);
    }

}
