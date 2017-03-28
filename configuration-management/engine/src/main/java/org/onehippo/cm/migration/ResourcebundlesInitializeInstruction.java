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
package org.onehippo.cm.migration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.json.JSONObject;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ConfigDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.onehippo.cm.impl.model.ValueImpl;

public class ResourcebundlesInitializeInstruction extends InitializeInstruction {

    public static final String CONFIGURATION_ROOT = "/hippo:configuration";
    public static final String TRANSLATIONS_NODE = "hippo:translations";
    public static final String TRANSLATIONS_ROOT = CONFIGURATION_ROOT + "/" + TRANSLATIONS_NODE;

    public static final String RESOURCEBUNDLES_NT = "hipposys:resourcebundles";
    public static final String RESOURCEBUNDLE_NT = "hipposys:resourcebundle";

    public ResourcebundlesInitializeInstruction(final EsvNode instructionNode, final Type type,
                                                final InitializeInstruction combinedWith) throws EsvParseException {
        super(instructionNode, type, combinedWith);
    }

    public void processResourceBundles(final ModuleImpl module, final ConfigDefinitionImpl resourceBundleParents,
                                       final Set<String> bundles) throws IOException, EsvParseException {
        JSONObject json = new JSONObject(IOUtils.toString(new FileInputStream(getResource()), "UTF-8"));
        parse(json, new Stack<>(), module.addSource(getSourcePath()), resourceBundleParents, bundles);
    }

    private void parse(final JSONObject json, final Stack<String> path, SourceImpl source,
                       final ConfigDefinitionImpl resourceBundleParents, final Set<String> bundles)
            throws EsvParseException {
        for (String key : json.keySet()) {
            if (!(json.get(key) instanceof JSONObject)) {
                throw new EsvParseException("Invalid resourcebundle: " + getResourcePath() + ". Expected json object");
            }
            JSONObject localesMap = (JSONObject) json.get(key);
            path.push(key);
            if (isBundle(localesMap)) {
                final String bundleName = buildName(path);
                final String bundlePath = TRANSLATIONS_ROOT + bundleName;
                if (bundles.contains(bundleName)) {
                    throw new EsvParseException("Translation bundle name " + bundleName + " in resourcebundle: "
                            + getResourcePath() + " already defined.");
                }
                final ConfigDefinitionImpl def = source.addConfigDefinition();
                final DefinitionNodeImpl bundleNode = new DefinitionNodeImpl(bundlePath, path.peek(), def);
                def.setNode(bundleNode);
                bundleNode.addProperty(JcrConstants.JCR_PRIMARYTYPE,
                        new ValueImpl(RESOURCEBUNDLES_NT, ValueType.NAME, false, false));
                bundles.add(bundleName);
                addResourceBundleParents(bundleName, resourceBundleParents);

                for (String locale : localesMap.keySet()) {
                    DefinitionNodeImpl localeNode = bundleNode.addNode(locale);
                    localeNode.addProperty(JcrConstants.JCR_PRIMARYTYPE,
                            new ValueImpl(RESOURCEBUNDLE_NT, ValueType.NAME, false, false));
                    Object properties = localesMap.get(locale);
                    if (properties instanceof JSONObject) {
                        JSONObject propertiesMap = (JSONObject) properties;
                        for (String name : propertiesMap.keySet()) {
                            Object value = propertiesMap.get(name);
                            if (value instanceof String) {
                                localeNode.addProperty(name, new ValueImpl((String) value));
                            }
                        }
                    }
                }
            } else {
                parse(localesMap, path, source, resourceBundleParents, bundles);
            }
            path.pop();
        }
    }

    private static boolean isBundle(final JSONObject map) {
        if (map.length() > 0) {
            Object value = map.get(map.keys().next());
            if (value instanceof JSONObject) {
                JSONObject childMap = (JSONObject) value;
                return childMap.length() > 0 && childMap.get(childMap.keys().next()) instanceof String;
            }
        }
        return false;
    }

    private static String buildName(final Stack<String> path) {
        final StringBuilder sb = new StringBuilder("/");
        final Iterator<String> iterator = path.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append("/");
            }
        }
        return sb.toString();
    }

    private void addResourceBundleParents(final String bundleName, final ConfigDefinitionImpl resourceBundleParents) {
        int lastSlash = bundleName.substring(1).lastIndexOf('/');
        if (lastSlash > -1) {
            String[] parentPaths = bundleName.substring(1, lastSlash+1).split("/");
            DefinitionNodeImpl translationsParentNode = (DefinitionNodeImpl)resourceBundleParents.getNode();
            if (translationsParentNode == null) {
                translationsParentNode = new DefinitionNodeImpl(TRANSLATIONS_ROOT, TRANSLATIONS_NODE, resourceBundleParents);
                resourceBundleParents.setNode(translationsParentNode);
            }
            // for all parents, add them if not yet added already
            for (String parentName : parentPaths) {
                DefinitionNodeImpl parentNode = translationsParentNode.getModifiableNodes().get(parentName);
                if (parentNode == null) {
                    parentNode = translationsParentNode.addNode(parentName);
                    parentNode.addProperty(JcrConstants.JCR_PRIMARYTYPE,
                            new ValueImpl(RESOURCEBUNDLES_NT, ValueType.NAME, false, false));
                }
                translationsParentNode = parentNode;
            }
        }
    }
}
