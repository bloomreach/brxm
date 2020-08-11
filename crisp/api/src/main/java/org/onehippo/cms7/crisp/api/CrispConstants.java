/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.api;

/**
 * CRISP related global constants.
 */
public class CrispConstants {

    /**
     * <code>HippoEvent</code> application name used internally in configuration change <code>HippoEvent</code> handling.
     */
    public static final String EVENT_APPLICATION_NAME = "crisp";

    /**
     * <code>HippoEvent</code> category name used internally in configuration change <code>HippoEvent</code> handling.
     */
    public static final String EVENT_CATEGORY_CONFIGURATION = "configuration";

    /**
     * <code>HippoEvent</code> action name used internally in configuration change <code>HippoEvent</code> handling.
     */
    public static final String EVENT_ACTION_UPDATE_CONFIGURATION = "updateConfiguration";

    /**
     * CRISP module configuration path in repository.
     */
    public static final String REGISTRY_MODULE_PATH = "/hippo:configuration/hippo:modules/crispregistry";

    /**
     * Node name of the CRISP module configuration node.
     */
    public static final String MODULE_CONFIG = "hippo:moduleconfig";

    /**
     * Node type name of the CRISP module configuration node.
     */
    public static final String NT_MODULE_CONFIG = "crisp:moduleconfig";

    /**
     * Node type name of the container node of all the CRISP <code>ResourceResolver</code> configuration nodes.
     */
    public static final String NT_RESOURCE_RESOLVER_CONTAINER = "crisp:resourceresolvercontainer";

    /**
     * Node type name of a CRISP <code>ResourceResolver</code> configuration node.
     */
    public static final String NT_RESOURCE_RESOLVER = "crisp:resourceresolver";

    /**
     * Node name of the container node of all the CRISP <code>ResourceResolver</code> configuration nodes.
     */
    public static final String RESOURCE_RESOLVER_CONTAINER = "crisp:resourceresolvercontainer";

    /**
     * Default CRISP module configuration node path.
     */
    public static final String DEFAULT_CRISP_MODULE_CONFIG_PATH = REGISTRY_MODULE_PATH + "/" + MODULE_CONFIG;

    public static final String CRISP_MODULE_CONFIG_PATH_PROP_NAME = "crisp.moduleconfig.path";

    /**
     * Property name of a CRISP ResourceResolver configuration node, containing its own <code>ResourceResolver</code>
     * bean configuration.
     */
    public static final String BEAN_DEFINITION = "crisp:beandefinition";

    /**
     * Property name of a CRISP ResourceResolver configuration node, containing property names that can be used
     * as variables in the value of {@link #BEAN_DEFINITION} property.
     */
    public static final String PROP_NAMES = "crisp:propnames";

    /**
     * Property name of a CRISP ResourceResolver configuration node, defines a resource scope
     * Valid values are - platform (for CMS & PLATFORM app types) and site name.
     */
    public static final String SITE_SCOPES = "crisp:sitescopes";

    /**
     * Property name of a CRISP ResourceResolver configuration node, containing property values that can be used
     * in variable interpolations in the value of {@link #BEAN_DEFINITION} property.
     */
    public static final String PROP_VALUES = "crisp:propvalues";

    private CrispConstants() {
    }
}
