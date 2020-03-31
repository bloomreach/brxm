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
 */
package org.hippoecm.frontend.translation.workflow;

import java.util.Set;

public interface Translations {

    /**
     * @return Set of available translation keys for a handle.
     */
    Set<String> getAvailableTranslations();

    /**
     * @return {@code true} if adding translations is allowed, otherwise false.
     */
    Boolean canAddTranslation();

    static Translations of(final Set<String> availableTranslations, final Boolean canAddTranslation){
        return new Translations() {
            @Override
            public Set<String> getAvailableTranslations() {
                return availableTranslations;
            }

            @Override
            public Boolean canAddTranslation() {
                return canAddTranslation;
            }
        };
    }

}
