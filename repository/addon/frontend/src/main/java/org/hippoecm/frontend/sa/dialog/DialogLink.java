/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.sa.core.IPluginContext;

public class DialogLink extends Panel {
    private static final long serialVersionUID = 1L;

    private PageCreator pageCreator;
    
    public DialogLink(String id, IModel linktext, IPluginContext context, Class clazz) {
        super(id);

        final DialogWindow dialogWindow = new DialogWindow("dialog");
        pageCreator = new DynamicDialogFactory(context, dialogWindow, clazz);
        panelAdders(linktext, dialogWindow);
    }

    public DialogLink(String id, IModel linktext, IDialogFactory dialogFactory) {
        super(id);

        final DialogWindow dialogWindow = new DialogWindow("dialog");
        pageCreator = new DynamicDialogFactory(dialogWindow, dialogFactory);
        panelAdders(linktext, dialogWindow);
    }

    public DialogLink(String id, IModel linktext, DialogWindow window) {
        super(id);

        panelAdders(linktext, window);
    }

    private void panelAdders(IModel linktext, final DialogWindow dialogWindow) {
        add(dialogWindow);

        AjaxLink link = new AjaxLink("dialog-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                dialogWindow.show(pageCreator.createPage());
            }
        };
        add(link);

        link.add(new Label("dialog-link-text", linktext));
    }

}
