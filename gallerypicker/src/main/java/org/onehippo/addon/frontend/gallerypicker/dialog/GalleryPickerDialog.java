/*
 * Copyright 2011-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.addon.frontend.gallerypicker.dialog;

import javax.jcr.Node;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.editor.plugins.linkpicker.GalleryUploadPanel;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GalleryPickerDialog extends LinkPickerDialog {

    private static Logger log = LoggerFactory.getLogger(GalleryPickerDialog.class);

    private final boolean enableUpload;
    private GalleryUploadPanel uploadPanel;

    public GalleryPickerDialog(IPluginContext context, IPluginConfig config, IModel<String> model) {
        super(context, config, model);
        enableUpload = config.getAsBoolean("enable.upload", false);
    }

    @Override
    protected Fragment createTopFragment(final String id) {
        final Fragment fragment;
        if (enableUpload) {
            fragment = new Fragment(id, "upload-fragment", this);
            uploadPanel = new GalleryUploadPanel("upload-panel",
                    new PropertyModel<>(this, "selectedFolderNode"),
                    getPluginContext(), getPluginConfig(), getGalleryProcessor()) {

                @Override
                protected void createGalleryItem(final FileUpload upload, final String galleryType) {
                    super.createGalleryItem(upload, galleryType);

                    // manually refresh feedback panel on an ajax request
                    Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
                    target.ifPresent(ajaxRequestTarget -> ajaxRequestTarget.add(feedback));
                }
            };
            fragment.add(uploadPanel);
        } else {
            // default empty top fragment
            fragment = super.createTopFragment(id);
        }
        return fragment;
    }

    /**
     * Return the selected node of the tree folder
     */
    public Node getSelectedFolderNode() {
        IModel<Node> folderModel = getFolderModel();
        if (folderModel != null) {
            return folderModel.getObject();
        } else {
            return null;
        }
    }

    @Override
    protected FeedbackPanel newFeedbackPanel(final String id) {
        return new FeedbackPanel(id, new ContainerFeedbackMessageFilter(this)) {{
            setOutputMarkupId(true);
        }};
    }

    protected GalleryProcessor getGalleryProcessor() {
        IPluginContext context = getPluginContext();
        GalleryProcessor processor = context.getService(getPluginConfig().getString("gallery.processor.id",
                "service.gallery.processor"), GalleryProcessor.class);
        if (processor != null) {
            return processor;
        }
        return new DefaultGalleryProcessor();
    }

    @Override
    protected void setOkEnabled(boolean isset) {
        super.setOkEnabled(isset);

        // function is called by parent constructor, when our constructor hasn't run yet...
        if (uploadPanel != null) {
            final Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
            target.ifPresent(ajaxRequestTarget -> ajaxRequestTarget.add(uploadPanel));
        }
    }
}
