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
package org.hippoecm.frontend.plugins.gallery.editor;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.yui.upload.FileUploadWidget;
import org.hippoecm.frontend.plugins.yui.upload.FileUploadWidgetSettings;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin for uploading images. The plugin can be configured by setting configuration options found in the
 * {@link FileUploadWidgetSettings}.
 *
 */
public class ImageUploadPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ImageUploadPlugin.class);

    private IValueMap types;

    public ImageUploadPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        // if the types config is not set, all extensions are allowed
        String typesConfig = config.getString("types");
        if (typesConfig != null) {
            types = new ValueMap(typesConfig);
        }

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

            FileUploadWidgetSettings settings = new FileUploadWidgetSettings(getPluginConfig());

            add(widget = new FileUploadWidget("multifile", settings) {
                @Override
                protected void onFileUpload(FileUpload fileUpload) {
                    handleUpload(fileUpload);
                }

            });
        }

        @Override
        protected void onSubmit() {
            widget.handleNonFlashSubmit();
        }

    }


    private void handleUpload(FileUpload upload) {
        String fileName = upload.getClientFileName();
        String mimeType = upload.getContentType();

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);

        // check if obligatory types/file extensions are set and matched
        if (types != null && types.getString(extension.toLowerCase()) == null) {
            String extensions = StringUtils.join(types.keySet().toArray(), ", ");
            getDialogService().show(
                    new ExceptionDialog(new StringResourceModel("unrecognized", ImageUploadPlugin.this, null,
                            new Object[]{extension, extensions}).getString()) {
                        public IValueMap getProperties() {
                            return DialogConstants.SMALL;
                        }

                    });
            log.warn("Unrecognised file type");
        } else {
            JcrNodeModel nodeModel = (JcrNodeModel) ImageUploadPlugin.this.getDefaultModel();
            Node node = nodeModel.getNode();
            try {
                GalleryProcessor processor = getPluginContext().getService(getPluginConfig().getString("gallery.processor.id",
                        "gallery.processor.service"), GalleryProcessor.class);
                if (processor == null) {
                    processor = new DefaultGalleryProcessor();
                }
                processor.initGalleryResource(node, upload.getInputStream(), mimeType, fileName, Calendar.getInstance());
                processor.validateResource(node, fileName);
            } catch (RepositoryException ex) {
                error(ex);
                log.error(ex.getMessage());
            } catch (IOException ex) {
                // FIXME: report back to user
                log.error(ex.getMessage());
            } catch (GalleryException ex) {
                error(ex);
                log.error(ex.getMessage());
            }
        }
    }
}
