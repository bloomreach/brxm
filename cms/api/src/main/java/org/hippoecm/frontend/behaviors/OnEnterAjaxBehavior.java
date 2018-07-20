/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public abstract class OnEnterAjaxBehavior extends AjaxFormSubmitBehavior {

    private static final long serialVersionUID = 1L;

    private IYuiContext _helper;

    public OnEnterAjaxBehavior() {
        super("enter");
    }

    /**
     * Don't call super since WicketAjax is loaded by Yui webapp behavior
     * TODO: webapp ajax is configurable, maybe check here and still load it.
     */
    @Override
    public final void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);

        if (_helper == null) {
            Page page = component.getPage();
            for (Behavior behavior : page.getBehaviors()) {
                if (behavior instanceof IYuiManager) {
                    _helper = ((IYuiManager) behavior).newContext();
                    _helper.addJavascriptReference(new JavaScriptResourceReference(OnEnterAjaxBehavior.class, "enter.js"));
                    break;
                }
            }
            if (_helper == null) {
                throw new IllegalStateException("Page has no yui manager behavior, unable to register module dependencies.");
            }
            _helper.addOnDomLoad("new Hippo.EnterHandler('" + component.getMarkupId() + "')");
        }
        _helper.renderHead(response);
    }
}
