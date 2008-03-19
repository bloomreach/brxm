/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.template.resource;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.template.model.ItemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUploadPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ResourceUploadPlugin.class);

    private FileUploadForm form;

    public ResourceUploadPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new ItemModel(model), parentPlugin);

        // Add upload form with ajax progress bar
        form = new FileUploadForm("form");
        add(form);
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel newModel = new JcrNodeModel(notification.getModel());
            form.setModel(newModel);
        }
        super.receive(notification);
    }

    private class FileUploadForm extends Form {
        private static final long serialVersionUID = 1L;

        private FileUploadField fileUploadField;

        public FileUploadForm(String name) {
            super(name);

            add(fileUploadField = new FileUploadField("fileInput"));
            add(new Button("submit", new Model("Upload")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    FileUploadForm.this.onSubmit();
                }
            });
        }

        @Override
        protected void onSubmit() {
            final FileUpload upload = fileUploadField.getFileUpload();
            if (upload != null) {
                ItemModel itemModel = (ItemModel) ResourceUploadPlugin.this.getModel();
                Node node = itemModel.getNodeModel().getNode();
                try {
                    node.setProperty("jcr:mimeType", "image/jpeg");
                    node.setProperty("jcr:data", upload.getInputStream());
                    node.setProperty("jcr:lastModified", Calendar.getInstance());

                    Channel channel = getTopChannel();
                    if (channel != null) {
                        Request request = channel.createRequest("flush", itemModel.getNodeModel());
                        channel.send(request);
                    }
                } catch (RepositoryException ex) {
                    // FIXME: report back to user
                    log.error(ex.getMessage());
                } catch (IOException ex) {
                    // FIXME: report back to user
                    log.error(ex.getMessage());
                }
            }
        }
    }

}
