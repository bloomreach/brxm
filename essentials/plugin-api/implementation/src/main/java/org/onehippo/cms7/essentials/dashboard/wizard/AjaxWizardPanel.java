package org.onehippo.cms7.essentials.dashboard.wizard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardModelListener;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.onehippo.cms7.essentials.dashboard.ui.EssentialsFeedbackPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class AjaxWizardPanel extends Panel implements IWizardModelListener, IWizard {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(AjaxWizardPanel.class);

    final Form<?> form;
    final WizardModel wizardModel;
    final FeedbackPanel feedbackPanel;
    final AjaxButton next;
    final AjaxButton prev;

    public AjaxWizardPanel(final String id) {
        super(id);

        feedbackPanel = new EssentialsFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        wizardModel = new WizardModel();

        form = new Form("form");

        final IModel<String> nextorfinish = getNextButtonLabel();
        final Label nextorfinishLabel = new Label("nextorfinish", nextorfinish);
        nextorfinishLabel.setOutputMarkupId(true);

        prev = new AjaxButton("prev") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final boolean previousAvailable = wizardModel.isPreviousAvailable();
                final EssentialsWizardStep activeStep1 = (EssentialsWizardStep) wizardModel.getActiveStep();
                activeStep1.setProcessed(false);
                if (previousAvailable) {
                    wizardModel.previous();
                    onActiveStepChanged(wizardModel.getActiveStep());
                    final EssentialsWizardStep activeStep = (EssentialsWizardStep) wizardModel.getActiveStep();
                    activeStep.setProcessed(false);
                    next.replace(new Label("nextorfinish", getNextButtonLabel()));
                    next.setEnabled(true);
                }
                setEnabled(wizardModel.isPreviousAvailable());
                target.add(form);
                target.add(feedbackPanel);
            }
        };

        final Label previousLabel = new Label("previouslabel", getPreviousButtonLabel());
        prev.add(previousLabel);

        next = new AjaxButton("next") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final boolean nextAvailable = wizardModel.isNextAvailable();
                final EssentialsWizardStep activeStep = (EssentialsWizardStep) wizardModel.getActiveStep();
                activeStep.setProcessed(true);
                if (nextAvailable) {
                    wizardModel.next();
                    onActiveStepChanged(wizardModel.getActiveStep());
                    if (!wizardModel.isNextAvailable()) {
                        replace(new Label("nextorfinish", getFinishButtonLabel()));
                    }
                } else {
                    setEnabled(false);
                    onFinish(target);
                }
                prev.setEnabled(wizardModel.isPreviousAvailable());
                target.add(form);
                target.add(feedbackPanel);
            }
        };

        next.add(nextorfinishLabel);

        form.add(prev);
        form.add(next);

        prev.setEnabled(wizardModel.isPreviousAvailable());

        add(form);
    }

    private void onFinish(final AjaxRequestTarget target) {
        onFinish();
    }

    @Override
    public IWizardModel getWizardModel() {
        return wizardModel;
    }

    @Override
    public void onActiveStepChanged(final IWizardStep newStep) {
        form.replace(newStep.getView("view", this, this));
    }

    @Override
    public void onCancel() {
    }

    @Override
    public void onFinish() {
        info("Form Finished");
    }

    public IModel<String> getNextButtonLabel() {
        return new StringResourceModel("next", this, null);
    }

    public IModel<String> getPreviousButtonLabel() {
        return new StringResourceModel("previous", this, null);
    }

    public IModel<String> getFinishButtonLabel() {
        return new StringResourceModel("finish", this, null);
    }

    private static class Overview extends RefreshingView<IWizardStep> {

        private static final long serialVersionUID = 1L;
        private final List<IModel<IWizardStep>> list = new ArrayList<>();
        private final WizardModel model;

        private Overview(final String id, final WizardModel model) {
            super(id);
            setOutputMarkupId(true);
            final Iterator<IWizardStep> iWizardStepIterator = model.stepIterator();
            while (iWizardStepIterator.hasNext()) {
                list.add(new Model<>(iWizardStepIterator.next()));
            }
            this.model = model;
        }


        @Override
        protected Iterator<IModel<IWizardStep>> getItemModels() {
            return list.iterator();
        }

        @Override
        protected void populateItem(final Item<IWizardStep> item) {
            final EssentialsWizardStep wizardStep = (EssentialsWizardStep) item.getModelObject();
            final int index = item.getIndex() + 1;
            final boolean processes = wizardStep.isProcessed();
            final boolean active = model.getActiveStep().equals(wizardStep);
            item.add(new AttributeModifier("class", active ? "active" : ""));
            item.add(new AttributeAppender("class", processes ? " complete" : ""));
            item.add(new AttributeModifier("data-target", String.format("#step%s", index)));
            item.add(new Label("no", index));
            item.add(new Label("title", wizardStep.getTitle()));
            final String s = String.valueOf(100 / list.size());
            item.add(new AttributeModifier("style", String.format("min-width: %s%%; max-width: %s%%;", s, s)));
        }


    }

    public void addWizard(IWizardStep step) {
        wizardModel.add(step);
    }

    public void initSteps() {
        wizardModel.reset();
        final Component view = wizardModel.getActiveStep().getView("view", this, this);
        form.addOrReplace(view);
        final Overview overview = new Overview("overview", wizardModel);
        form.add(overview);
    }
}
