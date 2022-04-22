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
package org.hippoecm.hst.pagemodelapi.common.context;

public class ApiVersionProvider {

    public enum ApiVersion {
        V10
    }

    private static ThreadLocal<ApiVersion> tlApiVersionHolder = new ThreadLocal<>();

    private ApiVersionProvider() {

    }

    public static ApiVersion get() {
        return tlApiVersionHolder.get();
    }


    public static void set(ApiVersion apiVersion) {
        tlApiVersionHolder.set(apiVersion);
    }


    public static void clear() {
        tlApiVersionHolder.remove();
    }

}
