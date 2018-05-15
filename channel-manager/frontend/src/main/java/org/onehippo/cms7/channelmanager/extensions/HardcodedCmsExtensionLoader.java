/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.extensions;

import java.util.Arrays;
import java.util.List;

public class HardcodedCmsExtensionLoader implements CmsExtensionLoader {

    @Override
    public List<CmsExtension> loadCmsExtensions() {
        final CmsExtensionBean testExtension1 = new CmsExtensionBean();
        testExtension1.setId("test1");
        testExtension1.setDisplayName("Test One");
        testExtension1.setContext(CmsExtensionContext.PAGE);
        testExtension1.setUrlPath("/site/test-extension");

        final CmsExtensionBean testExtension2 = new CmsExtensionBean();
        testExtension2.setId("test2");
        testExtension2.setDisplayName("Test Two");
        testExtension2.setContext(CmsExtensionContext.PAGE);
        testExtension2.setUrlPath("/site/test-extension");

        return Arrays.asList(testExtension1, testExtension2);
    }
}
