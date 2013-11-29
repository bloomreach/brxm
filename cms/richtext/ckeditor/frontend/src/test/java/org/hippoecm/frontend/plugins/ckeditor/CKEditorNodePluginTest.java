/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Tests {@link CKEditorNodePlugin}.
 */
public class CKEditorNodePluginTest {

    @Test
    public void defaultConfigurationIsValidJson() throws JSONException {
        new JSONObject(CKEditorNodePlugin.DEFAULT_EDITOR_CONFIG);
    }
}
