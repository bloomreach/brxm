/*
 *  Copyright 2012 Hippo.
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

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Simple {@link AbstractDialog} extension that adds wizard-like capability.
 */
public abstract class MultiStepDialog extends AbstractDialog<Node> {

    private int currentStep = 0;

    public MultiStepDialog() {
        this(null);
    }

    public MultiStepDialog(IModel<Node> model) {
        super(model);
        updateLabels();
    }

    private void updateLabels() {
        setOkLabel(getSteps()[currentStep].getOkLabel());
        setCancelLabel(getSteps()[currentStep].getCancelLabel());
        final String info = getSteps()[currentStep].getInfo();
        if (info != null) {
            info(info);
        }
    }

    @Override
    protected void handleSubmit() {
        if (getSteps().length > currentStep) {
            int result = getSteps()[currentStep].execute();
            currentStep += result;
            if (result != 0) {
                updateLabels();
            }
        } else {
            closeDialog();
        }
    }

    /**
     * @return  the array of steps this dialog implements
     */
    protected abstract Step[] getSteps();

    protected abstract class Step {

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
