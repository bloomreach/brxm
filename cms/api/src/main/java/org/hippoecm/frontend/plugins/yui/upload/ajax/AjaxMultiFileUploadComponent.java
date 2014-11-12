/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.upload.ajax;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.IBehaviorListener;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequestImpl;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.upload.DiskFileItemFactory;
import org.apache.wicket.util.upload.FileItem;
import org.apache.wicket.util.upload.FileUploadException;
import org.hippoecm.frontend.plugins.yui.upload.MagicMimeTypeFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AjaxMultiFileUploadComponent extends Panel {

    private static final Logger log = LoggerFactory.getLogger(AjaxMultiFileUploadComponent.class);

    private final ResourceReference flashResource = new PackageResourceReference(AjaxMultiFileUploadComponent.class, "res/uploader.swf");

    private class UploadBehavior extends AbstractAjaxBehavior {

        public void onRequest() {
            try {
                final ServletWebRequest request = (ServletWebRequest) RequestCycle.get().getRequest();
                HttpServletRequest httpServletRequest = request.getContainerRequest();
                MultipartServletWebRequest multipartServletWebRequest = new MultipartServletWebRequestImpl(httpServletRequest, request.getFilterPrefix(),
                        Bytes.MAX, getPage().getId(),
                        new DiskFileItemFactory(Application.get().getResourceSettings().getFileCleaner()) {

                            @Override
                            public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
                                FileItem item = super.createItem(fieldName, contentType, isFormField, fileName);
                                return new MagicMimeTypeFileItem(item);
                            }
                        });
                for (List<FileItem> files : multipartServletWebRequest.getFiles().values()) {
                    for (FileItem file : files) {
                        onFileUpload(new FileUpload(file));
                    }
                }
                setResponse("success");
            } catch (FileUploadException e) {
                log.error("Error handling upload request", e);
                setResponse("failed");
            }
        }

        private void setResponse(final String responseText) {
            RequestCycle.get().scheduleRequestHandlerAfterCurrent(new IRequestHandler() {

                @Override
                public void respond(IRequestCycle requestCycle) {
                    final WebResponse response = (WebResponse) requestCycle.getResponse();
                    final Application app = Application.get();
                    final String encoding = app.getRequestCycleSettings().getResponseRequestEncoding();
                    response.setContentType("text/xml; charset=" + encoding);

                    // Make sure it is not cached by a client
                    response.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
                    response.setHeader("Cache-Control", "no-cache, must-revalidate");
                    response.setHeader("Pragma", "no-cache");

                    response.write("<?xml version=\"1.0\" encoding=\"");
                    response.write(encoding);
                    response.write("\"?>");
                    response.write("<ajax-response>");
                    response.write(responseText);
                    response.write("</ajax-response>");
                }

                @Override
                public void detach(IRequestCycle requestCycle) {
                }
            });
        }
    }

    private UploadBehavior uploadBehavior;
    private AjaxMultiFileUploadSettings settings;

    public AjaxMultiFileUploadComponent(String id, AjaxMultiFileUploadSettings settings) {
        super(id, new Model<LinkedList<FileUpload>>(new LinkedList<FileUpload>()));

        setOutputMarkupId(true);
        settings.setFlashUrl(urlFor(flashResource, null).toString());

        add(new AjaxMultiFileUploadBehavior(settings) {

            @Override
            protected void onFinish(AjaxRequestTarget ajaxRequestTarget) {
                AjaxMultiFileUploadComponent.this.onFinish(ajaxRequestTarget);
            }
        });

        add(uploadBehavior = new UploadBehavior());

        this.settings = settings;
    }

    @Override
    protected void onBeforeRender() {
        String sessionId = Session.get().getId();
        String uploadUrl = ";jsessionid=" + sessionId + urlFor(uploadBehavior, IBehaviorListener.INTERFACE, new PageParameters()).toString();
        settings.setUploadUrl(uploadUrl);
        super.onBeforeRender();
    }

    protected abstract void onFileUpload(FileUpload fileUpload);

    protected abstract void onFinish(AjaxRequestTarget target);

}
