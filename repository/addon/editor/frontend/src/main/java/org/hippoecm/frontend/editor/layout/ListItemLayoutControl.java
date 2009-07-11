/*
 *  Copyright 2009 Hippo.
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

import java.util.List;

import org.hippoecm.frontend.editor.builder.BuilderContext;
import org.hippoecm.frontend.editor.builder.ILayoutAware;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListItemLayoutControl extends LayoutControl {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ListItemLayoutControl.class);

    private List<ListItemLayoutControl> siblings;

    public ListItemLayoutControl(BuilderContext builder, ILayoutAware service, ILayoutPad pad, String wicketId,
            List<ListItemLayoutControl> siblings) {
        super(builder, service, pad, wicketId);
        this.siblings = siblings;
    }

    public ILayoutPad getLayoutPad() {
        return pad;
    }

    @Override
    public void apply(ILayoutTransition transition) {
        if ("up".equals(transition.getName())) {
            moveUp(getSequenceId());
        } else if ("down".equals(transition.getName())) {
            moveUp(getSequenceId() + 1);
        }
    }

    protected void moveUp(int id) {
        IClusterConfig clusterConfig = builder.getTemplate();
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        IPluginConfig previous = null;
        int siblingCount = 0;
        for (IPluginConfig config : plugins) {
            String pluginWicketId = config.getString("wicket.id");
            if (pluginWicketId != null && pluginWicketId.equals(wicketId)) {
                if (siblingCount == id) {
                    if (previous != null) {
                        IPluginConfig backup = new JavaPluginConfig(config);
                        config.clear();
                        config.putAll(previous);

                        previous.clear();
                        previous.putAll(backup);
                    } else {
                        log.warn("Unable to move the first plugin further up");
                    }
                    break;
                } else {
                    previous = config;
                }
                siblingCount++;
            }
        }
    }

    protected int getSequenceId() {
        return siblings.indexOf(this);
    }

}
