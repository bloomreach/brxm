/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.onehippo.cm.model.Group;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.Orderable;
import org.onehippo.cm.model.Project;
import org.onehippo.cm.model.impl.GroupImpl;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import static org.onehippo.cm.model.Constants.AFTER_KEY;
import static org.onehippo.cm.model.Constants.GROUPS_KEY;
import static org.onehippo.cm.model.Constants.GROUP_KEY;
import static org.onehippo.cm.model.Constants.MODULES_KEY;
import static org.onehippo.cm.model.Constants.MODULE_KEY;
import static org.onehippo.cm.model.Constants.PROJECTS_KEY;
import static org.onehippo.cm.model.Constants.PROJECT_KEY;


public class ModuleDescriptorSerializer extends AbstractBaseSerializer {

    public ModuleDescriptorSerializer(final boolean explicitSequencing) {
        super(explicitSequencing);
    }

    public void serialize(final OutputStream outputStream, final Map<String, ? extends Group> groups) throws IOException {
        final Node node = representGroups(groups);
        serializeNode(outputStream, node);
    }

    private Node representGroups(final Map<String, ? extends Group> groups) {
        final List<NodeTuple> rootTuples = new ArrayList<>();

        final List<Node> groupNodes = groups.values().stream().map(this::representGroup)
                .collect(Collectors.toList());
        rootTuples.add(createStrSeqTuple(GROUPS_KEY, groupNodes));

        return new MappingNode(Tag.MAP, rootTuples, false);
    }

    private Node representGroup(final Group group) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.addAll(representOrderable(group, GROUP_KEY));

        final List<Node> projectNodes = group.getProjects().stream().map(this::representProject)
                .collect(Collectors.toList());
        tuples.add(createStrSeqTuple(PROJECTS_KEY, projectNodes));

        return new MappingNode(Tag.MAP, tuples, false);
    }

    private Node representProject(final Project project) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.addAll(representOrderable(project, PROJECT_KEY));

        final List<Node> moduleNodes = project.getModules().stream().map(this::representModule).collect(Collectors.toList());
        tuples.add(createStrSeqTuple(MODULES_KEY, moduleNodes));

        return new MappingNode(Tag.MAP, tuples, false);
    }

    private Node representModule(final Module module) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.addAll(representOrderable(module, MODULE_KEY));
        return new MappingNode(Tag.MAP, tuples, false);
    }

    private List<NodeTuple> representOrderable(final Orderable orderable, String nameProperty) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.add(createStrStrTuple(nameProperty, orderable.getName()));

        final Set<String> afters = orderable.getAfter();
        switch (afters.size()) {
            case 0:
                break;
            case 1:
                tuples.add(createStrStrTuple(AFTER_KEY, afters.iterator().next()));
                break;
            default:
                final List<Node> afterNodes = afters.stream()
                        .map(after -> new ScalarNode(Tag.STR, after, null, null, null))
                        .collect(Collectors.toList());
                tuples.add(createStrSeqTuple(AFTER_KEY, afterNodes, true));
                break;
        }
        return tuples;
    }

}
