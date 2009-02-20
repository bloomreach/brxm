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
package org.hippoecm.hst.configuration;

import javax.jcr.Repository;
import javax.jcr.Session;

public class HstSitesFactory {

    public static HstSites create(Repository repository, String nodePath) throws Exception {
        HstSites sites = null;
        Session session = null;
        
        try {
            session = repository.login();
            sites = new HstSitesService(session.getRootNode().getNode(nodePath));
        } finally {
            if (session != null)
                session.logout();
        }
        
        return sites;
    }
}
