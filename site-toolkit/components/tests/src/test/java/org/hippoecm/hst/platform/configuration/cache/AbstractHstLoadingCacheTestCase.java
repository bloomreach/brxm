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
package org.hippoecm.hst.platform.configuration.cache;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.platform.HstModelProvider;
import org.hippoecm.hst.platform.api.model.PlatformHstModel;
import org.hippoecm.hst.platform.model.InvalidationMonitor;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;

import static org.joor.Reflect.on;

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

        final HstModelProvider provider = HstServices.getComponentManager().getComponent(HstModelProvider.class);
        final PlatformHstModel hstModel = (PlatformHstModel) provider.getHstModel();

        this.hstNodeLoadingCache = on(hstModel).field("hstNodeLoadingCache").get();
        this.hstConfigurationLoadingCache = on(hstModel).field("hstConfigurationLoadingCache").get();

        final InvalidationMonitor invalidationMonitor = on(hstModel).field("invalidationMonitor").get();

        hstEventsDispatcher = on(invalidationMonitor).field("hstEventsDispatcher").get();

        hstEventsCollector = on(hstEventsDispatcher).field("hstEventsCollector").get();

        // HSTTWO-4354 TODO replace since hstEventsCollector and hstEventsDispatcher and hstModelMutex most likely won't be spring
        // wired any more
        this.hstManager = getComponent(HstManager.class.getName());
        this.hstModelMutex = getComponent("hstModelMutex");

    }

}
