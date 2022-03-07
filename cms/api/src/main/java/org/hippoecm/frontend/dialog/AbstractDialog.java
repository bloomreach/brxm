/*
 *  Copyright 2008-2022 Bloomreach (https://www.bloomreach.com)
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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.AjaxIndicatorAppender;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.buttons.ButtonStyle;
import org.hippoecm.frontend.buttons.ButtonType;
import org.hippoecm.frontend.form.PostOnlyForm;
import org.hippoecm.frontend.i18n.TranslatorUtils;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AjaxUpdatingWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wicket.contrib.input.events.key.KeyType;

/**
 * Utility class for implementing the {@link IDialogService.Dialog} interface. Provides OK and Cancel buttons by
 * default, and has support for fullscreen mode which is enabled by overriding {@code isFullscreenEnabled}.
 */
public abstract class AbstractDialog<T> extends PostOnlyForm<T> implements IDialogService.Dialog, IAjaxIndicatorAware {

    private static final Logger log = LoggerFactory.getLogger(AbstractDialog.class);

    private static final IMarkupCacheKeyProvider cacheKeyProvider = new DefaultMarkupCacheKeyProvider();
    private static final IMarkupResourceStreamProvider streamProvider = new DefaultMarkupResourceStreamProvider();

    private boolean fullscreen = false;
    private String buttonCssClass;
    private AjaxChannel ajaxChannel;
    private boolean handleSubmitEnabled = true;

    public enum ButtonPosition {
        FIRST,
        LAST
    }

    protected static class PersistentFeedbackMessagesModel extends FeedbackMessagesModel {

        private List<FeedbackMessage> messages;

        private PersistentFeedbackMessagesModel(final Component component) {
            super(component);
        }

        protected void reset() {
            messages = null;
        }

        @Override
        protected List<FeedbackMessage> processMessages(final List<FeedbackMessage> messages) {
            if (this.messages == null) {
                this.messages = messages;
            }
            return this.messages;
        }
    }

    private class Container extends Panel implements IMarkupCacheKeyProvider, IMarkupResourceStreamProvider {

        public Container(final String id) {
            super(id);
        }

        @Override
        public String getCacheKey(final MarkupContainer container, final Class containerClass) {
            return cacheKeyProvider.getCacheKey(AbstractDialog.this, AbstractDialog.this.getClass());
        }

        // implement IMarkupResourceStreamProvider.
        @Override
        public IResourceStream getMarkupResourceStream(final MarkupContainer container, final Class containerClass) {
            return streamProvider.getMarkupResourceStream(AbstractDialog.this, AbstractDialog.this.getClass());
        }

        // used for markup inheritance (<wicket:extend />)
        @Override
        public Markup getAssociatedMarkup() {
            try {
                final Markup markup = MarkupFactory.get().getMarkup(AbstractDialog.this, false);

                // If we found markup for this container
                if (markup != null && markup != Markup.NO_MARKUP) {
                    return markup;
                }

                return null;
            } catch (final MarkupException | MarkupNotFoundException ex) {
                // Re-throw it. The exception already contains all the information required.
                throw ex;
            } catch (final WicketRuntimeException ex) {
                // throw exception since there is no associated markup
                throw new MarkupNotFoundException(
                        exceptionMessage("Markup of type '" + getMarkupType().getExtension() + "' for component '" +
                                getClass().getName() + "' not found. Enable debug messages for " +
                                "org.apache.wicket.util.resource to get a list of all filenames tried"),
                        ex);
            }
        }
    }

    protected class ExceptionFeedbackPanel extends FeedbackPanel {

        /**
         * Create a feedback panel and display only messages of children components.
         */
        protected ExceptionFeedbackPanel(final String id) {
            this(id, new ContainerFeedbackMessageFilter(AbstractDialog.this));
        }

        /**
         * Create a feedback panel and apply a <code>filter</code> so that only messages accepted by the filter are
         * visible.
         */
        protected ExceptionFeedbackPanel(final String id, final IFeedbackMessageFilter filter) {
            super(id, filter);
            setOutputMarkupId(true);

            add(ClassAttribute.append(() -> hasFeedbackMessage() ? "feedback-enabled" : "feedback-disabled"));
        }

        protected class ExceptionLabel extends Panel {

            protected ExceptionLabel(final String id, final IModel<String> model, final Exception ex,
                                     final boolean escape) {
                super(id);
                setOutputMarkupId(true);

                final Link<String> link = new Link<>("message") {
                    @Override
                    public void onClick() {
                        RequestCycle.get().scheduleRequestHandlerAfterCurrent(new ErrorDownloadRequestTarget(ex));
                    }
                };
                final Label label = new Label("label", model);
                label.setEscapeModelStrings(escape);
                link.add(label);
                add(link);
            }
        }

        @Override
        protected Component newMessageDisplayComponent(final String id, final FeedbackMessage message) {
            final Serializable serializable = message.getMessage();
            if (serializable instanceof Exception) {
                final Exception ex = (Exception) message.getMessage();
                final IModel<String> exceptionModel = getExceptionTranslation(ex);
                return new ExceptionLabel(id, exceptionModel, ex,
                        ExceptionFeedbackPanel.this.getEscapeModelStrings());
            } else {
                final Label label = new Label(id);
                label.setDefaultModel(new Model<>(serializable == null ? "" : serializable.toString()));
                label.setEscapeModelStrings(ExceptionFeedbackPanel.this.getEscapeModelStrings());
                return label;
            }
        }

        @Override
        protected FeedbackMessagesModel newFeedbackMessagesModel() {
            return AbstractDialog.this.getFeedbackMessagesModel();
        }
    }

    protected IModel<String> getExceptionTranslation(final Throwable t, final Object... parameters) {
        return TranslatorUtils.getExceptionTranslation(AbstractDialog.this, t, parameters);
    }

    protected PersistentFeedbackMessagesModel feedbackMessages;
    protected FeedbackPanel feedback;
    private Component focusComponent;

    private final LinkedList<ButtonWrapper> buttons;
    private final ButtonWrapper ok;
    private final ButtonWrapper cancel;
    private final Panel container;

    private IDialogService dialogService;
    private AjaxIndicatorAppender indicator;

    private boolean isRemoved = false;
    protected boolean cancelled = false;

    public AbstractDialog() {
        this(null);
    }

    public AbstractDialog(final IModel<T> model) {
        super("form", model);

        container = new Container(IDialogService.DIALOG_WICKET_ID);
        container.add(this);

        feedback = newFeedbackPanel("feedback");
        final IFeedbackMessageFilter filter = feedback.getFilter();

        if (filter == null) {
            // make sure the feedback filters out messages unrelated to this dialog
            feedback.setFilter(new ContainerFeedbackMessageFilter(this));
        } else if (!(filter instanceof ContainerFeedbackMessageFilter)) {
            log.warn("The dialog '{}' uses a feedback panel with a filter that does not extend " +
                    "ContainerFeedbackMessageFilter. As a result, this dialog may show unrelated feedback messages.",
                    getClass());
        }

        feedback.setOutputMarkupId(true);
        add(feedback);

        buttons = new LinkedList<>();
        final ListView<ButtonWrapper> buttonsView = new ListView<>("buttons", buttons) {
            @Override
            protected void populateItem(final ListItem<ButtonWrapper> item) {
                final Button button = item.getModelObject().getButton();
                if (StringUtils.isNotEmpty(buttonCssClass)) {
                    button.add(ClassAttribute.append(buttonCssClass));
                }
                item.add(button);
            }
        };
        buttonsView.setReuseItems(true);
        buttonsView.setOutputMarkupId(true);
        add(buttonsView);

        ok = new ButtonWrapper(new ResourceModel("ok")) {

            @Override
            protected Button decorate(final Button button) {
                button.add(ClassAttribute.append("qa-dialog-btn-ok"));
                return super.decorate(button);
            }

            @Override
            protected void onSubmit() {
                if (handleSubmitEnabled){
                    handleSubmit();
                }
            }

            @Override
            protected void onUpdateAjaxAttributes(final AjaxRequestAttributes attributes) {
                attributes.setChannel(ajaxChannel);
            }
        };
        ok.setKeyType(KeyType.Enter);
        ok.setStyle(ButtonStyle.PRIMARY);
        ok.setType(ButtonType.SUBMIT);

        cancel = new ButtonWrapper(new ResourceModel("cancel")) {

            @Override
            protected Button decorate(final Button button) {
                button.setDefaultFormProcessing(false);
                button.add(ClassAttribute.append("qa-dialog-btn-cancel"));
                return super.decorate(button);
            }

            @Override
            protected void onSubmit() {
                cancelled = true;
                onCancel();
                closeDialog();
            }
        };
        cancel.setKeyType(KeyType.Escape);

        buttons.add(cancel);
        buttons.add(ok);

        if (isFullscreenEnabled()) {
            final AjaxButton goFullscreen = new AjaxButton(
                    DialogConstants.BUTTON,
                    () -> getString(fullscreen ? "exit-fullscreen" : "fullscreen")
            ) {
                @Override
                protected void onSubmit(final AjaxRequestTarget target) {
                    target.appendJavaScript(getFullscreenScript());
                    target.add(this); //update button label
                    fullscreen = !fullscreen;
                }
            };
            goFullscreen.add(ClassAttribute.append(ButtonStyle.SECONDARY.getCssClass()));
            addButton(goFullscreen);
        }

        if (addAjaxIndicator()) {
            add(indicator = new AjaxIndicatorAppender() {
                @Override
                protected CharSequence getIndicatorUrl() {
                    return RequestCycle.get()
                            .urlFor(new ResourceReferenceRequestHandler(DialogConstants.AJAX_LOADER_GIF));
                }
            });
        }
    }

    @Override
    public void onEvent(IEvent<?> event) {
        if (event.getPayload() instanceof HandleSubmitEnabledPayload){
            HandleSubmitEnabledPayload payload = (HandleSubmitEnabledPayload) event.getPayload();
            handleSubmitEnabled = payload.isHandleSubmitEnabled();
        }
    }

    protected boolean addAjaxIndicator() {
        return true;
    }

    /**
     * Construct javascript that looks up the latest dialog and executes the toggleFullscreen function. Additional
     * javascript can be added by overriding <code>getAdditionalFullscreenScript</code>
     *
     * @return Javascript that toggles the dialog between fullscreen and initial size
     */
    protected String getFullscreenScript() {
        final StringBuilder script = new StringBuilder();
        script.append("(function() {");
        script.append("var wDialog = Wicket.Window.get();");
        script.append("if (typeof wDialog !== 'undefined') {");
        script.append("    var fullscreen = wDialog.toggleFullscreen();");
        final String additional = getAdditionalFullscreenScript(fullscreen);
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
    @SuppressWarnings("unused")
    protected String getAdditionalFullscreenScript(final boolean isFullscreen) {
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
        if (feedbackMessages != null) {
            feedbackMessages.detach();
        }
        super.onDetach();
    }

    /**
     * Create a feedback panel and display only messages from children components.
     */
    protected FeedbackPanel newFeedbackPanel(final String id) {
        return new ExceptionFeedbackPanel(id);
    }

    protected final FeedbackMessagesModel getFeedbackMessagesModel() {
        if (feedbackMessages == null) {
            feedbackMessages = new PersistentFeedbackMessagesModel(this);
        }
        return feedbackMessages;
    }

    /**
     * Implement {@link IAjaxIndicatorAware}, to let ajax components in the dialog trigger the ajax indicator when they
     * trigger an ajax request.
     *
     * @return the markup id of the ajax indicator
     */
    @Override
    public String getAjaxIndicatorMarkupId() {
        if (indicator != null) {
            return indicator.getMarkupId();
        }
        return null;
    }

    protected final void closeDialog() {
        dialogService.close();
    }

    // button manipulation routines
    public void setNonAjaxSubmit() {
        ok.setAjax(false);
    }

    public void setAjaxChannel(final AjaxChannel ajaxChannel) {
        this.ajaxChannel = ajaxChannel;
    }

    protected void setOkEnabled(final boolean isset) {
        ok.setEnabled(isset);
    }

    protected void setOkVisible(final boolean isset) {
        ok.setVisible(isset);
    }

    protected void setOkLabel(final String label) {
        setOkLabel(Model.of(label));
    }

    protected void setOkLabel(final IModel<String> label) {
        ok.setLabel(label);
    }

    protected void setFocusOnOk() {
        setFocus(ok.getButton());
    }

    protected void setCancelEnabled(final boolean isset) {
        cancel.setEnabled(isset);
    }

    protected void setCancelVisible(final boolean isset) {
        cancel.setVisible(isset);
    }

    protected void setCancelLabel(final String label) {
        setCancelLabel(Model.of(label));
    }

    protected void setCancelLabel(final IModel<String> label) {
        cancel.setLabel(label);
    }

    protected void setFocusOnCancel() {
        setFocus(cancel.getButton());
    }

    protected void setOkKeyType(final KeyType keyType) {
        ok.setKeyType(keyType);
    }

    protected void setCancelKeyType(final KeyType keyType) {
        cancel.setKeyType(keyType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDialogService(final IDialogService dialogService) {
        this.dialogService = dialogService;
    }

    /**
     * Add a {@link Button} to the button bar. The id of the button must equal "button". The button will be added to the
     * left of the default buttons.
     */
    protected void addButton(final Button button) {
        addButton(button, ButtonPosition.FIRST);
    }

    /**
     * Add a {@link Button} to the button bar. The id of the button must equal "button".
     *
     * @param button         the button to add
     * @param buttonPosition to position the button left or right of the default buttons
     */
    protected void addButton(final Button button, final ButtonPosition buttonPosition) {
        if (DialogConstants.BUTTON.equals(button.getId())) {
            switch (buttonPosition) {
                case FIRST:
                    buttons.addFirst(new ButtonWrapper(button));
                    break;
                case LAST:
                    buttons.addLast(new ButtonWrapper(button));
            }
        } else {
            log.error("Failed to add button: component id is not '{}'", DialogConstants.BUTTON);
        }
    }

    /**
     * Remove a button from the button bar.
     */
    protected void removeButton(final Button button) {
        for (final ButtonWrapper bw : buttons) {
            if (bw.getButton().equals(button)) {
                buttons.remove(bw);
                break;
            }
        }
    }

    /**
     * Set a specific class that is added to all the buttons in the dialog
     */
    protected void setButtonCssClass(final String buttonCssClass) {
        this.buttonCssClass = buttonCssClass;
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
        if (!isRemoved && feedbackMessages != null) {
            feedbackMessages.reset();
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

    @Override
    public void onCancelFromCloseButton() {
        cancelled = true;
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
    public void render(final PluginRequestTarget target) {
        if (target != null) {
            if (!isRemoved) {
                target.add(feedback);
                for (final ButtonWrapper bw : buttons) {
                    if (bw.hasChanges()) {
                        target.add(bw.getButton());
                        bw.rendered();
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
        final Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        target.ifPresent(ajaxRequestTarget -> {
            for (final ButtonWrapper bw : buttons) {
                if (bw.getKeyType() != null) {
                    // the input behavior does not support removal, so we need to do this manually
                    final String script = String.format("if (window['shortcut']) { shortcut.remove('%s'); }\n", bw.getKeyType());
                    ajaxRequestTarget.prependJavaScript(script);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IValueMap getProperties() {
        return DialogConstants.LARGE;
    }

    public Component setFocus(final Component c) {
        if (focusComponent != null) {
            return c;
        }

        if (!c.getOutputMarkupId()) {
            c.setOutputMarkupId(true);
        }
        return focusComponent = c;
    }

    public AjaxUpdatingWidget<?> setFocus(final AjaxUpdatingWidget<?> widget) {
        setFocus(widget.getFocusComponent());
        return widget;
    }

    @Override
    public void process(final IFormSubmitter submittingComponent) {
        /*
         * Manually clear old feedback generated by CMS validation prior processing because
         * {@link org.hippoecm.frontend.plugins.cms.root.RootPlugin#RootPlugin(IPluginContext, IPluginConfig)}
         * configures to keep all feedback messages after each request cycle.
         */
        if (hasFeedbackMessage()) {
            getFeedbackMessages().clear();
        }

        super.process(submittingComponent);
    }
}
