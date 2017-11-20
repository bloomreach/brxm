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

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

/**
 * Exposes services for manipulating web.xml files.
 */
public interface WebXmlService {
    /**
     * Ensure that a certain HST bean class scanning pattern is registered in the project's Site web.xml.
     *
     * @param context {@link PluginContext} for accessing the project
     * @param pattern pattern for classpath scanning of annotated HST beans, e.g. 'org/example/**\/*.class'
     * @return        true if pattern is registered (or already there), false upon error
     */
    boolean addHstBeanClassPattern(PluginContext context, String pattern);
}
