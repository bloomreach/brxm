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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

public class Confirm extends Dialog<Void> {

    private DialogCallback okCallback;
    private DialogCallback cancelCallback;

    public Confirm(final String title, final String text) {
        setSize(DialogConstants.SMALL);
        setFocusOnCancel();
        setTitle(Model.of(title));

        add(new Label("text", Model.of(text)));
    }

    public Confirm ok(final DialogCallback okCallback) {
        this.okCallback = okCallback;
        return this;
    }

    public Confirm cancel(final DialogCallback cancelCallback) {
        this.cancelCallback = cancelCallback;
        return this;
    }

    @Override
    protected void onOk() {
        if (okCallback != null) {
            okCallback.call();
        }
    }

    @Override
    protected void onCancel() {
        if (cancelCallback != null) {
            cancelCallback.call();
        }
    }
}
