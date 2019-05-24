/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.migrator.validator;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.engine.migrator.ConfigurationMigrator;
import org.onehippo.cm.engine.migrator.MigrationException;
import org.onehippo.cm.engine.migrator.PostMigrator;
import org.onehippo.cm.model.ConfigurationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PostMigrator
public class ValidatorMigrator implements ConfigurationMigrator {

    private static final Logger log = LoggerFactory.getLogger(ValidatorMigrator.class);

    private static final String OLD_VALIDATORS_CONFIGURATION_LOCATION =
            "/hippo:configuration/hippo:frontend/cms/cms-validators";
    private static final String NEW_VALIDATORS_CONFIGURATION_LOCATION =
            "/hippo:configuration/hippo:modules/validation/hippo:moduleconfig";
    private static final String PLUGIN_CLASS = "plugin.class";
    private static final String REGEX_PATTERN = "regex_pattern";

    private static final Map<String, String> DEFAULT_VALIDATORS = Stream.of(new String[][]{
            {"non-empty", "org.hippoecm.frontend.editor.validator.plugins.NonEmptyCmsValidator"},
            {"html", "org.hippoecm.frontend.editor.validator.plugins.HtmlCmsValidator"},
            {"escaped", "org.hippoecm.frontend.editor.validator.plugins.EscapedCmsValidator"},
            {"email", "org.hippoecm.frontend.editor.validator.plugins.RegExCmsValidator"},
            {"references", "org.hippoecm.frontend.editor.validator.plugins.NodeReferenceValidator"},
    }).collect(Collectors.collectingAndThen(
            Collectors.toMap(data -> data[0], data -> data[1]),
            Collections::unmodifiableMap));

    @Override
    public boolean migrate(final Session session, final ConfigurationModel configurationModel, final boolean autoExportEnabled) {
        try {
            return doMigrate(session, autoExportEnabled);
        } catch (final RepositoryException e) {
            throw new MigrationException("ValidatorMigrator failed.", e);
        }
    }

    private boolean doMigrate(final Session session, final boolean autoExportEnabled) throws RepositoryException {
        if (!autoExportEnabled) {
            log.info("Autoexport not enabled :ValidatorMigrator does not need to do anything.");
            return false;
        }

        final boolean success = migrateValidatorConfiguration(session);
        if (!success) {
            throw new MigrationException("Could not migrate configuration for migrator ValidatorMigrator");
        }

        session.save();
        return true;
    }

    private boolean migrateValidatorConfiguration(final Session session) throws RepositoryException {
        final Node oldConfigLocation = session.getNode(OLD_VALIDATORS_CONFIGURATION_LOCATION);
        final Node newConfigLocation = session.getNode(NEW_VALIDATORS_CONFIGURATION_LOCATION);

        for (final String validatorName : DEFAULT_VALIDATORS.keySet()) {
            // check if default validator is available
            if (oldConfigLocation.hasNode(validatorName)) {
                final Node oldValidator = oldConfigLocation.getNode(validatorName);
                final String oldValidatorName = oldValidator.getName();
                final Node newValidator = newConfigLocation.getNode(oldValidatorName);
                final String defaultPluginClass = DEFAULT_VALIDATORS.get(oldValidatorName);

                log.info("Migrating validator " + oldValidatorName);
                if (oldValidator.hasProperty(PLUGIN_CLASS)) {
                    try {
                        final String pluginClass = oldValidator.getProperty(PLUGIN_CLASS).getString();
                        // config has not changed
                        if (StringUtils.equals(pluginClass, defaultPluginClass)) {
                            // copy regex pattern to new validator
                            if (oldValidator.hasProperty(REGEX_PATTERN)) {
                                setNewProperty(newValidator, oldValidator.getProperty(REGEX_PATTERN));
                            }
                            oldValidator.remove();
                        } else {
                            // config is not default: keep old validator, delete new validator
                            newValidator.remove();
                        }
                    } catch (final RepositoryException e) {
                        log.error("Migrating validator configuration failed.", e);
                        return false;
                    }
                }
            } else {
                log.warn("Default validator " + validatorName + " was removed, but now bootstrapped again with " +
                        "default values. Please verify if this validator should be deleted.");
            }
        }

        // Successfully moved validator configuration
        return true;
    }

    private static void setNewProperty(final Node configLocation, final Property property) throws RepositoryException {
        configLocation.setProperty(property.getName(), property.getString());
    }
}
