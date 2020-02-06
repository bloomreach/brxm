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

package org.hippoecm.repository.standardworkflow;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hippoecm.repository.api.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderImpl implements Folder {

    private static final Logger LOGGER = LoggerFactory.getLogger(FolderImpl.class);
    private Set<String> mixins = new HashSet<>();

    @Override
    public String addMixin(final String mixin) {
        LOGGER.debug("Add mixin: {}", mixin);
        mixins.add(mixin);
        return mixin;
    }

    @Override
    public String removeMixin(final String mixin) {
        LOGGER.debug("Remove mixin: {}", mixin);
        mixins.remove(mixin);
        return mixin;
    }

    @Override
    public Set<String> getMixins() {
        LOGGER.debug("Returning mixins: {}", String.join(", ", mixins));
        return new HashSet<>(mixins);
    }

    @Override
    public String toString() {
        return "FolderImpl{" +
                "mixins=" + String.join(",", mixins) +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FolderImpl)) {
            return false;
        }
        final FolderImpl folder = (FolderImpl) o;
        return mixins.equals(folder.mixins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mixins);
    }
}
