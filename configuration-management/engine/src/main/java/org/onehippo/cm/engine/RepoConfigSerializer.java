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
package org.onehippo.cm.engine;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.api.model.Project;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

public class RepoConfigSerializer extends AbstractBaseSerializer {

    public void serialize(final OutputStream outputStream, final Map<String, Configuration> configurations) throws IOException {
        final Node node = representRepoConfig(configurations);
        serializeNode(outputStream, node);
    }

    private Node representRepoConfig(final Map<String, Configuration> configurations) {
        final List<NodeTuple> rootTuples = new ArrayList<>();

        final List<Node> configurationNodes = new ArrayList<>();
        for (Configuration configuration : configurations.values()) {
            configurationNodes.add(representConfiguration(configuration));
        }
        rootTuples.add(createStrSeqTuple("configurations", configurationNodes));

        return new MappingNode(Tag.MAP, rootTuples, false);
    }

    private Node representConfiguration(final Configuration configuration) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.addAll(representOrderable(configuration));

        final List<Node> projectNodes = new ArrayList<>();
        for (Project project: configuration.getProjects()) {
            projectNodes.add(representProject(project));
        }
        tuples.add(createStrSeqTuple("projects", projectNodes));

        return new MappingNode(Tag.MAP, tuples, false);
    }

    private Node representProject(final Project project) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.addAll(representOrderable(project));

        final List<Node> moduleNodes = new ArrayList<>();
        for (Module module : project.getModules()) {
            moduleNodes.add(representModule(module));
        }
        tuples.add(createStrSeqTuple("modules", moduleNodes));

        return new MappingNode(Tag.MAP, tuples, false);
    }

    private Node representModule(final Module module) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.addAll(representOrderable(module));
        return new MappingNode(Tag.MAP, tuples, false);
    }

    private List<NodeTuple> representOrderable(final Orderable orderable) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.add(createStrStrTuple("name", orderable.getName()));

        final Set<String> afters = orderable.getAfter();
        switch (afters.size()) {
            case 0:
                break;
            case 1:
                tuples.add(createStrStrTuple("after", afters.iterator().next()));
                break;
            default:
                final List<Node> afterNodes = new ArrayList<>();
                for (String after : afters) {
                    afterNodes.add(new ScalarNode(Tag.STR, after, null, null, null));
                }
                tuples.add(createStrSeqTuple("after", afterNodes, true));
                break;
        }
        return tuples;
    }

}
