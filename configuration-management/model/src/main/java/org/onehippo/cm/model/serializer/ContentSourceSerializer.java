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
package org.onehippo.cm.model.serializer;

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cm.model.impl.source.ContentSourceImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;

public class ContentSourceSerializer extends SourceSerializer {
    public ContentSourceSerializer(ModuleContext moduleContext, SourceImpl source, boolean explicitSequencing) {
        super(moduleContext, source, explicitSequencing);
    }

    public Node representSource() {

        NodeTuple nodeTuple = representContentDefinition(((ContentSourceImpl) source).getContentDefinition());

        final List<NodeTuple> sourceTuples = new ArrayList<>();
        sourceTuples.add(nodeTuple);
        return new MappingNode(Tag.MAP, sourceTuples, false);
    }
}
