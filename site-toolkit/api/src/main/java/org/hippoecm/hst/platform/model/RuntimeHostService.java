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
package org.hippoecm.hst.platform.model;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;

public interface RuntimeHostService {

    /**
     *
     * @param hostName the host which makes the request (eg: cms.example.org)
     * @param sourceHostGroupName the host group which matches the request (eg: dev-localhost)
     * @param autoHostTemplateURL the URL which is defined in hst:autohosttemplate property (eg: https://*.example.org)
     * @param contextPath
     * @return the {@link VirtualHosts} object with the model for the {@code hostName} included
     */
    VirtualHosts create(String hostName, String sourceHostGroupName, String autoHostTemplateURL, String contextPath);
}
