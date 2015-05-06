/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.i18n;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * @author cngo
 * @version $Id$
 * @since 2015-02-05
 */
public class TranslatorUtils {

    public static final String EDITOR_TEMPLATES_NODETYPE = "editor:templates";
    public static final String EDITOR_TEMPLATESET_NODETYPE = "editor:templateset";
    public static final String CAPTION_PROPERTY = "caption";

    private TranslatorUtils(){}

    public static Map<String, String> getCriteria(String key) {
        Map<String, String> keys = new TreeMap<>();
        String realKey;
        if (key.indexOf(',') > 0) {
            realKey = key.substring(0, key.indexOf(','));
            IValueMap map = new ValueMap(key.substring(key.indexOf(',') + 1));
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof String) {
                    keys.put(entry.getKey(), (String) entry.getValue());
                }
            }
        } else {
            realKey = key;
        }
        keys.put(HippoNodeType.HIPPO_KEY, realKey);

        Locale locale = Session.get().getLocale();
        keys.put(HippoNodeType.HIPPO_LANGUAGE, locale.getLanguage());

        String value = locale.getCountry();
        if (value != null) {
            keys.put("country", locale.getCountry());
        }

        value = locale.getVariant();
        if (value != null) {
            keys.put("variant", locale.getVariant());
        }
        return keys;
    }

    /**
     * Given the doc-type node at /hippo:namespaces/${projectName}/${docType}, return the i18n translation configurations
     * of the node type 'hippostd:translations' at ${docType-path}/editor:templates/${first-frontend:plugincluster}/${translator.id}
     *
     * @param typeNode the node referring to a document type at /hippo:namespaces/${projectName}/${docType}
     * @throws TranslatorException
     */
    public static IPluginConfig getTranslationsConfig(final Node typeNode) throws TranslatorException {
        Node templateNode = getTemplateNode(typeNode);

        JcrClusterConfig clusterConfig = new JcrClusterConfig(new JcrNodeModel(templateNode));
        final String translatorId = getTranslatorId(clusterConfig);
        List<IPluginConfig> pluginConfigs = clusterConfig.getPlugins();
        if (pluginConfigs == null || pluginConfigs.isEmpty()){
            throw new TranslatorException("Cannot find the translator entry");
        }

        // find the first entry having its name that matches with the translatorId
        IPluginConfig translatorPlugin = null;
        for(IPluginConfig plugin : pluginConfigs) {
            if (StringUtils.equals(plugin.getName(), translatorId)){
                translatorPlugin = plugin;
                break;
            }
        }
        if (translatorPlugin == null){
            throw new TranslatorException("No translator entry of type 'frontend:plugin' is found");
        }
        return translatorPlugin.getPluginConfig(HippoStdNodeType.HIPPOSTD_TRANSLATIONS);
    }

    /**
     * Return the last path of the <code>translatorId</code>, e.g. '${cluster.id}.translator' -> 'translator'
     * @param clusterConfig
     * @return null if no translatorId is found
     */
    private static String getTranslatorId(final JcrClusterConfig clusterConfig) throws TranslatorException {
        final String translatorId = clusterConfig.getString(ITranslateService.TRANSLATOR_ID);
        if (StringUtils.isEmpty(translatorId)){
            throw new TranslatorException("The translatorId property is not found, image variant names won't be translated");
        }
        int dotIndex = translatorId.lastIndexOf(".");
        if (dotIndex > -1 && (dotIndex + 1) < translatorId.length()) {
            return translatorId.substring(dotIndex + 1);
        } else
            return translatorId;
    }

    /**
     * Get the first node of type 'frontend:plugincluster' under the 'editor:templates' node
     *
     * @param typeNode a node of type 'editor:templates'
     * @return the template node or thrown exceptions
     * @throws javax.jcr.RepositoryException
     * @throws org.hippoecm.frontend.i18n.TranslatorException if either no child node of type 'frontend:plugincluster'
     * is found, <code>typeNode</code> is null or not the type 'editor:templates'.
     */
    public static Node getTemplateNode(final Node typeNode) throws TranslatorException {
        try {
            if (typeNode == null || !typeNode.hasNode(EDITOR_TEMPLATES_NODETYPE)) {
                throw new TranslatorException("Invalid node of type " + EDITOR_TEMPLATES_NODETYPE);
            }
            final Node templateSetNode = typeNode.getNode(EDITOR_TEMPLATES_NODETYPE);
            if (templateSetNode == null || !templateSetNode.isNodeType(EDITOR_TEMPLATESET_NODETYPE)) {
                throw new TranslatorException("Invalid node of type " + EDITOR_TEMPLATESET_NODETYPE);
            }

            NodeIterator pluginClusterNodes = templateSetNode.getNodes();
            while (pluginClusterNodes.hasNext()) {
                final Node templateNode = pluginClusterNodes.nextNode();
                if (templateNode != null && templateNode.isNodeType(FrontendNodeType.NT_PLUGINCLUSTER)) {
                    return templateNode;
                }
            }
            throw new TranslatorException("Cannot find child node of type " + FrontendNodeType.NT_PLUGINCLUSTER);
        } catch (RepositoryException e) {
            throw new TranslatorException("Cannot find template node", e);
        }
    }

    public static IModel getTranslatedModel(final IPluginConfig translations, final Map<String, String> criteria) {
        if (translations == null || criteria == null){
            return null;
        }

        IPluginConfig keyConfig = translations.getPluginConfig((String) criteria.get(HippoNodeType.HIPPO_KEY));
        if (keyConfig == null) {
            return null;
        }

        Set<IPluginConfig> candidates = keyConfig.getPluginConfigSet();
        Set<ConfigWrapper> list = new HashSet<>((int) candidates.size());
        for (IPluginConfig candidate : candidates) {
            if (candidate.getString(HippoNodeType.HIPPO_LANGUAGE, "").equals(criteria.get(HippoNodeType.HIPPO_LANGUAGE))) {
                list.add(new ConfigWrapper(candidate, criteria));
            }
        }
        return new TranslationSelectionStrategy<>(criteria.keySet()).select(list).getModel();
    }
}
