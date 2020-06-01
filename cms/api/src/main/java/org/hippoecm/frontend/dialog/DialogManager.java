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
package org.hippoecm.frontend.dialog;

import java.util.Map;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Base manager for showing a dialog.
 */
public class DialogManager<ModelType> implements IDetachable {

    private static final String DIALOG_CONFIG = "dialogConfig";

    private final IPluginContext context;
    private final DialogConfig config;
    private final DialogBehavior behavior;

    private ScriptAction<ModelType> cancelAction;
    private ScriptAction<ModelType> closeAction;

    public DialogManager(final IPluginContext context, final IPluginConfig config) {
        this.context = context;
        this.config = new DialogConfig(config);

        behavior = new DialogBehavior() {
            @Override
            protected void showDialog(final Map<String, String> parameters) {
                onShowDialog(parameters);
            }
        };
    }

    public DialogBehavior getBehavior() {
        return behavior;
    }

    private void onShowDialog(final Map<String, String> parameters) {
        beforeShowDialog(parameters);

        final String paramsDialogConfig = parameters.get(DIALOG_CONFIG);
        final IPluginConfig mergedDialogConfig = config.getMerged(paramsDialogConfig);
        final Dialog<ModelType> dialog = createDialog(context, mergedDialogConfig, parameters);

        dialog.setCancelAction(cancelAction);
        dialog.setCloseAction(closeAction);

        getDialogService().show(dialog);
    }

    protected void beforeShowDialog(final Map<String, String> parameters) {
    }

    protected Dialog<ModelType> createDialog(final IPluginContext context, final IPluginConfig config, final Map<String, String> parameters) {
        return new Dialog<>();
    }

    private IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }

    public void setCancelAction(final ScriptAction<ModelType> cancelAction) {
        this.cancelAction = cancelAction;
    }

    public void setCloseAction(final ScriptAction<ModelType> closeAction) {
        this.closeAction = closeAction;
    }

    @Override
    public void detach() {
    }
}
