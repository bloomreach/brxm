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
package org.hippoecm.frontend.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;

public class ContextMenuBehavior extends AbstractDefaultAjaxBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String markupId = null;
    private IContextMenuManager manager;

    public void setShown(boolean isShown, String id, AjaxRequestTarget target) {
        if (isShown) {
            target.appendJavascript("Hippo.ContextMenu.show('" + markupId + "', '" + id + "');");
        } else {
            target.appendJavascript("Hippo.ContextMenu.hide('" + markupId + "');");
        }
    }

    @Override
    protected void onBind() {
        Component component = getComponent();
        assert (component instanceof IContextMenuManager);
        this.manager = (IContextMenuManager) component;

        if (getComponent() instanceof Page) {
            this.markupId = null;
        } else {
            this.markupId = component.getId();
        }
    }

    @Override
    protected CharSequence getPreconditionScript() {
        return "return Hippo.ContextMenu.isShown('" + markupId + "');";
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.renderJavascriptReference(new ResourceReference(ContextMenuBehavior.class, "contextmenu.js"));
        response.renderOnLoadJavascript((markupId == null ? "document.body" : "Wicket.$('" + markupId + "')")
                + ".onclick = function() { " + getCallbackScript() + " };");
        response.renderOnDomReadyJavascript("Hippo.ContextMenu.init();");

    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        manager.collapse(null, target);
    }

}
