/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.repository.api;

import java.io.Serializable;
import java.util.Set;

public interface Folder extends Serializable {

    /**
     * Returns the identifier of the folder
     *
     * @return identifier
     */
    String getIdentifier();

    /**
     * Add a mixin by name and return {@code true} if it was not present yet and {@code false} otherwise.
     *
     * @param mixin the name of the mixin
     * @return if the mixin being added was not present yet
     */
    boolean addMixin(String mixin);

    /**
     * Remove a mixin by name and return {@code true} if it was present and {@code false} otherwise.
     *
     * @param mixin the name of the mixin
     * @return if the mixin being removed was present
     */
    boolean removeMixin(String mixin);

    /**
     * @return {@link Set} containing mixin names
     */
    Set<String> getMixins();
}
