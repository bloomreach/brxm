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
package org.hippoecm.hst.site.container;

import java.io.Serializable;

import org.hippoecm.hst.core.container.ContainerException;

/**
 * HST Site Configurer which loads configuration and initialize/destroy HST container.
 */
public interface HstSiteConfigurer extends Serializable {

    /**
     * HST Container configuration XML file resource parameter.
     * e.g, file://${catalina.base}/conf/hst.xml
     */
    String HST_CONFIGURATION_PARAM = "hst-configuration";

    /**
     * HST Container configuration properties file resource parameter.
     * e.g, file://${catalina.base}/conf/hst.properties
     */
    String HST_CONFIG_PROPERTIES_PARAM = "hst-config-properties";

    /**
     * Flag to override properties by Java System properties.
     * Either "true" or "false".
     */
    String HST_SYSTEM_PROPERTIES_OVERRIDE_PARAM = "hst-system-properties-override";

    /**
     * The environment specific HST Container configuration properties file resource path,
     * which is looked up first if there's no configuration in the web application context level.
     * e.g, ${catalina.base}/conf/hst.properties
     */
    String HST_CONFIG_ENV_PROPERTIES_PARAM = "hst-env-config-properties";

    /**
     * Flag to make the initial loading of all hst configuration jcr nodes lazy (waiting for the first request) instead
     * of all the config nodes being actively fetched directly after the HST Spring Component Manager creation
     * Either "true" or "false". lazyHstConfigurationLoading
     */
    String HST_LAZY_HST_CONFIGURATION_LOADING_PARAM = "hst-lazy-configuration-loading";


    /**
     * Loads configuration and initialize HST Container.
     * @throws ContainerException if fails to load and initialize
     */
    void initialize() throws ContainerException;

    /**
     * Destroys HST Container.
     * @throws ContainerException if fails to destroy
     */
    void destroy() throws ContainerException;

}
