/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HstLinkAllRewritingIT extends AbstractHstLinkRewritingIT {

    @Test
    public void all_links_for_news_article_within_request_mount_sorted_on_length() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node newsArticle = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
        final List<HstLink> allLinks = linkCreator.createAll(newsArticle, requestContext, false);
        assertEquals("http://localhost/site/news/News1.html", allLinks.get(0).toUrlForm(requestContext, true));
        assertEquals("http://localhost/site/alsonews/news2/News1.html", allLinks.get(1).toUrlForm(requestContext, true));
    }


    @Test
    public void all_links_for_news_folder_within_request_mount_sorted_on_length() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node newsFolder = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/2009");
        final List<HstLink> allLinks = linkCreator.createAll(newsFolder, requestContext, false);
        assertEquals("http://localhost/site/news/2009", allLinks.get(0).toUrlForm(requestContext, true));
        assertEquals("http://localhost/site/alsonews/news2/2009", allLinks.get(1).toUrlForm(requestContext, true));
    }

    @Test
    public void all_links_for_news_article_cross_mount_sorted_on_length() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node newsArticle = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
        final List<HstLink> allLinks = linkCreator.createAll(newsArticle, requestContext, true);
        assertEquals("http://localhost/site/news/News1.html", allLinks.get(0).toUrlForm(requestContext, true));
        // note the mount is 'site2/intranet' hence the path 'news/News1.html' is shorter than 'alsonews/news2/News1.html'
        assertEquals("http://localhost/site2/intranet/news/News1.html", allLinks.get(1).toUrlForm(requestContext, true));
        assertEquals("http://localhost/site/alsonews/news2/News1.html", allLinks.get(2).toUrlForm(requestContext, true));
        assertEquals("http://localhost/site2/intranet/alsonews/news2/News1.html", allLinks.get(3).toUrlForm(requestContext, true));
    }

    @Test
    public void all_links_for_news_folder_cross_mount_sorted_on_length() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node newsFolder = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/2009");
        final List<HstLink> allLinks = linkCreator.createAll(newsFolder, requestContext, true);
        assertEquals("http://localhost/site/news/2009", allLinks.get(0).toUrlForm(requestContext, true));
        // note the mount is 'site2/intranet' hence the path 'news/News1.html' is shorter than 'alsonews/news2/News1.html'
        assertEquals("http://localhost/site2/intranet/news/2009", allLinks.get(1).toUrlForm(requestContext, true));
        assertEquals("http://localhost/site/alsonews/news2/2009", allLinks.get(2).toUrlForm(requestContext, true));
        assertEquals("http://localhost/site2/intranet/alsonews/news2/2009", allLinks.get(3).toUrlForm(requestContext, true));
    }


    @Test
    public void all_links_never_context_aware() throws Exception {
        // even though the current context resolves */foo/** the createAll still does not return the resolved URL
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/newsCtxOnly/foo");
        Node newsArticle = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
        final List<HstLink> allLinks = linkCreator.createAll(newsArticle, requestContext, false);
        assertEquals("http://localhost/site/news/News1.html", allLinks.get(0).toUrlForm(requestContext, true));
        assertEquals("http://localhost/site/alsonews/news2/News1.html", allLinks.get(1).toUrlForm(requestContext, true));

    }
}