/*
 *  Copyright 2009-2011 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.plugins;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.IPasswordValidationService;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.PasswordValidationStatus;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.widgets.PasswordWidget;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangePasswordShortcutPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ChangePasswordShortcutPlugin.class);

    private final String username;
    private User user;

    private String currentPassword;
    private String newPassword;
    private String checkPassword;
    
    private IPasswordValidationService passwordValidationService;
    
    private Label label;
            
    public ChangePasswordShortcutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        
        username = ((UserSession) Session.get()).getJcrSession().getUserID();
        
        try {
            user = new User(username);
        } catch (RepositoryException e) {
            log.error("Unable to create user {}", username, e);
        }
        
        AjaxLink link = new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                if (user != null && canChangePassword()) {
                    currentPassword = newPassword = checkPassword = "";
                    dialogService.show(new ChangePasswordShortcutPlugin.Dialog(context, config));
                } else {
                    dialogService.show(new ChangePasswordShortcutPlugin.CannotChangeDialog(context, config));
                }
            }
        };
        add(link);
        
        label = new Label("label", new AbstractReadOnlyModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (user.isPasswordExpired()) {
                    return new StringResourceModel("password-is-expired", ChangePasswordShortcutPlugin.this, null).getObject();
                }
                else if (passwordValidationService != null) {
                    if (passwordValidationService.isPasswordAboutToExpire(user)) {
                        long expirationTime = user.getPasswordExpirationTime();
                        Duration expirationDuration = Duration.valueOf(expirationTime - System.currentTimeMillis());
                        StringResourceModel model = new StringResourceModel(
                                "password-about-to-expire", 
                                ChangePasswordShortcutPlugin.this, 
                                null, 
                                new Object[] { expirationDuration.toString(getLocale()) });
                        return model.getObject();
                    }
                }
                return "";
            }
            
        });
        label.setOutputMarkupId(true);
        add(label);
        
        passwordValidationService = context.getService(IPasswordValidationService.class.getName(), IPasswordValidationService.class);
    }

    /**
     * Check if the user can change the password
     * @param user
     * @return
     */
    private boolean canChangePassword() {
        return !user.isExternal();
    }

    /**
     * Check the password
     * @return
     */
    private boolean checkPassword(char[] password) {
        return user.checkPassword(password);
    }

    /**
     * Set the user password
     * @param password
     * @return
     */
    private boolean setPassword(char[] password) {
        try {
            user.savePassword(new String(password));
            return true;
        } catch (RepositoryException e) {
            log.error("Error while setting user password", e);
        }
        return false;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getCheckPassword() {
        return checkPassword;
    }

    public void setCheckPassword(String checkPassword) {
        this.checkPassword = checkPassword;
    }

    public class Dialog extends AbstractDialog {

        private static final long serialVersionUID = 1L;

        private PasswordWidget currentWidget;
        
        private final IPasswordValidationService passwordValidationService; 

        public Dialog(final IPluginContext context, final IPluginConfig config) {
            setOkLabel(new StringResourceModel("change-label", ChangePasswordShortcutPlugin.this, null));

            replace(feedback = new FeedbackPanel("feedback") {
                private static final long serialVersionUID = 1L;
                @Override
                protected FeedbackMessagesModel newFeedbackMessagesModel() {
                    return Dialog.this.getFeedbackMessagesModel();
                }
            });
            feedback.setOutputMarkupId(true);

            currentWidget = new PasswordWidget("current-password", new PropertyModel(ChangePasswordShortcutPlugin.this,
                    "currentPassword"), new StringResourceModel("old-password-label",
                    ChangePasswordShortcutPlugin.this, null));
            currentWidget.setResetPassword(true);
            currentWidget.setOutputMarkupId(true);
            add(currentWidget);
            setFocus(currentWidget);

            final PasswordWidget newWidget = new PasswordWidget("new-password", new PropertyModel(
                    ChangePasswordShortcutPlugin.this, "newPassword"), new StringResourceModel("new-password-label",
                            ChangePasswordShortcutPlugin.this, null));
            newWidget.setResetPassword(false);
            add(newWidget);

            final PasswordWidget checkWidget = new PasswordWidget("check-password", new PropertyModel(
                    ChangePasswordShortcutPlugin.this, "checkPassword"), new StringResourceModel(
                            "new-password-label-again", ChangePasswordShortcutPlugin.this, null));
            checkWidget.setResetPassword(false);
            add(checkWidget);
            
            passwordValidationService = context.getService(IPasswordValidationService.class.getName(), IPasswordValidationService.class);
            
        }

        @Override
        public void onOk() {
            boolean ok = true;
            if (currentPassword == null || currentPassword.length() == 0) {
                error(new StringResourceModel("current-password-missing", ChangePasswordShortcutPlugin.this, null)
                .getString());
                ok = false;
            } else if (!checkPassword(currentPassword.toCharArray())) {
                error(new StringResourceModel("current-password-invalid", ChangePasswordShortcutPlugin.this, null)
                .getString());
                ok = false;
            }

            if (newPassword == null) {
                error(new StringResourceModel("new-password-missing", ChangePasswordShortcutPlugin.this, null)
                .getString());
                ok = false;
            } else {
                if (passwordValidationService != null) {
                    try {
                        List<PasswordValidationStatus> statuses = passwordValidationService.checkPassword(newPassword, user);
                        for (PasswordValidationStatus status : statuses) {
                            if (!status.accepted()) {
                                error(status.getMessage());
                                ok = false;
                            }
                        }
                    }
                    catch (RepositoryException e) {
                        log.error("Failure validating password using password validation service", e);
                        ok = false;
                    }
                }
                // fallback on pre 7.7 behavior
                else if (newPassword.length() < 4) {
                    error(new StringResourceModel("new-password-invalid", ChangePasswordShortcutPlugin.this, null).getString());
                    ok = false;
                }
            }

            if (checkPassword == null) {
                error(new StringResourceModel("confirm-password-missing", ChangePasswordShortcutPlugin.this, null)
                .getString());
                ok = false;
            }

            if (newPassword != null && !newPassword.equals(checkPassword)) {
                error(new StringResourceModel("passwords-do-not-match", ChangePasswordShortcutPlugin.this, null)
                .getString());
                ok = false;
            }

            if (ok) {
                if (!setPassword(newPassword.toCharArray())) {
                    error(new StringResourceModel("error-setting-password", ChangePasswordShortcutPlugin.this, null)
                    .getString());
                    log.warn("Setting the password by user '" + username + "' failed.");
                } else {
                    log.info("Password changed by user '" + username + "'.");
                }
            }

            // empty the current password
            currentPassword = "";
            setFocus(currentWidget);
            AjaxRequestTarget target = AjaxRequestTarget.get();
            if (target != null) {
                target.addComponent(currentWidget);
                target.addComponent(label);
            }
        }

        public IModel getTitle() {
            return new StringResourceModel("change-password-label", ChangePasswordShortcutPlugin.this, null);
        }

        @Override
        public IValueMap getProperties() {
            return SMALL;
        }
        
        
    }

    public class CannotChangeDialog extends AbstractDialog {

        private static final long serialVersionUID = 1L;

        public CannotChangeDialog(final IPluginContext context, final IPluginConfig config) {
            info(new StringResourceModel("cannot-change-passoword", ChangePasswordShortcutPlugin.this, null)
            .getString());
            setCancelVisible(false);
        }

        public IModel getTitle() {
            return new StringResourceModel("change-password-label", ChangePasswordShortcutPlugin.this, null);
        }
    }
}
