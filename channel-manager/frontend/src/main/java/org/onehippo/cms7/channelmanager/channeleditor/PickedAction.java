/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.channelmanager.channeleditor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugins.richtext.dialog.RichTextEditorAction;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorLink;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PickedAction<Item extends RichTextEditorLink> implements RichTextEditorAction<Item> {

    private static final Logger log = LoggerFactory.getLogger(PickedAction.class);

    private final String channelEditorId;
    private final String method;
    private final Model<Node> fieldNodeModel;

    PickedAction(final String channelEditorId, final String method, final Model<Node> fieldNodeModel) {
        this.channelEditorId = channelEditorId;
        this.method = method;
        this.fieldNodeModel = fieldNodeModel;
    }

    @Override
    public String getJavaScript(final Item pickedItem) {
        // The picker will have created a new facet node below the field node. Those changes should be saved,
        // otherwise the Visual Editing backend will overwrite those changes again and the CMS will keep references
        // to pending changes in deleted nodes.
        if (savePendingChanges()) {
            return String.format("Ext.getCmp('%s').%s(%s);", channelEditorId, method, pickedItem.toJsString());
        }
        return StringUtils.EMPTY;
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
            fieldNodeModel.get().refresh(false);
        } catch (RepositoryException e) {
            log.warn("Also failed to discard changes", e);
        }
    }
}
