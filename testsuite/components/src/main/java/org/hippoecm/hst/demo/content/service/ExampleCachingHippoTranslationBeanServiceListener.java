/**
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.demo.content.service;

import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.hst.content.service.translation.HippoTranslationBeanService;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;

/**
 * Demonstration purpose, simple JCR Observation Event Listener implementation,
 * simply to invalidate the internal cache of {@link ExampleCachingHippoTranslationBeanService}.
 */
public class ExampleCachingHippoTranslationBeanServiceListener implements EventListener, ComponentManagerAware {

    private ComponentManager componentManager;

    @Override
    public void onEvent(EventIterator events) {
        HippoTranslationBeanService service = componentManager.getComponent(HippoTranslationBeanService.class.getName());

        if (service instanceof CachingHippoTranslationBeanService) {
            ((CachingHippoTranslationBeanService) service).clearCache();
        }
    }

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

}
