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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack;
import org.hippoecm.frontend.plugins.standards.icon.HippoIconStack.Position;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentIconAndStateRenderer extends AbstractNodeRenderer {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DocumentIconAndStateRenderer.class);

    public Component getViewer(final String id, final Node node) {
        if (node == null) {
            log.warn("Using default icon for unknown node");
            return defaultTypeIcon(id);
        } else {
            return new Container(id, node);
        }
    }

    private static HippoIcon defaultTypeIcon(final String id) {
        return HippoIcon.fromSprite(id, Icon.EMPTY);
    }

    private static class Container extends Panel implements IDetachable {

        private final HippoIconStack icon;
        private final StateIconAttributes stateIconAttributes;
        private HippoIcon[] stateIcons = new HippoIcon[2];

        public Container(final String id, final Node node) {
            super(id);

            final JcrNodeModel nodeModel = new JcrNodeModel(node);
            stateIconAttributes = new StateIconAttributes(nodeModel);

            icon = new HippoIconStack("icon", IconSize.L);

            // icon#addCopyOf generates a new ID, so use a dummy ID for the pluggable type icon
            final HippoIcon typeIcon = getTypeIcon("dummyId", node);
            icon.addCopyOf(typeIcon);

            final Icon[] stateIcons = stateIconAttributes.getIcons();
            if (stateIcons.length >= 2) {
                this.stateIcons[0] = icon.addFromSprite(stateIcons[0], IconSize.M, Position.TOP_LEFT);
                this.stateIcons[1] = icon.addFromSprite(stateIcons[1], IconSize.M, Position.BOTTOM_LEFT);
            }

            add(icon);
        }

        private HippoIcon getTypeIcon(final String id, final Node node) {
            try {
                return IconRenderUtil.getDocumentOrFolderIcon(id, node);
            } catch (RepositoryException e) {
                log.warn("Unable to determine icon for node '{}', using default icon instead",
                        JcrUtils.getNodePathQuietly(node), e);
                return defaultTypeIcon(id);
            }
        }

        @Override
        protected void onBeforeRender() {
            if (hasBeenRendered()) {
                updateStateIcon();
            }
            super.onBeforeRender();
        }

        private void updateStateIcon() {
            final Icon[] newStateIcons = stateIconAttributes.getIcons();
            if (newStateIcons != null) {
                stateIcons[0] = icon.replaceFromSprite(stateIcons[0], newStateIcons[0], Position.TOP_LEFT);
                stateIcons[1] = icon.replaceFromSprite(stateIcons[1], newStateIcons[1], Position.BOTTOM_LEFT);
            }
        }

        @Override
        protected void onDetach() {
            if (stateIconAttributes != null) {
                stateIconAttributes.detach();
            }
            super.onDetach();
        }
    }

}
