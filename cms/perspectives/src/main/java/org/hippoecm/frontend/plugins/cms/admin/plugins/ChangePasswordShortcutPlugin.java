/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.IPasswordValidationService;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.IPasswordValidator;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.PasswordValidationStatus;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.widgets.PasswordWidget;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.LoginException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangePasswordShortcutPlugin extends RenderPlugin {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordShortcutPlugin.class);

    private static final long ONE_DAY_MS = 1000 * 3600 * 24;
    private static final String SECURITY_PATH = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.SECURITY_PATH;
    private static final Pattern EXPIRATION_PATTERN = Pattern.compile(".*((day|hour|minute|second|millisecond)s?)$");

    private final Label label;
    private final String username;
    private final long notificationPeriod;

    private User user;
    private String currentPassword;
    private String newPassword;
    private String checkPassword;
    private long passwordMaxAge = -1L;

    public ChangePasswordShortcutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        notificationPeriod = (long) getPluginConfig().getDouble("passwordexpirationnotificationdays", 3) * ONE_DAY_MS;

        final UserSession session = UserSession.get();
        // password max age is defined on the /hippo:configuration/hippo:security node
        try {
            final Node securityNode = session.getRootNode().getNode(SECURITY_PATH);
            if (securityNode.hasProperty(HippoNodeType.HIPPO_PASSWORDMAXAGEDAYS)) {
                passwordMaxAge = (long) (securityNode.getProperty(HippoNodeType.HIPPO_PASSWORDMAXAGEDAYS).getDouble() * ONE_DAY_MS);
            }
        }
        catch (final RepositoryException e) {
            log.error("Failed to determine configured password maximum age", e);
        }

        username = session.getJcrSession().getUserID();
        user = new User(username);

        final AjaxLink link = new AjaxLink("link") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final IDialogService dialogService = getDialogService();
                if (user != null && canChangePassword()) {
                    currentPassword = newPassword = checkPassword = "";
                    dialogService.show(new ChangePasswordDialog());
                } else {
                    dialogService.show(new CannotChangePasswordDialog());
                }
            }
        };
        add(link);

        final IModel<String> labelModel = ReadOnlyModel.of(() -> {
            if (user.isPasswordExpired()) {
                return translate("password-is-expired");
            } else if (isPasswordAboutToExpire(user)) {
                final long expirationTime = user.getPasswordExpirationTime();
                final Duration expirationDuration = Duration.valueOf(expirationTime - System.currentTimeMillis());
                String expiration = expirationDuration.toString(getLocale());

                final Matcher matcher = EXPIRATION_PATTERN.matcher(expiration);
                if (matcher.matches()) {
                    final String expirationMatch = matcher.group(1);
                    expiration = expiration.replace(expirationMatch, translate(expirationMatch));
                }

                return new StringResourceModel("password-about-to-expire", this)
                        .setParameters(expiration)
                        .getString();
            }
            return StringUtils.EMPTY;
        });
        label = new Label("label", labelModel) {
            @Override
            public boolean isVisible() {
                return StringUtils.isNotEmpty(labelModel.getObject());
            }
        };
        label.setOutputMarkupId(true);
        add(label);

        final HippoIcon icon = HippoIcon.fromSprite("change-password-icon", Icon.PENCIL_SQUARE);
        link.add(icon);
    }

    /**
     * Check if the user can change the password
     * @return true if the user can change the password, false otherwise
     */
    private boolean canChangePassword() {
        return !user.isExternal();
    }

    /**
     * Check the password
     * @return true if the password is correct, false otherwise
     */
    private boolean checkPassword(final char[] password) {
        return user.checkPassword(password);
    }

    /**
     * Set the user password
     * @param password the password to set
     * @return true if the password is successfully applied, false otherwise
     */
    private boolean setPassword(final char[] password) {
        try {
            user.savePassword(new String(password));
            return true;
        } catch (final RepositoryException e) {
            log.error("Error while setting user password", e);
        }
        return false;
    }

    private boolean isPasswordAboutToExpire(final User user) {
        if (passwordMaxAge > 0 && notificationPeriod > 0) {
            if (user.getPasswordLastModified() != null) {
                final long passwordLastModified = user.getPasswordLastModified().getTimeInMillis();
                if (passwordLastModified + passwordMaxAge - System.currentTimeMillis() < notificationPeriod) {
                    return true;
                }
            }
        }
        return false;
    }

    // Helper method to simplify #getString call from innerClass that implements #getString as well
    private String translate(final String key) {
        return getString(key);
    }

    // Helper method to simplify #getString call from innerClass that implements #getString as well
    private String translate(final String key, final IModel<?> model) {
        return getString(key, model);
    }

    private class ChangePasswordDialog extends Dialog {

        private final PasswordWidget currentWidget;
        private final PasswordWidget newWidget;
        private final PasswordWidget checkWidget;
        private final IPasswordValidationService passwordValidationService;
        private String feedbackLevel = "warning";

        public ChangePasswordDialog() {
            setOkLabel(translate("change-label"));
            setSize(DialogConstants.MEDIUM_AUTO);
            setTitle(Model.of(translate("change-password-label")));

            replace(feedback = new ChangePasswordFeedbackPanel("feedback") {
                @Override
                protected FeedbackMessagesModel newFeedbackMessagesModel() {
                    return ChangePasswordDialog.this.getFeedbackMessagesModel();
                }

            });
            feedback.setOutputMarkupId(true);
            feedback.add(CssClass.append(ReadOnlyModel.of(() -> feedbackLevel)));

            currentWidget = new PasswordWidget("current-password",
            PropertyModel.of(ChangePasswordShortcutPlugin.this, "currentPassword"),
            Model.of(translate("old-password-label")));
            currentWidget.setOutputMarkupId(true);
            add(currentWidget);
            setFocus(currentWidget);

            newWidget = new PasswordWidget("new-password",
                    PropertyModel.of(ChangePasswordShortcutPlugin.this, "newPassword"),
                    Model.of(translate("new-password-label")));
            newWidget.setResetPassword(false);
            newWidget.setOutputMarkupId(true);
            add(newWidget);

            checkWidget = new PasswordWidget("check-password",
                    PropertyModel.of(ChangePasswordShortcutPlugin.this, "checkPassword"),
                    Model.of(translate("new-password-label-again")));
            checkWidget.setResetPassword(false);
            checkWidget.setOutputMarkupId(true);
            add(checkWidget);

            passwordValidationService = getPluginContext().getService(IPasswordValidationService.class.getName(),
                    IPasswordValidationService.class);
            for (final IPasswordValidator validator : passwordValidationService.getPasswordValidators()) {
                info(validator.getDescription());
            }
        }

        @Override
        public void onOk() {
            boolean ok = true;
            if (currentPassword == null || currentPassword.length() == 0) {
                error(translate("current-password-missing"));
                ok = false;
            } else if (!checkPassword(currentPassword.toCharArray())) {
                error(translate("current-password-invalid"));
                ok = false;
            }

            newWidget.setResetPassword(false);
            checkWidget.setResetPassword(false);
            if (newPassword == null) {
                error(translate("new-password-missing"));
                ok = false;
            } else {
                if (passwordValidationService != null) {
                    try {
                        final List<PasswordValidationStatus> statuses = passwordValidationService.checkPassword(newPassword, user);
                        for (final PasswordValidationStatus status : statuses) {
                            if (!status.accepted()) {
                                error(status.getMessage());
                                ok = false;
                                newWidget.setResetPassword(true); // will clear the field
                                checkWidget.setResetPassword(true);
                            }
                        }
                    }
                    catch (final RepositoryException e) {
                        log.error("Failed to validate password using password validation service", e);
                        ok = false;
                    }
                }
                // fallback on pre 7.7 behavior
                else if (newPassword.length() < 4) {
                    error(translate("new-password-invalid"));
                    ok = false;
                }
            }

            if (checkPassword == null) {
                error(translate("confirm-password-missing"));
                ok = false;
            }

            if (newPassword != null && !newPassword.equals(checkPassword)) {
                error(translate("passwords-do-not-match"));
                ok = false;
                newWidget.setResetPassword(true);
                checkWidget.setResetPassword(true);
            }

            if (ok) {
                if (!setPassword(newPassword.toCharArray())) {
                    error(translate("error-setting-password"));
                    log.warn("Setting the password by user '{}' failed.", username);
                } else {
                    log.info("Password changed by user '{}'. Logging in again to update the CMS session credentials.",
                            username);
                    try {
                        UserSession.get().login(username, newPassword);
                        // create a new user, otherwise it will use the session of the previous login
                        user = new User(username);
                    } catch (final LoginException e) {
                        log.warn("User '{}' changed its password but failed to automatically login again.", username, e);
                    }
                }
            } else {
                feedbackLevel = "";
            }

            setFocus(currentWidget);
            final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(currentWidget);
                target.add(newWidget);
                target.add(checkWidget);
                target.add(label);
            }
        }
    }

    private class CannotChangePasswordDialog extends Dialog {

        CannotChangePasswordDialog() {
            setTitle(Model.of(translate("change-password-label")));
            setSize(DialogConstants.MEDIUM_AUTO);
            setCancelVisible(false);

            info(translate("cannot-change-password", ReadOnlyModel.of(() -> ChangePasswordShortcutPlugin.this)));
        }
    }

    private static class ChangePasswordFeedbackPanel extends FeedbackPanel {

        ChangePasswordFeedbackPanel(final String id) {
            super(id);
        }
    }
}
