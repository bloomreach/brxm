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
package org.hippoecm.frontend.widgets;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated There is no practical use-case for password primitive field type
 */
@Deprecated
public class PasswordTextFieldWidget extends AjaxUpdatingWidget<String> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PasswordTextFieldWidget.class);

    private MyModel myModel;

    public PasswordTextFieldWidget(final String id, final IModel<String> model) {
        super(id, model);

        myModel = new MyModel(model);

        final PasswordTextField pwd = new PasswordTextField("widget", myModel);
        pwd.setRequired(false);
        pwd.add(new AbstractValidator<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onValidate(IValidatable<String> validatable) {
                String modelValue = myModel.getObject() != null ? myModel.getObject() : "";
                String formValue = validatable.getValue() != null ? (String) validatable.getValue() : "";
                if (modelValue.length() == 0 && formValue.length() == 0) {
                    PasswordTextFieldWidget.this.error("Password is required");
                }
            }

            @Override
            public boolean validateOnNullValue() {
                return true;
            }
        });
        addFormField(pwd);
    }

    static class MyModel implements IChainingModel<String> {
        private static final long serialVersionUID = 1L;

        private JcrPropertyValueModel model;

        public MyModel(IModel<String> model) {
            setChainedModel(model);
        }

        @SuppressWarnings("unchecked")
        public IModel<String> getChainedModel() {
            return (IModel<String>) model;
        }

        public void setChainedModel(IModel<?> model) {
            if (model instanceof JcrPropertyValueModel) {
                this.model = (JcrPropertyValueModel) model;
            }
        }

        public void detach() {
            model.detach();
        }

        public String getObject() {
            if (model != null) {
                try {
                    Value value = model.getValue();
                    if (value != null) {
                        return model.getValue().getString();
                    }
                } catch (RepositoryException e) {
                    log.error("An error occurred while trying to get password value", e);
                }
            }
            return null;
        }

        /**
         * Special purpose override, makes sure the JcrPropertyValueModel will never be null or ""
         * (non-Javadoc)
         * @see org.apache.wicket.model.IModel#setObject(java.lang.Object)
         */
        public void setObject(String object) {
            if (object == null) {
                return;
            }
            String value = (String) object;
            if (value.length() > 0) {
                try {
                    model.setValue(model.getJcrPropertymodel().getProperty().getSession().getValueFactory().createValue(value));
                } catch(RepositoryException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        }
    }

}
