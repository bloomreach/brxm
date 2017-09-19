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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.plugins.richtext.model.AbstractPersistedMap;

public abstract class AbstractRichTextEditorDialog<ModelType extends AbstractPersistedMap> extends Dialog<ModelType> {

    public AbstractRichTextEditorDialog(final IModel<ModelType> model) {
        super(model);

        setTitleKey("dialog-title");
    }

    protected void checkState() {
        setOkEnabled(getModelObject().isValid() && getModelObject().hasChanged());
    }

    protected static class StringPropertyModel extends PropertyModel<String> {

        public StringPropertyModel(final Object modelObject, final String expression) {
            super(modelObject, expression);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class getObjectClass() {
            return String.class;
        }
    }

}
