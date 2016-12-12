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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.ConfigurationGroup;
import org.raml.v2.api.loader.ResourceLoader;
import org.raml.yagi.framework.grammar.BaseGrammar;
import org.raml.yagi.framework.grammar.RuleFactory;
import org.raml.yagi.framework.grammar.rule.KeyValueRule;
import org.raml.yagi.framework.grammar.rule.Rule;
import org.raml.yagi.framework.nodes.KeyValueNodeImpl;
import org.raml.yagi.framework.nodes.Node;
import org.raml.yagi.framework.nodes.snakeyaml.NodeParser;
import org.raml.yagi.framework.phase.GrammarPhase;

class ConfigurationParser {

    static final String MODULE_CONFIG_FILENAME = "module-config.yaml";

    private static ResourceLoader notFoundResourceLoader = (resourceName) -> null;

    @SuppressWarnings("unused")
    private static class ParsedProperty extends KeyValueNodeImpl {
        public ParsedProperty() {
            super();
        }
        public ParsedProperty(ParsedProperty parsedProperty) {
            super(parsedProperty);
        }
        @Override
        public String toString() {
            return super.toString();
        }
    }

    @SuppressWarnings("unused")
    private static class ParsedNode extends KeyValueNodeImpl {
        public ParsedNode() {
            super();
        }
        public ParsedNode(ParsedNode parsedNode) {
            super(parsedNode);
        }
        @Override
        public String toString() {
            return super.toString();
        }
    }

    private static class Grammar extends BaseGrammar {
        private Rule getSourceRule() {
            return objectType().with(nodeField());
        }
        private KeyValueRule nodeField() {
            return field(nodeKey(), nodeValue()).then(ParsedNode.class);
        }
        private Rule nodeKey() {
            return regex("/.*");
        }
        private Rule nodeValue() {
            final RuleFactory ruleFactory = () -> objectType()
                    .with(propertyField())
                    .with(nodeField());
            return named("nodeValue", ruleFactory);
        }
        private KeyValueRule propertyField() {
            return field(regex("[^/]{1}.*"), any()).then(ParsedProperty.class);
        }
        private Rule getModuleRule() {
            return objectType().with(dependsField());
        }
        private KeyValueRule dependsField() {
            return field(string("depends"), stringType());
        }
    }

    List<ConfigurationGroup> parse(final List<URL> urls) throws IOException {
        Map<String, ConfigurationGroupImpl> groups = new LinkedHashMap<>();
        Map<String, ConfigurationModuleImpl> modules = new LinkedHashMap<>();
        Map<ConfigurationModuleImpl, URL> moduleConfigs = new HashMap<>();
        List<URL> sourceConfigs = new ArrayList<>();

        for (URL url : urls) {
            final String[] parts = url.toString().split("/");
            if (parts.length < 3) {
                throw new IllegalArgumentException(
                        "URL must be composed of 3 or more elements, found only " + parts.length + " elements in "
                        + url.toString());
            }
            final String groupName = parts[parts.length - 3];
            final String moduleName = parts[parts.length - 2];
            final String fileName = parts[parts.length - 1];

            ConfigurationGroupImpl group = groups.get(groupName);
            if (group == null) {
                group = new ConfigurationGroupImpl(groupName);
                groups.put(groupName, group);
            }

            ConfigurationModuleImpl module = modules.get(groupName + "/" + moduleName);
            if (module == null) {
                module = new ConfigurationModuleImpl(group, moduleName);
                modules.put(groupName + "/" + moduleName, module);
                group.addModule(module);
            }

            if (MODULE_CONFIG_FILENAME.equals(fileName)) {
                moduleConfigs.put(module, url);
            } else {
                sourceConfigs.add(url);
            }
        }

        for (ConfigurationModuleImpl module : moduleConfigs.keySet()) {
            final URL moduleConfig = moduleConfigs.get(module);
            parseModuleConfig(moduleConfig, module, modules);
        }

        return new ArrayList<>(groups.values());
    }

    private void parseModuleConfig(final URL url,
                                   final ConfigurationModuleImpl module,
                                   final Map<String, ConfigurationModuleImpl> modules) throws IOException {
        final Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8);
        final Node rootNode = NodeParser.parse(notFoundResourceLoader, "/", reader);
        final GrammarPhase grammarPhase = new GrammarPhase(new Grammar().getModuleRule());
        final Node node = grammarPhase.apply(rootNode);
        final Node dependsNode = node.get("depends");
        if (dependsNode != null) {
            final String dependsModuleName = dependsNode.toString();
            final ConfigurationModuleImpl depends = modules.get(dependsModuleName);
            if (depends == null) {
                throw new IllegalArgumentException("Cannot find module " + dependsModuleName);
            }
            module.addDependency(depends);
        }
    }

    private Node parse(final Reader reader) {
        final Node rootNode = NodeParser.parse(notFoundResourceLoader, "/", reader);
        final GrammarPhase grammarPhase = new GrammarPhase(new Grammar().getSourceRule());
        return grammarPhase.apply(rootNode);
    }

}
