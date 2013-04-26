/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

import java.util.List;

import org.wicketstuff.js.ext.data.ExtStore;

/**
 * @version "$Id$"
 */
public interface DeviceStorage {

    /**
     * Important overwrite to retrieve the store needed by the DeviceManager. Every Device Service need to have at least
     * this method to populate the extjscombobox appropriately.
     *
     * @return the extstore used for the device manager combobox
     */
    public ExtStore<StyleableDevice> getStore();

    /**
     * @return List of Stylable objects used for the CSS util.
     */
    public List<StyleableDevice> getStylables();

}
