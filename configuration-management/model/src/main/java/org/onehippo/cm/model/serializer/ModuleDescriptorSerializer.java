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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.onehippo.cm.model.OrderableByName;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import com.google.common.collect.ImmutableList;

import static org.onehippo.cm.model.Constants.AFTER_KEY;
import static org.onehippo.cm.model.Constants.DEFAULT_EXPLICIT_SEQUENCING;
import static org.onehippo.cm.model.Constants.GROUP_KEY;
import static org.onehippo.cm.model.Constants.MODULE_KEY;
import static org.onehippo.cm.model.Constants.NAME_KEY;
import static org.onehippo.cm.model.Constants.PROJECT_KEY;

public class ModuleDescriptorSerializer extends AbstractBaseSerializer {

    public ModuleDescriptorSerializer() {
        this(DEFAULT_EXPLICIT_SEQUENCING);
    }
    public ModuleDescriptorSerializer(final boolean explicitSequencing) {
        super(explicitSequencing);
    }

    public void serialize(final OutputStream outputStream, final ModuleImpl module) throws IOException {

        final GroupImpl group = module.getProject().getGroup();

        final Node groupNode = representNode(group);
        final NodeTuple groupTuple = new NodeTuple(createStrScalar(GROUP_KEY), groupNode);

        final Node projectNode = representNode(module.getProject());
        final NodeTuple projectTuple = new NodeTuple(createStrScalar(PROJECT_KEY), projectNode);

        final Node moduleNode = representNode(module);
        final NodeTuple moduleTuple = new NodeTuple(createStrScalar(MODULE_KEY), moduleNode);

        final List<NodeTuple> rootTuples = ImmutableList.of(groupTuple, projectTuple, moduleTuple);
        final MappingNode mappingNode = new MappingNode(Tag.MAP, rootTuples, false);

        serializeNode(outputStream, mappingNode);
    }

    protected Node representNode(final OrderableByName orderable) {
        final ScalarNode nameScalar = createStrScalar(orderable.getName());
        if (CollectionUtils.isEmpty(orderable.getAfter())) {
            return nameScalar;
        } else {
            final List<NodeTuple> itemTuples = new ArrayList<>();
            final NodeTuple nameTuple = new NodeTuple(createStrScalar(NAME_KEY), nameScalar);
            itemTuples.add(nameTuple);
            Optional<NodeTuple> afterTuple = createAfterTuple(orderable);
            afterTuple.ifPresent(itemTuples::add);
            return new MappingNode(Tag.MAP, itemTuples, false);
        }
    }

    protected Optional<NodeTuple> createAfterTuple(final OrderableByName orderable) {

        final Set<String> afters = orderable.getAfter();
        switch (afters.size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(createStrStrTuple(AFTER_KEY, afters.iterator().next()));
            default:
                final List<Node> afterNodes = orderable.getAfter().stream().map(after -> new ScalarNode(Tag.STR, after, null, null, null)).collect(Collectors.toList());
                final NodeTuple afterTuple = createStrSeqTuple(AFTER_KEY, afterNodes, true);
                return Optional.of(afterTuple);
        }
    }
}
