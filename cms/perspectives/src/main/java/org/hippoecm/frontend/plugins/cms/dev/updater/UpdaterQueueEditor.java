/**
 * Copyright (C) 2012 Hippo
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
package org.hippoecm.frontend.plugins.cms.dev.updater;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;


public class UpdaterQueueEditor extends UpdaterEditor {

    public UpdaterQueueEditor(final IModel<?> model, final Panel container) {
        super(model, container);
    }

    @Override
    protected Component createOutputComponent(final String id) {
        return new UpdaterOutput(id, this, true);
    }

    @Override
    protected boolean isSaveButtonVisible() {
        return false;
    }

    @Override
    protected boolean isExecuteButtonVisible() {
        return false;
    }

    @Override
    protected boolean isDeleteButtonVisible() {
        return false;
    }

    @Override
    protected boolean isNewButtonVisible() {
        return false;
    }

    @Override
    protected boolean isNameFieldEnabled() {
        return false;
    }

    @Override
    protected boolean isPathFieldEnabled() {
        return false;
    }

    @Override
    protected boolean isQueryFieldEnabled() {
        return false;
    }

    @Override
    protected boolean isBatchSizeFieldEnabled() {
        return false;
    }

    @Override
    protected boolean isThrottleFieldEnabled() {
        return false;
    }

    @Override
    protected boolean isRadioVisible() {
        return false;
    }

    @Override
    protected boolean isPathFieldVisible() {
        return method.equals("path");
    }

    @Override
    protected boolean isQueryFieldVisible() {
        return method.equals("query");
    }
}
