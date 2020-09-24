/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standards.perspective;

/**
 * This interface is the java alternative of the NavLocation typescript interface
 * defined in the bloomreach navigation application.
 * <p>
 * See projects/navapp-communication/src/lib/parent-api.ts of the @bloomreach/navapp-communication dependency
 * in the package.json of the engine module.
 */
public interface ParentApi {

    void updateNavLocation(String path, String breadcrumbLabel, boolean addHistory);
}
