/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.i18n.types;

import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.i18n.TranslatorException;
import org.hippoecm.frontend.i18n.TranslatorUtils;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.model.nodetypes.NodeTypeModelWrapper;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeTranslator extends NodeTypeModelWrapper {

    final static Logger log = LoggerFactory.getLogger(TypeTranslator.class);

    private static final long serialVersionUID = 1L;

    private IPluginConfig translations = null;
    private TypeNameModel name;
    private transient boolean attached = false;
    private transient JcrNodeModel nodeModel;

    public TypeTranslator(JcrNodeTypeModel nodeTypeModel) {
        super(nodeTypeModel);
        name = new TypeNameModel();

        attach();
        if (nodeModel != null){
            try {
                Node docTypeNode = nodeModel.getNode();
                if (docTypeNode != null) {
                    translations = TranslatorUtils.getTranslationsConfig(docTypeNode);
                }
            } catch (TranslatorException e) {
                log.debug("Cannot retrieve i18n translation configuration", e);
            }
        }
    }

    public IModel<String> getTypeName() {
        return name;
    }

    public IModel<String> getValueName(String property, IModel<String> value) {
        attach();
        return new PropertyValueModel(property, value);
    }

    public IModel<String> getPropertyName(String compoundName) {
        attach();
        return new PropertyModel(compoundName);
    }

    @Override
    public void detach() {
        if (attached) {
            super.detach();
            name.onDetachTranslator();
            nodeModel = null;
            attached = false;
        }
    }

    // internals

    private void attach() {
        if (!attached) {
            String type = getNodeTypeModel().getType();
            if (type.contains(":")) {
                nodeModel = new JcrNodeModel("/hippo:namespaces/" + type.replace(':', '/'));
            } else {
                nodeModel = new JcrNodeModel("/hippo:namespaces/system/" + type);
            }
            attached = true;
        }
    }

    private JcrNodeModel getNodeModel() {
        attach();
        return nodeModel;
    }

    private class TypeNameModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;

        @Override
        protected String load() {
            String name = getNodeTypeModel().getType();
            JcrNodeModel nodeModel = getNodeModel();
            if (nodeModel != null) {
                Node node = nodeModel.getNode();
                if (node != null) {
                    try {
                        name = NodeNameCodec.decode(node.getName());
                        if (node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                            Locale locale = Session.get().getLocale();
                            NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                            while (nodes.hasNext()) {
                                Node child = nodes.nextNode();
                                if (child.isNodeType(HippoNodeType.HIPPO_TRANSLATION) && !child.hasProperty(HippoNodeType.HIPPO_PROPERTY)) {
                                    String language = child.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();
                                    if (locale.getLanguage().equals(language)) {
                                        return child.getProperty(HippoNodeType.HIPPO_MESSAGE).getString();
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }
            }
            return name;
        }

        void onDetachTranslator() {
            super.detach();
        }

        @Override
        public void detach() {
            TypeTranslator.this.detach();
        }
    }

    class PropertyValueModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;

        private String property;
        private IModel<String> value;

        PropertyValueModel(String property, IModel<String> value) {
            this.property = property;
            this.value = value;
        }

        @Override
        protected String load() {
            IModel<String> name = value;
            JcrNodeModel nodeModel = getNodeModel();
            if (nodeModel != null) {
                Node node = nodeModel.getNode();
                if (node != null) {
                    try {
                        if (node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                            Locale locale = Session.get().getLocale();
                            NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                            while (nodes.hasNext()) {
                                Node child = nodes.nextNode();
                                if (child.isNodeType(HippoNodeType.NT_TRANSLATION) && child.hasProperty(HippoNodeType.HIPPO_PROPERTY)
                                        && child.hasProperty(HippoNodeType.HIPPO_VALUE)) {
                                    if (child.getProperty(HippoNodeType.HIPPO_PROPERTY).getString().equals(property)
                                            && child.getProperty(HippoNodeType.HIPPO_VALUE).getString().equals(name.getObject())) {
                                        String language = child.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();
                                        if (locale.getLanguage().equals(language)) {
                                            return child.getProperty(HippoNodeType.HIPPO_MESSAGE).getString();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }
            }
            return name.getObject();
        }

        @Override
        public void detach() {
            super.detach();
            value.detach();
            TypeTranslator.this.detach();
        }

    }

    class PropertyModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;

        private String propertyName;

        PropertyModel(String propertyName) {
            this.propertyName = propertyName;
        }

        @Override
        protected String load() {
            String translatedName = translate(propertyName);
            if (StringUtils.isNotEmpty(translatedName)) {
                return translatedName;
            }

            // fallback to old translation mechanism
            translatedName = translateProperty(propertyName);
            if (StringUtils.isNotEmpty(translatedName)) {
                return translatedName;
            }

            // use caption as the last resort
            final String captionPropertyName = getCaptionProperty(propertyName);
            if (StringUtils.isNotEmpty(captionPropertyName)) {
                return captionPropertyName;
            }

            return getShortName(propertyName);
        }

        @Override
        public void detach() {
            super.detach();
            TypeTranslator.this.detach();
        }

    }

    /**
     * Translate field name using old i18n mechanism with 'hippo:translation' nodes directly under the doc-type node, i.e.
     * <code>/hippo:namespaces/${projectName}/${docType}/hippo:translation[X]</code>
     *
     * @deprecated It's recommended to use the new i18n translation mechanism for field name in {@link #translate(String)}
     * @return
     */
    @Deprecated
    private String translateProperty(final String propertyName) {
        JcrNodeModel nodeModel = getNodeModel();
        if (nodeModel != null) {
            Node node = nodeModel.getNode();
            if (node != null) {
                try {
                    if (node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                        Locale locale = Session.get().getLocale();
                        NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                        while (nodes.hasNext()) {
                            Node child = nodes.nextNode();
                            if (child.isNodeType(HippoNodeType.NT_TRANSLATION) && child.hasProperty(HippoNodeType.HIPPO_PROPERTY)) {
                                if (child.getProperty(HippoNodeType.HIPPO_PROPERTY).getString().equals(propertyName)) {
                                    String language = child.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();
                                    if (locale.getLanguage().equals(language)) {
                                        return child.getProperty(HippoNodeType.HIPPO_MESSAGE).getString();
                                    }
                                }
                            }
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Translate field name using the new i18n field name translation mechanism, i.e. the translator node of type
     * 'frontend:plugin' at <code>/hippo:namespaces/${projectName}/${docType}/editor:templates/_default_/translator</code>
     *
     * @param propertyName
     * @return
     */
    protected String translate(final String propertyName){
        final String shortName = getShortName(propertyName);
        Map<String, String> criteria = TranslatorUtils.getCriteria(shortName);

        IModel model = TranslatorUtils.getTranslatedModel(translations, criteria);
        if (model != null) {
            return (String) model.getObject();
        }
        return null;
    }

    private String getCaptionProperty(final String propertyName) {
        final String shortName = getShortName(propertyName);
        JcrNodeModel nodeModel = getNodeModel();
        if (nodeModel != null) {
            try {
                final Node templateNode = TranslatorUtils.getTemplateNode(nodeModel.getObject());
                if (templateNode != null) {
                    Node fieldTypeNode = templateNode.getNode(shortName);
                    if (fieldTypeNode != null && fieldTypeNode.isNodeType(FrontendNodeType.NT_PLUGIN)) {
                        if (fieldTypeNode.hasProperty(TranslatorUtils.CAPTION_PROPERTY)) {
                            return fieldTypeNode.getProperty(TranslatorUtils.CAPTION_PROPERTY).getString();
                        }
                    } else {
                        log.debug("Cannot find the field node type {}", propertyName);
                    }
                }
            } catch (TranslatorException | RepositoryException e) {
                log.debug("Cannot retrieve caption property of the field '{}'", propertyName, e);
            }
        }
        return null;
    }

    private static String getShortName(final String name) {
        int colonIndex = name.indexOf(":");
        if (colonIndex != -1 && colonIndex + 1 < name.length()) {
            return name.substring(colonIndex + 1);
        } else {
            return name;
        }
    }
}
