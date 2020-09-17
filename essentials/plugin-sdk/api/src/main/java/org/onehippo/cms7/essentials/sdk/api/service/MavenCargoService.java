/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.service;

import java.net.URL;

import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;

/**
 * MavenCargoService provides methods to manipulate the configuration of the Maven Cargo plugin,
 * located in the project's root pom.xml file.
 *
 * It can be @Inject-ed into an Essentials plugin's REST resource or custom {@code Instruction}.
 */
public interface MavenCargoService {
    /**
     * Ensure that a dependency with &lt;classpath&gt;shared&lt;/classpath&gt; is present in the cargo plugin's
     * configuration.
     *
     * @param dependency dependency to be added to shared classpath
     * @return           true if the dependency exists upon returning, false otherwise
     */
    boolean addDependencyToCargoSharedClasspath(final MavenDependency dependency);

    /**
     * Ensure that a deployable (war) is present in the cargo plugin configuration, with the specified context path.
     *
     * @param dependency    dependency representing the web application (type war)
     * @param webappContext context path under which to deploy the web application
     * @return              true if the deployable exists upon returning, false otherwise
     */
    boolean addDeployableToCargoRunner(final MavenDependency dependency, final String webappContext);

    /**
     * Merge some incoming definitions (specified as a skeleton POM in a classpath resource) with the Maven cargo
     * plugin configuration of the project's root pom.xml.
     *
     * @param incomingDefinitions Maven cargo plugin configuration to be merged in
     * @return              true upon success, false otherwise
     */
    boolean mergeCargoProfile(URL incomingDefinitions);

    /**
     * Ensure that the specified property is present in the cargo plugin's systemProperties section.
     *
     * @param propertyName  name of the property
     * @param propertyValue value of the property
     * @return              true upon success, false otherwise
     */
    boolean addSystemProperty(final String propertyName, final String propertyValue);
}
