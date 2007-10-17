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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;

public class DialogWindow extends ModalWindow {
    private static final long serialVersionUID = 1L;

    private JcrEvent jcrEvent;
    private JcrNodeModel nodeModel;

    public DialogWindow(String id, JcrNodeModel nodeModel, final boolean resetOnClose) {
        super(id);
        setCookieName(id);
        this.nodeModel = nodeModel;

        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 1L;
            public void onClose(AjaxRequestTarget target) {
                Home home = (Home) getWebPage();
                if (jcrEvent != null) {
                    home.update(target, jcrEvent);
                }
                if (resetOnClose) {
                    setResponsePage(home);
                    setRedirect(true);
                }
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

    public void setJcrEvent(JcrEvent event) {
        this.jcrEvent = event;
    }

    public void setNodeModel(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    public JcrNodeModel getNodeModel() {
        return nodeModel;
    }

}
