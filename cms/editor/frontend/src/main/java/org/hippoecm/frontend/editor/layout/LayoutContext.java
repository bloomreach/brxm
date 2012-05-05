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

import org.hippoecm.frontend.editor.builder.BuilderContext;
import org.hippoecm.frontend.editor.builder.ILayoutAware;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Context for layout editing plugins.  It implements the ILayoutControl interface
 * using the plugin.id config variable.
 */
public class LayoutContext implements ILayoutContext {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected final BuilderContext builder;
    protected final ILayoutPad pad;
    protected final String wicketId; // undecorated wicket.id
    private ILayoutAware service;

    public LayoutContext(BuilderContext builder, ILayoutAware service, ILayoutPad pad, String wicketId) {
        this.builder = builder;
        this.service = service;
        this.pad = pad;
        this.wicketId = wicketId;
    }

    public ILayoutAware getService() {
        return service;
    }

    public ILayoutPad getLayoutPad() {
        return pad;
    }

    public void apply(ILayoutTransition transition) {
        reparent(transition.getTarget());
    }

    protected void reparent(ILayoutPad target) {
        IPluginConfig config = builder.getEditablePluginConfig();
        config.put("wicket.id", LayoutHelper.getWicketId(target));
    }

}
