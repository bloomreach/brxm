/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.editor;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageBinary;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.FileUploadInfo;
import org.hippoecm.frontend.plugins.jquery.upload.single.FileUploadPanel;
import org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.ImageUploadValidationService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.UpdateFeedbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin for uploading images. The plugin can be configured by setting configuration options found in the {@link
 * org.hippoecm.frontend.plugins.jquery.upload.FileUploadWidgetSettings}.
 */
public class ImageUploadPlugin extends RenderPlugin {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadPlugin.class);

    private final IEditor.Mode mode;

    public ImageUploadPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);
        mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.EDIT);
        add(createFileUploadPanel());
        add(new EventStoppingBehavior("click"));
        setOutputMarkupId(true);
    }

    private FileUploadPanel createFileUploadPanel() {
        final FileUploadValidationService validator =
                ImageUploadValidationService.getValidationService(getPluginContext(), getPluginConfig());
        final FileUploadPreProcessorService preProcessorService =
                DefaultFileUploadPreProcessorService.getPreProcessorService(getPluginContext(), getPluginConfig());
        final FileUploadPanel panel = new FileUploadPanel("fileUpload", getPluginConfig(), validator,
                preProcessorService) {

            @Override
            protected void onBeforeUpload(final FileUploadInfo fileUploadInfo) {
                // clear all old feedback messages in other plugin instances (i.e. image variants) via the EditPerspective
                send(ImageUploadPlugin.this, Broadcast.BUBBLE, new UpdateFeedbackInfo(null, true));
            }

            @Override
            public void onFileUpload(final FileUpload fileUpload) throws FileUploadViolationException {
                handleUpload(fileUpload);
            }
        };
        panel.setVisible(mode == IEditor.Mode.EDIT);
        return panel;
    }

    private void handleUpload(FileUpload upload) throws FileUploadViolationException {
        String fileName = upload.getClientFileName();
        String mimeType = upload.getContentType();
        final GalleryProcessor processor = DefaultGalleryProcessor.getGalleryProcessor(getPluginContext(), getPluginConfig());

        JcrNodeModel nodeModel = (JcrNodeModel) getDefaultModel();
        Node node = nodeModel.getNode();
        try {
            ImageBinary image = new ImageBinary(node, upload.getInputStream(), fileName, mimeType);
            processor.initGalleryResource(node, image.getStream(), image.getMimeType(), image.getFileName(), Calendar.getInstance());
        } catch (IOException | GalleryException | RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.error("Cannot upload image ", e);
            } else {
                log.error("Cannot upload image: {}", e.getMessage());
            }
            throw new FileUploadViolationException(e.getMessage());
        }
    }
}
