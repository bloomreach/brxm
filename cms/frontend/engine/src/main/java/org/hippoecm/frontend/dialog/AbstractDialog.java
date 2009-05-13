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
import org.apache.wicket.IClusterable;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
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
        @Override
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
                    @Override
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

    class ButtonWrapper implements IClusterable {
        private static final long serialVersionUID = 1L;

        private Button button;

        private boolean ajax;
        private IModel label;
        private boolean visible;
        private boolean enabled;


        public ButtonWrapper(Button button) {
            this.button = button;
            visible = button.isVisible();
            enabled = button.isEnabled();
            label = button.getModel();

            if (button instanceof AjaxButton) {
                ajax = true;
            }
        }

        public ButtonWrapper(IModel label) {
            this(label, true);
        }

        public ButtonWrapper(IModel label, boolean ajax) {
            this.ajax = ajax;
            this.label = label;
            this.visible = true;
            this.enabled = true;
        }

        private Button createButton() {
            if (ajax) {
                AjaxButton button = new AjaxButton(getButtonId()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        ButtonWrapper.this.onSubmit();
                    }
                };
                button.setModel(label);
                return button;
            } else {
                Button button = new Button(getButtonId()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        ButtonWrapper.this.onSubmit();
                    }
                };
                button.setModel(label);
                return button;
            }
        }

        public Button getButton() {
            if(button == null) {
                button = createButton();
            }
            return decorate(button);
        }

        protected Button decorate(Button button) {
            button.setEnabled(enabled);
            button.setVisible(visible);
            return button;
        }

        public void setEnabled(boolean isset) {
            enabled = isset;
        }

        public void setVisible(boolean isset) {
            visible = isset;
        }

        public void setAjax(boolean c) {
            ajax = c;
        }

        public void setLabel(IModel label) {
            this.label = label;
            if (button != null) {
                button.setModel(label);
            }
            //TODO: test if this works or if it needs to add itself to the render target
        }

        protected void onSubmit() {
        }

        public boolean hasChanges() {
            if (!ajax) {
                return false;
            }

            if (button == null) {
                return true;
            }

            if (visible != button.isVisible()) {
                return true;
            }

            if (enabled != button.isEnabled()) {
                return true;
            }
            return false;
        }

    }

    private final static ResourceReference AJAX_LOADER_GIF = new ResourceReference(AbstractDialog.class,
    "ajax-loader.gif");

    protected FeedbackPanel feedback;
    private Component focusComponent;

    private LinkedList<ButtonWrapper> buttons;
    private final ButtonWrapper ok;
    private final ButtonWrapper cancel;

    private IDialogService dialogService;
    private Panel container;
    private WicketAjaxIndicatorAppender indicator;

    protected boolean cancelled = false;

    public AbstractDialog() {
        this(null);
    }

    public AbstractDialog(IModel model) {
        super("form", model);

        setOutputMarkupId(true);

        container = new Container(IDialogService.DIALOG_WICKET_ID);
        container.add(this);

        feedback = new ExceptionFeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        buttons = new LinkedList<ButtonWrapper>();
        ListView buttonsView = new ListView("buttons", new Model(buttons)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                item.add(((ButtonWrapper) item.getModelObject()).getButton());
            }
        };
        buttonsView.setReuseItems(true);
        buttonsView.setOutputMarkupId(true);
        add(buttonsView);

        ok = new ButtonWrapper(new StringResourceModel("ok", AbstractDialog.this, null)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit() {
                handleSubmit();
            }

        };
        buttons.add(ok);

        cancel = new ButtonWrapper(new ResourceModel("cancel")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit() {
                cancelled = true;
                onCancel();
                closeDialog();
            }

            @Override
            protected Button decorate(Button b) {
                b.setDefaultFormProcessing(false);
                return super.decorate(b);
            }

        };
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

    protected final void closeDialog() {
        dialogService.close();
    }

    public void setNonAjaxSubmit() {
        ok.setAjax(false);
    }

    protected void setOkEnabled(boolean isset) {
        ok.setEnabled(isset);
    }

    protected void setOkVisible(boolean isset) {
        ok.setVisible(isset);
    }

    protected void setOkLabel(String label) {
        setOkLabel(new Model(label));
    }

    protected void setOkLabel(IModel label) {
        ok.setLabel(label);
    }

    protected void setCancelEnabled(boolean isset) {
        cancel.setEnabled(isset);
    }

    protected void setCancelVisible(boolean isset) {
        cancel.setVisible(isset);
    }

    protected void setCancelLabel(String label) {
        setCancelLabel(new Model(label));
    }

    protected void setCancelLabel(IModel label) {
        cancel.setLabel(label);
    }

    public void setDialogService(IDialogService dialogService) {
        this.dialogService = dialogService;
    }

    protected String getButtonId() {
        return "button";
    }

    protected void addButton(Button button) {
        if (getButtonId().equals(button.getId())) {
            buttons.addFirst(new ButtonWrapper(button));
        } else {
            log.error("Failed to add button: component id is not '{}'", getButtonId());
        }
    }

    protected void removeButton(Button button) {
        for (ButtonWrapper bw : buttons) {
            if (bw.getButton().equals(button)) {
                buttons.remove(bw);
                break;
            }
        }
    }

    protected void handleSubmit() {
        onOk();
        if (!hasError()) {
            closeDialog();
        }
    }

    @Override
    protected final void onSubmit() {
        Page page = (Page) findParent(Page.class);
        if (page != null) {
            IFormSubmittingComponent submitButton = findSubmittingButton();
            if (submitButton == null) {
                handleSubmit();
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

    public Component setFocusComponent(Component c) {
        this.focusComponent = c;
        return c;
    }

    public void render(PluginRequestTarget target) {
        if (target != null) {
            target.addComponent(feedback);
            for (ButtonWrapper bw : buttons) {
                if (bw.hasChanges()) {
                    target.addComponent(bw.getButton());
                }
            }

            if (focusComponent != null) {
                target.focusComponent(focusComponent);
                focusComponent = null;
            }
        }
    }

    public void onClose() {
    }

    public IValueMap getProperties() {
        return new ValueMap();
    }

}
