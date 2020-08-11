/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.container.header;

import java.util.List;
import java.util.Map;

public interface AccessControlAllowHeadersService {

    /**
     * Returns a comma separate String of the acces control allowed headers, removing duplicates
     */
    String getAllowedHeadersString();

    /**
     * @return all the access control allowed headers. The returned List objects are immutable
     */
    Map<String, List<String>> getAllAllowedHeaders();

    /**
     * @return the immutable list of access control allowed headers for {@code module}
     */
    List<String> getAllowedHeaders(String module);

    /**
     * <p>
     *     Since every downstream project can contribute access control allowed headers, we keep them separate by
     *     {@code module} : The reason is that if the allowed headers for a module change, it can just set them again
     *     without the module having to know what the previous contributed allowed header was
     * </p>
     * @param module the module that contributed these allowed headers, for example 'targeting'
     * @param allowedHeaders the allowed headers contributed by the {@code module}, not allowed to be {@code null}
     */
    void setAllowedHeaders(String module, List<String> allowedHeaders);
}
