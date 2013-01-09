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

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
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
     * Set up a YUI 'contextmenu' event listener on the component. The callback function is
     * parameterized with the x&y coordinates of the click event.
     * Also stop the contextmenu event from propagating to prevent the browser's contextmenu
     * from rendering. 
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String handler = getCallbackScript().toString();
        String addEvent = "YAHOO.util.Event.addListener('" + getComponent().getMarkupId()
                + "','contextmenu', " +
                " function(env) { var " + MOUSE_X_PARAM + " = YAHOO.util.Event.getPageX(env); " +
                "var " + MOUSE_Y_PARAM + " = YAHOO.util.Event.getPageY(env);" +
                "YAHOO.util.Event.stopEvent(env); " + handler + " });";
        response.renderOnDomReadyJavascript(addEvent);
    }
    
    /**
     * Add the x&y coordinates to the callback url by concatenating them. The endresults should be something like 
     * var wcall=Wicket.Ajax.get('?wicket:interface:etc:etc:etc&mouseX=' + x + '&mosueY= ' + y + ''); 
     * The end single quote is needed because the result of this method is escaped with single quotes, 
     * resulting in the somewhat ugly + ''
     */
    @Override
    public CharSequence getCallbackUrl(boolean onlyTargetActivePage) {
        String url = super.getCallbackUrl(onlyTargetActivePage).toString();
        url += "&" + MOUSE_X_PARAM + "=' + x + '&" + MOUSE_Y_PARAM + "=' + y + '";
        return url;
    }
    
    public void collapse(AjaxRequestTarget target) {
        final MarkupContainer menu = getContextmenu();
        if (menu.isVisible() && getComponentToUpdate().isVisible()) {
            menu.setVisible(false);
            target.addComponent(getComponentToUpdate());
        }
    }
    
    public MarkupContainer getContextmenu() {
        return contextmenu;
    }

    public MarkupContainer getComponentToUpdate() {
        return componentToUpdate;
    }

}
