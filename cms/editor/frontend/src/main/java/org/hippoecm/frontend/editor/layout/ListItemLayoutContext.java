/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.layout;

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.editor.builder.BuilderContext;
import org.hippoecm.frontend.editor.builder.ILayoutAware;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListItemLayoutContext extends LayoutContext {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ListItemLayoutContext.class);

    private List<ListItemLayoutContext> siblings;

    public ListItemLayoutContext(BuilderContext builder, ILayoutAware service, ListItemPad pad, String wicketId, List<ListItemLayoutContext> siblings) {
        super(builder, service, pad, wicketId);
        this.siblings = siblings;
    }

    @Override
    public void apply(ILayoutTransition transition) {
        ListItemPad lip = (ListItemPad) pad;
        if (lip.getUpName().equals(transition.getName())) {
            moveUp(getSequenceId());
        } else if (lip.getDownName().equals(transition.getName())) {
            moveUp(getSequenceId() + 1);
        } else {
            super.apply(transition);
        }
    }

    protected void moveUp(int id) {
        IClusterConfig clusterConfig = builder.getTemplate();
        List<IPluginConfig> plugins = new LinkedList<IPluginConfig>(clusterConfig.getPlugins());
        int previous = -1;
        int siblingCount = 0;
        int index = 0;
        for (IPluginConfig config : plugins) {
            String pluginWicketId = config.getString("wicket.id");
            if (pluginWicketId != null && pluginWicketId.equals(wicketId)) {
                if (siblingCount == id) {
                    if (previous != -1) {
                        plugins.add(previous, plugins.remove(index));
                        if (index != (previous + 1)) {
                            plugins.add(index, plugins.remove(previous + 1));
                        }
                    } else {
                        log.warn("Unable to move the first plugin further up");
                    }
                    break;
                } else {
                    previous = index;
                }
                siblingCount++;
            }
            index++;
        }
        clusterConfig.setPlugins(plugins);
    }

    protected IPluginConfig getEditablePluginConfig() {
        int id = getSequenceId();
        IClusterConfig clusterConfig = builder.getTemplate();
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        int siblingCount = 0;
        for (IPluginConfig config : plugins) {
            String pluginWicketId = config.getString("wicket.id");
            if (pluginWicketId != null && pluginWicketId.equals(wicketId)) {
                if (siblingCount == id) {
                    return config;
                }
                siblingCount++;
            }
        }
        return null;
    }
    
    protected boolean isPadOccupied(ILayoutPad pad) {
        String wicketId = LayoutHelper.getWicketId(pad);
        IClusterConfig clusterConfig = builder.getTemplate();
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        for (IPluginConfig config : plugins) {
            if (wicketId.equals(config.getString("wicket.id"))) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected void reparent(ILayoutPad target) {
        IPluginConfig config = getEditablePluginConfig();
        if (config != null) {
            if (target.isList()) {
                config.put("wicket.id", LayoutHelper.getWicketId(target) + ".item");
            } else {
                if (!isPadOccupied(target)) {
                    config.put("wicket.id", LayoutHelper.getWicketId(target));
                } else {
                    log.warn("Target is already occupied");
                }
            }
        } else {
            log.warn("No plugin config found");
        }
    }
    
    protected int getSequenceId() {
        return siblings.indexOf(this);
    }

}
