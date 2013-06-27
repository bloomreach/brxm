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
package org.hippoecm.frontend.plugins.yui.upload.multifile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.MultiFileUploadField;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.IMultipartWebRequest;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.upload.FileUploadWidgetSettings;
import org.hippoecm.frontend.plugins.yui.upload.MagicMimeTypeFileItem;

public class MultiFileUploadComponent extends Panel {

    private static final ResourceReference JS = new JavascriptResourceReference(
            MultiFileUploadComponent.class, "MultiFileUploadFieldCustomized.js");
    private static final ResourceReference CSS = new JavascriptResourceReference(
            MultiFileUploadComponent.class, "res/MultiFileUpload.css");


    private final UploadField uploadField;
    private FileUploadWidgetSettings settings;

    /**
     * @deprecated Use {@link #MultiFileUploadComponent(String, org.hippoecm.frontend.plugins.yui.upload.FileUploadWidgetSettings)} instead.
     */
    @Deprecated
    public MultiFileUploadComponent(String id, int max) {
        this(id, createFileUploadSettings(max));
    }

    public MultiFileUploadComponent(final String componentId, final FileUploadWidgetSettings settings) {
        super(componentId);
        this.settings = settings;

        setOutputMarkupId(true);

        Form form = new Form("uploadform");
        add(form);

        uploadField = new UploadField("input", new Model<LinkedList<FileUpload>>(new LinkedList<FileUpload>()));
        form.add(uploadField);
    }

    private static FileUploadWidgetSettings createFileUploadSettings(final int max) {
        FileUploadWidgetSettings settings = new FileUploadWidgetSettings();
        settings.setMaxNumberOfFiles(max);
        return settings;
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        container.getHeaderResponse().renderCSSReference(CSS);
    }

    public Collection<FileUpload> getUploads() {
        return uploadField.getConvertedInput();
    }

    private class UploadField extends MultiFileUploadField {

        private static final String RESOURCE_SINGLE = "org.apache.wicket.mfu.caption.one";
        private static final String SELECTED_FILE = "org.apache.wicket.mfu.caption.selected";
        private static final String SELECTED_FILES = "org.apache.wicket.mfu.caption.selected.multiple";
        private static final String DELETE = "org.apache.wicket.mfu.delete";

        public UploadField(final String id, final IModel<? extends Collection<FileUpload>> model) {
            super(id, model, settings.getMaxNumberOfFiles());

            WebMarkupContainer container = (WebMarkupContainer) get("container");
            container.replace(new Label("caption", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return settings.getMaxNumberOfFiles() == 1 ?
                                getString(RESOURCE_SINGLE) : getString(RESOURCE_LIMITED, Model.of(settings));
                    }
                }) {

                @Override
                public boolean isVisible() {
                    return settings.isAlwaysShowLabel() || settings.getMaxNumberOfFiles() > 1 || !settings.isAutoUpload();
                }
            });
        }

        @Override
        public void renderHead(IHeaderResponse response) {
            response.renderJavascriptReference(JS);

            PackagedTextTemplate template =
                    new PackagedTextTemplate(MultiFileUploadComponent.class, "createMultiSelector.tpl");
            response.renderOnDomReadyJavascript(template.asString(getSettingsAsMap()));
        }

        private Map<String,Object> getSettingsAsMap() {
            Map<String, Object> settingsAsMap = new HashMap<String, Object>();

            settingsAsMap.put("inputName", getInputName());
            settingsAsMap.put("containerId", get("container").getMarkupId());
            settingsAsMap.put("maxNumberOfFiles", settings.getMaxNumberOfFiles());
            settingsAsMap.put("submitAfterSelect", settings.isAutoUpload());
            settingsAsMap.put("clearAfterSubmit", settings.isClearAfterUpload());
            settingsAsMap.put("uploadId", get("upload").getMarkupId());
            settingsAsMap.put("deleteLabel", getString(DELETE));
            settingsAsMap.put("listLabel", settings.getMaxNumberOfFiles() == 1 ?
                    getString(SELECTED_FILE) : getString(SELECTED_FILES));

            return settingsAsMap;
        }

        @Override
        protected Collection<FileUpload> convertValue(String[] value) throws ConversionException {
            Collection<FileUpload> uploads = null;
            final String[] fileNames = getInputAsArray();
            if (fileNames != null) {
                final IMultipartWebRequest request = (IMultipartWebRequest) getRequest();
                uploads = new ArrayList<FileUpload>(fileNames.length);
                for (String filename : fileNames) {
                    uploads.add(new FileUpload(new MagicMimeTypeFileItem(request.getFile(filename))));
                }
            }
            return uploads;
        }
    }
}
