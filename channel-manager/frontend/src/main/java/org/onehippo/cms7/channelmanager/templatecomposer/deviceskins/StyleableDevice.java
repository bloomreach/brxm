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

/**
 * The basic model interface for the device manager
 *
 * @version "$Id$"
 */
public interface StyleableDevice {

    /**
     * @return The CSS style for the iframe
     */
    public String getStyle();

    /**
     * @return The CSS style for the wrapper of the iframe
     */
    public String getWrapStyle();

    /**
     * @return Name which pops up in the combobox.
     */
    public String getName();

    /**
     * @return Identifier which is saved on the channel.
     */
    public String getId();

}
