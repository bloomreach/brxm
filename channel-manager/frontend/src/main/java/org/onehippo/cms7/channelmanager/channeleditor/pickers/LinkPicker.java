/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.channelmanager.channeleditor.pickers;

import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogManager;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.onehippo.cms.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Manages the picker dialog for internal link fields. The dialog is used to select a node in the repository.
 */
public class LinkPicker extends ChannelEditorPicker<String> {

    private static final Logger log = LoggerFactory.getLogger(LinkPicker.class);

    private final Model<String> dialogModel;

    public LinkPicker(final IPluginContext context, final String channelEditorId) {
        super(context, null, channelEditorId);
        dialogModel = Model.of(StringUtils.EMPTY);
    }

    @Override
    protected DialogManager<String> createDialogManager(final IPluginContext context, final IPluginConfig config) {
        return new DialogManager<String>(context, config) {
            @Override
            protected Dialog<String> createDialog(final IPluginContext context, final IPluginConfig config, final Map<String, String> parameters) {
                return new LinkPickerDialog(context, config, dialogModel);
            }

            @Override
            protected void beforeShowDialog(final Map<String, String> parameters) {
                dialogModel.setObject(parameters.get("uuid"));
            }
        };
    }

    @Override
    protected String toJson(final String uuid) {
        final ObjectNode picked = Json.object();
        picked.put("uuid", uuid);

        final Session session = UserSession.get().getJcrSession();
        try {
            final Node pickedNode = session.getNodeByIdentifier(uuid);
            picked.put("displayName", getNodeName(pickedNode));
        } catch (ItemNotFoundException e) {
            log.warn("Unable to find item: {} : {} ", uuid, e);
        } catch (RepositoryException e) {
            log.warn("Unable to retrieve name for item: {} : {} ", uuid, e);
        }

        return picked.toString();
    }

    private String getNodeName(final Node node) throws RepositoryException {
        return  node instanceof HippoNode
            ? ((HippoNode) node).getDisplayName()
            : node.getName();
    }
}
