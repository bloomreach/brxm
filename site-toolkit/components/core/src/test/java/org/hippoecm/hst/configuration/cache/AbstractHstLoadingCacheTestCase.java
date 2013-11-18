/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.cache;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;

public abstract class AbstractHstLoadingCacheTestCase extends AbstractTestConfigurations {

    protected HstNodeLoadingCache hstNodeLoadingCache;
    protected HstConfigurationLoadingCache hstConfigurationLoadingCache;
    protected HstEventsCollector hstEventsCollector;
    protected HstManager hstManager;
    protected HstEventsDispatcher hstEventsDispatcher;
    protected Object hstModelMutex;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.hstConfigurationLoadingCache = getComponent(HstConfigurationLoadingCache.class.getName());
        this.hstNodeLoadingCache = getComponent(HstNodeLoadingCache.class.getName());
        this.hstEventsCollector = getComponent("hstEventsCollector");
        this.hstEventsDispatcher =  getComponent("hstEventsDispatcher");
        this.hstManager = getComponent(HstManager.class.getName());
        this.hstModelMutex = getComponent("hstModelMutex");
    }

    protected Session createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

}
