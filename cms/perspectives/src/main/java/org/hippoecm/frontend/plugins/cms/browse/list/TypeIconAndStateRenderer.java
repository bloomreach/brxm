/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.list;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.StateIconAttributes;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeIconAndStateRenderer extends AbstractNodeRenderer {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TypeIconAndStateRenderer.class);

    public Component getViewer(final String id, final Node node) {
        if (node == null) {
            return HippoIcon.fromSprite(id, Icon.EMPTY_SMALL);
        } else {
            return new Container(id, node);
        }
    }

    private static class Container extends Panel implements IDetachable {

        public static final String WICKET_ID_TYPE_ICON = "typeIcon";
        public static final String WICKET_ID_STATE_ICON = "stateIcon";

        public Container(final String id, final Node node) {
            super(id, new JcrNodeModel(node));

            add(HippoIcon.fromSprite(WICKET_ID_TYPE_ICON, Icon.DOCUMENT_SMALL));
            add(getStateIcon(WICKET_ID_STATE_ICON));
        }

        @Override
        protected void onBeforeRender() {
            if (hasBeenRendered()) {
                replace(getStateIcon(WICKET_ID_STATE_ICON));
            }
            super.onBeforeRender();
        }

        private Component getStateIcon(final String id) {
            final JcrNodeModel nodeModel = (JcrNodeModel)getDefaultModel();
            final Node node = nodeModel.getNode();
            if (node != null) {
                try {
                    Icon stateIcon = determineStateIcon(node);
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

        private Icon determineStateIcon(final Node node) throws RepositoryException {
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
                    return Icon.STATE_NEW_TINY;
                } else if (current != null && draft == null) {
                    return Icon.STATE_LIVE_TINY;
                } else if (current != null && draft != null) {
                    return Icon.STATE_CHANGED_TINY;
                }
            }
            return null;
        }

    }

}
