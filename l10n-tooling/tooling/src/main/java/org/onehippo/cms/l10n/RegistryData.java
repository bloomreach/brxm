/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.l10n;

import java.util.HashMap;
import java.util.Map;

public class RegistryData {
    
    private BundleType bundleType;
    private Map<String, KeyData> keyData = new HashMap<>();

    public BundleType getBundleType() {
        return bundleType;
    }

    public void setBundleType(final BundleType bundleType) {
        this.bundleType = bundleType;
    }

    public Map<String, KeyData> getKeyData() {
        return keyData;
    }

    public void setKeyData(final Map<String, KeyData> keyData) {
        this.keyData = keyData;
    }
}
