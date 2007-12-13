/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.admin.upload;

import java.util.Calendar;

import javax.jcr.Node;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadProgressBar;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.util.lang.Bytes;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.repository.jackrabbit.JarExpander;

public class UploadPlugin extends Plugin {

    private static final long serialVersionUID = 1L;

    private class FileUploadForm extends Form {

        private static final long serialVersionUID = 1L;

        private FileUploadField fileUploadField;

        public FileUploadForm(String name, JcrNodeModel model) {
            super(name, model);
            setMultiPart(true);
            setMaxSize(Bytes.megabytes(5));
            add(fileUploadField = new FileUploadField("fileInput"));
        }

        protected void onSubmit() {
            final FileUpload upload = fileUploadField.getFileUpload();
            if (upload != null) {
                Node parent = ((JcrNodeModel) getModel()).getNode();
                if (parent != null) {
                    try {
                        Node node = null;
                        String name = upload.getClientFileName();
                        if (parent.hasNode(name)) {
                            node = parent.getNode(name);
                        } else {
                            node = parent.addNode(name);
                        }
                        node.setProperty("jcr:mimeType", "application/octet-stream");
                        node.setProperty("jcr:data", upload.getInputStream());
                        node.setProperty("jcr:lastModified", Calendar.getInstance());

                        JarExpander expander = new JarExpander(node);
                        expander.extract();
                    } catch (Exception ex) {
                        // FIXME: report back to user
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    private FileUploadForm form;

    public UploadPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        // Add upload form with ajax progress bar
        form = new FileUploadForm("form", model);
        form.add(new UploadProgressBar("progress", form));
        add(form);
    }

    public void update(AjaxRequestTarget target, PluginEvent event) {
        JcrNodeModel newModel = event.getNodeModel(JcrEvent.NEW_MODEL);
        if (newModel != null) {
            form.setModel(newModel);
        }
    }
}