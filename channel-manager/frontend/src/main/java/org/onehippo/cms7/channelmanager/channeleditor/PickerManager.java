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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.dialog.DialogManager;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;

/**
 * Base manager for a picker dialog in the channel-editor. Contains the generic code for handling the cancel
 * and close callbacks:
 *  - when done the method 'ChannelEditor#onPicked' is called.
 *  - when cancelled the method 'ChannelEditor#onPickCancelled' is called.
 */
public abstract class PickerManager<ModelType> extends DialogManager<ModelType> {

    private static final IPluginConfig DEFAULT_PICKER_CONFIG = new JavaPluginConfig();

    protected PickerManager(final IPluginContext context, final String channelEditorId) {
        this(context, DEFAULT_PICKER_CONFIG, channelEditorId);
    }

    protected   PickerManager(final IPluginContext context, final IPluginConfig defaultPickerConfig, final String channelEditorId) {
        super(context, defaultPickerConfig);

        setCancelAction(pickedItem -> getCancelScript(channelEditorId));
        setCloseAction(pickedItem -> {
            if (isValid(pickedItem)) {
                return getCloseScript(channelEditorId, toJsString(pickedItem));
            }
            return StringUtils.EMPTY;
        });
    }

    protected abstract String toJsString(final ModelType pickedItem);

    protected boolean isValid(final ModelType pickedItem) {
        return true;
    }

    private static String getCloseScript(final String channelEditorId, final String payload) {
        return String.format("Ext.getCmp('%s').onPicked(%s);", channelEditorId, payload);
    }

    private static String getCancelScript(final String channelEditorId) {
        return String.format("Ext.getCmp('%s').onPickCancelled();", channelEditorId);
    }
}
