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
package org.hippoecm.frontend.editor.plugins.resource;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.single.FileUploadPanel;
import org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.validation.DefaultUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_TEXT;

/**
 * Plugin for uploading resources into the JCR repository. This plugin can be configured with specific types, so not all
 * file types are allowed to be uploaded.
 */
public class ResourceUploadPlugin extends RenderPlugin {

    static final Logger log = LoggerFactory.getLogger(ResourceUploadPlugin.class);
    public static final String DEFAULT_ASSET_VALIDATION_SERVICE_ID = "service.gallery.asset.validation";

    private final IEditor.Mode mode;

    public ResourceUploadPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.EDIT);
        add(createFileUploadPanel());
        add(new EventStoppingBehavior("click"));
    }

    private FileUploadPanel createFileUploadPanel() {
        final FileUploadPanel panel = new FileUploadPanel("fileUpload", getPluginConfig(), getValidationService(),
                getPreProcessorService()) {
            @Override
            public void onFileUpload(final FileUpload fileUpload) throws FileUploadViolationException {
                handleUpload(fileUpload);
            }
        };
        panel.setVisible(mode == IEditor.Mode.EDIT);
        return panel;
    }

    private FileUploadValidationService getValidationService() {
        return DefaultUploadValidationService.getValidationService(getPluginContext(), getPluginConfig(),
                DEFAULT_ASSET_VALIDATION_SERVICE_ID);
    }

    private FileUploadPreProcessorService getPreProcessorService() {
        return DefaultFileUploadPreProcessorService.getPreProcessorService(getPluginContext(), getPluginConfig());
    }

    /**
     * Handles the file upload from the form.
     *
     * @param upload the {@link FileUpload} containing the upload information
     */
    private void handleUpload(FileUpload upload) throws FileUploadViolationException {
        String fileName = upload.getClientFileName();
        String mimeType = upload.getContentType();

        JcrNodeModel nodeModel = (JcrNodeModel) ResourceUploadPlugin.this.getDefaultModel();
        Node node = nodeModel.getNode();
        try {
            ResourceHelper.setDefaultResourceProperties(node, mimeType, upload.getInputStream(), fileName);
            if (MimeTypeHelper.isPdfMimeType(mimeType)) {
                InputStream inputStream = node.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
                ResourceHelper.handlePdfAndSetHippoTextProperty(node, inputStream);
            } else if (node.hasProperty(HIPPO_TEXT)) {
                node.getProperty(HIPPO_TEXT).remove();
            }
        } catch (RepositoryException | IOException ex) {
            if (log.isDebugEnabled()) {
                log.error("Cannot upload resource", ex);
            } else {
                log.error("Cannot upload resource: {}", ex.getMessage());
            }
            throw new FileUploadViolationException(ex.getMessage());
        }
    }

}
