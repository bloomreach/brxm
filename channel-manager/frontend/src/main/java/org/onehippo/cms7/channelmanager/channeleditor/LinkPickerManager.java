/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

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
 * Base class manager for a picker dialog in richtext fields. Contains the generic code for parsing AJAX parameters:
 *
 * - 'fieldId' contains the UUID of the compound node of the rich text field, so the dialog configuration can determine
 *   the field-node-specific settings
 */

public class LinkPickerManager<Item extends RichTextEditorLink> extends PickerManager<Item> {

    public static final Logger log = LoggerFactory.getLogger(LinkPickerManager.class);

    private String fieldId;

    LinkPickerManager(final IPluginContext context, final IPluginConfig defaultPickerConfig, final String channelEditorId) {
        super(context, defaultPickerConfig, channelEditorId);
    }

    @Override
    protected void onConfigure(final IPluginConfig defaultDialogConfig, final Map<String, String> parameters) {
        fieldId = parameters.get("fieldId");
    }

    @Override
    protected boolean isValid(final Item pickedItem) {
        return savePendingChanges();
    }

    @Override
    protected String toJsString(final Item pickedItem) {
        return pickedItem.toJsString();
    }

    Node getFieldNode() {
        try {
            return UserSession.get().getJcrSession().getNodeByIdentifier(fieldId);
        } catch (IllegalArgumentException | RepositoryException e) {
            log.info("Cannot find document '{}' while opening link picker", fieldId);
        }
        return null;
    }

    Model<Node> getFieldNodeModel() {
        return new Model<Node>() {
            @Override
            public Node get() {
                return getFieldNode();
            }

            @Override
            public void set(final Node value) {
            }

            @Override
            public void release() {
            }
        };
    }

    private boolean savePendingChanges() {
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

    private void discardChangesInField() {
        try {
            getFieldNodeModel().get().refresh(false);
        } catch (RepositoryException e) {
            log.warn("Also failed to discard changes", e);
        }
    }
}
