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

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;

public class ContextMenuBehavior extends AbstractDefaultAjaxBehavior {

    private static final long serialVersionUID = 1L;

    private boolean shown = false;

    private String getMarkupId() {
        if (getComponent() instanceof Page) {
            return "document.body";
        } else {
            return getComponent().getId();
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        String markupId = getMarkupId();

        response.renderJavascriptReference(new ResourceReference(ContextMenuBehavior.class, "contextmenu.js"));
        response.renderOnLoadJavascript(((getComponent() instanceof Page) ? "document.body" : "Wicket.$('" + markupId + "')")
                + ".onclick = function() { " + getCallbackScript() + " };");
        response.renderOnDomReadyJavascript("Hippo.ContextMenu.init();");
    }

    /**
     * Activate (show) the context menu.  Other open menus will be closed. 
     */
    public void activate(IContextMenu active) {
        AjaxRequestTarget target = AjaxRequestTarget.get();
        for (IContextMenu menu : getMenus(false)) {
            if (menu != active) {
                menu.collapse(target);
            }
        }
        show(target);
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
    protected CharSequence getPreconditionScript() {
        return "return Hippo.ContextMenu.isShown('" + getMarkupId() + "');";
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        for (IContextMenu menu : getMenus(false)) {
            menu.collapse(target);
        }
        hide(target);
    }

    private void show(AjaxRequestTarget target) {
        target.appendJavascript("Hippo.ContextMenu.show('" + getMarkupId() + "');");
        shown = true;
    }

    private void hide(AjaxRequestTarget target) {
        target.appendJavascript("Hippo.ContextMenu.hide('" + getMarkupId() + "');");
        shown = false;
    }

    private List<IContextMenu> getMenus(final boolean visibleOnly) {
        final List<IContextMenu> menus = new LinkedList<IContextMenu>();
        ((MarkupContainer) getComponent()).visitChildren(new IVisitor() {

            @SuppressWarnings("unchecked")
            public Object component(Component component) {
                if (component instanceof IContextMenu) {
                    if (!visibleOnly || component.isVisible()) {
                        menus.add((IContextMenu) component);
                    }
                    return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                } else if (component instanceof IContextMenuManager) {
                    return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                } else {
                    for (IBehavior behavior : (List<IBehavior>) component.getBehaviors()) {
                        if (behavior instanceof IContextMenu) {
                            if (!visibleOnly || component.isVisible()) {
                                menus.add((IContextMenu) behavior);
                            }
                            return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                        } else if (behavior instanceof IContextMenuManager) {
                            return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                        }
                    }
                }
                return CONTINUE_TRAVERSAL;
            }

        });
        return menus;
    }

}
