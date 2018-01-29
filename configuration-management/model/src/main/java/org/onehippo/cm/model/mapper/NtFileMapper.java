/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.mapper;

import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.DefinitionNode;
import org.onehippo.cm.model.tree.DefinitionProperty;
import org.onehippo.cm.model.tree.Value;

import static org.apache.jackrabbit.JcrConstants.NT_FILE;

public class NtFileMapper extends AbstractFileMapper {

    @Override
    public String apply(final Value value) {

        final DefinitionProperty property = value.getParent();
        final DefinitionNode resourceNode = property.getParent();
        if (resourceNode.isRoot()) {
            // TODO: perhaps we can still generate a file extension from jcr:mimeType
            return null;
        }

        final DefinitionNode fileNode = resourceNode.getParent();
        if (!isType(fileNode, JcrPaths.getSegment(NT_FILE))) {
            return null;
        }

        if (fileNode.isRoot()) {
            return constructFilePathFromJcrPath(fileNode.getPath());
        }
        else {
            final String folderPath = constructFilePathFromJcrPath(fileNode.getParent().getPath());
            final String name = mapNodeNameToFileName(fileNode.getName());
            return String.format("%s/%s", folderPath, name);
        }
    }
}
