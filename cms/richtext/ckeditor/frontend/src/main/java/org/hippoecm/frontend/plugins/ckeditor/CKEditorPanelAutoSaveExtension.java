/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor;

import java.util.Collections;

import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.ckeditor.hippoautosave.HippoAutoSave;
import org.hippoecm.frontend.plugins.ckeditor.hippopicker.HippoPicker;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.ckeditor.CKEditorConstants;

/**
 * Adds the CKEditor plugin 'hippoautosave'.
 */
public class CKEditorPanelAutoSaveExtension implements CKEditorPanelExtension {

    private final AbstractAjaxBehavior autoSaveBehavior;

    public CKEditorPanelAutoSaveExtension(AbstractAjaxBehavior autoSaveBehavior) {
        this.autoSaveBehavior = autoSaveBehavior;
    }

    @Override
    public void addConfiguration(final JSONObject editorConfig) throws JSONException {
        JsonUtils.appendToCommaSeparatedString(editorConfig, CKEditorConstants.CONFIG_EXTRA_PLUGINS, HippoAutoSave.PLUGIN_NAME);
        editorConfig.put(HippoAutoSave.CONFIG_CALLBACK_URL, autoSaveBehavior.getCallbackUrl());
    }

    @Override
    public Iterable<Behavior> getBehaviors() {
        return Collections.<Behavior>singleton(autoSaveBehavior);
    }

    @Override
    public void detach() {
        // nothing to do
    }

}
