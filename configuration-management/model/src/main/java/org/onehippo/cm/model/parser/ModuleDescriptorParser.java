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
package org.onehippo.cm.model.parser;

import java.io.InputStream;
import java.util.Map;

import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import static org.onehippo.cm.model.Constants.AFTER_KEY;
import static org.onehippo.cm.model.Constants.SCOPE_EXTENSION_KEY;
import static org.onehippo.cm.model.Constants.GROUP_KEY;
import static org.onehippo.cm.model.Constants.MODULE_KEY;
import static org.onehippo.cm.model.Constants.NAME_KEY;
import static org.onehippo.cm.model.Constants.PROJECT_KEY;
import static org.onehippo.cm.model.Constants.SCOPE_CORE_KEY;
import static org.onehippo.cm.model.Constants.SCOPE_KEY;

public class ModuleDescriptorParser extends AbstractBaseParser {

    public ModuleDescriptorParser(final boolean explicitSequencing) {
        super(explicitSequencing);
    }

    public ModuleImpl parse(final InputStream inputStream, final String location) throws ParserException {
        final Node node = composeYamlNode(inputStream, location);
        final Map<String, Node> rootNodeMap =
                asMapping(node, new String[]{GROUP_KEY, PROJECT_KEY, MODULE_KEY}, new String[]{SCOPE_KEY});

        final Node groupNode = rootNodeMap.get(GROUP_KEY);
        final Node projectNode = rootNodeMap.get(PROJECT_KEY);
        final Node moduleNode = rootNodeMap.get(MODULE_KEY);
        final Node scopeNode = rootNodeMap.get(SCOPE_KEY);

        final GroupImpl groupImpl = constructGroup(groupNode);
        final ProjectImpl project = constructProject(projectNode, groupImpl);

        return constructModule(moduleNode, scopeNode, project);
    }

    protected GroupImpl constructGroup(final Node src) throws ParserException {

        if (src instanceof ScalarNode) {
            return new GroupImpl(asStringScalar(src));
        } else {
            final Map<String, Node> groupMap = asMapping(src, new String[]{NAME_KEY}, new String[]{AFTER_KEY});
            final String name = asStringScalar(groupMap.get(NAME_KEY));
            final GroupImpl group = new GroupImpl(name);
            group.addAfter(asSingleOrSetOfStrScalars(groupMap.get(AFTER_KEY)));
            return group;
        }
    }

    protected ProjectImpl constructProject(final Node src, final GroupImpl parent) throws ParserException {
        if (src instanceof ScalarNode) {
            return parent.addProject(asStringScalar(src));
        } else {
            final Map<String, Node> sourceMap = asMapping(src, new String[]{NAME_KEY}, new String[]{AFTER_KEY});
            final String name = asStringScalar(sourceMap.get(NAME_KEY));
            final ProjectImpl project = parent.addProject(name);
            project.addAfter(asSingleOrSetOfStrScalars(sourceMap.get(AFTER_KEY)));
            return project;
        }
    }

    protected ModuleImpl constructModule(final Node src, final Node scopeNode, final ProjectImpl parent) throws ParserException {
        ModuleImpl module;
        if (src instanceof ScalarNode) {
            module = parent.addModule(asStringScalar(src));
        } else {
            final Map<String, Node> map =
                    asMapping(src, new String[]{NAME_KEY}, new String[]{AFTER_KEY, SCOPE_EXTENSION_KEY});
            final String name = asStringScalar(map.get(NAME_KEY));
            module = parent.addModule(name);
            module.addAfter(asSingleOrSetOfStrScalars(map.get(AFTER_KEY)));
        }

        if (scopeNode == null) {
            // null extensionName indicates a core module, but this isn't _explicitly_ a core module
            module.setExtensionName(null);
        }
        else {
            final String scope = asStringScalar(scopeNode);
            switch (scope) {
                case SCOPE_CORE_KEY:
                    // this is explicitly marked as a core module
                    module.setExplicitCore(true);
                    break;
                case SCOPE_EXTENSION_KEY:
                    // we don't know the actual extension name here, so use a constant value as stand-in
                    module.setExplicitCore(false);
                    module.setExtensionName(SCOPE_EXTENSION_KEY);
                    break;
                default:
                    throw new ParserException("Module scope must have value \"core\" or \"extension\" but found: " + scope);
            }
        }

        return module;
    }
}
