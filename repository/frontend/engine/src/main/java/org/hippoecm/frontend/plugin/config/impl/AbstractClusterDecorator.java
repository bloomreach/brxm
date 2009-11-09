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

import java.util.Iterator;
import java.util.List;

import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.config.ClusterConfigEvent;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public abstract class AbstractClusterDecorator extends AbstractPluginDecorator implements IClusterConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IObserver observer;
    
    public AbstractClusterDecorator(IClusterConfig upstream) {
        super(upstream);
    }

    protected IClusterConfig getUpstream() {
        return (IClusterConfig) upstream;
    }

    public List<IPluginConfig> getPlugins() {
        return (List<IPluginConfig>) wrap(getUpstream().getPlugins());
    }

    public List<String> getProperties() {
        return getUpstream().getProperties();
    }

    public List<String> getReferences() {
        return getUpstream().getReferences();
    }

    public List<String> getServices() {
        return getUpstream().getServices();
    }

    @Override
    public void startObservation() {
        super.startObservation();
        IObservationContext obContext = getObservationContext();
        obContext.registerObserver(observer = new IObserver() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return upstream;
            }

            public void onEvent(Iterator<? extends IEvent> events) {
                EventCollection<ClusterConfigEvent> collection = new EventCollection<ClusterConfigEvent>();
                while (events.hasNext()) {
                    IEvent event = events.next();
                    if (event instanceof ClusterConfigEvent) {
                        ClusterConfigEvent cce = (ClusterConfigEvent) event;
                        collection.add(new ClusterConfigEvent(AbstractClusterDecorator.this, wrapConfig(cce.getPlugin()), cce.getType()));
                    }
                }
                if (collection.size() > 0) {
                    getObservationContext().notifyObservers(collection);
                }
            }
            
        });
    }

    @Override
    public void stopObservation() {
        IObservationContext obContext = getObservationContext();
        obContext.unregisterObserver(observer);
        super.stopObservation();
    }
}
