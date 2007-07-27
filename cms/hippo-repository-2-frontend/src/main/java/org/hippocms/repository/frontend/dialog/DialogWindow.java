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
package org.hippocms.repository.frontend.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippocms.repository.frontend.BrowserSession;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public class DialogWindow extends ModalWindow {
    private static final long serialVersionUID = 1L;
    
    public DialogWindow(String id, final JcrNodeModel model) {
        super(id);
        setCookieName(id);

        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 1L;
            public void onClose(AjaxRequestTarget target) {
                BrowserSession session = (BrowserSession)getSession();
                session.updateAll(target, model);
            }
        });
    }

    public AjaxLink dialogLink(String id) {
        return new AjaxLink(id) {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                show(target);
            }
        };
    }

}
