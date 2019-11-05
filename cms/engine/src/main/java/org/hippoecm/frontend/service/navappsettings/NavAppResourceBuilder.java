/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend.service.navappsettings;

import java.net.URI;

import org.hippoecm.frontend.service.NavAppResource;
import org.hippoecm.frontend.service.ResourceType;

final class NavAppResourceBuilder {

    private URI url;
    private ResourceType resourceType;

    NavAppResourceBuilder resourceUrl(URI url) {
        this.url = url;
        return this;
    }

    NavAppResourceBuilder resourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    NavAppResource build() {
        return new NavAppResource() {
            @Override
            public URI getUrl() {
                return url;
            }

            @Override
            public ResourceType getResourceType() {
                return resourceType;
            }
        };
    }
}
