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

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.yaml.snakeyaml.nodes.Node;

import static org.onehippo.cm.engine.Constants.CONFIGURATIONS_KEY;
import static org.onehippo.cm.engine.Constants.CONFIGURATION_KEY;
import static org.onehippo.cm.engine.Constants.PROJECTS_KEY;
import static org.onehippo.cm.engine.Constants.PROJECT_KEY;
import static org.onehippo.cm.engine.Constants.AFTER_KEY;
import static org.onehippo.cm.engine.Constants.MODULES_KEY;
import static org.onehippo.cm.engine.Constants.MODULE_KEY;

public class RepoConfigParser extends AbstractBaseParser {

    public RepoConfigParser(final boolean explicitSequencing) {
        super(explicitSequencing);
    }

    public Map<String, Configuration> parse(final InputStream inputStream, final String location) throws ParserException {
        final Node node = composeYamlNode(inputStream, location);

        final Map<String, Configuration> result = new LinkedHashMap<>();
        final Map<String, Node> sourceMap = asMapping(node, new String[]{CONFIGURATIONS_KEY}, null);

        for (Node configurationNode : asSequence(sourceMap.get(CONFIGURATIONS_KEY))) {
            constructConfiguration(configurationNode, result);
        }

        return result;
    }

    private void constructConfiguration(final Node src, final Map<String, Configuration> parent) throws ParserException {
        final Map<String, Node> configurationMap = asMapping(src, new String[]{CONFIGURATION_KEY, PROJECTS_KEY}, new String[]{AFTER_KEY});
        final String name = asStringScalar(configurationMap.get(CONFIGURATION_KEY));
        final ConfigurationImpl configuration = new ConfigurationImpl(name);
        configuration.addAfter(asSingleOrSetOfStrScalars(configurationMap.get(AFTER_KEY)));
        parent.put(name, configuration);

        for (Node projectNode : asSequence(configurationMap.get(PROJECTS_KEY))) {
            constructProject(projectNode, configuration);
        }
    }

    private void constructProject(final Node src, final ConfigurationImpl parent) throws ParserException {
        final Map<String, Node> sourceMap = asMapping(src, new String[]{PROJECT_KEY, MODULES_KEY}, new String[]{AFTER_KEY});
        final String name = asStringScalar(sourceMap.get(PROJECT_KEY));
        final ProjectImpl project = parent.addProject(name);
        project.addAfter(asSingleOrSetOfStrScalars(sourceMap.get(AFTER_KEY)));

        for (Node moduleNode : asSequence(sourceMap.get(MODULES_KEY))) {
            constructModule(moduleNode, project);
        }
    }

    private void constructModule(final Node src, final ProjectImpl parent) throws ParserException {
        final Map<String, Node> map = asMapping(src, new String[]{MODULE_KEY}, new String[]{AFTER_KEY});
        final String name = asStringScalar(map.get(MODULE_KEY));
        final ModuleImpl module = parent.addModule(name);
        module.addAfter(asSingleOrSetOfStrScalars(map.get(AFTER_KEY)));
    }

}
