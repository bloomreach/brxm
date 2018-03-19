/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.channeleditor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorLink;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for a picker dialog in rich text fields invoked through the channel editor. Contains the generic code for
 * parsing AJAX parameters:
 * <ul>
 *     <li>'fieldId' contains the UUID of the compound node of the rich text field, so the dialog configuration can
 *     determine the field-node-specific settings</li>
 *</ul>
 * <p>
 * Handles save and discard of the node referenced by field "nodeId".
 * </p>
 */
public class RichTextPicker<T extends RichTextEditorLink> extends ChannelEditorPicker<T> {

    private static final Logger log = LoggerFactory.getLogger(RichTextPicker.class);

    private String nodeId;

    RichTextPicker(final IPluginContext context, final IPluginConfig config, final String channelEditorId) {
        super(context, config, channelEditorId);
    }

    void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    protected boolean saveChanges(final T pickedItem) {
        // The picker will have created a new facet node below the field node. Those changes should be saved,
        // otherwise the Visual Editing backend will overwrite those changes again and the CMS will keep references
        // to pending changes in deleted nodes.
        final Session session = UserSession.get().getJcrSession();
        try {
            session.save();
            return true;
        } catch (RepositoryException e) {
            final String user = session.getUserID();
            log.warn("User '{}' failed to save session when closing picker, discarding changes. Cause:", user, e);
            discardChangesInField();
            return false;
        }
    }

    @Override
    protected String toJson(final T pickedItem) {
        return pickedItem.toJsString();
    }

    private Node getFieldNode() {
        try {
            return UserSession.get().getJcrSession().getNodeByIdentifier(nodeId);
        } catch (IllegalArgumentException | RepositoryException e) {
            log.info("Cannot find document '{}' while opening link picker", nodeId);
        }
        return null;
    }

    Model<Node> getFieldNodeModel() {
        return new NodeModel() {
            @Override
            public Node get() {
                return getFieldNode();
            }
        };
    }

    private void discardChangesInField() {
        final Node node = getFieldNode();
        if (node == null) {
            log.warn("No node found with UUID %s, can not discard changes.", nodeId);
            return;
        }

        try {
            node.refresh(false);
        } catch (RepositoryException e) {
            log.warn("Failed to discard changes for node with UUID %s", nodeId, e);
        }
    }

    private static abstract class NodeModel implements Model<Node> {

        @Override
        public final void set(final Node node) {
        }

        @Override
        public final void release() {
        }
    }
}
