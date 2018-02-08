/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.DefinitionNode;
import org.onehippo.cm.model.tree.DefinitionProperty;
import org.onehippo.cm.model.tree.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File mapper for hippo:resource node type -- assumes that either hippo:filename or the node name of the parent asset
 * set node will be a reasonable default file name. (This does NOT go up to the grandparent hippo:handle and extract
 * the hippo:name that might be set by a user "Rename..." action in the CMS. This choice is debatable.)
 */
public class HippoResourceFileMapper extends AbstractFileMapper {

    private static final Logger logger = LoggerFactory.getLogger(HippoResourceFileMapper.class);

    static final JcrPathSegment HIPPO_RESOURCE = JcrPaths.getSegment("hippo:resource");
    private static final String DEFAULT_FILENAME = "data.bin";
    private static final JcrPathSegment HIPPO_FILENAME = JcrPaths.getSegment("hippo:filename");

    @Override
    public String apply(Value value) {

        try {
            final DefinitionProperty property = value.getParent();
            final DefinitionNode resourceNode = property.getParent();

            if (!isType(resourceNode, HIPPO_RESOURCE)) {
                return null;
            }

            final DefinitionProperty filename = resourceNode.getProperty(HIPPO_FILENAME);
            String name;
            String folderPath = ".";

            // if the hippo:resource has an explicit hippo:filename property, use that
            if (filename != null) {
                name = filename.getValue().getString();
            }
            // otherwise, use the parent node's name as the filename
            // EXCEPT! first handle the odd case of a hippo:resource directly attached to the repository root node
            else if (resourceNode.getJcrPath().getParent().isRoot()) {
                name = DEFAULT_FILENAME;
                folderPath = constructFilePathFromJcrPath(resourceNode.getPath());
            }
            else {
                // the parent node usually has a good name, but it might be an SNS
                name = mapNodeNameToFileName(resourceNode.getJcrPath().getParent().getLastSegment().suppressIndex().toString());

                // one might be tempted to use the parent node to generate the folder path, since we'll repeat "asset"
                // BUT! hippo:resource might appear in more contexts than just assets, so we need to be conservative
                folderPath = constructFilePathFromJcrPath(resourceNode.getPath());
            }
            return String.format("%s/%s", folderPath, name);
        } catch (Exception e) {
            logger.error("HippoResourceFileMapper failed", e);
            return null;
        }
    }

}
