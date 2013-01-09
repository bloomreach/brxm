/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.mock.core.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.jcr.Credentials;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.servlet.ServletContext;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.hippoecm.hst.mock.util.MockBeanTestHelper;
import org.junit.Test;

public class TestMockHstRequestContext {
    
    @Test
    public void testSimpleProperties() throws Exception {
        MockHstRequestContext bean = new MockHstRequestContext();
        
        ServletContext servletContext = EasyMock.createNiceMock(ServletContext.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "servletContext", servletContext);
        
        Session session = EasyMock.createNiceMock(Session.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "session", session);
        
        HstContainerURL baseURL = EasyMock.createNiceMock(HstContainerURL.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "baseURL", baseURL);
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "contextNamespace", "test-contextNamespace");
        
        HstURLFactory urlFactory = EasyMock.createNiceMock(HstURLFactory.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "URLFactory", urlFactory);
        
        ResolvedMount resolvedMount = EasyMock.createNiceMock(ResolvedMount.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "resolvedMount", resolvedMount);
        
        ResolvedSiteMapItem resolvedSiteMapItem = EasyMock.createNiceMock(ResolvedSiteMapItem.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "resolvedSiteMapItem", resolvedSiteMapItem);
        
        HstLinkCreator linkCreator = EasyMock.createNiceMock(HstLinkCreator.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "hstLinkCreator", linkCreator);
        
        HstSiteMapMatcher siteMapMatcher = EasyMock.createNiceMock(HstSiteMapMatcher.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "siteMapMatcher", siteMapMatcher);
        
        HstSiteMenus siteMenus = EasyMock.createNiceMock(HstSiteMenus.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "hstSiteMenus", siteMenus);
        
        HstQueryManagerFactory hstQueryManagerFactory = EasyMock.createNiceMock(HstQueryManagerFactory.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "hstQueryManagerFactory", hstQueryManagerFactory);
        
        Credentials defaultCredentials = EasyMock.createNiceMock(Credentials.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "defaultCredentials", defaultCredentials);
        
        ContainerConfiguration containerConfiguration = EasyMock.createNiceMock(ContainerConfiguration.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "containerConfiguration", containerConfiguration);
        
        ContextCredentialsProvider contextCredentialsProvider = EasyMock.createNiceMock(ContextCredentialsProvider.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "contextCredentialsProvider", contextCredentialsProvider);
        
        Subject subject = new Subject();
        MockBeanTestHelper.verifyReadWriteProperty(bean, "subject", subject);
        
        Locale preferredLocale = Locale.getDefault();
        MockBeanTestHelper.verifyReadWriteProperty(bean, "preferredLocale", preferredLocale);
        
        List<Locale> locales = Arrays.asList(new Locale [] { preferredLocale });
        bean.setLocales(locales);
        Enumeration<Locale> localeEnum = bean.getLocales();
        assertTrue(localeEnum.hasMoreElements());
        assertEquals(preferredLocale, localeEnum.nextElement());
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "pathSuffix", "test-pathSuffix");
        
        VirtualHost virtualHost = EasyMock.createNiceMock(VirtualHost.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "virtualHost", virtualHost);
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "embeddedRequest", true);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "portletContext", true);
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "embeddingContextPath", "test-embeddingContextPath");
        
        ResolvedMount resolvedEmbeddingMount = EasyMock.createNiceMock(ResolvedMount.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "resolvedEmbeddingMount", resolvedEmbeddingMount);
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "embeddingContextPath", "test-targetComponentPath");
    }
    
}
