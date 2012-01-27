/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.validator;

import org.apache.wicket.Component;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

final class ValidationFeedback extends Component implements IFeedbackLogger {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    ValidationFeedback(String id, IModel<?> model) {
        super(id, model);
    }

    @Override
    protected void onRender(MarkupStream markupStream) {
        throw new UnsupportedOperationException();
    }

    public void error(String messageKey, Object[] parameters) {
        error(new StringResourceModel(messageKey, this, getDefaultModel(), parameters, messageKey).getObject());
    }

    public void warn(String messageKey, Object[] parameters) {
        warn(new StringResourceModel(messageKey, this, getDefaultModel(), parameters, messageKey).getObject());
    }
}
