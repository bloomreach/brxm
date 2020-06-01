/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Loadable detachable model that wraps a delegate string model and removes all line breaks from the delegate model
 * string once it is loaded.
 */
class StringWithoutLineBreaksModel extends LoadableDetachableModel<String> {

    private static final String LINE_BREAKS_REGEX = "(\\r|\\n)";

    private IModel<String> delegate;

    StringWithoutLineBreaksModel(IModel<String> delegate) {
        this.delegate = delegate;
    }

    @Override
    protected String load() {
        if (delegate != null) {
            final String mayContainLineBreaks = delegate.getObject();
            if (mayContainLineBreaks != null) {
                return mayContainLineBreaks.replaceAll(LINE_BREAKS_REGEX, "");
            }
        }
        return null;
    }

}
