/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.section;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.util.string.JavascriptUtils;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public abstract class OnEnterAjaxBehavior extends AjaxFormSubmitBehavior {

    private static final long serialVersionUID = 1L;

    private IYuiContext _helper;

    public OnEnterAjaxBehavior() {
        super("onchange");
    }

    /**
     * Don't call super since WicketAjax is loaded by Yui webapp behavior
     * TODO: webapp ajax is configurable, maybe check here and still load it.
     */
    @Override
    public final void renderHead(IHeaderResponse response) {
        if (_helper == null) {
            Component component = getComponent();
            Page page = component.getPage();
            for (IBehavior behavior : page.getBehaviors()) {
                if (behavior instanceof IYuiManager) {
                    _helper = ((IYuiManager) behavior).newContext();
                    _helper.addJavascriptReference(new ResourceReference(OnEnterAjaxBehavior.class, "enter.js"));
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

    @Override
    protected CharSequence getPreconditionScript() {
        return "return Wicket.$$('" + getComponent().getMarkupId() + "') && Wicket.$$('" + getForm().getMarkupId() + "')";
    }
}
