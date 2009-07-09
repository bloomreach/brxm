package org.hippoecm.hst.plugins.frontend.editor.description;

import java.io.IOException;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.ImageUtils;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.hippoecm.hst.plugins.frontend.editor.domain.Descriptive;
import org.hippoecm.hst.plugins.frontend.util.IOUtil;

public class DescriptionPanel extends Panel {
    private static final long serialVersionUID = 1L;

    IPluginContext context;
    IPluginConfig config;

    public DescriptionPanel(String id, IModel model, IPluginContext context, IPluginConfig config) {
        super(id, model);

        this.context = context;
        this.config = config;

        TextAreaWidget descWidget = new TextAreaWidget("description", new PropertyModel(model, "description"));
        descWidget.setRenderBodyOnly(true);
        descWidget.setCols("25");
        add(descWidget);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        renderIconFragment();
    }

    private void renderIconFragment() {
        Fragment draw = null;
        Descriptive desc = (Descriptive) getModelObject();
        if (desc.getIconResource() == null) {
            addOrReplace(draw = new NoIconSelected("icon", "noIconSelected", this));
        } else {
            addOrReplace(draw = new IconSelected("icon", "iconSelected", this));
        }
        IRequestTarget target = RequestCycle.get().getRequestTarget();
        if (AjaxRequestTarget.class.isAssignableFrom(target.getClass())) {
            ((AjaxRequestTarget) target).addComponent(draw);
        }

    }

    class NoIconSelected extends Fragment {
        private static final long serialVersionUID = 1L;

        public NoIconSelected(String id, String markupId, MarkupContainer markupProvider) {
            super(id, markupId, markupProvider);

            setOutputMarkupId(true);

            add(new AjaxLink("iconSelector") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    IDialogService dialogService = context.getService(IDialogService.class.getName(),
                            IDialogService.class);
                    UploadDialog dialog = new UploadDialog();
                    dialogService.show(dialog);
                }
            });
        }

        class UploadDialog extends AbstractDialog {
            private static final long serialVersionUID = 1L;

            public UploadDialog() {
                setNonAjaxSubmit();
                add(new UploadForm());
            }
            
            @Override
            public IValueMap getProperties() {
                return SMALL;
            }

            public IModel getTitle() {
                return new Model(getString("dialog.upload.thumbnail"));
            }

        }

        class UploadForm extends Form {
            private static final long serialVersionUID = 1L;

            private final FileUploadField uploadField;

            public UploadForm() {
                super("form");

                setMultiPart(true);
                setMaxSize(Bytes.megabytes(5));
                add(uploadField = new FileUploadField("input"));
            }

            @Override
            protected void onSubmit() {
                final FileUpload upload = uploadField.getFileUpload();
                if (upload != null) {
                    try {
                        Descriptive desc = (Descriptive) DescriptionPanel.this.getModelObject();
                        desc.setIconResource(IOUtil.obtainResource(ImageUtils.createThumbnail(upload.getInputStream(),
                                100, upload.getContentType())));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    renderIconFragment();
                } else {
                    error("Something went wrong during upload.");
                }
            }
        }
    }

    class IconSelected extends Fragment {
        private static final long serialVersionUID = 1L;

        public IconSelected(String id, String markupId, MarkupContainer markupProvider) {
            super(id, markupId, markupProvider);

            setOutputMarkupId(true);

            Descriptive desc = (Descriptive) DescriptionPanel.this.getModelObject();
            NonCachingImage img = new NonCachingImage("image", desc.getIconResource()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    int width;
                    try {
                        width = config.getInt("icon.size");
                    } catch (StringValueConversionException e) {
                        width = 0;
                    }
                    if (width > 0) {
                        tag.put("width", width);
                    }
                }
            };
            add(img);

        }
    }
}
