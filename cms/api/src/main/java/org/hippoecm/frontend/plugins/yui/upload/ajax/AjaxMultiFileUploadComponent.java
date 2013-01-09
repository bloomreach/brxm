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

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Application;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.IBehaviorListener;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.upload.DiskFileItemFactory;
import org.apache.wicket.util.upload.FileItem;
import org.apache.wicket.util.upload.FileUploadException;
import org.hippoecm.frontend.plugins.yui.upload.MagicMimeTypeFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AjaxMultiFileUploadComponent extends Panel {

    private static final Logger log = LoggerFactory.getLogger(AjaxMultiFileUploadComponent.class);

    private final ResourceReference flashResource = new ResourceReference(AjaxMultiFileUploadComponent.class, "res/uploader.swf");

    private class UploadBehavior extends AbstractAjaxBehavior {

        public void onRequest() {
            MultipartServletWebRequest multipartServletWebRequest;
            try {
                HttpServletRequest httpServletRequest = ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest();
                multipartServletWebRequest = new MultipartServletWebRequest(httpServletRequest, Bytes.MAX,
                        new DiskFileItemFactory() {

                            @Override
                            public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
                                FileItem item = super.createItem(fieldName, contentType, isFormField, fileName);
                                return new MagicMimeTypeFileItem(item);
                            }
                        });
                for (FileItem fi : multipartServletWebRequest.getFiles().values()) {
                    onFileUpload(new FileUpload(fi));
                }
                setResponse("success");
            } catch (FileUploadException e) {
                log.error("Error handling upload request", e);
                setResponse("failed");
            }
        }

        private void setResponse(final String responseText) {
            RequestCycle.get().setRequestTarget(new IRequestTarget() {

                public void respond(RequestCycle requestCycle) {
                    final WebResponse response = (WebResponse) requestCycle.getResponse();
                    final Application app = Application.get();
                    final String encoding = app.getRequestCycleSettings().getResponseRequestEncoding();
                    response.setCharacterEncoding(encoding);
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

                public void detach(RequestCycle requestCycle) {
                }
            });
        }
    }

    private UploadBehavior uploadBehavior;
    private AjaxMultiFileUploadSettings settings;

    public AjaxMultiFileUploadComponent(String id, AjaxMultiFileUploadSettings settings) {
        super(id, new Model(new LinkedList<FileUpload>()));

        setOutputMarkupId(true);
        settings.setFlashUrl(urlFor(flashResource).toString());

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
        String uploadUrl = ";jsessionid=" + sessionId + urlFor(uploadBehavior, IBehaviorListener.INTERFACE).toString();
        settings.setUploadUrl(uploadUrl);
        super.onBeforeRender();
    }

    protected abstract void onFileUpload(FileUpload fileUpload);

    protected abstract void onFinish(AjaxRequestTarget target);

}
