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

package org.hippoecm.frontend.plugins.xinha.dialog.images;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.ImageUtils;
import org.hippoecm.frontend.plugins.xinha.dialog.AbstractBrowserDialog;
import org.hippoecm.frontend.plugins.xinha.services.images.XinhaImage;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageBrowserDialog extends AbstractBrowserDialog<XinhaImage> implements IHeaderContributor {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger LOGGER = LoggerFactory.getLogger(ImageBrowserDialog.class);

    public final static List<String> ALIGN_OPTIONS = Arrays.asList("top", "middle", "bottom", "left", "right");

    public ImageBrowserDialog(IPluginContext context, final IPluginConfig config, final IModel<XinhaImage> model) {
        super(context, config, model);

        createUploadForm(config);

        add(new TextFieldWidget("alt", new StringPropertyModel(model, XinhaImage.ALT)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        DropDownChoice<String> align = new DropDownChoice<String>("align", new StringPropertyModel(model,
                XinhaImage.ALIGN), ALIGN_OPTIONS, new IChoiceRenderer<String>() {
            private static final long serialVersionUID = 1L;

            public Object getDisplayValue(String object) {
                return new StringResourceModel(object, ImageBrowserDialog.this, null).getString();
            }

            public String getIdValue(String object, int index) {
                return object;
            }

        });
        align.add(new AjaxFormComponentUpdatingBehavior("onChange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        align.setOutputMarkupId(true);
        align.setNullValid(false);
        add(align);

        checkState();
    }

    private void createUploadForm(final IPluginConfig config) {
        Form uploadForm = new Form("uploadForm");


        uploadForm.setOutputMarkupId(true);
        final FileUploadField uploadField = new FileUploadField("uploadField");
        uploadField.setOutputMarkupId(true);
        uploadForm.add(uploadField);


        uploadForm.add(new AjaxButton("uploadButton", uploadForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                final FileUpload upload = uploadField.getFileUpload();
                if (upload != null) {
                    try {
                        String filename = upload.getClientFileName();
                        String mimetype;


                        mimetype = upload.getContentType();
                        InputStream istream = upload.getInputStream();
                        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                        HippoNode node = null;
                        try {
                            //Get the selected folder from the folderReference Service
                            Node folderNode = ((JcrNodeModel)folderReference.getModel()).getNode();


                            //TODO replace shortcuts with custom workflow category(?)
                            GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow("shortcuts", folderNode);
                            String nodeName = getNodeNameCodec().encode(filename);
                            String localName = getLocalizeCodec().encode(filename);
                            List<String> galleryTypes = workflow.getGalleryTypes();
                            Document document = workflow.createGalleryItem(nodeName, galleryTypes.get(0));
                            node = (HippoNode) (((UserSession) Session.get())).getJcrSession().getNodeByUUID(document.getIdentity());
                            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                            if (!node.getLocalizedName().equals(localName)) {
                                defaultWorkflow.localizeName(localName);
                            }
                        } catch (WorkflowException ex) {
                            LOGGER.error(ex.getMessage());
                            error(ex);
                        } catch (MappingException ex) {
                            LOGGER.error(ex.getMessage());
                            error(ex);
                        } catch (RepositoryException ex) {
                            LOGGER.error(ex.getMessage());
                            error(ex);
                        }
                        if (node != null) {
                            try {
                                ImageUtils.galleryProcessor(config).makeImage(node, istream, mimetype, filename);
                                node.getSession().save();
                                uploadField.setModel(null);
                                target.addComponent(uploadField);
                            } catch (RepositoryException ex) {
                                LOGGER.error(ex.getMessage());
                                error(ex);
                                try {
                                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                                    defaultWorkflow.delete();
                                } catch (WorkflowException e) {
                                    LOGGER.error(e.getMessage());
                                } catch (MappingException e) {
                                    LOGGER.error(e.getMessage());
                                } catch (RepositoryException e) {
                                    LOGGER.error(e.getMessage());
                                }
                                try {
                                    node.getSession().refresh(false);
                                } catch (RepositoryException e) {
                                    // deliberate ignore
                                }
                            }
                        }
                    } catch (IOException ex) {
                        LOGGER.info("upload of image truncated");
                        error("Unable to read the uploaded image");
                    }
                } else {
                    error("Please select a file to upload");
                }
            }
        });

        add(uploadForm);
    }

    @Override
    protected void onOk() {
        if (getModelObject().isValid()) {
            getModelObject().save();
        } else {
            error("Please select an image");
        }
    }

    public void renderHead(IHeaderResponse response) {
        final String IMAGE_BROWSER_DIALOG_CSS = "ImageBrowserDialog.css";
        ResourceReference dialogCSS = new ResourceReference(ImageBrowserDialog.class, IMAGE_BROWSER_DIALOG_CSS);
        response.renderCSSReference(dialogCSS);
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=845,height=525");
    }

    private StringCodec getNodeNameCodec() {
        ISettingsService settingsService = this.context.getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

     private StringCodec getLocalizeCodec() {
        ISettingsService settingsService = context.getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }
}
