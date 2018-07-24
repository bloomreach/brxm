/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

public class ContextMenuBehavior extends AbstractDefaultAjaxBehavior {

    private static final JavaScriptResourceReference CONTEXTMENU_JS = new JavaScriptResourceReference(ContextMenuBehavior.class, "contextmenu.js");

    private boolean shown = false;

    private String getMarkupId() {
        if (getComponent() instanceof Page) {
            return "document.body";
        } else {
            return getComponent().getId();
        }
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        String markupId = getMarkupId();

        response.render(JavaScriptHeaderItem.forReference(CONTEXTMENU_JS));
        final String loadScript = ((getComponent() instanceof Page) ? "document.body" : "Wicket.$('" + markupId + "')")
                + ".onclick = function() { " + getCallbackScript() + " };";
        response.render(OnDomReadyHeaderItem.forScript(loadScript));
        response.render(OnDomReadyHeaderItem.forScript("Hippo.ContextMenu.init();"));
    }

    /**
     * Activate (show) the context menu.  Other open menus will be closed.
     */
    public void activate(IContextMenu active) {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            for (IContextMenu menu : getMenus(false)) {
                if (menu != active) {
                    menu.collapse(target);
                }
            }
            show(target);
        }
    }

    /**
     * Close all open context menu's.
     */
    public void collapseAll() {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        for (IContextMenu menu : getMenus(false)) {
            menu.collapse(target);
        }
        if (target != null) {
            hide(target);
        }
    }

    /**
     * Check context menus.  If a menu is no longer visible, it will be hidden.
     */
    public void checkMenus(AjaxRequestTarget target) {
        if (shown) {
            List<IContextMenu> menus = getMenus(true);
            if (menus.size() == 0) {
                hide(target);
            }
        }
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        for (IContextMenu menu : getMenus(false)) {
            menu.collapse(target);
        }
        hide(target);
    }

    @Override
    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
        final AjaxCallListener ajaxCallListener = new AjaxCallListener();
        ajaxCallListener.onPrecondition("return Hippo.ContextMenu.isShown('" + getMarkupId() + "');");
        attributes.getAjaxCallListeners().add(ajaxCallListener);
        final AjaxChannel channel = attributes.getChannel();
        if (channel != null) {
            attributes.setChannel(channel);
        }
    }

    private void show(AjaxRequestTarget target) {
        target.appendJavaScript("Hippo.ContextMenu.show('" + getMarkupId() + "');");
        shown = true;
    }

    private void hide(AjaxRequestTarget target) {
        target.appendJavaScript("Hippo.ContextMenu.hide('" + getMarkupId() + "');");
        shown = false;
    }

    private List<IContextMenu> getMenus(final boolean visibleOnly) {
        final List<IContextMenu> menus = new LinkedList<>();
        ((MarkupContainer) getComponent()).visitChildren(new IVisitor<Component, Void>() {

            public void component(Component component, IVisit<Void> visit) {
                if (component instanceof IContextMenu) {
                    if (!visibleOnly || component.isVisible()) {
                        menus.add((IContextMenu) component);
                    }
                    visit.dontGoDeeper();
                } else if (component instanceof IContextMenuManager) {
                    visit.dontGoDeeper();
                } else {
                    for (Behavior behavior : component.getBehaviors()) {
                        if (behavior instanceof IContextMenu) {
                            if (!visibleOnly || component.isVisible()) {
                                menus.add((IContextMenu) behavior);
                            }
                            visit.dontGoDeeper();
                        } else if (behavior instanceof IContextMenuManager) {
                            visit.dontGoDeeper();
                        }
                    }
                }
            }

        });
        return menus;
    }

}
