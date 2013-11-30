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
package org.hippoecm.frontend.plugins.gallery.editor;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageBinary;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.yui.upload.FileUploadWidget;
import org.hippoecm.frontend.plugins.yui.upload.FileUploadWidgetSettings;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin for uploading images. The plugin can be configured by setting configuration options found in the {@link
 * FileUploadWidgetSettings}.
 */
public class ImageUploadPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ImageUploadPlugin.class);

    private IValueMap types;

    public ImageUploadPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        FileUploadForm form = new FileUploadForm("form");
        add(form);
        String mode = config.getString("mode", "edit");
        form.setVisible("edit".equals(mode));

        add(new EventStoppingBehavior("onclick"));
    }

    private class FileUploadForm extends Form {
        private static final long serialVersionUID = 1L;

        private FileUploadWidget widget;

        public FileUploadForm(String name) {
            super(name);

            String serviceId = getPluginConfig().getString(FileUploadValidationService.VALIDATE_ID, "service.gallery.image.validation");
            FileUploadValidationService validator = getPluginContext().getService(serviceId, FileUploadValidationService.class);
            FileUploadWidgetSettings settings = new FileUploadWidgetSettings(getPluginConfig());

            add(widget = new FileUploadWidget("multifile", settings, validator) {
                @Override
                protected void onFileUpload(FileUpload fileUpload) {
                    handleUpload(fileUpload);
                }
            });
        }

        @Override
        protected void onSubmit() {
            widget.onFinishHtmlUpload();
        }

    }

    private void handleUpload(FileUpload upload) {
        String fileName = upload.getClientFileName();
        String mimeType = upload.getContentType();

        String serviceId = getPluginConfig().getString("gallery.processor.id", "gallery.processor.service");
        GalleryProcessor processor = getPluginContext().getService(serviceId, GalleryProcessor.class);
        if (processor == null) {
            processor = new DefaultGalleryProcessor();
        }

        JcrNodeModel nodeModel = (JcrNodeModel) getDefaultModel();
        Node node = nodeModel.getNode();

        try {
            ImageBinary image = new ImageBinary(node, upload.getInputStream(), fileName, mimeType);
            processor.initGalleryResource(node, image.getStream(), image.getMimeType(), image.getFileName(), Calendar.getInstance());
            processor.validateResource(node, image.getFileName());
        } catch (IOException | GalleryException | RepositoryException e) {
            error(e);
            log.error(e.getMessage());
        }
    }
}
