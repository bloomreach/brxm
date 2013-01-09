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
package org.hippoecm.frontend.plugins.console.menu.save;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugins.console.menu.content.ContentImportDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveDialog extends AbstractDialog<Node> {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ContentImportDialog.class);

    public SaveDialog() {
        Component message;
        try {
            HippoNode rootNode = (HippoNode) UserSession.get().getJcrSession().getRootNode();
            if (rootNode.getSession().hasPendingChanges()) {
                StringBuilder buf = new StringBuilder("Pending changes:\n");
                appendPendingChangesFromNodeToBuffer(rootNode, buf,"\n");
                message = new MultiLineLabel("message", buf.toString());
            } else {
                message = new Label("message", "There are no pending changes");
                setOkVisible(false);
                setFocusOnCancel();
            }
        } catch (RepositoryException e) {
            log.error("Error while rendering save dialog", e);
            message = new Label("message", "exception: " + e.getMessage());
            setOkVisible(false);
            setFocusOnCancel();
        }
        add(message);
        setFocusOnOk();
    }

    @Override
    public void onOk() {
        try {
            Session jcrSession = UserSession.get().getJcrSession();

            HippoNode rootNode = (HippoNode) jcrSession.getRootNode();
            StringBuilder buffer = new StringBuilder("User made changes at: ");
            appendPendingChangesFromNodeToBuffer(rootNode, buffer,",");

            jcrSession.save();
            //only log when the save is successful
            logEvent("write-changes",jcrSession.getUserID(),buffer.toString());
            jcrSession.refresh(false);
        } catch (AccessDeniedException e) {
            error(e.getClass().getName() + ": " + e.getMessage());
        } catch (RepositoryException e) {
            log.error("Error while saving content from the console", e);
            error(e.getClass().getName() + ": " + e.getMessage());
        }
    }


    private void appendPendingChangesFromNodeToBuffer(final HippoNode rootNode, final StringBuilder buf,
                                                      final String delimiter) throws RepositoryException {
        NodeIterator it = rootNode.pendingChanges();
        if (it.hasNext()) {
            while (it.hasNext()) {
                Node node = it.nextNode();
                buf.append(node.getPath());
                if(it.hasNext()) {
                    buf.append(delimiter);
                }
            }
        }
    }

    private void logEvent(String action, String user, String message) {
        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final HippoEvent event = new HippoEvent("console");
            event.category("console").user(user).action(action);
            event.message(message);
            eventBus.post(event);
        }
    }

    public IModel getTitle() {
        return new Model<String>("Save Session");
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

}
