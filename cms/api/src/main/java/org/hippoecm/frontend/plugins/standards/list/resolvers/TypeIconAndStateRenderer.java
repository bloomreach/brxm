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
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeIconAndStateRenderer extends AbstractNodeRenderer {

    private static final TypeIconAndStateRenderer INSTANCE = new TypeIconAndStateRenderer();
    private static final Logger log = LoggerFactory.getLogger(TypeIconAndStateRenderer.class);

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

        public static final String WICKET_ID_TYPE_ICON = "typeIcon";
        public static final String WICKET_ID_STATE_ICON = "stateIcon";

        public Container(final String id, final Node node) {
            super(id, new JcrNodeModel(node));

            add(getTypeIcon(WICKET_ID_TYPE_ICON));
            add(getStateIcon(WICKET_ID_STATE_ICON));
        }

        @Override
        protected void onBeforeRender() {
            if (hasBeenRendered()) {
                replace(getStateIcon(WICKET_ID_STATE_ICON));
            }
            super.onBeforeRender();
        }

        private Component getTypeIcon(final String id) {
            if (isCompound()) {
                return HippoIcon.fromSprite(id, Icon.FILE_COMPOUND, IconSize.L);
            } else {
                return HippoIcon.fromSprite(id, Icon.FILE, IconSize.L);
            }
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

        private Component getStateIcon(final String id) {
            final JcrNodeModel nodeModel = (JcrNodeModel)getDefaultModel();
            final Node node = nodeModel.getNode();
            if (node != null) {
                try {
                    CmsIcon stateIcon = determineStateIcon(node);
                    if (stateIcon != null) {
                        return HippoIcon.inline(id, stateIcon);
                    }
                } catch (RepositoryException e) {
                    log.info("Unable to determine state icon of '{}'", JcrUtils.getNodePathQuietly(node), e);
                }
            }
            EmptyPanel noIcon = new EmptyPanel(id);
            noIcon.setRenderBodyOnly(true);
            return noIcon;
        }

        private CmsIcon determineStateIcon(final Node node) throws RepositoryException {
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
                    return CmsIcon.OVERLAY_MINUS_CIRCLE;
                } else if (current != null && draft == null) {
                    return CmsIcon.OVERLAY_CHECK_CIRCLE;
                } else if (current != null && draft != null) {
                    return CmsIcon.OVERLAY_CHECK_CIRCLE_EXCLAMATION_TRIANGLE;
                }
            }
            return null;
        }

    }

}
