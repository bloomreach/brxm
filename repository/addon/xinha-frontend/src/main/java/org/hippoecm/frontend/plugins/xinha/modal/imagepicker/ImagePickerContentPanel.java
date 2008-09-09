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
package org.hippoecm.frontend.plugins.xinha.modal.imagepicker;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaContentPanel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalWindow;
import org.hippoecm.frontend.plugins.xinha.modal.imagepicker.ImageItemFactory.ImageItem;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImagePickerContentPanel extends XinhaContentPanel<XinhaImage> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private final static Logger log = LoggerFactory.getLogger(ImagePickerContentPanel.class);

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_THUMBNAIL_WIDTH = "50";

    private ImageItemFactory imageItemFactory;
    private ImageItem selectedItem;

    public ImagePickerContentPanel(XinhaModalWindow modal, JcrNodeModel nodeModel,
            final EnumMap<XinhaImage, String> values) {
        super(modal, nodeModel, values);

        imageItemFactory = new ImageItemFactory(nodeModel, values);
        selectedItem = imageItemFactory.createImageItem();
        
        ok.setEnabled(selectedItem.isValid());

        // ******************************************************************
        // Image resource nodes various formats
        final FormComponent dropdown = new DropDownChoice("resourcenodes", new PropertyModel(this,
                "selectedItem.selectedResourceDefinition"), selectedItem.getResourceDefinitions()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean wantOnSelectionChangedNotifications() {
                return true;
            }

            @Override
            protected void onSelectionChanged(Object newSelection) {
                selectedItem.setSelectedResourceDefinition((String) newSelection);
            }
        };
        dropdown.setOutputMarkupId(true);
        dropdown.setEnabled(false);

        dropdown.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (selectedItem.getSelectedResourceDefinition() != null) {
                    target.addComponent(ok.setEnabled(true));
                }
            }
        });
        form.add(dropdown);

        // ******************************************************************
        // preview of the selected image
        final PreviewImage selectedImagePreview = new PreviewImage("selectedImagePreview", new PropertyModel(this,
                "selectedItem.primaryUrl"), DEFAULT_THUMBNAIL_WIDTH, null);
        selectedImagePreview.setOutputMarkupId(true);
        form.add(selectedImagePreview);

        // ******************************************************************
        // image listing
        final List<ImageItem> items = getImageItems(nodeModel);
        form.add(new ListView("item", items) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem item) {

                final ImageItem imageItem = (ImageItem) item.getModelObject();

                WebMarkupContainer entry = new WebMarkupContainer("entry");
                entry.add(new PreviewImage("thumbnail", imageItem.getPrimaryUrl(), DEFAULT_THUMBNAIL_WIDTH, null));

                final AjaxLink link = new AjaxLink("callback") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        selectedItem = imageItem;
                        target.addComponent(selectedImagePreview);

                        if (selectedItem.getResourceDefinitions().size() > 1) {
                            target.addComponent(dropdown.setEnabled(true));
                            //TODO: maybe no reset?
                            //selectedImage.setSelectedResourceDefinition(null);
                        } else if (dropdown.isEnabled()) {
                            target.addComponent(dropdown.setEnabled(false));
                        }

                        target.addComponent(ok.setEnabled(selectedItem.isValid()));
                        target.addComponent(feedback);
                    }
                };
                link.add(entry);
                item.add(link);

            }
        });
    }
    
    @Override
    protected String getXinhaParameterName(XinhaImage k) {
        return k.getValue();
    }

    @Override
    protected void onDetach() {
        this.nodeModel.detach();
        super.onDetach();
    }

    @Override
    protected void onOk() {
        if (selectedItem.getUuid() == null) {
            return;
        }
        values.put(XinhaImage.URL, selectedItem.getUrl());

        Node node = nodeModel.getNode();
        String nodeName = selectedItem.getNodeName();
        try {
            if (node.hasNode(nodeName)) {
                return;
            }
            Node facetselect = node.addNode(nodeName, HippoNodeType.NT_FACETSELECT);
            //todo fetch corresponding uuid of the chosen imageset
            facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, selectedItem.getUuid());
            facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});
            facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
            facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});
            // need a node save (the draft so no problem) to visualize images
            node.save();
        } catch (RepositoryException e) {
            log
                    .error("An error occured while trying to save new image facetSelect[" + selectedItem.getUuid()
                            + "]", e);
        }
    }

    private List<ImageItem> getImageItems(JcrNodeModel nodeModel2) {
        List<ImageItem> items = new ArrayList<ImageItem>();
        String gallerySearchPath = "/content/gallery-search";
        try {
            Session session = nodeModel.getNode().getSession();
            Node gallerySearchNode = (Node) session.getItem(gallerySearchPath);
            Node resultset = gallerySearchNode.getNode(HippoNodeType.HIPPO_RESULTSET);
            NodeIterator imageNodesIt = resultset.getNodes();
            while (imageNodesIt.hasNext()) {
                HippoNode imageNode = (HippoNode) imageNodesIt.nextNode();
                // nextNode can return null
                if (imageNode == null) {
                    continue;
                }
                Node canonical = imageNode.getCanonicalNode();
                if (canonical != null && canonical.getParent().isNodeType("mix:referenceable")) {
                    try { //test if canonical node has a primaryItem
                        canonical.getPrimaryItem().getName();
                        items.add(imageItemFactory.createImageItem(canonical));
                    } catch (ItemNotFoundException e) {
                        log.error("gallery node does not have a primary item: skipping node: " + canonical.getPath());
                    }
                }
            }
        } catch (PathNotFoundException e) {
            log.error("Gallery Search node missing: " + gallerySearchPath);
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage(), e);
        }
        return items;
    }
}
