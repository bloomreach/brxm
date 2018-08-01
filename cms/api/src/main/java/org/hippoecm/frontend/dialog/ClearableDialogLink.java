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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;

abstract public class ClearableDialogLink extends Panel {

    private final AjaxLink dialogLink;
    private final AjaxLink clearLink;

    public ClearableDialogLink(final String id, final IModel linktext, final IDialogFactory dialogFactory,
                               final IDialogService dialogService) {
        this(id, linktext, new DialogAction(dialogFactory, dialogService));
    }

    public ClearableDialogLink(final String id, final IModel linktext, final DialogAction action) {
        super(id, linktext);

        dialogLink = new AjaxLink("dialog-link") {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                action.execute();
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);

                if(!isClearVisible()) {
                    tag.addBehavior(CssClass.append("linefill"));
                }
            }
        };
        add(dialogLink);
        dialogLink.add(new Label("dialog-link-text", linktext));

        clearLink = new AjaxLink("clear-link") {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                onClear();
            }

            @Override
            public boolean isEnabled() {
                return canClear();
            }

            @Override
            public boolean isVisible() {
                return isClearVisible();
            }
        };
        add(clearLink);
    }

    public void enable() {
        dialogLink.setEnabled(true);
        clearLink.setEnabled(true);
    }

    public void disable() {
        dialogLink.setEnabled(false);
        clearLink.setEnabled(false);
    }

    abstract public void onClear();

    public boolean canClear() {
        return isClearVisible();
    }

    public boolean isClearVisible() {
        return true;
    }
}
