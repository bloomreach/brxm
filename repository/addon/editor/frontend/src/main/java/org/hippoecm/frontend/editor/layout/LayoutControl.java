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

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.editor.builder.BuilderContext;
import org.hippoecm.frontend.editor.builder.ILayoutAware;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context for layout editing plugins.  It implements the ILayoutControl interface
 * using the plugin.id config variable.
 */
public class LayoutControl implements ILayoutControl {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(LayoutControl.class);

    public static final String WICKET_ID = "wicket.id";

    protected final BuilderContext builder;
    protected final ILayoutPad pad;
    protected final String wicketId; // undecorated wicket.id
    private ILayoutAware service;

    public LayoutControl(BuilderContext builder, ILayoutAware service, ILayoutPad pad, String wicketId) {
        this.builder = builder;
        this.service = service;
        this.pad = pad;
        this.wicketId = wicketId;
    }

    public ILayoutAware getService() {
        return service;
    }

    public List<ILayoutTransition> getTransitions() {
        List<String> upstream = pad.getTransitions();
        List<ILayoutTransition> transitions = new ArrayList<ILayoutTransition>(upstream.size());
        for (String key : upstream) {
            transitions.add(pad.getTransition(key));
        }
        return transitions;
    }

    public void apply(ILayoutTransition transition) {
        reparent(transition.getTarget().getName());
    }

    protected void reparent(String target) {
        IPluginConfig config = builder.getEditablePluginConfig();
        config.put("wicket.id", target);
    }

}
