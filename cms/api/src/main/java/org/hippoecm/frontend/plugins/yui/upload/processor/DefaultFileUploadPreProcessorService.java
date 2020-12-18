/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.yui.upload.processor;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.TemporaryFileItem;
import org.hippoecm.frontend.plugins.yui.upload.model.IUploadPreProcessor;
import org.hippoecm.frontend.plugins.yui.upload.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link FileUploadPreProcessorService}. This implementation contains a list of {@link
 * IUploadPreProcessor} and executes them all when the method process is invoked.
 */
public class DefaultFileUploadPreProcessorService implements FileUploadPreProcessorService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFileUploadPreProcessorService.class);

    private List<IUploadPreProcessor> preProcessors = new LinkedList<>();

    public static FileUploadPreProcessorService getPreProcessorService(final IPluginContext pluginContext,
                                                                       final IPluginConfig pluginConfig) {
        return getPreProcessorService(pluginContext, pluginConfig, DEFAULT_ID);
    }

    public static FileUploadPreProcessorService getPreProcessorService(final IPluginContext pluginContext,
                                                                       final IPluginConfig pluginConfig,
                                                                       final String id) {
        String serviceId = pluginConfig.getString(FileUploadPreProcessorService.PRE_PROCESSOR_ID, id);
        FileUploadPreProcessorService preProcessorService = pluginContext.getService(serviceId,
                FileUploadPreProcessorService.class);

        if (preProcessorService == null) {
            preProcessorService = new DefaultFileUploadPreProcessorService();
            log.info("Cannot load pre processor service with id '{}', using the default service '{}'",
                    serviceId, preProcessorService.getClass().getName());
        }
        return preProcessorService;
    }

    protected final void addPreProcessor(IUploadPreProcessor preProcessor) {
        preProcessors.add(preProcessor);
    }


    @Override
    public FileUpload process(final FileItem fileItem, final FileUpload originalFileUpload) throws Exception {


        if (CollectionUtils.isEmpty(preProcessors)) {
            return originalFileUpload;
        }

        return getFileUpload(fileItem, originalFileUpload, uploadedFile -> {
            preProcessors.forEach(preProcessor -> {
                try {
                    preProcessor.process(uploadedFile);
                } catch (Exception exception) {
                    log.error(String.format("There was an error when executing the file upload pre processor %s",
                            preProcessor.getClass().getName()), exception);
                }
            });
        });
    }

    /**
     * Since the fileItem is not modifiable (its OutputStream is already closed), this internal method creates a
     * temporary file where we will load the Byte[] from the fileItem. The different pre-processors will be able to
     * update that file and in the end we will generate a new FileItem using the Byte[] from the temporary file.
     *
     * @param fileItem
     * @param originalFileUpload
     * @return
     */
    private FileUpload getFileUpload(final FileItem fileItem, final FileUpload originalFileUpload,
                                     final Consumer<UploadedFile> processor) throws Exception {
        File tempFile = null;
        OutputStream outputStream = null;
        try {
            // See CMS-14330: invoking closeStreams is required, otherwise an error may happen in Windows
            originalFileUpload.closeStreams();

            tempFile = originalFileUpload.writeToTempFile();
            final UploadedFile uploadedFile = new UploadedFile(tempFile, fileItem);


            processor.accept(uploadedFile);

            final DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory() {
                @Override
                public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
                    FileItem item = super.createItem(fieldName, contentType, isFormField, fileName);
                    return new TemporaryFileItem(item);
                }
            };

            final FileItem newFileItem = diskFileItemFactory.createItem(uploadedFile.getFieldName(),
                    uploadedFile.getContentType(),
                    uploadedFile.isFormField(), uploadedFile.getFileName());
            outputStream = newFileItem.getOutputStream();
            outputStream.write(Files.readAllBytes(tempFile.toPath()));
            return new FileUpload(newFileItem);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }
}
