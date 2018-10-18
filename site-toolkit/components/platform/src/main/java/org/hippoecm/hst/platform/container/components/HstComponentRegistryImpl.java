/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.container.components;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentMetadata;
import org.hippoecm.hst.core.component.HstComponentMetadataReader;
import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstComponentRegistryImpl
 *
 */
public class HstComponentRegistryImpl implements HstComponentRegistry {
    
    static Logger log = LoggerFactory.getLogger(HstComponentRegistryImpl.class);

    private boolean awaitingTermination = false;

    final private Map<String, HstComponentHolder> hstComponentHolderMap = new ConcurrentHashMap<>();

    public HstComponent getComponent(HstContainerConfig requestContainerConfig, String componentId) {
        HstComponentHolder holder = hstComponentHolderMap.get(componentId);
        
        if (holder != null) {
            return holder.getComponent();
        }
        return null;
    }

    public HstComponentMetadata getComponentMetadata(HstContainerConfig requestContainerConfig, String componentId) {
        HstComponentHolder holder = hstComponentHolderMap.get(componentId);
        
        if (holder != null) {
            return holder.getComponentMetadata();
        }
        return null;
    }

    public synchronized void registerComponent(HstContainerConfig requestContainerConfig, String componentId, HstComponent component) {
        HstComponentMetadata componentMetadata = HstComponentMetadataReader.getHstComponentMetadata(component.getClass());
        HstComponentHolder componentHolder = new HstComponentHolder(component, componentMetadata);
        hstComponentHolderMap.put(componentId, componentHolder);
    }

    public synchronized void unregisterComponent(HstContainerConfig requestContainerConfig, String componentId) {
        HstComponentHolder holder = hstComponentHolderMap.remove(componentId);
        
        if (holder != null) {
            try {
                holder.getComponent().destroy();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception occurred during destroying component: {}", e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception occurred during destroying component: {}", e.toString());
                }
            }
        }
    }
    
    public synchronized void unregisterAllComponents() {
        if (this.hstComponentHolderMap.isEmpty()) {
            return;
        }

        for (HstComponentHolder hstComponentHolder : hstComponentHolderMap.values()) {
            final HstComponent component = hstComponentHolder.getComponent();
            if (component != null) {
                try {
                    component.destroy();
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Exception occurred during destroying component: {}", e.toString(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Exception occurred during destroying component: {}", e.toString());
                    }
                }
            }
        }
        hstComponentHolderMap.clear();

    }

    private static class HstComponentHolder {

        private HstComponent component;
        private HstComponentMetadata componentMetadata;

        private HstComponentHolder(final HstComponent component, final HstComponentMetadata componentMetadata) {
            this.component = component;
            this.componentMetadata = componentMetadata;
        }

        public HstComponent getComponent() {
            return component;
        }

        public HstComponentMetadata getComponentMetadata() {
            return componentMetadata;
        }
    }

    @Override
    public boolean isAwaitingTermination() {
        return awaitingTermination;
    }

    public void setAwaitingTermination(final boolean awaitingTermination) {
        this.awaitingTermination = awaitingTermination;
    }
}
