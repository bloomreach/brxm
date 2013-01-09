/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.model.nodetypes.NodeTypeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeTranslator extends NodeTypeModelWrapper {

    final static Logger log = LoggerFactory.getLogger(TypeTranslator.class);

    private static final long serialVersionUID = 1L;

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
            String name = getNodeTypeModel().getType();
            JcrNodeModel nodeModel = getNodeModel();
            if (nodeModel != null) {
                Node node = nodeModel.getNode();
                if (node != null) {
                    try {
                        name = NodeNameCodec.decode(node.getName());
                        if (node.isNodeType("hippo:translated")) {
                            Locale locale = Session.get().getLocale();
                            NodeIterator nodes = node.getNodes("hippo:translation");
                            while (nodes.hasNext()) {
                                Node child = nodes.nextNode();
                                if (child.isNodeType("hippo:translation") && !child.hasProperty("hippo:property")) {
                                    String language = child.getProperty("hippo:language").getString();
                                    if (locale.getLanguage().equals(language)) {
                                        return child.getProperty("hippo:message").getString();
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
            int colonIndex = propertyName.indexOf(":");
            if (colonIndex != -1 && colonIndex + 1 < propertyName.length()) {
                return propertyName.substring(colonIndex + 1);
            } else {
                return propertyName;
            }
        }

        @Override
        public void detach() {
            super.detach();
            TypeTranslator.this.detach();
        }
    }

}
