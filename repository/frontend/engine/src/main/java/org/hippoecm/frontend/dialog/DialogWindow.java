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

import java.util.LinkedList;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.INotificationListener;
import org.hippoecm.frontend.plugin.channel.IRequestHandler;
import org.hippoecm.frontend.plugin.channel.MessageContext;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;

public class DialogWindow extends ModalWindow implements INotificationListener, IRequestHandler {
    private static final long serialVersionUID = 1L;

    private JcrNodeModel nodeModel;
    private LinkedList<Request> queue;

    public DialogWindow(String id, JcrNodeModel nodeModel, final Channel channel, Channel proxy) {
        super(id);
        setCookieName(id);
        this.nodeModel = nodeModel;
        this.queue = new LinkedList<Request>();

        if (channel != null) {
            channel.subscribe(this);
        }
        if (proxy != null) {
            proxy.register(this);
        }

        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 1L;

            public void onClose(AjaxRequestTarget target) {
                // forward the requests that have been sent by children (i.e. the dialog)
                if (channel != null) {
                    LinkedList<MessageContext> contexts = new LinkedList<MessageContext>();
                    while (queue.size() > 0) {
                        Request request = queue.removeLast();
                        channel.send(request);
                        contexts.add(request.getContext());
                    }
                    while (contexts.size() > 0) {
                        contexts.remove().apply(target);
                    }
                } else {
                    if (queue.size() > 0) {
                        queue = new LinkedList<Request>();
                    }
                }
            }
        });
    }

    public JcrNodeModel getNodeModel() {
        return nodeModel;
    }

    // implement IRequestHandler

    public void handle(Request request) {
        // put requests from children in a queue.  These requests are sent while the
        // modal window is present and the page that contains this dialogwindow cannot
        // be updated.
        queue.add(request);
    }

    // implement INotificationListener

    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            nodeModel = new JcrNodeModel(notification.getData());
        }
    }
}
