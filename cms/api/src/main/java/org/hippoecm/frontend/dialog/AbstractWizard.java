/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.extensions.ajax.markup.html.AjaxIndicatorAppender;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugins.standards.wizard.AjaxWizard;
import org.hippoecm.frontend.widgets.AjaxUpdatingWidget;

public class AbstractWizard<T> extends AjaxWizard implements IDialogService.Dialog, IAjaxIndicatorAware {

    private IDialogService dialogService;
    private Component focusComponent;
    private AjaxIndicatorAppender indicator;

    public AbstractWizard() {
        super("content");

        setOutputMarkupId(true);

        if (addAjaxIndicator()) {
            add(indicator = new AjaxIndicatorAppender() {
                @Override
                protected CharSequence getIndicatorUrl() {
                    return RequestCycle.get().urlFor(new ResourceReferenceRequestHandler(DialogConstants.AJAX_LOADER_GIF));
                }
            });
        }
    }

    public AbstractWizard(IModel<T> model) {
        this();

        setDefaultModel(model);
    }

    @Override
    protected void init(final IWizardModel wizardModel) {
        super.init(wizardModel);

        getForm().get(FEEDBACK_ID).setOutputMarkupId(true);
    }

    /**
     * Gets model
     *
     * @return model
     */
    @SuppressWarnings("unchecked")
    public final IModel<T> getModel() {
        return (IModel<T>) getDefaultModel();
    }

    /**
     * Sets model
     *
     * @param model
     */
    public final void setModel(IModel<T> model) {
        setDefaultModel(model);
    }

    /**
     * Gets model object
     *
     * @return model object
     */
    @SuppressWarnings("unchecked")
    public final T getModelObject() {
        return (T) getDefaultModelObject();
    }

    /**
     * Sets model object
     *
     * @param object
     */
    public final void setModelObject(T object) {
        setDefaultModelObject(object);
    }

    @Override
    public void onActiveStepChanged(final IWizardStep newStep) {
        super.onActiveStepChanged(newStep);
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.add(this);
        }
    }

    @Override
    public void onCancel() {
        dialogService.close();
    }

    @Override
    public void onFinish() {
        if (!hasError()) {
            dialogService.close();
        }
    }

    protected final boolean hasError() {
        FeedbackPanel feedback = (FeedbackPanel) getForm().get(FEEDBACK_ID);
        return feedback.anyErrorMessage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IValueMap getProperties() {
        return DialogConstants.LARGE;
    }

    public Component getComponent() {
        return this;
    }

    @Override
    public IModel<String> getTitle() {
        return null;
    }

    public void onClose() {
    }

    public void render(PluginRequestTarget target) {
        if (target != null) {
            target.add(getForm().get(FEEDBACK_ID));
            if (focusComponent != null) {
                target.focusComponent(focusComponent);
                focusComponent = null;
            }
        }
    }

    /**
     * Implement {@link org.apache.wicket.ajax.IAjaxIndicatorAware}, to let ajax components in the dialog trigger the
     * ajax indicator when they trigger an ajax request.
     *
     * @return the markup id of the ajax indicator
     */
    public String getAjaxIndicatorMarkupId() {
        if (indicator != null) {
            return indicator.getMarkupId();
        }
        return null;
    }

    protected boolean addAjaxIndicator() {
        return true;
    }

    public void setDialogService(IDialogService service) {
        dialogService = service;
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
