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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.json.JSONArray;
import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.IBehaviorListener;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.WebApplication;
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
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.MagicMimeTypeFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

/**
 * A panel that combines all the parts of the file uploader
 */
public abstract class FileUploadWidget extends Panel {
    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(FileUploadWidget.class);

    private FileUploadBar fileUploadBar;
    private FileUploadWidgetSettings settings;

    private AbstractAjaxBehavior ajaxCallbackDoneBehavior;
    private AjaxFileUploadBehavior ajaxFileUploadBehavior;

    private class AjaxFileUploadBehavior extends AbstractAjaxBehavior {
        private static final long serialVersionUID = 1L;

        public static final String JQUERY_FILEUPLOAD_FILES = "files";
        public static final String JQUERY_FILEUPLOAD_NAME = "name";
        public static final String JQUERY_FILEUPLOAD_SIZE = "size";

        @Override
        public void onRequest() {
            ServletWebRequest servletWebRequest = (ServletWebRequest) RequestCycle.get().getRequest();
            try {
                DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory(Application.get().getResourceSettings().getFileCleaner()) {
                    @Override
                    public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
                        FileItem item = super.createItem(fieldName, contentType, isFormField, fileName);
                        return new MagicMimeTypeFileItem(item);
                    }
                };

                MultipartServletWebRequest multipartServletWebRequest = servletWebRequest.
                        newMultipartWebRequest(getMaxSize(), getPage().getId(), diskFileItemFactory);

                List<FileItem> allFiles = new ArrayList<>();
                for (List<FileItem> files : multipartServletWebRequest.getFiles().values()) {
                    allFiles.addAll(files);
                    for (FileItem file : files) {
                        onFileUpload(file);
                    }
                }
                setResponseSuccess(servletWebRequest, allFiles);
            } catch (FileUploadException e) {
                log.error("Error handling file upload request", e);
                setResponseFailed(servletWebRequest);
            }
        }

        protected void setResponseFailed(final ServletWebRequest request) {
            JSONObject jsonResponse = new JSONObject();
            try {
                jsonResponse.put("error", "Error handling file upload request");
                setResponse(request, jsonResponse.toString());
            } catch (JSONException e) {
                log.error("Error creating JSON response", e);
            }
        }

        protected void setResponseSuccess(final ServletWebRequest request, final List<FileItem> files) {
            final String responseContent;
            if (wantsHtml(request)) {
                responseContent = generateHtmlResponse(files);
            } else {
                responseContent = generateJsonResponse(files);
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
                contentType = "application/json";
            }

            TextRequestHandler textRequestHandler = new TextRequestHandler(contentType, encoding, responseContent);
            RequestCycle.get().scheduleRequestHandlerAfterCurrent(textRequestHandler);
        }

        private String generateJsonResponse(final List<FileItem> files) {
            JSONObject jsonResponse = new JSONObject();
            JSONArray jsonFiles = new JSONArray();

            for (FileItem fileItem : files) {
                JSONObject fileJson = new JSONObject();
                try {
                    fileJson.put(JQUERY_FILEUPLOAD_NAME, fileItem.getName());
                    fileJson.put(JQUERY_FILEUPLOAD_SIZE, fileItem.getSize());
                } catch (JSONException e) {
                    try {
                        fileJson.put("error", e.getMessage());
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
                    jsonResponse.put("error", e.getMessage());
                } catch (JSONException e1) {
                    log.error("Error creating JSON response for file uploading", e1);
                }
            }
            return jsonResponse.toString();
        }

        private String generateHtmlResponse(final List<FileItem> files) {
            String jsonResponse = generateJsonResponse(files);
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
    };

    public FileUploadWidget(final String uploadPanel, final IPluginConfig pluginConfig, final IPluginContext pluginContext) {
        super(uploadPanel);

        this.settings = new FileUploadWidgetSettings(pluginConfig);
        createComponents();
    }

    private void createComponents() {
        ajaxFileUploadBehavior = new AjaxFileUploadBehavior();
        add(ajaxFileUploadBehavior);

        ajaxCallbackDoneBehavior = new AbstractAjaxBehavior(){

            /**
             * Called when a request to a behavior is received.
             */
            @Override
            public void onRequest() {
                WebApplication app = (WebApplication)getComponent().getApplication();
                AjaxRequestTarget target = app.newAjaxRequestTarget(getComponent().getPage());

                RequestCycle requestCycle = RequestCycle.get();
                requestCycle.scheduleRequestHandlerAfterCurrent(target);

                respond();
            }

            private void respond() {
                log.debug("Finished uploading");
                FileUploadWidget.this.onFileUploadFinished();
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

    @Override
    protected void onBeforeRender() {
        String uploadUrl = urlFor(ajaxFileUploadBehavior, IBehaviorListener.INTERFACE, new PageParameters()).toString();
        settings.setUploadUrl(uploadUrl);

        String uploadDoneNotificationUrl = ajaxCallbackDoneBehavior.getCallbackUrl().toString();
        settings.setUploadDoneNotificationUrl(uploadDoneNotificationUrl);

        super.onBeforeRender();
    }

    private Bytes getMaxSize() {
        return Bytes.bytes(settings.getMaxFileSize());
    }

    protected abstract void onFileUpload(FileItem fileItem) throws FileUploadException;

    protected abstract void onFileUploadFinished();
}
