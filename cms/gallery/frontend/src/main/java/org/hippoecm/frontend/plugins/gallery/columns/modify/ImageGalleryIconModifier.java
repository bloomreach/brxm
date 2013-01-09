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
package org.hippoecm.frontend.plugins.gallery.columns.modify;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

@Deprecated
public class ImageGalleryIconModifier extends AbstractNodeAttributeModifier {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ImageGalleryIconModifier.class);

    static class GalleryFolderAttributeModel extends LoadableDetachableModel {
        private static final long serialVersionUID = 1L;

        private JcrNodeModel nodeModel;

        public GalleryFolderAttributeModel(JcrNodeModel model) {
            this.nodeModel = model;
        }

        @Override
        protected Object load() {
            Node node = nodeModel.getNode();
            if (node != null) {
                try {

                    if (node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")) {
                        return "folder-16";
                    }
                    if (node.isNodeType("hippo:handle")) {
                        //FIXME is it better to check if the child node is image too ?
                        return "image-16";
                    }

                } catch (RepositoryException ex) {
                    log.error("Unable to determine whether node is a folder", ex);
                }
            }
            return null;
        }

        @Override
        public void detach() {
            super.detach();
            nodeModel.detach();
        }
    }

    @Override
    protected AttributeModifier getCellAttributeModifier(Node node) {
        return new CssClassAppender(new GalleryFolderAttributeModel(new JcrNodeModel(node)));
    }

    @Override
    protected AttributeModifier getColumnAttributeModifier() {
        return new CssClassAppender(new Model("icon-16"));
    }

}
