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
 * @version "$Id$"
 */
public interface PluginConfigService extends AutoCloseable{

    /**
     * Stores given configuration
     *
     * @param document Document instance
     */
    boolean write(Document document);

    boolean write(Document document, String pluginId);

    <T extends Document> T read(String pluginClass, Class<T> clazz);

    /**
     * Reads config document
     *
     * @return null object if nothing found
     */
    <T extends Document> T read(Class<T> clazz);
}
