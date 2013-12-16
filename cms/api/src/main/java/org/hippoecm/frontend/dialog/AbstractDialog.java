/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.AjaxIndicatorAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.DefaultMarkupCacheKeyProvider;
import org.apache.wicket.markup.DefaultMarkupResourceStreamProvider;
import org.apache.wicket.markup.IMarkupCacheKeyProvider;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.MarkupException;
import org.apache.wicket.markup.MarkupFactory;
import org.apache.wicket.markup.MarkupNotFoundException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AjaxUpdatingWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wicket.contrib.input.events.key.KeyType;

/**
 * Utility class for implementing the {@link IDialogService.Dialog} interface. Provides OK and Cancel buttons by
 * default, and has support for fullscreen mode which is enabled by overriding {@code isFullscreenEnabled}.
 */
public abstract class AbstractDialog<T> extends Form<T> implements IDialogService.Dialog, IAjaxIndicatorAware {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractDialog.class);

    @Deprecated
    protected final static IValueMap SMALL = new ValueMap("width=380,height=250").makeImmutable();
    @Deprecated
    protected final static IValueMap MEDIUM = new ValueMap("width=475,height=375").makeImmutable();
    @Deprecated
    protected final static IValueMap LARGE = new ValueMap("width=855,height=450").makeImmutable();

    static private IMarkupCacheKeyProvider cacheKeyProvider = new DefaultMarkupCacheKeyProvider();
    static private IMarkupResourceStreamProvider streamProvider = new DefaultMarkupResourceStreamProvider();

    private boolean fullscreen = false;

    protected static class PersistentFeedbackMessagesModel extends FeedbackMessagesModel {
        private static final long serialVersionUID = 1L;
        private List<FeedbackMessage> messages;

        private PersistentFeedbackMessagesModel(Component component) {
            super(component);
        }

        protected void reset() {
            messages = null;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected List processMessages(final List messages) {
            if (this.messages == null) {
                this.messages = messages;
            }
            return this.messages;
        }

    }

    @SuppressWarnings("unchecked")
    private class Container extends Panel implements IMarkupCacheKeyProvider, IMarkupResourceStreamProvider {
        private static final long serialVersionUID = 1L;

        public Container(String id) {
            super(id);
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
        public Markup getAssociatedMarkup() {
            try {
                Markup markup = MarkupFactory.get().getMarkup(AbstractDialog.this, false);

                // If we found markup for this container
                if ((markup != null) && (markup != Markup.NO_MARKUP)) {
                    return markup;
                }

                return null;
            } catch (MarkupException ex) {
                // re-throw it. The exception contains already all the information
                // required.
                throw ex;
            } catch (MarkupNotFoundException ex) {
                // re-throw it. The exception contains already all the information
                // required.
                throw ex;
            } catch (WicketRuntimeException ex) {
                // throw exception since there is no associated markup
                throw new MarkupNotFoundException(
                        exceptionMessage("Markup of type '" + getMarkupType().getExtension() +
                                "' for component '" + getClass().getName() + "' not found." +
                                " Enable debug messages for org.apache.wicket.util.resource to get a list of all filenames tried"),
                        ex);
            }
        }
    }

    protected class ExceptionFeedbackPanel extends FeedbackPanel {
        private static final long serialVersionUID = 1L;

        protected ExceptionFeedbackPanel(String id) {
            super(id);
            setOutputMarkupId(true);
        }

        protected class ExceptionLabel extends Panel {
            private static final long serialVersionUID = 1L;

            protected ExceptionLabel(String id, IModel<String> model, final Exception ex, boolean escape) {
                super(id);
                setOutputMarkupId(true);

                Link<String> link = new Link<String>("message") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick() {
                        RequestCycle.get().scheduleRequestHandlerAfterCurrent(new ErrorDownloadRequestTarget(ex));
                    }
                };
                Label label = new Label("label", model);
                label.setEscapeModelStrings(escape);
                link.add(label);
                add(link);
            }
        }

        @Override
        protected Component newMessageDisplayComponent(String id, FeedbackMessage message) {
            Serializable serializable = message.getMessage();
            if (serializable instanceof Exception) {
                Exception ex = (Exception) message.getMessage();
                IModel<String> exceptionModel = getTranslatedException(ex);
                return new ExceptionLabel(id, exceptionModel, ex,
                        ExceptionFeedbackPanel.this.getEscapeModelStrings());
            } else {
                Label label = new Label(id);
                label.setDefaultModel(new Model<String>(serializable == null ? "" : serializable.toString()));
                label.setEscapeModelStrings(ExceptionFeedbackPanel.this.getEscapeModelStrings());
                return label;
            }
        }

        @Override
        protected FeedbackMessagesModel newFeedbackMessagesModel() {
            return AbstractDialog.this.getFeedbackMessagesModel();
        }
    }

    protected IModel<String> getTranslatedException(final Exception ex) {
        String key = "exception,type=${type},message=${message}";
        Map<String, String> details = new HashMap<String, String>();
        details.put("type", ex.getClass().getName());
        details.put("message", ex.getMessage());
        StackTraceElement[] elements = ex.getStackTrace();
        if (elements.length > 0) {
            StackTraceElement top = elements[0];
            details.put("clazz", top.getClassName());
            key += ",class=${clazz}";
        }
        return new StringResourceModel(key, AbstractDialog.this,
                new Model<Serializable>((Serializable) details), ex.getLocalizedMessage());

    }

    protected PersistentFeedbackMessagesModel fmm;
    protected FeedbackPanel feedback;
    private Component focusComponent;

    private LinkedList<ButtonWrapper> buttons;
    private final ButtonWrapper ok;
    private final ButtonWrapper cancel;

    private IDialogService dialogService;
    private Panel container;
    private AjaxIndicatorAppender indicator;

    protected boolean cancelled = false;
    private boolean isRemoved = false;

    public AbstractDialog() {
        this((IModel<T>) null);
    }

    public AbstractDialog(IModel<T> model) {
        super("form", model);

        container = new Container(IDialogService.DIALOG_WICKET_ID);
        container.add(this);

        feedback = newFeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        buttons = new LinkedList<ButtonWrapper>();
        ListView<ButtonWrapper> buttonsView = new ListView<ButtonWrapper>("buttons", buttons) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ButtonWrapper> item) {
                item.add(item.getModelObject().getButton());
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
        ok.setKeyType(KeyType.Enter);
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
            protected Button decorate(final Button button) {
                button.add(new AjaxEventBehavior("onclick") {

                    @Override
                    protected void onComponentTag(final ComponentTag tag) {
                        super.onComponentTag(tag);
                        tag.put("type", "button");
                    }

                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        onSubmit();
                    }
                });
                button.setDefaultFormProcessing(false);
                return super.decorate(button);
            }

        };
        cancel.setKeyType(KeyType.Escape);
        buttons.add(cancel);

        if (isFullscreenEnabled()) {
            final AjaxButton goFullscreen = new AjaxButton(DialogConstants.BUTTON,
                    new AbstractReadOnlyModel<String>() {
                        @Override
                        public String getObject() {
                            return getString(fullscreen ? "exit-fullscreen" : "fullscreen");
                        }
                    }) {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    target.appendJavaScript(getFullscreenScript());
                    target.add(this); //update button label
                    fullscreen = !fullscreen;
                }
            };
            addButton(goFullscreen);
        }

        add(indicator = new AjaxIndicatorAppender() {
            private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getIndicatorUrl() {
                return RequestCycle.get().urlFor(new ResourceReferenceRequestHandler(DialogConstants.AJAX_LOADER_GIF));
            }
        });

    }

    /**
     * Construct javascript that looks up the latest dialog and executes the toggleFullscreen function. Additional
     * javascript can be added by overriding <code>getAdditionalFullscreenScript</code>
     *
     * @return Javascript that toggles the dialog between fullscreen and initial size
     */
    protected String getFullscreenScript() {
        StringBuilder script = new StringBuilder();
        script.append("(function() {");
        script.append("var wDialog = Wicket.Window.get();");
        script.append("if (typeof wDialog !== 'undefined') {");
        script.append("    var fullscreen = wDialog.toggleFullscreen();");
        String additional = getAdditionalFullscreenScript(fullscreen);
        if (additional != null) {
            script.append(additional);
        }
        script.append("}");
        script.append("})();");
        return script.toString();
    }

    /**
     * Add custom javascript to be executed after the dialog has gone fullscreen or returned to it's initial size.
     *
     * @param isFullscreen flag indicating the current fullscreen state
     * @return Custom javascript that is executed after the dialog changed fullscreen state
     */
    protected String getAdditionalFullscreenScript(boolean isFullscreen) {
        return null;
    }

    /**
     * If this method returns true a fullscreen button will be added to the dialog which toggle's the between fullscreen
     * and initial size.
     *
     * @return true to enable fullscreen support
     */
    protected boolean isFullscreenEnabled() {
        return false;
    }

    @Override
    public UserSession getSession() {
        return UserSession.get();
    }

    @Override
    protected void onDetach() {
        if (fmm != null) {
            fmm.detach();
        }
        super.onDetach();
    }

    protected FeedbackPanel newFeedbackPanel(String id) {
        return new ExceptionFeedbackPanel(id);
    }

    protected final FeedbackMessagesModel getFeedbackMessagesModel() {
        if (fmm == null) {
            fmm = new PersistentFeedbackMessagesModel(this);
        }
        return fmm;
    }

    /**
     * Implement {@link IAjaxIndicatorAware}, to let ajax components in the dialog trigger the ajax indicator when they
     * trigger an ajax request.
     *
     * @return the markup id of the ajax indicator
     */
    public String getAjaxIndicatorMarkupId() {
        return indicator.getMarkupId();
    }

    protected final void closeDialog() {
        dialogService.close();
    }

    // button manipulation routines

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
        setOkLabel(new Model<String>(label));
    }

    protected void setOkLabel(IModel<String> label) {
        ok.setLabel(label);
    }

    protected void setFocusOnOk() {
        setFocus(ok.getButton());
    }

    protected void setCancelEnabled(boolean isset) {
        cancel.setEnabled(isset);
    }

    protected void setCancelVisible(boolean isset) {
        cancel.setVisible(isset);
    }

    protected void setCancelLabel(String label) {
        setCancelLabel(new Model<String>(label));
    }

    protected void setCancelLabel(IModel<String> label) {
        cancel.setLabel(label);
    }

    protected void setFocusOnCancel() {
        setFocus(cancel.getButton());
    }

    protected void setOkKeyType(KeyType keyType) {
        ok.setKeyType(keyType);
    }

    protected void setCancelKeyType(KeyType keyType) {
        cancel.setKeyType(keyType);
    }

    /**
     * {@inheritDoc}
     */
    public void setDialogService(IDialogService dialogService) {
        this.dialogService = dialogService;
    }

    @Deprecated
    protected String getButtonId() {
        log.warn("getButtonId is deprecated, use DialogConstants.BUTTON instead");
        return DialogConstants.BUTTON;
    }

    /**
     * Add a {@link Button} to the button bar.  The id of the button must equal "button".
     */
    protected void addButton(Button button) {
        if (DialogConstants.BUTTON.equals(button.getId())) {
            buttons.addFirst(new ButtonWrapper(button));
        } else {
            log.error("Failed to add button: component id is not '{}'", DialogConstants.BUTTON);
        }
    }

    /**
     * Remove a button from the button bar.
     */
    protected void removeButton(Button button) {
        for (ButtonWrapper bw : buttons) {
            if (bw.getButton().equals(button)) {
                buttons.remove(bw);
                break;
            }
        }
    }

    /**
     * Method that handles the submit to a form.
     */
    protected void handleSubmit() {
        onOk();
        if (!hasError()) {
            closeDialog();
        }
    }

    @Override
    protected void delegateSubmit(final IFormSubmitter submittingComponent) {
        super.delegateSubmit(submittingComponent);

        if (submittingComponent == null) {
            handleSubmit();
        }
    }

    @Override
    protected final void onSubmit() {
        tryResetFeedbackModel();
    }

    @Override
    protected final void onError() {
        tryResetFeedbackModel();
    }

    private void tryResetFeedbackModel() {
        if (!isRemoved && fmm != null) {
            fmm.reset();
        }
    }

    /**
     * Callback method invoked when the user clicks the 'OK' button. When no errors are reported, this will cause the
     * dialog to be closed.
     */
    protected void onOk() {
    }

    /**
     * Callback method invoked when the user clicks the 'Cancel' button.
     */
    protected void onCancel() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getComponent() {
        return container;
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        this.isRemoved = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PluginRequestTarget target) {
        if (target != null) {
            if (!isRemoved) {
                target.add(feedback);
                for (ButtonWrapper bw : buttons) {
                    if (bw.hasChanges()) {
                        target.add(bw.getButton());
                    }
                }
            }

            if (focusComponent != null) {
                target.focusComponent(focusComponent);
                focusComponent = null;
            }
        }
    }

    /**
     * Implement onClose callback, invoked when the dialog is closed.  Make sure the keyboard shortcuts are cleaned up
     * correctly.  Subclasses overriding this method should also invoke super#onClose();
     */
    @Override
    public void onClose() {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            for (ButtonWrapper bw : buttons) {
                if (bw.getKeyType() != null) {
                    // the input behavior does not support removal, so we need to do this manually
                    target.prependJavaScript("shortcut.remove('" + bw.getKeyType() + "');\n");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IValueMap getProperties() {
        return DialogConstants.LARGE;
    }

    public Component setFocus(Component c) {
        if (focusComponent != null) {
            return c;
        }

        if (!c.getOutputMarkupId()) {
            c.setOutputMarkupId(true);
        }
        return focusComponent = c;
    }

    public AjaxUpdatingWidget<?> setFocus(AjaxUpdatingWidget<?> widget) {
        setFocus(widget.getFocusComponent());
        return widget;
    }

}
