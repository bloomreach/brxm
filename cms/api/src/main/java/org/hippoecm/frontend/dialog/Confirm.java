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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.ReadOnlyModel;

public class Confirm extends Dialog<Void> {

    private static final String DEFAULT_TITLE_KEY = "confirm-title";
    private static final String DEFAULT_TEXT_KEY = "confirm-text";

    private IModel<String> text;
    private DialogCallback okCallback;
    private DialogCallback cancelCallback;

    public Confirm() {
        setSize(DialogConstants.SMALL);
        setFocusOnCancel();
        setTitleKey(DEFAULT_TITLE_KEY);

        add(new Label("text", ReadOnlyModel.of(() -> getText().getObject())));
    }

    public Confirm title(final IModel<String> title) {
        setTitle(title);
        return this;
    }

    public Confirm title(final String titleKey) {
        return title(titleKey, null);
    }

    public Confirm title(final String titleKey, final IModel<?> titleModel) {
        return title(titleKey, titleModel, null);
    }

    public Confirm title(final String titleKey, final IModel<?> titleModel, final Component component) {
        setTitle(getString(titleKey, titleModel, component));
        return this;
    }

    public Confirm text(final IModel<String> text) {
        this.text = text;
        return this;
    }

    public Confirm text(final String textKey) {
        return text(textKey, null);
    }

    public Confirm text(final String textKey, final IModel<?> textModel) {
        return text(textKey, textModel, null);
    }

    public Confirm text(final String textKey, final IModel<?> textModel, final Component resolver) {
        text = getString(textKey, textModel, resolver);
        return this;
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
    protected void onDetach() {
        if (text != null) {
            text.detach();
        }
        super.onDetach();
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

    private IModel<String> getString(final String key, final IModel<?> model, final Component component) {
        final Component resolver = component == null ? this : component;
        return Model.of(resolver.getString(key, model, StringUtils.EMPTY));
    }

    private IModel<String> getText() {
        if (text == null) {
            text(DEFAULT_TEXT_KEY);
        }
        return text;
    }
}
