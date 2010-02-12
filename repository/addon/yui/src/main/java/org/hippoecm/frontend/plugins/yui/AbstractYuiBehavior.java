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
package org.hippoecm.frontend.plugins.yui;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public class AbstractYuiBehavior extends AbstractBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Component component;
    private IYuiContext _helper;

    @Override
    public void bind(Component component) {
        super.bind(component);
        this.component = component;
    }

    /**
     * Override to implement header contrib
     * @param helper
     */
    public void addHeaderContribution(IYuiContext helper) {
    }

    /**
     * Don't call super since WicketAjax is loaded by Yui webapp behavior
     * TODO: webapp ajax is configurable, maybe check here and still load it.
     */
    @Override
    public final void renderHead(IHeaderResponse response) {
        if (_helper == null) {
            Page page = component.getPage();
            for (IBehavior behavior : page.getBehaviors()) {
                if (behavior instanceof IYuiManager) {
                    _helper = ((IYuiManager) behavior).newContext();
                    addHeaderContribution(_helper);
                    break;
                }
            }
            if (_helper == null) {
                throw new IllegalStateException("Page has no yui manager behavior, unable to register module dependencies.");
            }
        }
        onRenderHead(response);
        _helper.renderHead(response);
    }

    protected void onRenderHead(IHeaderResponse response) {
    }
    
}
