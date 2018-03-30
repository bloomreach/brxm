/**
 * Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin.updater;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;


public class UpdaterRegistryEditor extends UpdaterEditor {

    private static final long serialVersionUID = 1L;

    public UpdaterRegistryEditor(final IModel<?> model, final IPluginContext context, final Panel container) {
        super(model, context, container);
    }

    @Override
    protected boolean isStopButtonVisible() {
        return false;
    }

    @Override
    protected boolean isUndoButtonVisible() {
        return false;
    }

    @Override
    protected boolean isSaveButtonEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isUndoButtonEnabled() {
        return false;
    }

    @Override
    protected boolean isDeleteButtonEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isNameFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isDescriptionFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isPathFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isQueryFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isParametersFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isBatchSizeFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isThrottleFieldEnabled() {
        return isUpdater();
    }

    @Override
    protected boolean isDryRunCheckBoxVisible() {
        return false;
    }

    @Override
    protected boolean isScriptEditorReadOnly() {
        return false;
    }

    @Override
    protected boolean isLogLevelFieldEnabled() {
        return true;
    }

}
