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
package org.hippoecm.frontend.editor.list.resolvers;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.Observable;
import org.hippoecm.frontend.model.event.ObservablePropertyModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateTypeIconAttributeModifier extends AbstractNodeAttributeModifier {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TemplateTypeIconAttributeModifier.class);

    private static class StateIconAttributes implements IDetachable, IObservable {
        private static final long serialVersionUID = 1L;

        private JcrNodeModel nodeModel;
        private Observable nthandle;
        private transient String cssClass;
        private transient boolean loaded = false;

        StateIconAttributes(JcrNodeModel nodeModel) {
            this.nodeModel = nodeModel;
            this.nthandle = new Observable(nodeModel);
        }

        @SuppressWarnings("unused")
        public String getCssClass() {
            load();
            return cssClass;
        }

        public void detach() {
            loaded = false;
            cssClass = null;
            nodeModel.detach();
            nthandle.detach();
        }

        void load() {
            if (!loaded) {
                cssClass = "document-16";
                nthandle.setTarget(null);
                try {
                    Node node = nodeModel.getNode();
                    if (node != null && node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                        String prefix = node.getParent().getName();
                        NamespaceRegistry nsReg = node.getSession().getWorkspace().getNamespaceRegistry();
                        String currentUri = nsReg.getURI(prefix);

                        Node ntHandle = node.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                        nthandle.setTarget(new JcrNodeModel(ntHandle));
                        NodeIterator variants = ntHandle.getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE);

                        Node current = null;
                        Node draft = null;
                        while (variants.hasNext()) {
                            Node variant = variants.nextNode();
                            if (variant.isNodeType(HippoNodeType.NT_REMODEL)) {
                                String uri = variant.getProperty(HippoNodeType.HIPPO_URI).getString();
                                if (currentUri.equals(uri)) {
                                    current = variant;
                                }
                            } else {
                                draft = variant;
                            }
                        }

                        if (current == null && draft != null) {
                            cssClass = "state-new-16";
                        } else if (current != null && draft == null) {
                            cssClass = "state-live-16";
                        } else if (current != null && draft != null) {
                            cssClass = "state-changed-16";
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error("Unable to obtain state properties", ex);
                }
                loaded = true;
            }
        }

        public void setObservationContext(IObservationContext<? extends IObservable> context) {
            nthandle.setObservationContext(context);
        }

        public void startObservation() {
            nthandle.startObservation();
        }

        public void stopObservation() {
            nthandle.stopObservation();
        }
    }

    @Override
    public AttributeModifier getColumnAttributeModifier() {
        return new CssClassAppender(new Model("icon-16"));
    }

    @Override
    public AttributeModifier[] getCellAttributeModifiers(Node node) {
        StateIconAttributes attrs = new StateIconAttributes(new JcrNodeModel(node));
        AttributeModifier[] attributes = new AttributeModifier[1];
        attributes[0] = new CssClassAppender(new ObservablePropertyModel(attrs, "cssClass"));
        return attributes;
    }
}
