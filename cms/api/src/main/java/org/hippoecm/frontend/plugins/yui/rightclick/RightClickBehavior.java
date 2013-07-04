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
package org.hippoecm.frontend.plugins.yui.rightclick;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.hippoecm.frontend.behaviors.IContextMenu;

/**
 * This behavior adds a right-click event-handler to the component.
 */
public abstract class RightClickBehavior extends AbstractDefaultAjaxBehavior implements IContextMenu {

    private static final long serialVersionUID = 1L;

    public static final String MOUSE_X_PARAM = "x";
    public static final String MOUSE_Y_PARAM = "y";

    MarkupContainer contextmenu;
    MarkupContainer componentToUpdate;

    public RightClickBehavior(MarkupContainer contextmenu, MarkupContainer componentToUpdate) {
        this.contextmenu = contextmenu;
        this.componentToUpdate = componentToUpdate;
    }

    /**
     * Set up a YUI 'contextmenu' event listener on the component. The callback function is parameterized with the x&y
     * coordinates of the click event. Also stop the contextmenu event from propagating to prevent the browser's
     * contextmenu from rendering.
     */
    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);

        String attributesAsJson = renderAjaxAttributes(component).toString();
        String addEvent =
                "YAHOO.util.Event.addListener('" + component.getMarkupId() + "','contextmenu', " +
                        "function(env) {\n" +
                        "  var x = YAHOO.util.Event.getPageX(env),\n" +
                        "      y = YAHOO.util.Event.getPageY(env),\n" +
                        "      call = new Wicket.Ajax.Call(),\n" +
                        "      attributes = jQuery.extend({}, " + attributesAsJson + ");\n"+
                        "  YAHOO.util.Event.stopEvent(env);\n" +
                        "  call.ajax(attributes);\n" +
                        "});";
        response.render(OnDomReadyHeaderItem.forScript(addEvent));
    }

    @Override
    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        final List<CharSequence> dep = attributes.getDynamicExtraParameters();
        dep.add("return { " + MOUSE_X_PARAM + ": x , " + MOUSE_Y_PARAM + ": y };");
    }

    public void collapse(AjaxRequestTarget target) {
        final MarkupContainer menu = getContextmenu();
        if (menu.isVisible() && getComponentToUpdate().isVisible()) {
            menu.setVisible(false);
            target.add(getComponentToUpdate());
        }
    }

    public MarkupContainer getContextmenu() {
        return contextmenu;
    }

    public MarkupContainer getComponentToUpdate() {
        return componentToUpdate;
    }

}
