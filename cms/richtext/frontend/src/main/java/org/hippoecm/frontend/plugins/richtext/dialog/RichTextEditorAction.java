/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.dialog;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.dialog.ScriptAction;

/**
 * Represents an action on a rich text editor instance.
 *
 * @deprecated use {@link ScriptAction} instead
 */
@Deprecated
public interface RichTextEditorAction<ModelType> extends IClusterable {

    /**
     * Produces the JavaScript to execute this action on the client-side.
     * @param model the model that this action operates on.
     * @return JavaScript code that executes this action.
     */
    String getJavaScript(ModelType model);

}
