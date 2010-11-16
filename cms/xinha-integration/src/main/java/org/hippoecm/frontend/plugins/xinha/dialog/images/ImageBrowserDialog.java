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
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.xinha.dialog.AbstractBrowserDialog;
import org.hippoecm.frontend.plugins.xinha.services.images.XinhaImage;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.ThrottledTextFieldWidget;
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

    static final Logger log = LoggerFactory.getLogger(ImageBrowserDialog.class);

    public final static List<String> ALIGN_OPTIONS = Arrays.asList("top", "middle", "bottom", "left", "right");

    private ImagePickerSettings settings;

    public ImageBrowserDialog(IPluginContext context, final IPluginConfig config, final IModel<XinhaImage> model) {
        super(context, config, model);

        settings = new ImagePickerSettings();
        if (config.containsKey("preferred.resource.names")) {
            settings.setPreferredResourceNames(config.getStringArray("preferred.resource.names"));
        }

        createUploadForm(config);

        add(new ThrottledTextFieldWidget("alt", new StringPropertyModel(model, XinhaImage.ALT)) {
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

    @SuppressWarnings("unchecked")
    private void createUploadForm(final IPluginConfig config) {
        Form<?> uploadForm = new Form("uploadForm");

        uploadForm.setOutputMarkupId(true);
        final FileUploadField uploadField = new FileUploadField("uploadField");
        uploadField.setOutputMarkupId(true);

        final AjaxButton uploadButton = new AjaxButton("uploadButton", uploadForm) {
            private static final long serialVersionUID = 1L;

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
                            Node folderNode = getFolderModel().getObject();

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
                            log.error(ex.getMessage());
                            error(ex);
                        } catch (MappingException ex) {
                            log.error(ex.getMessage());
                            error(ex);
                        } catch (RepositoryException ex) {
                            log.error(ex.getMessage());
                            error(ex);
                        }
                        if (node != null) {
                            try {
                                getGalleryProcessor().makeImage(node, istream, mimetype, filename);
                                node.getSession().save();
                                uploadField.setModel(null);
                                target.addComponent(uploadField);
                            } catch (RepositoryException ex) {
                                log.error(ex.getMessage());
                                error(ex);
                                try {
                                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                                    defaultWorkflow.delete();
                                } catch (WorkflowException e) {
                                    log.error(e.getMessage());
                                } catch (MappingException e) {
                                    log.error(e.getMessage());
                                } catch (RepositoryException e) {
                                    log.error(e.getMessage());
                                }
                                try {
                                    node.getSession().refresh(false);
                                } catch (RepositoryException e) {
                                    // deliberate ignore
                                }
                            } catch (GalleryException ex) {
                                log.error(ex.getMessage());
                                error(ex);
                            }
                        }
                    } catch (IOException ex) {
                        log.info("upload of image truncated");
                        error("Unable to read the uploaded image");
                    }
                } else {
                    error("Please select a file to upload");
                }
            }
        };

        uploadButton.setOutputMarkupId(true);
        uploadField.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                uploadButton.setEnabled(true);
                target.addComponent(uploadButton);
            }
        });
        uploadButton.setEnabled(false);
        uploadForm.add(uploadField);
        uploadForm.add(uploadButton);

        add(uploadForm);

        //OMG: ugly workaround.. Input[type=file] is rendered differently on OSX in all browsers..
        WebRequestCycle requestCycle = (WebRequestCycle) RequestCycle.get();
        HttpServletRequest httpServletReq = requestCycle.getWebRequest().getHttpServletRequest();
        String ua = httpServletReq.getHeader("User-Agent");
        if (ua.indexOf("Macintosh") > -1) {
            uploadField.add(new AttributeAppender("class", true, new Model<String>("browse-button-osx"), " "));
            uploadButton.add(new AttributeAppender("class", true, new Model<String>("upload-button-osx"), " "));
        }
    }

    @Override
    protected void onOk() {
        XinhaImage image = getModelObject();

        //FIXME: Because our Image picker doesn't allow users to choose between the child-node-definitions
        // (thumbnail, original, etc) we work around this by selecting the "preferred" by default.
        String facetSelect = image.getFacetSelectPath();
        if (facetSelect != null && facetSelect.indexOf('/') > 0) {
            String basePath = facetSelect.substring(0, facetSelect.lastIndexOf('/') + 1);
            boolean match = false;
            for (String resourceName : settings.getPreferredResourceNames()) {
                image.setFacetSelectPath(basePath + resourceName);
                if (image.isValid()) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                image.setFacetSelectPath(facetSelect);
            }
        }
        if (image.isValid()) {
            image.save();
        } else {
            error("Please select an image");
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        final String IMAGE_BROWSER_DIALOG_CSS = "ImageBrowserDialog.css";
        ResourceReference dialogCSS = new ResourceReference(ImageBrowserDialog.class, IMAGE_BROWSER_DIALOG_CSS);
        response.renderCSSReference(dialogCSS);
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=845,height=525");
    }

    protected GalleryProcessor getGalleryProcessor() {
        IPluginContext context = getPluginContext();
        GalleryProcessor processor = context.getService(getPluginConfig().getString("gallery.processor.id",
                "gallery.processor.service"), GalleryProcessor.class);
        if (processor != null) {
            return processor;
        }
        return new DefaultGalleryProcessor();
    }

    private StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

    private StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }
}
