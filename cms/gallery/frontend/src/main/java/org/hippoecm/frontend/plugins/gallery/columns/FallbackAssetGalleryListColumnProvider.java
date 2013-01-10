/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.columns;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.gallery.columns.modify.MimeTypeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.IListColumnProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.NodeComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

public class FallbackAssetGalleryListColumnProvider implements IListColumnProvider {

    static final Logger log = LoggerFactory.getLogger(FallbackAssetGalleryListColumnProvider.class);

    public IHeaderContributor getHeaderContributor() {
        return null;
    }

    public List<ListColumn<Node>> getColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        ListColumn column = new ListColumn(new ClassResourceModel("assetgallery-type", Translations.class), "type");
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new MimeTypeAttributeModifier());
        column.setComparator(new MimeTypeComparator());
        column.setCssClass("assetgallery-type");
        columns.add(column);

        column = new ListColumn(new ClassResourceModel("assetgallery-name", Translations.class), "name");
        column.setComparator(new NameComparator());
        column.setCssClass("assetgallery-name");
        columns.add(column);

        column = new ListColumn(new ClassResourceModel("assetgallery-size", Translations.class), "size");
        column.setRenderer(new SizeRenderer());
        column.setComparator(new SizeComparator());
        column.setCssClass("assetgallery-size");
        columns.add(column);

        return columns;
    }

    public List<ListColumn<Node>> getExpandedColumns() {
        return getColumns();
    }

    class MimeTypeComparator extends NodeComparator {

        @Override
        public int compare(JcrNodeModel nodeModel1, JcrNodeModel nodeModel2) {
            try {
                String mimeType1 = "";
                Node n1 = getCanonicalNode(nodeModel1);
                if (n1.isNodeType(HippoNodeType.NT_HANDLE) && n1.hasNode(n1.getName())) {
                    Node imageSet = n1.getNode(n1.getName());
                    Item primItem = JcrHelper.getPrimaryItem(imageSet);
                    if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        mimeType1 = ((Node) primItem).getProperty("jcr:mimeType").getString();
                    }
                }

                String mimeType2 = "";
                Node n2 = getCanonicalNode(nodeModel2);
                if (n2.isNodeType(HippoNodeType.NT_HANDLE) && n2.hasNode(n2.getName())) {
                    Node imageSet = n2.getNode(n2.getName());
                    Item primItem = JcrHelper.getPrimaryItem(imageSet);
                    if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        mimeType2 = ((Node) primItem).getProperty("jcr:mimeType").getString();
                    }
                }
                return String.CASE_INSENSITIVE_ORDER.compare(mimeType1, mimeType2);
            } catch (RepositoryException e) {
                return 0;
            }
        }
    }

    public class SizeRenderer extends AbstractNodeRenderer {

        ByteSizeFormatter formatter = new ByteSizeFormatter(1);

        @Override
        protected Component getViewer(String id, Node node) throws RepositoryException {
            if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                Node imageSet = node.getNode(node.getName());
                try {
                    Item primItem = JcrHelper.getPrimaryItem(imageSet);
                    if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        long length = ((Node) primItem).getProperty("jcr:data").getLength();
                        return new Label(id, formatter.format(length));
                    } else {
                        log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                    }
                } catch (ItemNotFoundException e) {
                    log.warn("ImageSet must have a primary item. " + node.getPath()
                            + " probably not of correct image set type");
                }
            }
            return new Label(id);
        }

    }

    class SizeComparator extends NodeComparator {

        @Override
        public int compare(JcrNodeModel nodeModel1, JcrNodeModel nodeModel2) {
            try {
                long size1 = 0;
                Node n1 = getCanonicalNode(nodeModel1);
                if (n1.isNodeType(HippoNodeType.NT_HANDLE) && n1.hasNode(n1.getName())) {
                    Node imageSet = n1.getNode(n1.getName());
                    Item primItem = JcrHelper.getPrimaryItem(imageSet);
                    if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        size1 = ((Node) primItem).getProperty("jcr:data").getLength();
                    }
                }

                long size2 = 0;
                Node n2 = getCanonicalNode(nodeModel2);
                if (n2.isNodeType(HippoNodeType.NT_HANDLE) && n2.hasNode(n2.getName())) {
                    Node imageSet = n2.getNode(n2.getName());
                    Item primItem = JcrHelper.getPrimaryItem(imageSet);
                    if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        size2 = ((Node) primItem).getProperty("jcr:data").getLength();
                    }
                }

                long diff = size1 - size2;
                if (diff < 0) {
                    return -1;
                } else if (diff > 0) {
                    return 1;
                }
                return 0;
            } catch (RepositoryException e) {
                return 0;
            }
        }

    }
}
