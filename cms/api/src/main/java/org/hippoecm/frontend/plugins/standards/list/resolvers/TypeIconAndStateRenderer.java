/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack.Position;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeIconAndStateRenderer extends AbstractNodeRenderer {

    private static final Logger log = LoggerFactory.getLogger(TypeIconAndStateRenderer.class);

    private static final TypeIconAndStateRenderer INSTANCE = new TypeIconAndStateRenderer();
    private static final Icon[] EMPTY_STATE_ICONS = new Icon[]{Icon.EMPTY, Icon.EMPTY};

    private TypeIconAndStateRenderer() {
    }

    public static TypeIconAndStateRenderer getInstance() {
        return INSTANCE;
    }

    public Component getViewer(final String id, final Node node) {
        if (node == null) {
            return HippoIcon.fromSprite(id, Icon.EMPTY);
        } else {
            return new Container(id, node);
        }
    }

    private static class Container extends Panel implements IDetachable {

        private final HippoIconStack icon;
        private HippoIcon[] stateIcons = new HippoIcon[2];

        public Container(final String id, final Node node) {
            super(id, new JcrNodeModel(node));

            icon = new HippoIconStack("icon", IconSize.L);
            icon.addFromSprite(isCompound() ? Icon.FILE_COMPOUND : Icon.FILE, IconSize.L);

            Icon[] newStateIcons = getStateIcons();
            stateIcons[0] = icon.addFromSprite(newStateIcons[0], IconSize.M, Position.TOP_LEFT);
            stateIcons[1] = icon.addFromSprite(newStateIcons[1], IconSize.M, Position.BOTTOM_LEFT);

            add(icon);
        }

        @Override
        protected void onBeforeRender() {
            if (hasBeenRendered()) {
                Icon[] newIcons = getStateIcons();
                stateIcons[0] = icon.replaceFromSprite(stateIcons[0], newIcons[0], Position.TOP_LEFT);
                stateIcons[1] = icon.replaceFromSprite(stateIcons[1], newIcons[1], Position.BOTTOM_LEFT);
            }
            super.onBeforeRender();
        }

        private Icon[] getStateIcons() {
            final JcrNodeModel nodeModel = (JcrNodeModel)getDefaultModel();
            final Node node = nodeModel.getNode();
            if (node != null) {
                try {
                    return determineStateIcons(node);
                } catch (RepositoryException e) {
                    log.info("Unable to determine state icon of '{}'", JcrUtils.getNodePathQuietly(node), e);
                }
            }
            return EMPTY_STATE_ICONS;
        }

        private boolean isCompound() {
            final JcrNodeModel nodeModel = (JcrNodeModel)getDefaultModel();
            final Node node = nodeModel.getNode();
            if (node != null) {
                try {
                    if (node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)
                            && node.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
                        Node ntHandle = node.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                        if (ntHandle.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
                            Node variant = ntHandle.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                            final Property superTypeProperty = variant.getProperty(HippoNodeType.HIPPO_SUPERTYPE);
                            if (superTypeProperty.isMultiple()) {
                                for (Value value : superTypeProperty.getValues()) {
                                    if ("hippo:compound".equals(value.getString())) {
                                        return true;
                                    }
                                }
                            } else {
                                return "hippo:compound".equals(superTypeProperty.getString());
                            }
                        }
                    }
                } catch (RepositoryException e) {
                    log.info("Unable to determine if template node is of type compound for '{}'", JcrUtils.getNodePathQuietly(node), e);
                }
            }
            return false;
        }

        private Icon[] determineStateIcons(final Node node) throws RepositoryException {
            if (node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                String prefix = node.getParent().getName();
                NamespaceRegistry nsReg = node.getSession().getWorkspace().getNamespaceRegistry();
                String currentUri = nsReg.getURI(prefix);

                Node ntHandle = node.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
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
                    return new Icon[] {Icon.MINUS_CIRCLE, Icon.EMPTY};
                } else if (current != null && draft == null) {
                    return new Icon[] {Icon.CHECK_CIRCLE, Icon.EMPTY};
                } else if (current != null && draft != null) {
                    return new Icon[] {Icon.CHECK_CIRCLE, Icon.EXCLAMATION_TRIANGLE};
                }
            }
            return EMPTY_STATE_ICONS;
        }

    }

}
