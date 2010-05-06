/*
 * Copyright 2010 Hippo.
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

package org.hippoecm.frontend.widgets.upload;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.MultiFileUploadField;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import java.util.Collection;
import java.util.LinkedList;

public class UploadWidget extends Panel {
    final static String SVN_ID = "$Id$";

    interface UploadComponent extends IClusterable {
        Component getFocusComponent();
        Component getComponent();
        Collection<FileUpload> getUploads();
    }

    private final UploadComponent uploadComponent;

    public UploadWidget(String id) {
        super(id);
        setOutputMarkupId(true);

        //TODO: detect if flash is enabled, otherwise fallback to javascript which should be enough for Hippo atm.
        //For now only use default MultiFileUpload from Wicket
        uploadComponent = new MultiFileUpload("upload");
        add(uploadComponent.getComponent());

    }

    public Component getFocusComponent() {
        return uploadComponent.getFocusComponent();
    }

    public Collection<FileUpload> getUploads() {
        return uploadComponent.getUploads();
    }

    private class MultiFileUpload extends Fragment implements UploadComponent {

        private static final String MULTI_FILE_UPLOAD_CSS = "MultiFileUpload.css";
        private static final String MULTI_FILE_UPLOAD_CUSTOM_JS = "MultiFileUploadFieldCustomized.js";
        
        private final MultiFileUploadField uploadField;

        public MultiFileUpload(String id) {
            super(id, "multiFileUpload", UploadWidget.this);
            setOutputMarkupId(true);

            add(uploadField = new MultiFileUploadField("input", new Model(new LinkedList<FileUpload>())) {
                @Override
                public void renderHead(IHeaderResponse response) {
                    super.renderHead(response);

                    ResourceReference jsReference = new ResourceReference(UploadWidget.class,
                            MULTI_FILE_UPLOAD_CUSTOM_JS);
                    response.renderJavascriptReference(jsReference);

                }
            });

        }

        public Component getFocusComponent() {
            return uploadField;
        }

        public Component getComponent() {
            return this;
        }

        public Collection<FileUpload> getUploads() {
            return uploadField.getConvertedInput();
        }

        @Override
        public void renderHead(HtmlHeaderContainer container) {
            super.renderHead(container);

            ResourceReference cssResourceReference = new ResourceReference(UploadWidget.class, MULTI_FILE_UPLOAD_CSS);
            container.getHeaderResponse().renderCSSReference(cssResourceReference);
        }
    }

}
