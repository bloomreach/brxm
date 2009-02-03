/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateIconAttributeModifier extends AbstractNodeAttributeModifier {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(StateIconAttributeModifier.class);

    private static final String PREFIX = "state-";
    private static final String SUFFIX = "-16";

    private static class StateIconAttributes implements IDetachable {
        private static final long serialVersionUID = 1L;

        private JcrNodeModel nodeModel;
        private transient String cssClass;
        private transient String summary;
        private transient boolean loaded = false;

        StateIconAttributes(JcrNodeModel nodeModel) {
            this.nodeModel = nodeModel;
        }

        public String getSummary() {
            load();
            return summary;
        }

        public String getCssClass() {
            load();
            return cssClass;
        }

        public void detach() {
            loaded = false;
            summary = null;
            cssClass = null;
            nodeModel.detach();
        }

        void load() {
            if (!loaded) {
                try {
                    Node node = nodeModel.getNode();
                    if (node != null && node.hasNode(node.getName())) {
                        Node canonicalNode = node.getNode(node.getName());
                        if (canonicalNode.hasProperty("hippostd:stateSummary")) {
                            cssClass = PREFIX + canonicalNode.getProperty("hippostd:stateSummary").getString() + SUFFIX;
                        }
                        IModel stateModel = new JcrPropertyValueModel(new JcrPropertyModel(canonicalNode
                                .getProperty("hippostd:stateSummary")));
                        summary = (String) new TypeTranslator(new JcrNodeTypeModel("hippostd:publishableSummary"))
                                .getValueName("hippostd:stateSummary", stateModel).getObject();
                    }
                } catch (RepositoryException ex) {
                    log.error("Unable to obtain state properties", ex);
                }
                loaded = true;
            }
        }
    }

    @Override
    public AttributeModifier getColumnAttributeModifier(Node node) {
        return new CssClassAppender(new Model("icon-16"));
    }

    @Override
    public AttributeModifier[] getCellAttributeModifiers(Node node) {
        StateIconAttributes attrs = new StateIconAttributes(new JcrNodeModel(node));
        AttributeModifier[] attributes = new AttributeModifier[2];
        attributes[0] = new CssClassAppender(new PropertyModel(attrs, "cssClass"));
        attributes[1] = new AttributeAppender("title", new PropertyModel(attrs, "summary"), " ");
        return attributes;
    }
}
