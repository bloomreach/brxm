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
import static org.junit.Assert.assertTrue;

public class TestRequestContextBeansCaching extends AbstractBeanTestCase {

    private ContentBeansTool contentBeansTool;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        contentBeansTool = new DefaultContentBeansTool() {
            @Override
            public ObjectConverter getObjectConverter() {
                return TestRequestContextBeansCaching.this.getObjectConverter();
            }
        };
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
    public void testContentBeansRequestScopeCached() throws Exception {

        HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ModifiableRequestContextProvider.set(ctx);
        ((HstMutableRequestContext)ctx).setContentBeansTool(contentBeansTool);

        assertTrue(ctx.getContentBean() == ctx.getContentBean());
        assertTrue(ctx.getSiteContentBaseBean() == ctx.getSiteContentBaseBean());

        // TODO below is not yet cached
//        assertTrue(ctx.getContentBean().getParentBean() == ctx.getContentBean().getParentBean());
//        assertTrue(ctx.getContentBean().getParentBean().getBean("news") == ctx.getContentBean().getParentBean().getBean("news"));
//        assertTrue(ctx.getContentBean().getParentBean().getBean("news/2009") == ctx.getContentBean().getParentBean().getBean("news/2009"));

    }

}
