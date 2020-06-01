/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugins.tagging.model;

import java.util.List;
import java.util.Map;

public class Configuration {
    private List<String> jcrContentTypes;
    private Map<String, Object> parameters;

    public List<String> getJcrContentTypes() {
        return jcrContentTypes;
    }

    public void setJcrContentTypes(final List<String> jcrContentTypes) {
        this.jcrContentTypes = jcrContentTypes;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(final Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
