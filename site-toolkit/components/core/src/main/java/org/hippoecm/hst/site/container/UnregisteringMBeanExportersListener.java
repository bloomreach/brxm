/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.container;

import java.util.Map;

import com.google.common.eventbus.Subscribe;

import org.apache.commons.collections.MapUtils;
import org.hippoecm.hst.container.event.ComponentManagerBeforeReplacedEvent;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UnregisteringMBeanExportersListener
 * <P>
 * A component bean, responsible for unregistering MBeanExporters before the component manager is replaced by a new one.
 * </P>
 * <P>
 * Note: we need to unregister MBeans first from the old component manager
 * because old component manager will be destroyed after the new component manager is initialized
 * and old component manager will trigger unregistering the newly registered MBeans when destroying.
 * </P>
 */
public class UnregisteringMBeanExportersListener implements ComponentManagerAware {

    private static Logger log = LoggerFactory.getLogger(UnregisteringMBeanExportersListener.class);

    private ComponentManager componentManager;

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public void init() {
        componentManager.registerEventSubscriber(this);
    }

    public void destroy() {
        componentManager.unregisterEventSubscriber(this);
    }

    @Subscribe
    public void onComponentManagerBeforeReloadedEvent(ComponentManagerBeforeReplacedEvent event) {
        try {
            Map<String, UnregisterableMBeanExporter> unregisterableMBeanExportersMap = componentManager.getComponentsOfType(UnregisterableMBeanExporter.class);
            
            if (!MapUtils.isEmpty(unregisterableMBeanExportersMap)) {
                for (Map.Entry<String, UnregisterableMBeanExporter> entry : unregisterableMBeanExportersMap.entrySet()) {
                    log.info("Unregistering MBeans from the exporter, '{}'.", entry.getKey());
                    entry.getValue().unregisterBeans();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to unregister MBeans from the old component manager.", e);
        }
    }

}
