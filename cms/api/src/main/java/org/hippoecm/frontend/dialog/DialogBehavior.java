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
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class DialogBehavior<ModelType> extends AbstractDefaultAjaxBehavior {

    private final IPluginContext context;
    private final ConfigProvider configProvider;

    private ScriptAction<ModelType> cancelAction;
    private ScriptAction<ModelType> closeAction;

    public DialogBehavior(final IPluginContext context, final IPluginConfig config) {
        this(context, new ConfigProvider(config));
    }

    public DialogBehavior(final IPluginContext context, final ConfigProvider configProvider) {
        this.context = context;
        this.configProvider = configProvider;
    }

    public void setCancelAction(final ScriptAction<ModelType> cancelAction) {
        this.cancelAction = cancelAction;
    }

    public void setCloseAction(final ScriptAction<ModelType> closeAction) {
        this.closeAction = closeAction;
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        final Map<String, String> parameters = getParameters();
        final IPluginConfig config = configProvider.get(parameters);
        final Dialog<ModelType> dialog = createDialog(context, config);
        dialog.setCancelAction(cancelAction);
        dialog.setCloseAction(closeAction);
        getDialogService().show(dialog);
    }

    protected Dialog<ModelType> createDialog(final IPluginContext context, final IPluginConfig config) {
        return new Dialog<>();
    }

    protected Map<String, String> getParameters() {
        final Request request = RequestCycle.get().getRequest();
        final IRequestParameters parameters = request.getPostParameters();
        return parameters.getParameterNames()
                .stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> parameters.getParameterValue(name).toString()
                ));
    }

    private IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }
}
