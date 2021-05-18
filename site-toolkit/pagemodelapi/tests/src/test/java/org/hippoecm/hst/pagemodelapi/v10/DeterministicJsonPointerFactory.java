/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v10;

import org.hippoecm.hst.pagemodelapi.v10.core.container.JsonPointerFactoryImpl;

/**
 * Deterministic json pointer creation to make sure that integration tests result in the same PMA response all the time
 */
public class DeterministicJsonPointerFactory extends JsonPointerFactoryImpl {

    private static final ThreadLocal<DeterministicJsonPointerFactory> tlDeterministicJsonPointerFactoryHolder = new ThreadLocal<>();

    public static DeterministicJsonPointerFactory get() {
        return tlDeterministicJsonPointerFactoryHolder.get();
    }

    private long id = 0;
    public static void reset() {
        tlDeterministicJsonPointerFactoryHolder.set(new DeterministicJsonPointerFactory());
    }

    @Override
    public String createJsonPointerId() {
        return createJsonPointerIdForString("id" + get().id++);
    }

}
