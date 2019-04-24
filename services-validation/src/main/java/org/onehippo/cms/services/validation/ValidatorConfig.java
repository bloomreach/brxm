package org.onehippo.cms.services.validation;

import java.util.Map;

/**
 * Wrapper around the configuration of a validator.
 */
interface ValidatorConfig {

    /**
     * @return the name of a validator
     */
    String getName();

    /**
     * @return the fully qualified Java class name of a validator.
     */
    String getClassName();

    /**
     * @return the custom configuration properties of a validator,
     * or an empty map if no custom properties have been configured.
     */
    Map<String, String> getProperties();

}
