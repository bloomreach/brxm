/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.service;

import java.util.Map;

/**
 * ContextXmlService provides functionality for manipulating Tomcat's context.xml file.
 *
 * It can be @Inject-ed into an Essentials plugin's REST resource ({@code BaseResource}) or custom {@code Instruction}.
 */
public interface ContextXmlService {
    /**
     * Ensure that context.xml defines a &lt;Resource/&gt; with the specified name and attributes.
     *
     * @param name       name of the Resource
     * @param attributes additional attribute key-value pairs (on top of the name attribute)
     * @return           true if the Resource exists upon returning, false otherwise
     */
    boolean addResource(String name, Map<String, String> attributes);

    /**
     * Ensure that context.xml defines an &lt;Environment/&gt; with the specified name and attributes.
     *
     * @param name       name of the Environment
     * @param attributes additional attribute key-value pairs (on top of the name attribute)
     * @return           true if the Environment exists upon returning, false otherwise
     */
    boolean addEnvironment(String name, Map<String, String> attributes);
}
