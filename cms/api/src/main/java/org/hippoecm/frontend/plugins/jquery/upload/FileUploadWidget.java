/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.json.JSONArray;
import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.IBehaviorListener;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.upload.DiskFileItemFactory;
import org.apache.wicket.util.upload.FileItem;
import org.apache.wicket.util.upload.FileUploadException;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

/**
 * A panel that combines all the parts of the file uploader
 */
public abstract class FileUploadWidget extends Panel {
    private static final long serialVersionUID = 1L;
    public static final String APPLICATION_JSON = "application/json";
    private enum ResponseType {
        OK,
        FAILED

    }
    final Logger log = LoggerFactory.getLogger(FileUploadWidget.class);

    private long fileUploadCounter = 0;
    private int nNumberOfFiles = 0;

    private FileUploadBar fileUploadBar;
    private FileUploadWidgetSettings settings;

    private AbstractAjaxBehavior ajaxCallbackDoneBehavior;
    private AjaxFileUploadBehavior ajaxFileUploadBehavior;

    private class AjaxFileUploadBehavior extends AbstractAjaxBehavior {
        private static final long serialVersionUID = 1L;

        public static final String JQUERY_FILEUPLOAD_FILES = "files";
        public static final String JQUERY_FILEUPLOAD_NAME = "name";
        public static final String JQUERY_FILEUPLOAD_SIZE = "size";
        public static final String JQUERY_FILEUPLOAD_ERROR = "error";

        @Override
        public void onRequest() {
            ServletWebRequest servletWebRequest = (ServletWebRequest) RequestCycle.get().getRequest();
            try {
                MultipartServletWebRequest multipartServletWebRequest = createMultipartWebRequest(servletWebRequest);

                Map<String, FileUploadInfo> allUploadedFiles = new HashMap<>();
                // try to upload all files
                for (List<FileItem> files : multipartServletWebRequest.getFiles().values()) {
                    for (FileItem file : files) {
                        // save file info prior uploading because temporary files may be deleted,
                        // thus their file sizes won't be correct.
                        FileUploadInfo fileUploadInfo = new FileUploadInfo(file.getName(), file.getSize());
                        try {
                            log.debug("Processed a file: {}", file.getName());
                            onFileUpload(file);
                        } catch (FileUploadViolationException e) {
                            for (String errorMsg : e.getViolationMessages()) {
                                fileUploadInfo.addErrorMessage(errorMsg);
                            }
                            if (log.isDebugEnabled()) {
                                log.debug("Uploading file '{}' has some violation: {}", file.getName(),
                                        StringUtils.join(fileUploadInfo.getErrorMessages().toArray()), e);
                            }
                        } finally {
                            // remove from cache
                            file.delete();
                        }
                        // increase file counter after processed a file
                        increaseFileUploadingCounter();
                        allUploadedFiles.put(file.getName(), fileUploadInfo);
                    }
                }
                setResponse(servletWebRequest, allUploadedFiles);
            } catch (FileUploadException e) {
                log.error("Error handling file upload request", e);
                String responseContent = String.format("{\"error\": \"%s\"}", "Error handling file upload request");
                setResponse(servletWebRequest, responseContent);
            }
        }

        /**
         * Create a multi-part request containing uploading files that supports disk caching.
         * @param request
         * @return the multi-part request or exception thrown if there's any error.
         * @throws FileUploadException
         */
        private MultipartServletWebRequest createMultipartWebRequest(final ServletWebRequest request) throws FileUploadException {
            DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory(Application.get().getResourceSettings().getFileCleaner()) {
                @Override
                public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
                    FileItem item = super.createItem(fieldName, contentType, isFormField, fileName);
                    return new TemporaryFileItem(item);
                }
            };

            try {
                long contentLength = Long.valueOf(request.getHeader("Content-Length"));
                if (contentLength > 0) {
                    return request.newMultipartWebRequest(Bytes.bytes(contentLength), getPage().getId(), diskFileItemFactory);
                } else {
                    throw new FileUploadException("Invalid file upload content length");
                }
            } catch (NumberFormatException e) {
                throw new FileUploadException("Invalid file upload content length", e);
            }
        }

        protected void setResponse(final ServletWebRequest request, final Map<String, FileUploadInfo> uploadedFiles) {
            for (String filename : uploadedFiles.keySet()) {
                List<String> errorMessages = uploadedFiles.get(filename).getErrorMessages();
                if (!errorMessages.isEmpty()) {
                    log.error("file {} contains errors: {}", filename,
                            StringUtils.join(errorMessages, ";"));
                }
            }

            final String responseContent;
            if (wantsHtml(request)) {
                responseContent = generateHtmlResponse(uploadedFiles);
            } else {
                responseContent = generateJsonResponse(uploadedFiles);
            }
            setResponse(request, responseContent);
        }

        private void setResponse(final ServletWebRequest request, final String responseContent) {
            final Application app = Application.get();
            final String encoding = app.getRequestCycleSettings().getResponseRequestEncoding();

            final String contentType;
            if (wantsHtml(request)) {
                contentType = "text/html; charset=" + encoding;
            } else {
                contentType = APPLICATION_JSON;
            }
            TextRequestHandler textRequestHandler = new TextRequestHandler(contentType, encoding, responseContent);
            RequestCycle.get().scheduleRequestHandlerAfterCurrent(textRequestHandler);
        }

        private String generateJsonResponse(final Map<String, FileUploadInfo> uploadedFiles) {
            JSONObject jsonResponse = new JSONObject();
            JSONArray jsonFiles = new JSONArray();

            for (String fileName: uploadedFiles.keySet()) {
                JSONObject fileJson = new JSONObject();
                try {
                    fileJson.put(JQUERY_FILEUPLOAD_NAME, fileName);
                    long size = uploadedFiles.get(fileName).getSize();
                    fileJson.put(JQUERY_FILEUPLOAD_SIZE, size);
                    List<String> errors = uploadedFiles.get(fileName).getErrorMessages();
                    if (errors != null && !errors.isEmpty()) {
                        fileJson.put(JQUERY_FILEUPLOAD_ERROR, StringUtils.join(errors, ";"));
                    }
                } catch (JSONException e) {
                    try {
                        fileJson.put(JQUERY_FILEUPLOAD_ERROR, e.getMessage());
                    } catch (JSONException e1) {
                        log.error("Error creating JSON response for file uploading", e1);
                    }
                }
                jsonFiles.put(fileJson);
            }

            try {
                jsonResponse.put(JQUERY_FILEUPLOAD_FILES, jsonFiles);
            } catch (JSONException e) {
                try {
                    jsonResponse.put(JQUERY_FILEUPLOAD_ERROR, e.getMessage());
                } catch (JSONException e1) {
                    log.error("Error creating JSON response for file uploading", e1);
                }
            }
            return jsonResponse.toString();
        }

        private String generateHtmlResponse(final Map<String, FileUploadInfo> uploadedFiles) {
            String jsonResponse = generateJsonResponse(uploadedFiles);
            String escapedJson = escapeHtml(jsonResponse);
            return escapedJson;
        }

        /**
         * Decides what should be the response's content type depending on the 'Accept' request header. HTML5 browsers work
         * with "application/json", older ones use IFrame to make the upload and the response should be HTML. Read
         * http://blueimp.github.com/jQuery-File-Upload/ docs for more info.
         *
         * @param request
         * @return
         */
        protected boolean wantsHtml(ServletWebRequest request) {
            String acceptHeader = request.getHeader("Accept");
            return !Strings.isEmpty(acceptHeader) && acceptHeader.contains("text/html");
        }

        /**
         * Class to store file information prior uploading
         */
        private class FileUploadInfo {
            private final String fileName;
            private final long size;
            private final List<String> errorMessages = new ArrayList<>();

            public FileUploadInfo(final String fileName, final long size) {
                this.fileName = fileName;
                this.size = size;
            }

            public void addErrorMessage(final String message) {
                this.errorMessages.add(message);
            }

            /**
             * Return either an empty list or a list of error messages occurred in uploading
             * @return
             */
            public List<String> getErrorMessages() {
                return Collections.unmodifiableList(errorMessages);
            }

            public long getSize() {
                return size;
            }
        }
    }

    public FileUploadWidget(final String uploadPanel, final IPluginConfig pluginConfig, final FileUploadValidationService validator) {
        super(uploadPanel);
        this.settings = new FileUploadWidgetSettings(pluginConfig, validator);
        createComponents();
    }

    private void createComponents() {
        ajaxFileUploadBehavior = new AjaxFileUploadBehavior();
        add(ajaxFileUploadBehavior);

        ajaxCallbackDoneBehavior = new AbstractAjaxBehavior(){

            /**
             * Handle notification from the file upload dialog. The notification contains number of files to be uploaded
             * in the following JSON format:
             * {
             *     total: #numberOfFiles
             * }
             *
             * The response is either of following:
             * {
             *     status: 'OK'|'FAILED'
             * }
             */
            @Override
            public void onRequest() {
                HttpServletRequest request = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
                try {
                    // The 'total' key contains expected #files uploaded.
                    JSONObject json = new JSONObject(IOUtils.toString(request.getReader()));
                    int numberOfFiles = json.getInt("total");
                    if (numberOfFiles < 0 || numberOfFiles > settings.getMaxNumberOfFiles()) {
                        log.error("Invalid notification parameter from jquery file upload dialog: numberOfFiles={}", numberOfFiles);
                        response(ResponseType.FAILED);
                        return;
                    }
                    log.debug("Number of files to be uploaded:{}", numberOfFiles);
                    response(ResponseType.OK);
                    FileUploadWidget.this.nNumberOfFiles = numberOfFiles;
                    if (fileUploadCounter < numberOfFiles) {
                        log.debug("Haven't received all files yet: {}/{}", fileUploadCounter, numberOfFiles);
                    } else {
                        // Received all files
                        onFinished();
                    }
                } catch (IOException | JSONException e) {
                    log.error("Failed to process the close notification from jquery file upload dialog", e);
                    response(ResponseType.FAILED);
                }
            }

            private void response(final ResponseType responseType) {
                String content = String.format("{\"status\":\"%s\"}", responseType.name());
                TextRequestHandler textRequestHandler = new
                        TextRequestHandler(APPLICATION_JSON, "UTF-8", content);
                RequestCycle.get().scheduleRequestHandlerAfterCurrent(textRequestHandler);
            }

        };
        add(ajaxCallbackDoneBehavior);

        // The buttons toolbar. Mandatory
        fileUploadBar = new FileUploadBar("fileUploadBar", settings);
        add(fileUploadBar);

        // The template used by jquery.fileupload-ui.js to show the files
        // scheduled for upload (i.e. the added files).
        // Optional
        FileUploadTemplate uploadTemplate = new FileUploadTemplate("uploadTemplate");
        add(uploadTemplate);

        // The template used by jquery.fileupload-ui.js to show the uploaded files
        // Optional
        FileDownloadTemplate downloadTemplate = new FileDownloadTemplate("downloadTemplate");
        add(downloadTemplate);
    }

    /**
     * Call this method every time an uploading file has been processed
     */
    private void increaseFileUploadingCounter() {
        this.fileUploadCounter++;
        log.debug("# uploaded files: {}", fileUploadCounter);
        // if nNumberOfFiles <= 0 means that the notification message from client has not been sent yet
        if (nNumberOfFiles > 0 && fileUploadCounter >= nNumberOfFiles) {
            log.debug("Received all files");
            onFinished();
        }
    }
    @Override
    protected void onBeforeRender() {
        // Obtain callback urls used for uploading files & notification
        String uploadUrl = urlFor(ajaxFileUploadBehavior, IBehaviorListener.INTERFACE, new PageParameters()).toString();
        settings.setUploadUrl(uploadUrl);

        String uploadDoneNotificationUrl = ajaxCallbackDoneBehavior.getCallbackUrl().toString();
        settings.setUploadDoneNotificationUrl(uploadDoneNotificationUrl);
        super.onBeforeRender();
    }

    protected abstract void onFileUpload(FileItem fileItem) throws FileUploadViolationException;

    protected abstract void onFinished();
}
