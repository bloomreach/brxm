/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

/**
 * Base class for behaviors that want to use YUI modules. It uses a {@link IYuiContext} to register all 
 * required components. The {@link IYuiContext} is created by a (global) {@link IYuiManager} which, in this case, lives
 * inside the {@link Page} (as an {@link Behavior}) that is retrieved by <code>component.getPage()</code>
 *  
 *  <p>
 *  Subclasses should override <code>addHeaderContribution(IYuiContext context)</code> to get access to the 
 *  {@link IYuiContext}.
 *  </p>
 */
public class AbstractYuiBehavior extends Behavior {

    private static final long serialVersionUID = 1L;

    private Component component;
    private IYuiContext context;

    @Override
    public void bind(Component component) {
        super.bind(component);
        this.component = component;
    }

    protected Component getComponent() {
        return component;
    }

    /**
     * Override this method to get access to the IYuiContext
     * 
     * @param context The IYuiContext this behavior can use to register YUI-modules and the likes.
     */
    public void addHeaderContribution(IYuiContext context) {
    }

    /**
     * Don't call super since WicketAjax is loaded by Yui webapp behavior
     * TODO: webapp ajax is configurable, maybe check here and still load it.
     */
    @Override
    public final void renderHead(Component component, IHeaderResponse response) {
        if (context == null) {
            Page page = component.getPage();
            for (Behavior behavior : page.getBehaviors()) {
                if (behavior instanceof IYuiManager) {
                    context = ((IYuiManager) behavior).newContext();
                    addHeaderContribution(context);
                    break;
                }
            }
            if (context == null) {
                throw new IllegalStateException(
                        "Page has no yui manager behavior, unable to register module dependencies.");
            }
        }
        context.renderHead(response);
        onRenderHead(response);
    }
    
    /**
     * Hook method for doing some custom renderHead logic.
     *  
     * @param response
     */
    protected void onRenderHead(IHeaderResponse response) {
    }

}
