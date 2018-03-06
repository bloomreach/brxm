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

    private final IPluginContext context;
    private final IPluginConfig config;

    private DialogBehavior behavior;
    private ScriptAction<ModelType> cancelAction;
    private ScriptAction<ModelType> closeAction;

    public DialogManager(final IPluginContext context, final IPluginConfig config) {
        this.context = context;
        this.config = config;
    }

    @Override
    public void detach() {
    }

    public void setCancelAction(final ScriptAction<ModelType> cancelAction) {
        this.cancelAction = cancelAction;
    }

    public void setCloseAction(final ScriptAction<ModelType> closeAction) {
        this.closeAction = closeAction;
    }

    public DialogBehavior getBehavior() {
        if (behavior == null) {
            behavior = createBehavior(context, config);
        }
        return behavior;
    }

    public String getCallbackUrl() {
        return getBehavior().getCallbackUrl().toString();
    }

    protected DialogBehavior createBehavior(final IPluginContext context, final IPluginConfig config) {
        return new DialogBehavior() {
            @Override
            protected void showDialog(final Map<String, String> parameters) {
                onShowDialog(parameters);
            }
        };
    }

    protected Dialog<ModelType> createDialog(final IPluginContext context, final IPluginConfig config, final Map<String, String> parameters) {
        return new Dialog<>();
    }

    private void onShowDialog(final Map<String, String> parameters) {
        final Dialog<ModelType> dialog = createDialog(context, config, parameters);
        dialog.setCancelAction(cancelAction);
        dialog.setCloseAction(closeAction);
        getDialogService().show(dialog);
    }

    private IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }
}
