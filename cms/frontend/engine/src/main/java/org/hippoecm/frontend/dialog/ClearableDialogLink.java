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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

abstract public class ClearableDialogLink extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private AjaxLink dialogLink;
    private AjaxLink clearLink;

    public ClearableDialogLink(String id, IModel linktext, final IDialogFactory dialogFactory,
            final IDialogService dialogService) {
        this(id, linktext, new DialogAction(dialogFactory, dialogService));
    }

    public ClearableDialogLink(String id, IModel linktext, final DialogAction action) {
        super(id, linktext);

        dialogLink = new AjaxLink("dialog-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                action.execute();
            }
        };
        add(dialogLink);
        dialogLink.add(new Label("dialog-link-text", linktext));

        clearLink = new AjaxLink("clear-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClear();
            }

            @Override
            public boolean isEnabled() {
                return canClear();
            }
            
            @Override
            public boolean isVisible() {
                return isClearVisable();
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
        return isClearVisable();
    }

    public boolean isClearVisable() {
        return true;
    }
}
