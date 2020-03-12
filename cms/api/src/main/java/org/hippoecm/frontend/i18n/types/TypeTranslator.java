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
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.i18n.TranslatorException;
import org.hippoecm.frontend.i18n.TranslatorUtils;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.model.nodetypes.NodeTypeModelWrapper;
import org.hippoecm.repository.api.NodeNameCodec;
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
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
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
            return value.getObject();
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

            final String translation = getStringFromBundle(propertyName);
            if (translation != null) {
                return translation;
            }

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
}
