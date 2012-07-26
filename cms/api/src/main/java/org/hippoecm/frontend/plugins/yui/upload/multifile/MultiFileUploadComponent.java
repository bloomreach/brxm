/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.yui.upload.multifile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.MultiFileUploadField;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.IMultipartWebRequest;
import org.apache.wicket.util.convert.ConversionException;
import org.hippoecm.frontend.plugins.yui.upload.MagicMimeTypeFileItem;

public class MultiFileUploadComponent extends Panel {

    private static final String MULTI_FILE_UPLOAD_CSS = "MultiFileUpload.css";
    private static final String MULTI_FILE_UPLOAD_CUSTOM_JS = "MultiFileUploadFieldCustomized.js";

    private final MultiFileUploadField uploadField;

    public MultiFileUploadComponent(String id, int max) {
        super(id);

        setOutputMarkupId(true);

        Form form = new Form("uploadform");
        add(form);
        form.add(uploadField = new MultiFileUploadField("input", new Model(new LinkedList<FileUpload>()), max) {
            private static final long serialVersionUID = 1L;

            @Override
            public void renderHead(IHeaderResponse response) {
                super.renderHead(response);

                response.renderJavascriptReference(new ResourceReference(MultiFileUploadComponent.class,
                        MULTI_FILE_UPLOAD_CUSTOM_JS));
            }

            @Override
            protected Collection<FileUpload> convertValue(String[] value) throws ConversionException {
                Collection<FileUpload> uploads = null;
                final String[] filenames = getInputAsArray();
                if (filenames != null) {
                    final IMultipartWebRequest request = (IMultipartWebRequest) getRequest();
                    uploads = new ArrayList<FileUpload>(filenames.length);
                    for (String filename : filenames) {
                        uploads.add(new FileUpload(new MagicMimeTypeFileItem(request.getFile(filename))));
                    }
                }
                return uploads;
            }
        });
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);

        container.getHeaderResponse().renderCSSReference(
                new ResourceReference(MultiFileUploadComponent.class, MULTI_FILE_UPLOAD_CSS));
    }

    public Collection<FileUpload> getUploads() {
        return uploadField.getConvertedInput();
    }
}
