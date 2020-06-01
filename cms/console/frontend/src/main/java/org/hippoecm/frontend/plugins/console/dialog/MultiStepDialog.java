/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.dialog;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;

/**
 * Simple {@link org.hippoecm.frontend.dialog.AbstractDialog} extension that adds multi-step capability. If you need wizard functionality
 * use {@link org.hippoecm.frontend.plugins.standards.wizard.AjaxWizard} instead.
 */
public abstract class MultiStepDialog<T> extends AbstractDialog<T> {

    private int currentStep = 0;

    public MultiStepDialog() {
        this(null);
    }

    public MultiStepDialog(IModel<T> model) {
        super(model);
        updateLabels();
    }

    private void updateLabels() {
        final List<Step> steps = getSteps();
        if (steps.size() > currentStep) {
            final Step step = steps.get(currentStep);
            setOkLabel(step.getOkLabel());
            setCancelLabel(step.getCancelLabel());
            final String info = step.getInfo();
            if (info != null) {
                info(info);
            }
        }
    }

    @Override
    protected void handleSubmit() {
        final List<Step> steps = getSteps();
        if (steps.size() > currentStep) {
            int result = steps.get(currentStep).execute();
            currentStep += result;
            if (result != 0) {
                updateLabels();
            }
        } else {
            closeDialog();
        }
    }

    protected abstract List<Step> getSteps();

    protected abstract class Step implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * @return the number of steps to go forward (or backward in the case of a negative integer)
         */
        protected abstract int execute();

        protected IModel<String> getOkLabel() {
            return new Model<String>("OK");
        }

        protected IModel<String> getCancelLabel() {
            return new Model<String>("Cancel");
        }

        public String getInfo() {
            return null;
        }
    }

    public class DoneStep extends Step {

        private static final long serialVersionUID = 1L;

        @Override
        public int execute() {
            closeDialog();
            return 0;
        }

        @Override
        protected IModel<String> getOkLabel() {
            return new Model<String>("Done");
        }
    }

}
