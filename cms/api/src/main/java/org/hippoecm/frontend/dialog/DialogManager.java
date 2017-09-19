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
package org.hippoecm.frontend.dialog;

import java.util.Map;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Base manager for showing a dialog.
 *
 * - 'dialogConfig' contains the configuration for the dialog as serialized JSON.
 *
 * The behavior can be called by the frontend to open the dialog.
 */
public class DialogManager<ModelType> implements IClusterable {

    private final IPluginContext context;
    private final ConfigProvider configProvider;

    private DialogBehavior<ModelType> behavior;
    private ScriptAction<ModelType> cancelAction;
    private ScriptAction<ModelType> closeAction;

    public DialogManager(final IPluginContext context, final IPluginConfig defaultDialogConfig) {
        this.context = context;

        configProvider = new ConfigProvider(defaultDialogConfig) {
            @Override
            public IPluginConfig get(final Map<String, String> parameters) {
                onConfigure(defaultDialogConfig, parameters);
                return super.get(parameters);
            }
        };
    }

    public DialogBehavior getBehavior() {
        if (behavior == null) {
            behavior = createBehavior(context, configProvider);
        }

        if (behavior != null) {
            behavior.setCancelAction(cancelAction);
            behavior.setCloseAction(closeAction);
        }

        return behavior;
    }

    protected void setCancelAction(final ScriptAction<ModelType> cancelAction) {
        this.cancelAction = cancelAction;
    }

    protected void setCloseAction(final ScriptAction<ModelType> closeAction) {
        this.closeAction = closeAction;
    }

    protected DialogBehavior<ModelType> createBehavior(final IPluginContext context, final ConfigProvider configProvider) {
        return new DialogBehavior<>(context, configProvider);
    }

    protected void onConfigure(final IPluginConfig defaultDialogConfig, final Map<String, String> parameters) {
    }
}
