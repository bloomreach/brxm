/*
 * Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.seo.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.proxy.Invoker;
import org.apache.commons.proxy.ProxyFactory;
import org.easymock.EasyMock;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.mock.core.sitemenu.MockHstSiteMenu;
import org.hippoecm.hst.mock.core.sitemenu.MockHstSiteMenuItem;
import org.hippoecm.hst.mock.core.sitemenu.MockHstSiteMenus;
import org.hippoecm.hst.util.KeyValue;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class SEOHelperComponentTest {

    private static Logger log = LoggerFactory.getLogger(SEOHelperComponentTest.class);

    private SEOHelperComponent helperComp;
    private MySEOHelperComponentParamsInfo paramsInfo;
    private MockHstRequest request;
    private MockHstResponse response;
    private MockHstRequestContext requestContext;
    private Node contentNode;
    private HippoBean contentBean;
    private Calendar publicationDate = Calendar.getInstance();
    private Calendar lastModificationDate = Calendar.getInstance();
    private List<KeyValue<String, Element>> headElements = new LinkedList<>();

    @Before
    public void setUp() throws Exception {

        publicationDate.set(2011, Calendar.JULY, 25);
        contentNode = new MockNode("mock", "hippostdpubwf:document");
        contentBean = EasyMock.createNiceMock(HippoBean.class);
        EasyMock.expect(contentBean.getName()).andReturn("CONTENTDOCUMENT").anyTimes();
        EasyMock.expect(contentBean.getNode()).andReturn(contentNode).anyTimes();
        EasyMock.expect(contentBean.getProperty("hippostdpubwf:publicationDate")).andReturn(publicationDate).anyTimes();
        EasyMock.expect(contentBean.getProperty("hippostdpubwf:lastModificationDate")).andReturn(lastModificationDate).anyTimes();
        EasyMock.replay(contentBean);

        paramsInfo = new MySEOHelperComponentParamsInfo();
        paramsInfo.setSiteTitle("Example.com");
        paramsInfo.setSiteDublinCoreCopyrightLink("http://www.example.com/dc/copyright.html");
        paramsInfo.setEnableDublinCoreLanguage("true");

        helperComp = new SEOHelperComponent() {
            @Override
            protected SEOHelperComponentParamsInfo getComponentParametersInfo(HstRequest request) {
                return paramsInfo;
            }

        };

        MockHstSiteMenuItem parentSiteMenuItem = new MockHstSiteMenuItem();
        parentSiteMenuItem.setName("PARENTSITEMENUITEM1");

        MockHstSiteMenuItem siteMenuItem = new MockHstSiteMenuItem();
        siteMenuItem.setName("SITEMENUITEM1");
        siteMenuItem.setParentItem(parentSiteMenuItem);

        MockHstSiteMenu siteMenu = new MockHstSiteMenu();
        siteMenu.setSelectSiteMenuItem(siteMenuItem);

        MockHstSiteMenus siteMenus = new MockHstSiteMenus();
        siteMenus.addSiteMenu("main", siteMenu);

        requestContext = new MockHstRequestContext();
        requestContext.setHstSiteMenus(siteMenus);

        request = new MockHstRequest();
        request.setRequestContext(requestContext);
        request.setLocale(new Locale("en", "US"));
        requestContext.setContentBean(contentBean);

        response = new MockHstResponse();

    }

    @Test
    public void testBasicUsage() throws Exception {
        headElements.clear();
        helperComp.doBeforeRender(request, response);

        assertTrue(StringUtils.isNotBlank(paramsInfo.getDublinCoreSchemaLink()));

        assertTrue(StringUtils.isNotBlank(paramsInfo.getDublinCoreTermsLink()));

        assertTrue(StringUtils.isNotBlank(paramsInfo.getSiteDublinCoreCopyrightLink()));
        assertEquals(paramsInfo.getSiteDublinCoreCopyrightLink(), request.getAttribute("dublinCoreCopyrightLink"));

        assertTrue("true".equalsIgnoreCase(paramsInfo.getEnableDublinCoreLanguage()));
        assertTrue(StringUtils.isBlank(paramsInfo.getDublinCoreLanguage()));
        assertEquals("en-US", request.getAttribute("dublinCoreLanguage"));

        assertEquals("Example.com - PARENTSITEMENUITEM1 - SITEMENUITEM1", request.getAttribute("title"));

        assertEquals(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(publicationDate), request.getAttribute("dublinCoreTermsCreated"));

        assertEquals(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(lastModificationDate), request.getAttribute("dublinCoreTermsModified"));
    }

    @Test
    public void testContentBeanWithTitleProperty() throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory();
        contentBean = (HippoBean) proxyFactory.createInvokerProxy(
                new Invoker() {
                    public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
                        if ("getTitle".equals(method.getName())) {
                            return "CONTENTDOCUMENTTITLE";
                        }
                        if ("getNode".equals(method.getName())) {
                            return contentNode;
                        }
                        return null;
                    }
                },
                new Class [] { Titled.class, HippoBean.class });
        requestContext.setContentBean(contentBean);

        helperComp.doBeforeRender(request, response);

        assertEquals("Example.com - PARENTSITEMENUITEM1 - CONTENTDOCUMENTTITLE", request.getAttribute("title"));
    }

    @Test
    public void testContentBeanWithSeoTitle() throws Exception {
        final Node seo = contentNode.addNode("dummy-compound-name", SEOHelperComponent.SEO_COMPOUND_NODETYPE);
        seo.setProperty(SEOHelperComponent.SEO_TITLE_PROPERTY, "SEOTITLE");

        helperComp = new SEOHelperComponent() {
            @Override
            protected SEOHelperComponentParamsInfo getComponentParametersInfo(HstRequest request) {
                return paramsInfo;
            }
        };

        requestContext.setContentBean(contentBean);

        helperComp.doBeforeRender(request, response);

        assertEquals("Example.com - PARENTSITEMENUITEM1 - SEOTITLE", request.getAttribute("title"));

        seo.remove();
    }

    @Test
    public void testContentBeanWithTitleAndKeywordsProperty() throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory();
        contentBean = (HippoBean) proxyFactory.createInvokerProxy(
                new Invoker() {
                    public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
                        if ("getTitle".equals(method.getName())) {
                            return "CONTENTDOCUMENTTITLE";
                        }
                        if ("getKeywords".equals(method.getName())) {
                            return "Key1,Key2,Key3";
                        }
                        if ("getNode".equals(method.getName())) {
                            return contentNode;
                        }
                        return null;
                    }
                },
                new Class [] { Titled.class, Keyworded.class, HippoBean.class });
        requestContext.setContentBean(contentBean);

        helperComp.doBeforeRender(request, response);

        assertEquals("Example.com - PARENTSITEMENUITEM1 - CONTENTDOCUMENTTITLE : Key1,Key2,Key3", request.getAttribute("title"));
    }

    @Test
    public void testContentBeanWithTitleAndKeywordsAndDescriptionProperty() throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory();
        contentBean = (HippoBean) proxyFactory.createInvokerProxy(
                new Invoker() {
                    public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
                        if ("getTitle".equals(method.getName())) {
                            return "CONTENTDOCUMENTTITLE";
                        }
                        if ("getKeywords".equals(method.getName())) {
                            return "Key1,Key2,Key3";
                        }
                        if ("getDescription".equals(method.getName())) {
                            return "The description of the document";
                        }
                        if ("getNode".equals(method.getName())) {
                            return contentNode;
                        }
                        return null;
                    }
                },
                new Class [] { Titled.class, Keyworded.class, Descripted.class, HippoBean.class });
        requestContext.setContentBean(contentBean);

        helperComp.doBeforeRender(request, response);

        assertEquals("Example.com - PARENTSITEMENUITEM1 - CONTENTDOCUMENTTITLE : Key1,Key2,Key3", request.getAttribute("title"));
        assertEquals("Key1,Key2,Key3", request.getAttribute("metaKeywords"));
        assertEquals("The description of the document", request.getAttribute("metaDescription"));
    }

    @Test
    public void testContentBeanWithSeoDescription() throws Exception{
        final Node seo = contentNode.addNode("dummy-compound-name", SEOHelperComponent.SEO_COMPOUND_NODETYPE);
        seo.setProperty(SEOHelperComponent.SEO_DESCRIPTION_PROPERTY, "SEODESCRIPTION");

        helperComp = new SEOHelperComponent() {
            @Override
            protected SEOHelperComponentParamsInfo getComponentParametersInfo(HstRequest request) {
                return paramsInfo;
            }
        };

        requestContext.setContentBean(contentBean);

        helperComp.doBeforeRender(request, response);

        assertEquals("SEODESCRIPTION", request.getAttribute("metaDescription"));

        seo.remove();
    }

    @Test
    public void testWithDefaultParams() throws Exception {
        paramsInfo = new MySEOHelperComponentParamsInfo();

        helperComp.doBeforeRender(request, response);

        assertTrue(StringUtils.isNotBlank(paramsInfo.getDublinCoreSchemaLink()));
        assertEquals(paramsInfo.getDublinCoreSchemaLink(), request.getAttribute("dublinCoreSchemaLink"));

        assertTrue(StringUtils.isNotBlank(paramsInfo.getDublinCoreTermsLink()));
        assertEquals(paramsInfo.getDublinCoreTermsLink(), request.getAttribute("dublinCoreTermsLink"));

        assertTrue(StringUtils.isBlank(paramsInfo.getSiteDublinCoreCopyrightLink()));
        assertEquals(paramsInfo.getSiteDublinCoreCopyrightLink(), request.getAttribute("dublinCoreCopyrightLink"));

        assertEquals("PARENTSITEMENUITEM1 - SITEMENUITEM1", request.getAttribute("title"));

        assertEquals(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(publicationDate), request.getAttribute("dublinCoreTermsCreated"));

        assertEquals(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(lastModificationDate), request.getAttribute("dublinCoreTermsModified"));
    }

    @Test
    public void testWithNonPublishableContent() throws Exception {
        contentNode.setPrimaryType("not:hippostdpubwf:document");

        helperComp.doBeforeRender(request, response);

        assertTrue(StringUtils.isNotBlank(paramsInfo.getDublinCoreSchemaLink()));
        assertEquals(paramsInfo.getDublinCoreSchemaLink(), request.getAttribute("dublinCoreSchemaLink"));

        assertTrue(StringUtils.isNotBlank(paramsInfo.getDublinCoreTermsLink()));
        assertEquals(paramsInfo.getDublinCoreTermsLink(), request.getAttribute("dublinCoreTermsLink"));

        assertTrue(StringUtils.isNotBlank(paramsInfo.getSiteDublinCoreCopyrightLink()));
        assertEquals(paramsInfo.getSiteDublinCoreCopyrightLink(), request.getAttribute("dublinCoreCopyrightLink"));

        assertEquals("Example.com - PARENTSITEMENUITEM1 - SITEMENUITEM1", request.getAttribute("title"));

        assertNotSame(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(publicationDate), request.getAttribute("dublinCoreTermsCreated"));

        assertNotSame(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(lastModificationDate), request.getAttribute("dublinCoreTermsModified"));

        contentNode.setPrimaryType("hippostdpubwf:document");
    }

    private class MySEOHelperComponentParamsInfo implements SEOHelperComponentParamsInfo {

        private String documentAttribute = "document";
        private String menuName = "main";
        private String menuAttribute = "menu";
        private String paramsInfoAttribute = "paramsInfo";
        private String documentTitleBeanProperties = "title";
        private String documentKeywordsBeanProperties = "keywords";
        private String documentDescriptionBeanProperties = "description";
        private String documentImageBeanProperties = "image";
        private String siteTitle;
        private String dublinCoreSchemaLink = "http://purl.org/dc/elements/1.1/";
        private String dublinCoreTermsLink = "http://purl.org/dc/terms/";
        private String dublinCoreLanguage;
        private String enableDublinCoreLanguage;
        private String siteDublinCoreCopyrightLink;
        private boolean headersInTemplate;
        private boolean menuItemAllowExpanded;
        private boolean keywordsInDocumentTitle = true;
        private String defaultMetaKeywords = "default meta keywords";
        private String defaultMetaDescription = "default meta description";
        private boolean siteTitleInTemplate = false;
        private String templatePageTitle = "%(siteTitle) - %(menuItem) - %(pageTitle) : %(keywords)";

        public String getDocumentAttribute() {
            return documentAttribute;
        }

        public void setDocumentAttribute(String documentAttribute) {
            this.documentAttribute = documentAttribute;
        }

        public String getMenuName() {
            return menuName;
        }

        public void setMenuName(String menuName) {
            this.menuName = menuName;
        }

        public String getMenuAttribute() {
            return menuAttribute;
        }

        public boolean isMenuItemAllowExpanded() {
            return menuItemAllowExpanded;
        }

        public void setMenuAttribute(String menuAttribute) {
            this.menuAttribute = menuAttribute;
        }

        public String getParamsInfoAttribute() {
            return paramsInfoAttribute;
        }

        public void setParamsInfoAttribute(String paramsInfoAttribute) {
            this.paramsInfoAttribute = paramsInfoAttribute;
        }

        public String getDocumentTitleBeanProperties() {
            return documentTitleBeanProperties;
        }

        public void setDocumentTitleBeanProperties(String documentTitleBeanProperties) {
            this.documentTitleBeanProperties = documentTitleBeanProperties;
        }

        public String getDocumentKeywordsBeanProperties() {
            return documentKeywordsBeanProperties;
        }

        public void setDocumentKeywordsBeanProperties(String documentKeywordsBeanProperties) {
            this.documentKeywordsBeanProperties = documentKeywordsBeanProperties;
        }

        public String getDocumentDescriptionBeanProperties() {
            return documentDescriptionBeanProperties;
        }

        @Override
        public String getDocumentImageBeanProperties() {
            return documentImageBeanProperties;
        }

        public void setDocumentDescriptionBeanProperties(String documentDescriptionBeanProperties) {
            this.documentDescriptionBeanProperties = documentDescriptionBeanProperties;
        }

        public String getSiteTitle() {
            return siteTitle;
        }

        public void setSiteTitle(String siteTitle) {
            this.siteTitle = siteTitle;
        }

        public String getDublinCoreSchemaLink() {
            return dublinCoreSchemaLink;
        }

        public void setDublinCoreSchemaLink(String dublinCoreSchemaLink) {
            this.dublinCoreSchemaLink = dublinCoreSchemaLink;
        }

        public String getDublinCoreTermsLink() {
            return dublinCoreTermsLink;
        }

        public void setDublinCoreTermsLink(String dublinCoreTermsLink) {
            this.dublinCoreTermsLink = dublinCoreTermsLink;
        }

        public String getSiteDublinCoreCopyrightLink() {
            return siteDublinCoreCopyrightLink;
        }

        public void setSiteDublinCoreCopyrightLink(String siteDublinCoreCopyrightLink) {
            this.siteDublinCoreCopyrightLink = siteDublinCoreCopyrightLink;
        }

        public String getDublinCoreLanguage() {
            return dublinCoreLanguage;
        }

        public void setDublinCoreLanguage(String dublinCoreLanguage) {
            this.dublinCoreLanguage = dublinCoreLanguage;
        }

        public String getEnableDublinCoreLanguage() {
            return  enableDublinCoreLanguage;
        }

        public void setEnableDublinCoreLanguage(String enableDublinCoreLanguage) {
            this.enableDublinCoreLanguage = enableDublinCoreLanguage;
        }

        public boolean isHeadersInTemplate() {
            return headersInTemplate;
        }

        public void setHeadersInTemplate(boolean headersInTemplate) {
            this.headersInTemplate = headersInTemplate;
        }

        public void setMenuItemAllowExpanded(boolean menuItemAllowExpanded) {
            this.menuItemAllowExpanded = menuItemAllowExpanded;
        }

        public boolean isKeywordsInDocumentTitle() {
            return keywordsInDocumentTitle;
        }

        public void setKeywordsInDocumentTitle(boolean keywordsInDocumentTitle) {
            this.keywordsInDocumentTitle = keywordsInDocumentTitle;
        }

        public String getDefaultMetaKeywords() {
            return defaultMetaKeywords;
        }

        public String getDefaultMetaDescription() {
            return defaultMetaDescription;
        }

        public boolean isSiteTitleInTemplate() {
            return siteTitleInTemplate;
        }

        public String getTemplatePageTitle() {
            return templatePageTitle;
        }
    }

    public interface Titled {
        String getTitle();
    }

    public interface Descripted {
        String getDescription();
    }

    public interface Keyworded {
        String getKeywords();
    }
}
