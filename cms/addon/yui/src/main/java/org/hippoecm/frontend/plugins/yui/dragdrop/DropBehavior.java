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
package org.hippoecm.frontend.plugins.yui.dragdrop;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public abstract class DropBehavior extends AbstractDragDropBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public DropBehavior(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected String getHeaderContributorFilename() {
        return "Drop.js";
    }
    
    @Override
    protected Class<? extends IBehavior> getHeaderContributorClass() {
        return DropBehavior.class;
    }
    
    @Override
    protected void respond(AjaxRequestTarget target) {
    }
    
    public abstract void onDrop(IModel model, AjaxRequestTarget target);
    
    
    public String getComponentMarkupId() {
        return getComponent().getMarkupId();
    }
    
}
