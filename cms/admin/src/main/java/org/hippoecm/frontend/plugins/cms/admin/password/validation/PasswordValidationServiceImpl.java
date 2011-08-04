/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.password.validation;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordValidationServiceImpl extends Plugin implements IPasswordValidationService {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PasswordValidationServiceImpl.class);
    
    private static final long THREEDAYS = 1000 * 3600 * 24 * 3;
    
    private List<IPasswordValidator> validators;
    
    // number of optional validators that should pass
    private int requiredStrength;
    
    private long passwordMaxAge = -1l;
    private long notificationPeriod = THREEDAYS;

    public PasswordValidationServiceImpl(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        loadConfiguration();
        context.registerService(this, IPasswordValidationService.class.getName());
    }
    
    private void loadConfiguration() {
        // get the child pluginconfig nodes that define the validators
        Set<IPluginConfig> configs = getPluginConfig().getPluginConfigSet();
        validators = new ArrayList<IPasswordValidator>(configs.size());
        int numberOfOptionalValidators = 0;
        for (IPluginConfig config : configs) {
            String validatorClassName = config.getString("validator.class");
            try {
                Class<IPasswordValidator> validatorClass = (Class<IPasswordValidator>) Class.forName(validatorClassName);
                Constructor<IPasswordValidator> validatorConstructor = validatorClass.getConstructor(IPluginConfig.class);
                IPasswordValidator validator = validatorConstructor.newInstance(config);
                if (validator.isOptional()) {
                    numberOfOptionalValidators++;
                }
                validators.add(validator);
            } catch (Exception e) {
                log.error("Failed to create password validator: " + e.toString());
            }
        }
        
        // get the required password strength and the password expiration notification period
        requiredStrength = getPluginConfig().getInt("password.strength", 0);
        if (requiredStrength > numberOfOptionalValidators) {
            log.error("The value of the property password.strength is larger than the number of available " +
            		"optional password validators. This way, no attempt at creating a new password can succeed.");
        }
        notificationPeriod = getPluginConfig().getLong("password.expirationNotificationPeriod", THREEDAYS);
        
        // password max age is defined on the /hippo:configuration/hippo:security node
        try {
            Node securityNode = ((UserSession) Session.get()).getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH).getNode(HippoNodeType.SECURITY_PATH);
            if (securityNode.hasProperty(HippoNodeType.HIPPO_PASSWORDMAXAGE)) {
                passwordMaxAge = securityNode.getProperty(HippoNodeType.HIPPO_PASSWORDMAXAGE).getLong();
            }
        }
        catch (RepositoryException e) {
            log.error("Failed to determine configured password maximum age", e);
        }
    }

    @Override
    public List<PasswordValidationStatus> checkPassword(String password, User user) throws RepositoryException {
        List<PasswordValidationStatus> result = new ArrayList<PasswordValidationStatus>(validators.size());
        int strength = 0;
        List<String> optionalValidatorDescriptions = new ArrayList<String>(validators.size());
        for (IPasswordValidator validator : validators) {
            PasswordValidationStatus status = validator.checkPassword(password, user);
            if (validator.isOptional()) {
                optionalValidatorDescriptions.add(validator.getDescription());
                if (status.accepted()) {
                    strength++;
                }
            }
            else {
                result.add(status);
            }
        }
        if (requiredStrength > strength) {
            String message = new ClassResourceModel("message", PasswordValidationServiceImpl.class, new Object[] { new Integer(requiredStrength) }).getObject();
            int counter = 1;
            for (String validatorDescription : optionalValidatorDescriptions) {
                message += counter++ + ") " + validatorDescription + " ";
            }
            result.add(new PasswordValidationStatus(message, false));
        }
        return result;
    }
    
    @Override
    public boolean isPasswordAboutToExpire(User user) {
        if (passwordMaxAge > 0) {
            if (notificationPeriod > 0) {
                if (user.getPasswordLastModified() != null) {
                    long passwordLastModified = user.getPasswordLastModified().getTimeInMillis();
                    if (passwordLastModified + passwordMaxAge - System.currentTimeMillis() < notificationPeriod) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
}
