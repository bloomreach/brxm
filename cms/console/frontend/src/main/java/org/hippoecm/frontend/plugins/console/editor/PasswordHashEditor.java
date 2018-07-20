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
package org.hippoecm.frontend.plugins.console.editor;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.PasswordHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class PasswordHashEditor extends Panel {

    public static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PasswordHashEditor.class);

    private final int passwordLength;
    private final TextFieldWidget passwordField;

    public PasswordHashEditor(String id, final int passwordLength, JcrPropertyModel propertyModel, IModel<String> valueModel) {
        super(id);
        this.passwordLength = passwordLength;
        setOutputMarkupId(true);
        // generate link:
        final PasswordGenerateLink generateLink = new PasswordGenerateLink("password-generate-link", propertyModel, valueModel);
        add(generateLink);
        // hash link
        final PasswordHashLink hashLink = new PasswordHashLink("password-hash-link", propertyModel, valueModel);
        add(hashLink);
        // input field

        passwordField = new TextFieldWidget("password-input", valueModel) {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // do nothing

                target.focusComponent(this);
                target.add(PasswordHashEditor.this);
            }
        };
        passwordField.setOutputMarkupId(true);
        passwordField.setSize("60");
        add(passwordField);
    }

    private class PasswordGenerateLink extends AjaxLink<String> {
        private static final long serialVersionUID = 1L;
        private String linkText;
        private final JcrPropertyModel propertyModel;

        public PasswordGenerateLink(final String id, final JcrPropertyModel propertyModel, final IModel<String> valueModel) {
            super(id, valueModel);
            this.propertyModel = propertyModel;
            linkText = "Generate random password";
            add(new Label("password-generate-link-text", new PropertyModel<String>(this, "linkText")));
            add(new AttributeAppender("style", new AbstractReadOnlyModel<Object>() {
                @Override
                public Object getObject() {
                    return "color:blue";
                }
            }, " "));
        }

        @Override
        public boolean isEnabledInHierarchy() {
            return true;
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {
            final String password = genPassword(passwordLength);
            if (!Strings.isNullOrEmpty(password)) {
                try {
                    final Property property = propertyModel.getProperty();
                    property.setValue(password);
                    target.add(passwordField);
                    target.focusComponent(passwordField);
                } catch (RepositoryException e) {
                    log.error("Error generating password", e);
                }

            }

        }
    }

    private class PasswordHashLink extends AjaxLink<String> {
        private static final long serialVersionUID = 1L;
        private String linkText;
        private final JcrPropertyModel propertyModel;

        public PasswordHashLink(final String id, final JcrPropertyModel propertyModel, final IModel<String> valueModel) {
            super(id, valueModel);
            this.propertyModel = propertyModel;
            linkText = "Hash password";
            add(new Label("password-hash-link-text", new PropertyModel<String>(this, "linkText")));
            add(new AttributeAppender("style", new AbstractReadOnlyModel<Object>() {
                @Override
                public Object getObject() {
                    return "color:blue";
                }
            }, " "));
        }

        @Override
        public boolean isEnabledInHierarchy() {
            return true;
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {
            final String password = getModelObject();
            if (Strings.isNullOrEmpty(password)) {
                log.warn("Password null or empty, generate new password first");
                return;
            }
            // check if hash:
            if (password.startsWith("$SHA-")) {
                log.warn("Already hashed: Please generate password first");
                return;
            }

            final String passwordHash = createPasswordHash(password);
            if (!Strings.isNullOrEmpty(passwordHash)) {
                try {
                    final Property property = propertyModel.getProperty();
                    property.setValue(passwordHash);

                } catch (RepositoryException e) {
                    log.error("Error generating password", e);
                }

            }

            target.add(PasswordHashEditor.this);
        }
    }


    private static String genPassword(final int len) {
        final Random rnd = new Random();
        final char password[] = new char[len];
        for (int i = 0; i < len; i++) {
            password[i] = CHARACTERS.charAt(rnd.nextInt(CHARACTERS.length()));
        }
        return new String(password);
    }

    public static String createPasswordHash(final String password) {
        try {
            return PasswordHelper.getHash(password.toCharArray());
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Unable to hash password", e);
        }
        return null;
    }
}
