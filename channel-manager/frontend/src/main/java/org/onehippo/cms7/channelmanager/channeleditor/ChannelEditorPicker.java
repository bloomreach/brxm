/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.dialog.DialogBehavior;
import org.hippoecm.frontend.dialog.DialogManager;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public abstract class ChannelEditorPicker<T> implements IDetachable {

    private final DialogManager<T> dialogManager;

    ChannelEditorPicker(final IPluginContext context, final IPluginConfig config,  final String channelEditorId) {
        dialogManager = createDialogManager(context, config);
        dialogManager.setCancelAction(pickedItem -> getCancelScript(channelEditorId));
        dialogManager.setCloseAction(pickedItem -> isValid(pickedItem)
                ? getCloseScript(channelEditorId, toJson(pickedItem))
                : StringUtils.EMPTY);
    }

    protected DialogManager<T> createDialogManager(final IPluginContext context, final IPluginConfig config) {
        return new DialogManager<>(context, config);
    }

    public DialogBehavior getBehavior() {
        return dialogManager.getBehavior();
    }

    @Override
    public void detach() {
        dialogManager.detach();
    }

    /**
     * Convert picked item to JSON
     *
     * @param pickedItem The object that was picked in the dialog
     * @return The picked object converted to JSON string format
     */
    protected abstract String toJson(final T pickedItem);

    protected boolean isValid(final T pickedItem) {
        return true;
    }

    private static String getCloseScript(final String channelEditorId, final String payload) {
        return String.format("Ext.getCmp('%s').onPicked(%s);", channelEditorId, payload);
    }

    private static String getCancelScript(final String channelEditorId) {
        return String.format("Ext.getCmp('%s').onPickCancelled();", channelEditorId);
    }
}
