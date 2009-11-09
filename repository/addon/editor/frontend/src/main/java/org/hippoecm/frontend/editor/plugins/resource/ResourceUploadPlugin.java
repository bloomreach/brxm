/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor.plugins.resource;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUploadPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ResourceUploadPlugin.class);

    private IValueMap types;
    private FileUploadForm form;

    public ResourceUploadPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        types = new ValueMap(config.getString("types"));

        // Add upload form with ajax progress bar
        form = new FileUploadForm("form");
        add(form);

        add(new EventStoppingBehavior("onclick"));

        String mode = config.getString("mode", "edit");
        if (!"edit".equals(mode)) {
            form.setVisible(false);
        }
    }

    private class FileUploadForm extends Form {
        private static final long serialVersionUID = 1L;

        private FileUploadField fileUploadField;

        public FileUploadForm(String name) {
            super(name);

            add(fileUploadField = new FileUploadField("fileInput"));

            add(new Button("submit", new StringResourceModel("upload", ResourceUploadPlugin.this, null)));
        }

        @Override
        protected void onSubmit() {
            final FileUpload upload = fileUploadField.getFileUpload();
            if (upload != null) {
                String fileName = upload.getClientFileName();
                String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
                
                String type = types.getString(extension.toLowerCase());
                if (type != null) {
                    JcrNodeModel nodeModel = (JcrNodeModel) ResourceUploadPlugin.this.getModel();
                    Node node = nodeModel.getNode();
                    try {
                        node.setProperty("jcr:mimeType", type);
                        node.setProperty("jcr:data", upload.getInputStream());
                        node.setProperty("jcr:lastModified", Calendar.getInstance());
                    } catch (RepositoryException ex) {
                        // FIXME: report back to user
                        log.error(ex.getMessage());
                    } catch (IOException ex) {
                        // FIXME: report back to user
                        log.error(ex.getMessage());
                    }
                } else {
                    String extensions = StringUtils.join(types.keySet().toArray(), ", ");
                    getDialogService().show(
                            new ExceptionDialog(new StringResourceModel("unrecognized", ResourceUploadPlugin.this,
                                    null, new Object[] { extension, extensions }).getString()) {

                                public IValueMap getProperties() {
                                    return SMALL;
                                }

                            });
                    log.warn("Unrecognised file type");
                }
            }
        }
    }

}
