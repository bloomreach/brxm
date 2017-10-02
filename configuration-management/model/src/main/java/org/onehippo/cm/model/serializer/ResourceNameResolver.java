/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.serializer;

/**
 * Unique resource name manager
 */
public interface ResourceNameResolver {
    /**
     * Generates unique name and add it to known list of names.
     * @param name
     * @return unique name within already known names
     */
    String generateName(String name);

    /**
     * Adds a known name to ensure {@link #generateName(String)} will generate a unique name when provided with
     * a same (conflicting) name.
     * @param name known name
     * @throws IllegalStateException if name already is known.
     */
    public void seedName(final String name);

    /**
     * Clears the known names
     */
    public void clear();
}
