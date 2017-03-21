/*
 * Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.gallery.Translations;
import org.hippoecm.frontend.plugins.gallery.columns.modify.MimeTypeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.IListColumnProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.NodeComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;
import org.hippoecm.frontend.skin.DocumentListColumn;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FallbackAssetGalleryListColumnProvider implements IListColumnProvider {

    private static final FallbackAssetGalleryListColumnProvider INSTANCE = new FallbackAssetGalleryListColumnProvider();

    private static final Logger log = LoggerFactory.getLogger(FallbackAssetGalleryListColumnProvider.class);

    private FallbackAssetGalleryListColumnProvider() {
    }

    public static FallbackAssetGalleryListColumnProvider getInstance() {
        return INSTANCE;
    }

    public List<ListColumn<Node>> getColumns() {
        return Arrays.asList(
                createTypeColumn(),
                createNameColumn(),
                createSizeColumn()
        );
    }

    public List<ListColumn<Node>> getExpandedColumns() {
        return getColumns();
    }

    private static ListColumn<Node> createTypeColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("assetgallery-type", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "type");
        column.setRenderer(EmptyRenderer.getInstance());
        column.setAttributeModifier(new MimeTypeAttributeModifier());
        column.setComparator(new MimeTypeComparator());
        column.setCssClass(DocumentListColumn.ICON.getCssClass());
        return column;
    }

    private static ListColumn<Node> createNameColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("assetgallery-name", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "name");
        column.setComparator(NameComparator.getInstance());
        column.setCssClass(DocumentListColumn.NAME.getCssClass());
        return column;
    }

    private static ListColumn<Node> createSizeColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("assetgallery-size", Translations.class);
        final ListColumn<Node> column = new ListColumn<>(displayModel, "size");
        column.setRenderer(new SizeRenderer());
        column.setComparator(new SizeComparator());
        column.setCssClass(DocumentListColumn.SIZE.getCssClass());
        return column;
    }

    public IHeaderContributor getHeaderContributor() {
        return null;
    }

    private static class MimeTypeComparator extends NodeComparator {

        @Override
        public int compare(JcrNodeModel nodeModel1, JcrNodeModel nodeModel2) {
            try {
                final String mimeType1 = getMimeType(nodeModel1);
                final String mimeType2 = getMimeType(nodeModel2);
                return String.CASE_INSENSITIVE_ORDER.compare(mimeType1, mimeType2);
            } catch (RepositoryException e) {
                log.debug("Error while comparing MIME type of assets '{}' and '{}'. Assuming they're equal.",
                        JcrUtils.getNodePathQuietly(nodeModel1.getNode()),
                        JcrUtils.getNodePathQuietly(nodeModel2.getNode()),
                        e);
                return 0;
            }
        }

        private String getMimeType(final JcrNodeModel nodeModel) throws RepositoryException {
            final Node node = getCanonicalNode(nodeModel);
            if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                Node imageSet = node.getNode(node.getName());
                Item primaryItem = JcrHelper.getPrimaryItem(imageSet);
                if (primaryItem.isNode()) {
                    Node primaryItemNode = (Node) primaryItem;
                    if (primaryItemNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
                        return primaryItemNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
                    }
                }
            }
            return StringUtils.EMPTY;
        }
    }

    private static class SizeRenderer extends AbstractNodeRenderer {

        ByteSizeFormatter formatter = new ByteSizeFormatter(1);

        @Override
        protected Component getViewer(String id, Node node) throws RepositoryException {
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                final String nodeName = node.getName();
                if (node.hasNode(nodeName)) {
                    Node imageSet = node.getNode(nodeName);
                    try {
                        final Item primItem = JcrHelper.getPrimaryItem(imageSet);
                        if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                            long length = ((Node) primItem).getProperty(JcrConstants.JCR_DATA).getLength();
                            return new Label(id, formatter.format(length));
                        } else {
                            log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                        }
                    } catch (ItemNotFoundException e) {
                        log.warn("ImageSet must have a primary item. " + node.getPath()
                                + " probably not of correct image set type");
                    }
                }
            }
            return new Label(id);
        }

    }

    private static class SizeComparator extends NodeComparator {

        @Override
        public int compare(JcrNodeModel nodeModel1, JcrNodeModel nodeModel2) {
            try {
                long size1 = getSize(nodeModel1);
                long size2 = getSize(nodeModel2);
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

        private long getSize(final JcrNodeModel nodeModel) throws RepositoryException {
            final Node node = getCanonicalNode(nodeModel);
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                final String nodeName = node.getName();
                if (node.hasNode(nodeName)) {
                    final Node imageSet = node.getNode(nodeName);
                    final Item primaryItem = JcrHelper.getPrimaryItem(imageSet);
                    if (primaryItem.isNode() && ((Node) primaryItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        return ((Node) primaryItem).getProperty(JcrConstants.JCR_DATA).getLength();
                    }
                }
            }
            return 0;
        }

    }
}
