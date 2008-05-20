/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.core.impl;

import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.sa.core.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaPluginConfig extends ValueMap implements IPluginConfig {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JavaPluginConfig.class);

    public JavaPluginConfig() {
    }

    public IPluginConfig[] getConfigArray(String key) {
        return null;
    }

}
