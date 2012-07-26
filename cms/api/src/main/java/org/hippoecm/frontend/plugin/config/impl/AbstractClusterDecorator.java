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
package org.hippoecm.frontend.plugin.config.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.config.ClusterConfigEvent;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public abstract class AbstractClusterDecorator extends AbstractPluginDecorator implements IClusterConfig {

    private static final long serialVersionUID = 1L;

    private IObserver<IClusterConfig> observer;
    
    public AbstractClusterDecorator(IClusterConfig upstream) {
        super(upstream);
    }

    protected IClusterConfig getUpstream() {
        return (IClusterConfig) upstream;
    }

    @SuppressWarnings("unchecked")
    public List<IPluginConfig> getPlugins() {
        return (List<IPluginConfig>) wrap(getUpstream().getPlugins());
    }

    public void setPlugins(List<IPluginConfig> plugins) {
        throw new UnsupportedOperationException("Modifying a readonly cluster configuration");
    }

    public final List<String> getProperties() {
        return Collections.unmodifiableList(getUpstream().getProperties());
    }

    public final List<String> getReferences() {
        return Collections.unmodifiableList(getUpstream().getReferences());
    }

    public final List<String> getServices() {
        return Collections.unmodifiableList(getUpstream().getServices());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected IObservationContext<IClusterConfig> getObservationContext() {
        return (IObservationContext<IClusterConfig>) super.getObservationContext();
    }
    
    @Override
    public void startObservation() {
        super.startObservation();
        final IObservationContext<IClusterConfig> obContext = getObservationContext();
        obContext.registerObserver(observer = new IObserver<IClusterConfig>() {
            private static final long serialVersionUID = 1L;

            public IClusterConfig getObservable() {
                return getUpstream();
            }

            public void onEvent(Iterator<? extends IEvent<IClusterConfig>> events) {
                EventCollection<IEvent<IClusterConfig>> collection = new EventCollection<IEvent<IClusterConfig>>();
                while (events.hasNext()) {
                    IEvent<IClusterConfig> event = events.next();
                    if (event instanceof ClusterConfigEvent) {
                        ClusterConfigEvent cce = (ClusterConfigEvent) event;
                        collection.add(new ClusterConfigEvent(AbstractClusterDecorator.this, wrapConfig(cce.getPlugin()), cce.getType()));
                    }
                }
                if (collection.size() > 0) {
                    obContext.notifyObservers(collection);
                }
            }
            
        });
    }

    @Override
    public void stopObservation() {
        IObservationContext<IClusterConfig> obContext = getObservationContext();
        obContext.unregisterObserver(observer);
        super.stopObservation();
    }
}
