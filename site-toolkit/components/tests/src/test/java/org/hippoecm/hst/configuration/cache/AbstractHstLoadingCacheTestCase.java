/*
 *  Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstEventsCollector;
import org.hippoecm.hst.platform.configuration.cache.HstEventsDispatcher;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.test.AbstractTestConfigurations;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_CONFIGURATIONS;

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
        this.hstNodeLoadingCache = new HstNodeLoadingCache(getRepository(), getAdminCredentials(), "/hst:hst");
        this.hstConfigurationLoadingCache = new HstConfigurationLoadingCache(hstNodeLoadingCache,
                "/hst:hst/" + NODENAME_HST_CONFIGURATIONS + "/");

        hstEventsCollector = new HstEventsCollector("/hst:hst");
        hstEventsDispatcher = new HstEventsDispatcher(hstEventsCollector, Arrays.asList(hstConfigurationLoadingCache, hstNodeLoadingCache));

        // HSTTWO-4354 TODO replace since hstEventsCollector and hstEventsDispatcher and hstModelMutex most likely won't be spring
        // wired any more
        this.hstManager = getComponent(HstManager.class.getName());
        this.hstModelMutex = getComponent("hstModelMutex");

    }

}
