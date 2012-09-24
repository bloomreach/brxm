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
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;

public class DialogLink extends Panel {
    private static final long serialVersionUID = 1L;

    protected AjaxLink link;

    public DialogLink(String id, IModel linktext, final IDialogFactory dialogFactory, final IDialogService dialogService) {
        this(id, linktext, new DialogAction(dialogFactory, dialogService));
    }

    public DialogLink(String id, IModel linktext, final DialogAction action) {
        super(id, linktext);

        setOutputMarkupId(true);
        link = new AjaxLink("dialog-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                action.execute();
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                // don't let event propagate any further; the original page is invalid
                // when the dialog is opened.
                return new EventStoppingDecorator(super.getAjaxCallDecorator());
            }
        };

        add(link);
        link.add(new Label("dialog-link-text", linktext));
    }

    public void enable() {
        link.setEnabled(true);
    }

    public void disable() {
        link.setEnabled(false);
    }

}
