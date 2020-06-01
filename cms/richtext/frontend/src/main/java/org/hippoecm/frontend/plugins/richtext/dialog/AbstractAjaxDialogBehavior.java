/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.dialog;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.dialog.DialogManager;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.model.AbstractPersistedMap;

/**
 * @deprecated Please use {@link DialogManager} instead.
 */
@Deprecated
public abstract class AbstractAjaxDialogBehavior<ModelType extends AbstractPersistedMap> extends AbstractDefaultAjaxBehavior {
    private static final long serialVersionUID = 1L;

    private final IPluginContext context;
    private final IPluginConfig config;
    private RichTextEditorAction<ModelType> cancelAction;
    private RichTextEditorAction<ModelType> closeAction;

    public AbstractAjaxDialogBehavior(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;
    }

    public void setCancelAction(final RichTextEditorAction<ModelType> cancelAction) {
        this.cancelAction = cancelAction;
    }

    public void setCloseAction(final RichTextEditorAction<ModelType> closeAction) {
        this.closeAction = closeAction;
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        final AbstractRichTextEditorDialog<ModelType> dialog = createDialog();
        dialog.setCancelAction(cancelAction);
        dialog.setCloseAction(closeAction);
        getDialogService().show(dialog);
    }

    protected abstract AbstractRichTextEditorDialog<ModelType> createDialog();

    private IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }

    protected Map<String, String> getParameters() {
        final Request request = RequestCycle.get().getRequest();
        final HashMap<String, String> parameters = new HashMap<>();
        final IRequestParameters requestParameters = request.getPostParameters();
        for (String key : requestParameters.getParameterNames()) {
            parameters.put(key, requestParameters.getParameterValue(key).toString());
        }
        return parameters;
    }

    protected IPluginContext getPluginContext() {
        return context;
    }

    protected IPluginConfig getPluginConfig() {
        return config;
    }

}
