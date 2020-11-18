/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload.behaviors;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.json.JSONArray;
import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.page.IPageManager;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.file.FileCleanerTrackerAdapter;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.TemporaryFileItem;
import org.hippoecm.frontend.plugins.yui.upload.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The file upload behavior to handle uploads sent by ajax requests. The component container can override the following
 * events:
 * <ul>
 *     <li>{@link #onBeforeUpload(FileUploadInfo)}</li>
 *     <li>{@link #process(FileUpload)}</li>
 *     <li>{@link #onUploadError(FileUploadInfo)}</li>
 *     <li>{@link #onAfterUpload(FileItem, FileUploadInfo)}</li>
 * </ul>
 */
public abstract class AjaxFileUploadBehavior extends AbstractAjaxBehavior {
    private static final Logger log = LoggerFactory.getLogger(AjaxFileUploadBehavior.class);

    public static final String APPLICATION_JSON = "application/json";

    public static final String JQUERY_FILEUPLOAD_FILES = "files";
    public static final String JQUERY_FILEUPLOAD_NAME = "name";
    public static final String JQUERY_FILEUPLOAD_SIZE = "size";
    public static final String JQUERY_FILEUPLOAD_ERROR = "error";
    private final WebMarkupContainer container;
    private final DiskFileItemFactory diskFileItemFactory;

    public AjaxFileUploadBehavior(final WebMarkupContainer container) {
        this.container = container;
        this.diskFileItemFactory = new DiskFileItemFactory() {
            @Override
            public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
                FileItem item = super.createItem(fieldName, contentType, isFormField, fileName);
                return new TemporaryFileItem(item);
            }
        };
    }

    @Override
    public void onRequest() {
        ServletWebRequest servletWebRequest = (ServletWebRequest) RequestCycle.get().getRequest();
        try {
            MultipartServletWebRequest multipartServletWebRequest = createMultipartWebRequest(servletWebRequest);
            multipartServletWebRequest.parseFileParts();

            Map<String, FileUploadInfo> allUploadedFiles = new HashMap<>();
            // try to upload all files
            for (List<FileItem> files : multipartServletWebRequest.getFiles().values()) {
                for (FileItem file : files) {
                    // save file info prior uploading because temporary files may be deleted,
                    // thus their file sizes won't be correct.
                    FileUploadInfo fileUploadInfo = new FileUploadInfo(file.getName(), file.getSize());

                    onBeforeUpload(fileUploadInfo);
                    try {
                        FileUpload fileUpload = new FileUpload(file);
                        log.debug("Validating file: {}", file.getName());
                        validate(fileUpload);
                        log.debug("Pre-processing file: {}", file.getName());
                        fileUpload = preProcess(file, fileUpload);
                        log.debug("Processed a file: {}", file.getName());
                        process(fileUpload);
                    } catch (FileUploadViolationException e) {
                        e.getViolationMessages().forEach(errorMsg -> fileUploadInfo.addErrorMessage(errorMsg));
                        if (log.isDebugEnabled()) {
                            log.debug("Uploading file '{}' has some violation: {}", file.getName(),
                                    StringUtils.join(fileUploadInfo.getErrorMessages().toArray(), ";"), e);
                        }
                        onUploadError(fileUploadInfo);
                    } catch (Exception e) {
                        log.error(String.format("There was a problem processing the file %s, the file couldn't be " +
                                "uploaded", file.getName()), e);
                    } finally {
                        // remove from cache
                        file.delete();
                    }
                    onAfterUpload(file, fileUploadInfo);
                    allUploadedFiles.put(file.getName(), fileUploadInfo);
                }
            }
            setResponse(servletWebRequest, allUploadedFiles);
        } catch (FileUploadException e) {
            final String message = "Error handling file upload request";
            log.error(message, e);
            setResponse(servletWebRequest, message);
        }
    }

    /**
     * Since the fileItem is not modifiable (its OutputStream is already closed), this internal method creates a
     * temporary file where we will load the Byte[] from the fileItem. The different pre-processors will be able to
     * update that file and in the end we will generate a new FileItem using the Byte[] from the temporary file.
     * @param fileItem
     * @param originalFileUpload
     * @return
     * @throws IOException
     */
    private FileUpload preProcess(FileItem fileItem, final FileUpload originalFileUpload) throws Exception {
        File tempFile = null;
        try {
            tempFile = originalFileUpload.writeToTempFile();
            UploadedFile uploadedFile = new UploadedFile(tempFile, fileItem);
            preProcess(uploadedFile);

            fileItem = diskFileItemFactory.createItem(uploadedFile.getFieldName(), uploadedFile.getContentType(),
                    uploadedFile.isFormField(), uploadedFile.getFileName());
            OutputStream outputStream = fileItem.getOutputStream();
            outputStream.write(Files.readAllBytes(tempFile.toPath()));
            return new FileUpload(fileItem);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    /**
     * Event is fired before processing the uploaded file.
     */
    protected void onBeforeUpload(final FileUploadInfo fileUploadInfo) {
    }

    protected void onUploadError(final FileUploadInfo fileUploadInfo) {
    }

    protected void validate(final FileUpload fileUpload) throws FileUploadViolationException {
    }

    protected void preProcess(final UploadedFile uploadedFile) {
    }

    protected abstract void process(final FileUpload fileUpload) throws FileUploadViolationException;

    /**
     * Create a multi-part request containing uploading files that supports disk caching.
     *
     * @param request
     * @return the multi-part request or exception thrown if there's any error.
     * @throws FileUploadException
     */
    private MultipartServletWebRequest createMultipartWebRequest(final ServletWebRequest request) throws FileUploadException {
        diskFileItemFactory.setFileCleaningTracker(new FileCleanerTrackerAdapter(Application.get().getResourceSettings().getFileCleaner()));
        
        try {
            long contentLength = Long.valueOf(request.getHeader("Content-Length"));
            if (contentLength > 0) {
                return request.newMultipartWebRequest(Bytes.bytes(contentLength), container.getPage().getId(), diskFileItemFactory);
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
                log.warn("file {} contains errors: {}", filename,
                        StringUtils.join(errorMessages, ";"));
            }
        }
        onResponse(request, uploadedFiles);

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
        // touch page and commit request so page gets released (detached)
        final Session session = Session.get();
        final IPageManager pageManager = session.getPageManager();
        final Page page = container.getPage();
        pageManager.touchPage(page);
        pageManager.commitRequest();
    }

    private String generateJsonResponse(final Map<String, FileUploadInfo> uploadedFiles) {
        JSONObject jsonResponse = new JSONObject();
        JSONArray jsonFiles = new JSONArray();

        for (String fileName : uploadedFiles.keySet()) {
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
        String escapedJson = StringEscapeUtils.escapeHtml4(jsonResponse);
        return escapedJson;
    }

    /**
     * Decides what should be the response's content type depending on the 'Accept' request header. HTML5 browsers
     * work with "application/json", older ones use IFrame to make the upload and the response should be HTML. Read
     * http://blueimp.github.com/jQuery-File-Upload/ docs for more info.
     *
     * @param request
     */
    protected boolean wantsHtml(ServletWebRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return !Strings.isEmpty(acceptHeader) && acceptHeader.contains("text/html");
    }

    protected void onAfterUpload(final FileItem file, final FileUploadInfo fileUploadInfo) {}

    protected void onResponse(final ServletWebRequest request, final Map<String, FileUploadInfo> uploadedFiles) {}

}
