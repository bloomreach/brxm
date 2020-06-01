/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogManager;
import org.hippoecm.frontend.plugins.richtext.model.AbstractPersistedMap;

/**
 * @deprecated Please use {@link DialogManager} instead.
 */
@Deprecated
public abstract class AbstractRichTextEditorDialog<T extends AbstractPersistedMap> extends Dialog<T> {

    private RichTextEditorAction<T> cancelAction;
    private RichTextEditorAction<T> closeAction;

    public AbstractRichTextEditorDialog(IModel<T> model) {
        super(model);

        setTitleKey("dialog-title");
    }

    protected void checkState() {
        setOkEnabled(getModelObject().isValid() && getModelObject().hasChanged());
    }

    public void setCancelAction(final RichTextEditorAction<T> cancelAction) {
        this.cancelAction = cancelAction;
    }

    public void setCloseAction(final RichTextEditorAction<T> closeAction) {
        this.closeAction = closeAction;
    }

    @Override
    public void onClose() {
        final RichTextEditorAction<T> action = cancelled ? cancelAction : closeAction;
        if (action != null) {
            final String script = action.getJavaScript(getModelObject());
            if (StringUtils.isNotBlank(script)) {
                AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                if (target != null) {
                    target.getHeaderResponse().render(OnDomReadyHeaderItem.forScript(script));
                }
            }
        }
        super.onClose();
    }

    protected static class StringPropertyModel extends PropertyModel<String> {

        public StringPropertyModel(Object modelObject, String expression) {
            super(modelObject, expression);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class getObjectClass() {
            return String.class;
        }
    }

}
