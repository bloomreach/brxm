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

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IClusterConfigListener;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public abstract class AbstractClusterDecorator extends AbstractPluginDecorator implements IClusterConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private List<IClusterConfigListener> cclisteners;

    public AbstractClusterDecorator(IClusterConfig upstream) {
        super(upstream);
        this.cclisteners = new LinkedList<IClusterConfigListener>();
        upstream.addClusterConfigListener(new IClusterConfigListener() {
            private static final long serialVersionUID = 1L;

            public void onPluginAdded(IPluginConfig config) {
                for (IClusterConfigListener listener : cclisteners) {
                    listener.onPluginAdded(wrapConfig(config));
                }
            }

            public void onPluginChanged(IPluginConfig config) {
                for (IClusterConfigListener listener : cclisteners) {
                    listener.onPluginChanged(wrapConfig(config));
                }
            }

            public void onPluginRemoved(IPluginConfig config) {
                for (IClusterConfigListener listener : cclisteners) {
                    listener.onPluginRemoved(wrapConfig(config));
                }
            }
            
        });
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

    public void addClusterConfigListener(IClusterConfigListener listener) {
        cclisteners.add(listener);
    }

    public void removeClusterConfigListener(IClusterConfigListener listener) {
        cclisteners.remove(listener);
    }

}
