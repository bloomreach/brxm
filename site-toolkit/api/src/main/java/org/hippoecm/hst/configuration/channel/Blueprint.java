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
package org.hippoecm.hst.configuration.channel;

/**
 * A Blueprint is a "node" provided by the developers that is used to create and manage channels by the ChannelManager,
 * and is defined by the node type hst:blueprint. Please see the hst.cnd for the node type definition.
 */
public interface Blueprint {

    /**
     * Unique id for this blueprint
     *
     * @return The node name
     */
    String getId();

    /**
     * The name of the blue print as provided in the property hst:name, if the property doesn't
     * exist, the id (node name) is returned as the name.
     *
     * @return the name of the blue print.
     */
    String getName();

    /**
     * @return value of hst:description of the blueprint node, returns null if the property doesn't exist.
     */
    String getDescription();

    /**
     * @return the CMS plugin that is able to edit the configuration for this blueprint
     */
    String getCmsPluginClass();

    /**
     * Class for interface that is exposed at runtime to components
     *
     * @return
     */
    String getParameterInfo();


}
