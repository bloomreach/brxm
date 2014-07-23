/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.core.beans;


import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.tool.ContentBeansTool;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.site.HstServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestRequestContextBeansCaching extends AbstractBeanTestCase {

    private DefaultContentBeansTool contentBeansTool;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        HstQueryManagerFactory qmf = getComponent(HstQueryManagerFactory.class.getName());
        contentBeansTool = new DefaultContentBeansTool(qmf);
        contentBeansTool.setAnnotatedClassesResourcePath("classpath*:org/hippoecm/hst/core/beans/**.class");
    }

    @After
    public void tearDown() throws Exception {
        ModifiableRequestContextProvider.clear();
        super.tearDown();
    }

    @Test
    public void testObjectBeanManagersCachedOnRequestContext() throws Exception {

        HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx);
        ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);

        assertTrue(ctx.getObjectBeanManager() == ctx.getObjectBeanManager());

        final ObjectBeanManager ctxOBM1 = ctx.getObjectBeanManager(ctx.getSession());
        final ObjectBeanManager ctxOBM2 = ctx.getObjectBeanManager(ctx.getSession());

        assertTrue(ctxOBM1 == ctxOBM2);

        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
        SimpleCredentials creds = getComponent(Credentials.class.getName() + ".default");
        Session nonDefaultSession = repository.login(creds);

        // although userIds are equal, getting a HstQueryManager or ObjectBeansManager through request context must
        // return different instances for HstQueryManager / ObjectBeansManager since for example in case of
        // faceted navigation with free text search, we need different object bean managers to populate different
        // virtual states, although the backed sessions have same userID
        assertTrue(nonDefaultSession.getUserID().equals(ctx.getSession().getUserID()));

        final ObjectBeanManager nonDefaultOBM1 = ctx.getObjectBeanManager(nonDefaultSession);
        final ObjectBeanManager nonDefaultOBM2 = ctx.getObjectBeanManager(nonDefaultSession);

        assertFalse(nonDefaultOBM1 == ctxOBM1);
        assertTrue(nonDefaultOBM1 == nonDefaultOBM2);

        Session nonDefaultSessionAgain = repository.login(creds);
        final ObjectBeanManager nonDefaultOBMAgain = ctx.getObjectBeanManager(nonDefaultSessionAgain);

        assertFalse(nonDefaultOBM1 == nonDefaultOBMAgain);

    }

   @Test
   public void testQueryManagersCachedOnRequestContext() throws Exception {

       HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
       ModifiableRequestContextProvider.set(ctx);
       ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);
       ((HstMutableRequestContext)ctx).setHstQueryManagerFactory((HstQueryManagerFactory)getComponent(HstQueryManagerFactory.class.getName()));

       assertTrue(ctx.getQueryManager(ctx.getSession()) == ctx.getQueryManager(ctx.getSession()) );
       assertTrue(ctx.getQueryManager() == ctx.getQueryManager());


       final HstQueryManager ctxQM1 = ctx.getQueryManager(ctx.getSession());
       final HstQueryManager ctxQM2 = ctx.getQueryManager(ctx.getSession());

       assertTrue(ctxQM1 == ctxQM2);

       Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
       SimpleCredentials creds = getComponent(Credentials.class.getName() + ".default");
       Session nonDefaultSession = repository.login(creds);

       // although userIds are equal, getting a HstQueryManager or ObjectBeansManager through request context must
       // return different instances for HstQueryManager / ObjectBeansManager since for example in case of
       // faceted navigation with free text search, we need different object bean managers to populate different
       // virtual states, although the backed sessions have same userID
       assertTrue(nonDefaultSession.getUserID().equals(ctx.getSession().getUserID()));

       final HstQueryManager nonDefaultQM1 = ctx.getQueryManager(nonDefaultSession);
       final HstQueryManager nonDefaultQM2 = ctx.getQueryManager(nonDefaultSession);

       assertFalse(nonDefaultQM1 == ctxQM1);
       assertTrue(nonDefaultQM1 == nonDefaultQM2);

       Session nonDefaultSessionAgain = repository.login(creds);
       final HstQueryManager nonDefaultQMAgain = ctx.getQueryManager(nonDefaultSessionAgain);

       assertFalse(nonDefaultQM1 == nonDefaultQMAgain);

   }


    @Test
    public void testContentBeansNotRequestScopeCached() throws Exception {

        HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx);
        ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);

        assertFalse(ctx.getContentBean() == ctx.getContentBean());
        assertFalse(ctx.getSiteContentBaseBean() == ctx.getSiteContentBaseBean());

    }

    @Test
    public void testContentBeansRequestScopeCached() throws Exception {

        HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx);
        ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);
        ((HstMutableRequestContext)ctx).setCachingObjectConverter(true);

        assertTrue(ctx.getContentBean() == ctx.getContentBean());
        assertTrue(ctx.getSiteContentBaseBean() == ctx.getSiteContentBaseBean());

    }

    @Test
    public void testChildContentBeansNotRequestScopeCached() throws Exception {

        HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx);
        // contentBeansTool has a non caching object converter
        ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);

        final HippoBean siteContentBaseBean = ctx.getSiteContentBaseBean();
        assertFalse(siteContentBaseBean == ctx.getSiteContentBaseBean());

        final Object news = siteContentBaseBean.getBean("News");
        assertNotNull(news);
        assertFalse(siteContentBaseBean.getBean("News") == siteContentBaseBean.getBean("News"));
        assertNotNull(siteContentBaseBean.getBean("News/2009"));
        assertFalse(siteContentBaseBean.getBean("News/2009") == siteContentBaseBean.getBean("News/2009"));

    }

    @Test
    public void testChildContentBeansRequestScopeCached() throws Exception {

        HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx);
        // contentBeansTool has a non caching object converter
        ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);
        ((HstMutableRequestContext)ctx).setCachingObjectConverter(true);

        final HippoBean siteContentBaseBean = ctx.getSiteContentBaseBean();
        assertTrue(siteContentBaseBean == ctx.getSiteContentBaseBean());

        final Object news = siteContentBaseBean.getBean("News");
        assertNotNull(news);
        assertTrue(siteContentBaseBean.getBean("News") == siteContentBaseBean.getBean("News"));
        assertNotNull(siteContentBaseBean.getBean("News/2009"));
        assertTrue(siteContentBaseBean.getBean("News/2009") == siteContentBaseBean.getBean("News/2009"));

        // second request
        HstRequestContext ctx2 = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx2);// contentBeansTool now with caching object mananger
        ((HstMutableRequestContext)ctx2).setContentBeansTool(contentBeansTool);
        final HippoBean siteContentBaseBean2 = ctx2.getSiteContentBaseBean();
        assertFalse(siteContentBaseBean == siteContentBaseBean2);
        final HippoBean news2 = siteContentBaseBean2.getBean("News");
        assertFalse(news == news2);
    }

    @Test
    public void testAncestorContentBeansNotRequestScopeCached() throws Exception {

        HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx);
        // contentBeansTool now with caching object mananger
        ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);

        final HippoBean contentBean = ctx.getContentBean();
        assertFalse(contentBean == ctx.getContentBean());

        final HippoBean parentBean = contentBean.getParentBean();
        assertNotNull(parentBean);
        assertFalse(parentBean == contentBean.getParentBean());

    }

    @Test
    public void testAncestorContentBeansRequestScopeCached() throws Exception {

        HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx);
        // contentBeansTool now with caching object mananger
        ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);
        ((HstMutableRequestContext)ctx).setCachingObjectConverter(true);

        final HippoBean contentBean = ctx.getContentBean();
        assertTrue(contentBean == ctx.getContentBean());

        final HippoBean parentBean = contentBean.getParentBean();
        assertNotNull(parentBean);
        assertTrue(parentBean == contentBean.getParentBean());

        // second request
        HstRequestContext ctx2 = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx2);// contentBeansTool now with caching object mananger
        ((HstMutableRequestContext)ctx2).setContentBeansTool(contentBeansTool);
        final HippoBean contentBean2 = ctx2.getContentBean();
        assertFalse(contentBean == contentBean2);
        final HippoBean parentBean2 = contentBean2.getParentBean();
        assertFalse(parentBean == parentBean2);
    }

    @Test
    public void testSearchResultBeansAreNotRequestScopeCached() throws Exception {
        HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx);
        // contentBeansTool now with caching object mananger
        ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);
        ((HstMutableRequestContext)ctx).setHstQueryManagerFactory((HstQueryManagerFactory)getComponent(HstQueryManagerFactory.class.getName()));

        final HippoBeanIterator hippoBeans1 = ctx.getQueryManager().createQuery(ctx.getSiteContentBaseBean()).execute().getHippoBeans();
        final HippoBeanIterator hippoBeans2 = ctx.getQueryManager().createQuery(ctx.getSiteContentBaseBean()).execute().getHippoBeans();
        while (hippoBeans1.hasNext()) {
            assertFalse(hippoBeans1.nextHippoBean() == hippoBeans2.nextHippoBean());
        }
    }

    @Test
      public void testSearchResultBeansAreRequestScopeCached() throws Exception {
        HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx);
        // contentBeansTool now with caching object mananger
        ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);
        ((HstMutableRequestContext)ctx).setCachingObjectConverter(true);
        ((HstMutableRequestContext)ctx).setHstQueryManagerFactory((HstQueryManagerFactory)getComponent(HstQueryManagerFactory.class.getName()));

        final HippoBeanIterator hippoBeans1 = ctx.getQueryManager().createQuery(ctx.getSiteContentBaseBean()).execute().getHippoBeans();
        final HippoBeanIterator hippoBeans2 = ctx.getQueryManager().createQuery(ctx.getSiteContentBaseBean()).execute().getHippoBeans();
        while (hippoBeans1.hasNext()) {
            assertTrue(hippoBeans1.nextHippoBean() == hippoBeans2.nextHippoBean());
        }
    }

}
