/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.config;

/**
 * PluginParameterService is used by an Essentials plugin to signal certain parameters to the dashboard, which are
 * used to control how the user can interact with the plugin.
 */
public interface PluginParameterService {

    /**
     * This predicate indicates if the plugin has a "set-up phase", i.e. if resources need to be pushed into
     * the project repository or sources. If the plugin descriptor specifies a "packageFile" (using the dashboard's
     * generalized way to do the set-up), this predicate is ignored.
     *
     * @return flag indicating if this plugin has a setup phase.
     */
    boolean hasSetup();

    /**
     * This predicate indicates if the plugin has parameters for controlling the generalized setup procedure.
     *
     * It is only considered if the plugin indicates, by way of the "packageFile" parameter, that generalized setup
     * is applicable. The Dashboard can use this information to short-circuit the setup phase.
     *
     * @return flag indicating if parameters are to be considered during generalized setup.
     */
    boolean hasGeneralizedSetupParameters();

    /**
     * This predicate indicates if, upon completion of the setup phase, a rebuild/restart cycle is necessary.
     * Typically, such a cycle is necessary when resources (classes, images, rendering templates etc) are installed
     * into the project.
     *
     * @return flag indicating if a rebuild is necessary after the setup phase.
     */
    boolean doesSetupRequireRebuild();

    /**
     * This predicate indicates if a plugin provides a configuration screen, which can (still) be used when the
     * plugin is fully installed and set up (installState "installed").
     *
     * It is up to the plugin's front-end to also show this (or a different) configuration screen before the setup
     * phase was completed.
     *
     * @return flag indicating if a configuration option should be displayed for a fully installed plugin.
     */
    boolean hasConfiguration();
}
