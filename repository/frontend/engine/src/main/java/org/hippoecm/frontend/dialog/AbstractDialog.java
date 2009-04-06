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
package org.hippoecm.frontend.dialog;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.WicketAjaxIndicatorAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.DefaultMarkupCacheKeyProvider;
import org.apache.wicket.markup.DefaultMarkupResourceStreamProvider;
import org.apache.wicket.markup.IMarkupCacheKeyProvider;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.MarkupException;
import org.apache.wicket.markup.MarkupNotFoundException;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDialog extends Form implements IDialogService.Dialog, IAjaxIndicatorAware {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    static final Logger log = LoggerFactory.getLogger(AbstractDialog.class);

    @SuppressWarnings("unchecked")
    private class Container extends Panel implements IMarkupCacheKeyProvider, IMarkupResourceStreamProvider {
        private static final long serialVersionUID = 1L;

        private IMarkupCacheKeyProvider cacheKeyProvider;
        private IMarkupResourceStreamProvider streamProvider;

        public Container(String id) {
            super(id);
            cacheKeyProvider = new DefaultMarkupCacheKeyProvider();
            streamProvider = new DefaultMarkupResourceStreamProvider();
        }

        public String getCacheKey(MarkupContainer container, Class containerClass) {
            return cacheKeyProvider.getCacheKey(AbstractDialog.this, AbstractDialog.this.getClass());
        }

        // implement IMarkupResourceStreamProvider.
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class containerClass) {
            return streamProvider.getMarkupResourceStream(AbstractDialog.this, AbstractDialog.this.getClass());
        }

        // used for markup inheritance (<wicket:extend />)
        public MarkupStream getAssociatedMarkupStream(final boolean throwException) {
            try {
                return getApplication().getMarkupSettings().getMarkupCache().getMarkupStream(AbstractDialog.this,
                        false, throwException);
            } catch (MarkupException ex) {
                // re-throw it. The exception contains already all the information
                // required.
                throw ex;
            } catch (WicketRuntimeException ex) {
                // throw exception since there is no associated markup
                throw new MarkupNotFoundException(
                        exceptionMessage("Markup of type '"
                                + getMarkupType()
                                + "' for component '"
                                + AbstractDialog.this.getClass().getName()
                                + "' not found."
                                + " Enable debug messages for org.apache.wicket.util.resource to get a list of all filenames tried"),
                        ex);
            }
        }
    }

    private final static ResourceReference AJAX_LOADER_GIF = new ResourceReference(AbstractDialog.class,
            "ajax-loader.gif");

    protected FeedbackPanel feedback;
    private LinkedList<Button> buttons;
    protected final Button ok;
    protected final Button cancel;
    private IDialogService dialogService;
    private Panel container;
    private WicketAjaxIndicatorAppender indicator;
    private AjaxFormSubmitBehavior ajaxSubmit;
    protected boolean cancelled = false;

    public AbstractDialog() {
        this(null);
    }

    protected class ExceptionFeedbackPanel extends FeedbackPanel {

        boolean expanded;

        protected ExceptionFeedbackPanel(String id) {
            super(id);
            setOutputMarkupId(true);
            expanded = false;
        }

        protected class ExceptionLabel extends Panel {

            private WebMarkupContainer details;
            private AjaxLink link;

            protected ExceptionLabel(String id, IModel model, Exception ex, boolean escape) {
                super(id);
                setOutputMarkupId(true);
                add(link = new AjaxLink("message") {
                        public void onClick(final AjaxRequestTarget target) {
                            // the following does not work as the list is re-created
                            //   details.setVisible(!details.isVisible());
                            //   target.addComponent(details);
                            // so instead here we remember the previous state in the parent
                            expanded = !expanded;
                            target.addComponent(ExceptionFeedbackPanel.this);
                        }
                    });
                Label label;
                link.add(label = new Label("label", model));
                label.setEscapeModelStrings(escape);
                add(details = new WebMarkupContainer("details"));
                details.setVisible(expanded); // use workaround iso: details.setVisible(false);
                details.setOutputMarkupId(true);
                if (ex != null) {
                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                    ex.printStackTrace(new PrintStream(ostream));
                    details.add(new Label("exceptionClass", ex.getClass().getName()));
                    details.add(new Label("exceptionMessage", ex.getLocalizedMessage()));
                    details.add(new Label("exceptionTrace", ostream.toString()));
                } else {
                    details.add(new Label("exceptionClass"));
                    details.add(new Label("exceptionMessage"));
                    details.add(new Label("exceptionTrace"));
                }
            }
        }

        @Override
        protected Component newMessageDisplayComponent(String id, FeedbackMessage message) {
            Serializable serializable = message.getMessage();
            if (serializable instanceof Exception) {
                Exception ex = (Exception) serializable;
                Map<String, String> details = new HashMap<String, String>();
                details.put("type", ex.getClass().getName());
                details.put("message", ex.getMessage());
                ExceptionLabel label = new ExceptionLabel(id, new StringResourceModel("exception,${type},${message}", AbstractDialog.this, new Model((Serializable) details), ex.getLocalizedMessage()), ex, ExceptionFeedbackPanel.this.getEscapeModelStrings());
                return label;
            } else {
                Label label = new Label(id);
                label.setModel(new Model(serializable == null ? "" : serializable.toString()));
                label.setEscapeModelStrings(ExceptionFeedbackPanel.this.getEscapeModelStrings());
                return label;
            }
        }

        @Override
        protected FeedbackMessagesModel newFeedbackMessagesModel() {
            return new FeedbackMessagesModel(this) {
                private List messages;
                @Override
                protected List processMessages(final List messages) {
                    this.messages = messages;
                    return messages;
                }
                @Override
                public void detach() {
                    if (messages == null || messages.size() == 0) {
                        super.detach();
                    }
                }
            };
        }
    }

    public AbstractDialog(IModel model) {
        super("form", model);

        setOutputMarkupId(true);

        container = new Container(IDialogService.DIALOG_WICKET_ID);
        container.add(this);

        feedback = new ExceptionFeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        buttons = new LinkedList<Button>();
        add(new ListView("buttons", new Model(buttons)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                Button button = (Button) item.getModelObject();
                if (!button.getId().equals("button")) {
                    log.error("button's id not correct");
                }
                item.add(button);
            }
        });

        ok = new Button(getButtonId()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                onOk();
                if (!hasError()) {
                    closeDialog();
                }
            }
        };
        ok.add(new Label("label", new ResourceModel("ok")));
        buttons.add(ok);

        setAjaxSubmit(true);

        cancel = new AjaxButton(getButtonId(), this) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                cancelled = true;
                onCancel();
                closeDialog();
            }
        }.setDefaultFormProcessing(false);
        cancel.add(new Label("label", new ResourceModel("cancel")));
        buttons.add(cancel);

        add(indicator = new WicketAjaxIndicatorAppender() {
            private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getIndicatorUrl() {
                return RequestCycle.get().urlFor(AJAX_LOADER_GIF);
            }
        });
    }

    protected FeedbackPanel newFeedbackPanel(String id) {
        return new ExceptionFeedbackPanel(id);
    }

    public String getAjaxIndicatorMarkupId() {
        return indicator.getMarkupId();
    }

    @Override
    public boolean isTransparentResolver() {
        return true;
    }

    public void setAjaxSubmit(boolean isset) {
        if (isset) {
            if (ajaxSubmit == null) {
                ok.add(ajaxSubmit = new AjaxFormSubmitBehavior(this, "onclick") {
                    private static final long serialVersionUID = 1L;

                    protected CharSequence getEventHandler() {
                        return new AppendingStringBuffer(super.getEventHandler()).append("; return false;");
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                    }
                });
            }
        } else {
            if (ajaxSubmit != null) {
                ok.remove(ajaxSubmit);
                ajaxSubmit = null;
            }
        }
    }

    public void setDialogService(IDialogService dialogService) {
        this.dialogService = dialogService;
    }

    protected final void closeDialog() {
        dialogService.close();
    }

    protected String getButtonId() {
        return "button";
    }

    protected void addButton(Button button) {
        if (getButtonId().equals(button.getId())) {
            buttons.addFirst(button);
        } else {
            log.error("Failed to add button: component id is not '{}'", getButtonId());
        }
    }

    protected void removeButton(Button button) {
        buttons.remove(button);
    }

    @Override
    protected final void onSubmit() {
        Page page = (Page) findParent(Page.class);
        if (page != null) {
            IFormSubmittingComponent submitButton = findSubmittingButton();
            if (submitButton == null) {
                onOk();
                if (!hasError()) {
                    closeDialog();
                }
            }
        }
    }

    protected void onOk() {
    }

    protected void onCancel() {
    }

    public Component getComponent() {
        return container;
    }

    public void render(PluginRequestTarget target) {
        if (target != null) {
            target.addComponent(feedback);
            target.addComponent(ok);
        }
    }

    public void onClose() {
    }

    public IValueMap getProperties() {
        return new ValueMap();
    }

}
