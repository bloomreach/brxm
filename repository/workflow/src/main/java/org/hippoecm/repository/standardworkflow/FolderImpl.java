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
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toSet;
import static org.hippoecm.repository.util.JcrUtils.getMixinNodeTypes;

final class FolderImpl implements Folder {

    private static final Logger log = LoggerFactory.getLogger(FolderImpl.class);

    private final String identifier;
    private final Set<String> mixins;

    private FolderImpl(String identifier, Set<String> mixins) {
        this.identifier = identifier;
        this.mixins = mixins;
    }

    FolderImpl(Node folderNode) throws RepositoryException {
        this(folderNode.getIdentifier(), Stream.of(getMixinNodeTypes(folderNode)).map(NodeType::getName).collect(toSet()));
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean addMixin(final String mixin) {
        log.debug("Add mixin: {}", mixin);
        return mixins.add(mixin);
    }

    @Override
    public boolean removeMixin(final String mixin) {
        log.debug("Remove mixin: {}", mixin);
        return mixins.remove(mixin);
    }

    @Override
    public Set<String> getMixins() {
        log.debug("Returning mixins: {}", mixins);
        return new HashSet<>(mixins);
    }

    @Override
    public String toString() {
        return "FolderImpl{" +
                "mixins=" + mixins +
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
