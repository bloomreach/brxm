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
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeTranslator extends NodeTypeModelWrapper {

    final static Logger log = LoggerFactory.getLogger(TypeTranslator.class);

    private static final long serialVersionUID = 1L;
    private static final String JCR_NAME = "jcr:name";
    private static final String HIPPO_TYPES = "hippo:types";
    private static final String TRANSLATIONS_PATH = "/hippo:configuration/hippo:translations";

    private IPluginConfig translations = null;
    private TypeNameModel name;
    private transient boolean attached = false;
    private transient JcrNodeModel nodeModel;

    public TypeTranslator(JcrNodeTypeModel nodeTypeModel) {
        super(nodeTypeModel);
        name = new TypeNameModel();
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

            final String translation = getStringFromBundle(JCR_NAME);
            if (translation != null) return translation;

            String name = getNodeTypeModel().getType();

            JcrNodeModel nodeModel = getNodeModel();
            if (nodeModel == null) {
                return name;
            }
            Node node = nodeModel.getNode();
            if (node == null){
                return name;
            }

            try {
                name = NodeNameCodec.decode(node.getName());
                if (node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                    String language = Session.get().getLocale().getLanguage();
                    NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                    while (nodes.hasNext()) {
                        Node child = nodes.nextNode();
                        if (findTranslationNode(child, language)) {
                            hippoTranslatedDeprecationWarning(node.getPath());
                            return child.getProperty(HippoNodeType.HIPPO_MESSAGE).getString();
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return name;
        }

        private boolean findTranslationNode(final Node node, final String matchingLanguage) throws RepositoryException {
            if (node.isNodeType(HippoNodeType.HIPPO_TRANSLATION) && !node.hasProperty(HippoNodeType.HIPPO_PROPERTY)
                    && node.hasProperty(HippoNodeType.HIPPO_LANGUAGE)) {
                String language = node.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();
                return StringUtils.equals(matchingLanguage, language);
            }
            return false;
        }

        void onDetachTranslator() {
            super.detach();
        }

        @Override
        public void detach() {
            TypeTranslator.this.detach();
        }
    }



    private String getBundleName() {
        String type = getNodeTypeModel().getType();
        if (!type.contains(":")) {
            type = "hipposys:" + type;
        }
        return HIPPO_TYPES + "." + type;
    }

    private String getStringFromBundle(final String key) {
        final LocalizationService service = HippoServiceRegistry.getService(LocalizationService.class);
        if (service != null) {
            final ResourceBundle bundle = service.getResourceBundle(getBundleName(), Session.get().getLocale());
            if (bundle != null) {
                return bundle.getString(key);
            }
        }
        return null;
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

            final String key = property + "=" + value.getObject();
            final String translation = getStringFromBundle(key);
            if (translation != null) {
                return translation;
            }

            String name = value.getObject();

            JcrNodeModel nodeModel = getNodeModel();
            if (nodeModel == null) {
                return name;
            }
            Node node = nodeModel.getNode();
            if (node == null) {
                return name;
            }

            try {
                if (!node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                    return name;
                }
                String language = Session.get().getLocale().getLanguage();
                NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    if (findTranslationNode(child, name, language, property)) {
                        hippoTranslatedDeprecationWarning(node.getPath());
                        return child.getProperty(HippoNodeType.HIPPO_MESSAGE).getString();
                    }
                }
            }
            catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return name;
        }

        @Override
        public void detach() {
            super.detach();
            value.detach();
            TypeTranslator.this.detach();
        }

        /**
         * Return true if the given <code>node</code> containing the matching translation of given name, language and property.
         * @param node
         * @param matchingName
         * @param matchingLanguage
         * @return
         * @throws RepositoryException
         */
        private boolean findTranslationNode(final Node node,
                                                   final String matchingName,
                                                   final String matchingLanguage,
                                                   final String matchingProperty) throws RepositoryException {
            if (node.isNodeType(HippoNodeType.NT_TRANSLATION)
                    && node.hasProperty(HippoNodeType.HIPPO_PROPERTY)
                    && node.hasProperty(HippoNodeType.HIPPO_VALUE)
                    && node.hasProperty(HippoNodeType.HIPPO_LANGUAGE)) {

                String property = node.getProperty(HippoNodeType.HIPPO_PROPERTY).getString();
                String name = node.getProperty(HippoNodeType.HIPPO_VALUE).getString();
                String language = node.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();

                return StringUtils.equals(property, matchingProperty)
                        && StringUtils.equals(name, matchingName)
                        && StringUtils.equals(language, matchingLanguage);
            }
            return false;
        }
    }

    private static void hippoTranslatedDeprecationWarning(String nodePath) {
        log.warn("Usage of deprecated hippo:translated model at {}, " +
                        "store translations of document types at {} instead",
                nodePath, TRANSLATIONS_PATH);
    }

    class PropertyModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;

        private String propertyName;

        PropertyModel(String propertyName) {
            this.propertyName = propertyName;
        }

        @Override
        protected String load() {

            final String translation = getStringFromBundle(propertyName);
            if (translation != null) {
                return translation;
            }

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
     */
    @Deprecated
    private String translateProperty(final String propertyName) {
        JcrNodeModel nodeModel = getNodeModel();
        if (nodeModel == null) {
            return null;
        }
        Node node = nodeModel.getNode();
        if (node == null) {
            return null;
        }
        try {
            if (node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                String matchingLanguage = Session.get().getLocale().getLanguage();
                NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    if (findTranslationNode(child, propertyName, matchingLanguage)) {
                        hippoTranslatedDeprecationWarning(node.getPath());
                        return child.getProperty(HippoNodeType.HIPPO_MESSAGE).getString();
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    /**
     * Translate field name using the new i18n field name translation mechanism, i.e. the translator node of type
     * 'frontend:plugin' at <code>/hippo:namespaces/${projectName}/${docType}/editor:templates/_default_/translator</code>
     */
    @Deprecated
    protected String translate(final String propertyName) {
        String docTypePath = null;
        if (translations == null){
            attach();
            try {
                Node docTypeNode = nodeModel.getNode();
                if (docTypeNode != null) {
                    translations = TranslatorUtils.getTranslationsConfig(docTypeNode);
                    docTypePath = JcrUtils.getNodePathQuietly(docTypeNode);
                }
            } catch (TranslatorException e) {
                log.debug("Cannot retrieve i18n translation configuration", e);
            }
        }

        final String shortName = getShortName(propertyName);
        IModel model = TranslatorUtils.getTranslatedModel(translations, TranslatorUtils.getCriteria(shortName));
        String translation = (model != null) ? (String) model.getObject() : null;
        if (translation != null) {
            hippoTranslatedDeprecationWarning(docTypePath);
        }
        return translation;
    }

    private String getCaptionProperty(final String propertyName) {
        final String shortName = getShortName(propertyName);
        JcrNodeModel nodeModel = getNodeModel();
        if (nodeModel == null) {
            return null;
        }
        try {
            final Node templateNode = TranslatorUtils.getTemplateNode(nodeModel.getObject());
            Node fieldTypeNode = templateNode.getNode(shortName);
            if (fieldTypeNode != null && fieldTypeNode.isNodeType(FrontendNodeType.NT_PLUGIN)) {
                return fieldTypeNode.getProperty(TranslatorUtils.CAPTION_PROPERTY).getString();
            } else {
                log.debug("Cannot find the field node type '{}'", propertyName);
            }
        } catch (TranslatorException | RepositoryException e) {
            log.debug("Cannot retrieve caption property of the field '{}'", propertyName, e);
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

    private static boolean findTranslationNode(final Node node, final String matchingProperty, final String matchingLanguage) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_TRANSLATION)
                && node.hasProperty(HippoNodeType.HIPPO_PROPERTY)
                && node.hasProperty(HippoNodeType.HIPPO_LANGUAGE)) {

            String property = node.getProperty(HippoNodeType.HIPPO_PROPERTY).getString();
            String language = node.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();
            return StringUtils.equals(property, matchingProperty) && StringUtils.equals(matchingLanguage, language);
        }
        return false;
    }
}
