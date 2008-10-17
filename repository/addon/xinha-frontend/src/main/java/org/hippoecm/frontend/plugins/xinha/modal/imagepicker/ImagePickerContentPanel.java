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

import java.util.EnumMap;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaContentPanel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalWindow;
import org.hippoecm.frontend.plugins.xinha.modal.imagepicker.ImageItemFactory.ImageItem;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImagePickerContentPanel extends XinhaContentPanel<XinhaImage> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private final static Logger log = LoggerFactory.getLogger(ImagePickerContentPanel.class);

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_THUMBNAIL_WIDTH = "50";

    private ImageItem selectedItem;
    private ImageItemDAO imageItemDAO;
    private String alt;

    public ImagePickerContentPanel(XinhaModalWindow modal, final EnumMap<XinhaImage, String> values,
            final ImageItemDAO imageItemDAO) {
        super(modal, values);

        this.imageItemDAO = imageItemDAO;
        this.selectedItem = imageItemDAO.create(values);

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
        final List<ImageItem> items = imageItemDAO.getItems();
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

        form.add(new TextFieldWidget("alt", new PropertyModel(this, "alt")));
    }

    @Override
    protected String getXinhaParameterName(XinhaImage k) {
        return k.getValue();
    }

    @Override
    protected void onDetach() {
        this.imageItemDAO.detach();
        super.onDetach();
    }

    @Override
    protected void onOk() {
        if (imageItemDAO.saveOrUpdate(selectedItem)) {
            values.put(XinhaImage.URL, selectedItem.getUrl());
            values.put(XinhaImage.ALT, alt);
        }
    }
}
