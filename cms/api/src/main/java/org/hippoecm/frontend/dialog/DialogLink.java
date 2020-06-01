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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;
import static org.apache.wicket.ajax.attributes.AjaxRequestAttributes.EventPropagation.BUBBLE;

public class DialogLink extends Panel {
    private static final long serialVersionUID = 1L;

    protected AjaxLink link;

    public DialogLink(final String id, final IModel<String> linkText, final IDialogFactory dialogFactory, final IDialogService dialogService) {
        this(id, linkText, new DialogAction(dialogFactory, dialogService));
    }

    public DialogLink(final String id, final IModel<String> linkText, final DialogAction action) {
        super(id, linkText);

        setOutputMarkupId(true);
        link = new AjaxLink("dialog-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                action.execute();
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(BUBBLE);
            }
        };

        add(link);
        link.add(new Label("dialog-link-text", linkText));
    }

    public DialogLink(final String id, final IModel<String> linkText, final IDialogFactory dialogFactory, final IDialogService dialogService, final KeyType[] keyTypes) {
        this(id, linkText, dialogFactory, dialogService);
        link.add(new InputBehavior(keyTypes, EventType.click));
    }

    public void enable() {
        link.setEnabled(true);
    }

    public void disable() {
        link.setEnabled(false);
    }

}
