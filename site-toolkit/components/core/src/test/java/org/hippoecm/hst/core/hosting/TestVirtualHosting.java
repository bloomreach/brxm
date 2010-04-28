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
package org.hippoecm.hst.core.hosting;


import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsManager;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestVirtualHosting extends AbstractSpringTestCase {

        private VirtualHostsManager virtualHostsManager;

        @Override
        public void setUp() throws Exception {
            super.setUp();
            this.virtualHostsManager = getComponent(VirtualHostsManager.class.getName());
        }

     
        @Test
        public void testDefaultHost(){
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                assertTrue("Expected from the hst testcontents default hostname to be 127.0.0.1. ", "127.0.0.1".equals(vhosts.getDefaultHostName()));
                
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
            
        }
        
        
        /*
         *  we configured the following hosts for test content:
         *  
         *  127.0.0.1
         *  localhost
         *  org
         *    ` onehippo
         *            |- www
         *            `- preview
         *          
         */
        @Test
        public void testAllHosts(){
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                List<VirtualHost> allHosts = vhosts.getVirtualHosts(false);
               
                /*
                 * we expect the following hosts now:
                 * 1
                 * 0.1
                 * 0.0.1
                 * 127.0.0.1
                 * localhost
                 * org
                 * onehippo.org
                 * www.onehippo.org
                 * preview.onehippo.org
                 */
                
                 assertTrue("We expect 9 hosts in total", allHosts.size() == 9);
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
            
        }
        
        /*
         * From all the hosts above, actually only the host 127.0.0.1, localhost, www.onehippo.org and preview.onehippo.org do have 
         * a SiteMount. Thus the number of mounted hosts should be 4
         */
        @Test
        public void testMountedHosts(){
            
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                List<VirtualHost> mountedHosts = vhosts.getVirtualHosts(true);
                assertTrue("We expect 4 mounted hosts in total", mountedHosts.size() == 9);
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
            
        }
        
        
        @Test
        public void testFoundHostAndMatch(){
            MockHttpServletRequest request = new MockHttpServletRequest();
            
            request.setLocalPort(8081);
            request.setScheme("http");
            request.setServerName("127.0.0.1");
            request.setPathInfo("/news/2009");
            try {
                VirtualHosts vhosts = virtualHostsManager.getVirtualHosts();
                ResolvedSiteMapItem resolvedSiteMapItem = vhosts.matchSiteMapItem(request);
                
            } catch (RepositoryNotAvailableException e) {
                e.printStackTrace();
            }
        }
        
         
}
