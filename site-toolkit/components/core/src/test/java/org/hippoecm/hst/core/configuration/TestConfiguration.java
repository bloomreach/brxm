/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.configuration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hippoecm.hst.configuration.ConfigurationViewUtilities;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.request.BasicHstSiteMapMatcher;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.jpox.util.ViewUtils;
import org.junit.Test;

public class TestConfiguration extends AbstractSpringTestCase {

    protected static final String TESTPROJECT_NAME = "testproject";

    private HstSites hstSites;
    private HstSiteMapMatcher hstSiteMapMatcher;
    private HstSite hstSite;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.hstSites = (HstSites) getComponent(HstSites.class.getName());
        hstSiteMapMatcher = new BasicHstSiteMapMatcher();
        hstSite = hstSites.getSite(TESTPROJECT_NAME);
    }

    /**
     * The hst:sitemap structure we expect the unit test to look like is as follows:
     * 
     * news
     *    `-*
     *      |- january  
     *      `- *
     *         |- *
     *         `- **
     */
    
    @Test
    public void testUrl1(){
        
        ResolvedSiteMapItem res = hstSiteMapMatcher.match("news/2009", hstSite);
        assertTrue("Relative content path for 'news/2009' must be to be 'News/2009'", "News/2009".equals(res.getRelativeContentPath()));
        assertTrue("Param1 must resolve to '2009'", "2009".equals(res.getResolvedProperty("param1")));
        assertNull("Param2 must be null ",res.getResolvedProperty("param2"));
        
    }
    
    @Test
    public void testUrl2(){
        
        ResolvedSiteMapItem res = hstSiteMapMatcher.match("news/2009/february/myArticle", hstSite);
        assertTrue("Relative content path for 'news/2009/february/myArticle' must be to be 'News/2009/february/myArticle'", "News/2009/february/myArticle".equals(res.getRelativeContentPath()));
        assertTrue("Param1 must resolve to  '2009'", "2009".equals(res.getResolvedProperty("param1")));
        assertTrue("Param2 must resolve to  'february'", "february".equals(res.getResolvedProperty("param2")));
        assertTrue("Param3 must resolve to  'myArticle'", "myArticle".equals(res.getResolvedProperty("param3")));
    }
    
    @Test
    public void testUrl3_matchANY(){
        
        StringBuffer buffer = new StringBuffer();
        
        ConfigurationViewUtilities.view(buffer, hstSite.getSiteMap());
        System.out.println(buffer.toString());
        
        ResolvedSiteMapItem res = hstSiteMapMatcher.match("news/2009/february/day2/8oclock/16min/4sec/MyArticle", hstSite);
        System.out.println(res.getRelativeContentPath());
        assertTrue("Relative content path for 'news/2009/february/day2/8oclock/16min/4sec/MyArticle' must be to be 'News/2009/february/day2/8oclock/16min/4sec/MyArticle'", "News/2009/february/day2/8oclock/16min/4sec/MyArticle".equals(res.getRelativeContentPath()));
        assertTrue("Param1 must resolve to  '2009'", "2009".equals(res.getResolvedProperty("param1")));
        assertTrue("Param2 must resolve to  'february'", "february".equals(res.getResolvedProperty("param2")));
        assertTrue("Param3 must resolve to  'day2/8oclock/16min/4sec/MyArticle'", "day2/8oclock/16min/4sec/MyArticle".equals(res.getResolvedProperty("param3")));
    }
    
   

}
