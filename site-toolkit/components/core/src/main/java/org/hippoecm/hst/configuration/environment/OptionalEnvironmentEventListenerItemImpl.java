/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.environment;

import org.hippoecm.hst.core.jcr.EventListenerItemImpl;

import static org.hippoecm.hst.configuration.environment.EnvironmentUtils.getActiveHostGroup;
import static org.hippoecm.hst.configuration.environment.EnvironmentUtils.getActiveHostGroupPath;

public class OptionalEnvironmentEventListenerItemImpl extends EventListenerItemImpl {

    private String absolutePath = null;

    @Override
    public boolean isEnabled() {
        if (getActiveHostGroup() == null) {
            return false;
        }
        return super.isEnabled();
    }

    @Override
    public String getAbsolutePath() {
        if (absolutePath != null) {
            return absolutePath;
        }
        absolutePath = getActiveHostGroupPath();
        return absolutePath;
    }


}
