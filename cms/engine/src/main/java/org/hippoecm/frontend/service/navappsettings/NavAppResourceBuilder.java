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
import java.util.Objects;

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
        return new NavAppResourceImpl(url, resourceType);
    }

    private static final class NavAppResourceImpl implements NavAppResource {
        private final URI url;
        private final ResourceType resourceType;

        private NavAppResourceImpl(URI url, ResourceType resourceType) {
            Objects.requireNonNull(url);
            Objects.requireNonNull(resourceType);
            this.url = url;
            this.resourceType = resourceType;
        }

        @Override
        public URI getUrl() {
            return url;
        }

        @Override
        public ResourceType getResourceType() {
            return resourceType;
        }

        @Override
        public String toString() {
            return resourceType + ", " + url;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final NavAppResourceImpl that = (NavAppResourceImpl) o;

            if (!url.equals(that.url)) {
                return false;
            }
            return resourceType == that.resourceType;
        }

        @Override
        public int hashCode() {
            int result = url.hashCode();
            result = 31 * result + resourceType.hashCode();
            return result;
        }
    }
}
