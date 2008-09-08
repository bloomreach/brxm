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
package org.hippoecm.frontend.plugins.gallery.upload;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.markup.html.panel.Panel;

class ButtonBar extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";
    private static final long serialVersionUID = 1L;

    public ButtonBar(String id, UploadWizard wizard) {
        super(id);
        add(new AgainLink("again", wizard));
        add(new CancelLink("cancel", wizard));
        add(new FinishLink("finish", wizard));
    }

    private class CancelLink extends AbstractWizardLink {
        @SuppressWarnings("unused")
        private final static String SVN_ID = "$Id: CancelLink.java 12815 2008-08-01 10:03:35Z wgrevink $";
        private static final long serialVersionUID = 1L;

        public CancelLink(String id, UploadWizard wizard) {
            super(id, wizard);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            wizard.getWizardModel().cancel();
        }

        @Override
        public boolean isVisible() {
            return wizard.getWizardModel().isNextAvailable();
        }
    }

    private class AgainLink extends AbstractWizardLink {
        private static final long serialVersionUID = 1L;

        public AgainLink(String id, UploadWizard wizard) {
            super(id, wizard);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            wizard.getWizardModel().previous();
            target.addComponent(wizard);
        }

        @Override
        public final boolean isVisible() {
            return !wizard.getWizardModel().isNextAvailable();
        }

    }

    private class FinishLink extends AbstractWizardLink {
        private static final long serialVersionUID = 1L;

        public FinishLink(String id, UploadWizard wizard) {
            super(id, wizard);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            wizard.getWizardModel().finish();
        }

        @Override
        public boolean isVisible() {
            return !wizard.getWizardModel().isNextAvailable();
        }

    }

    private abstract class AbstractWizardLink extends AjaxLink {
        private static final long serialVersionUID = 1L;

        protected final Wizard wizard;

        public AbstractWizardLink(String id, Wizard wizard) {
            super(id);
            setOutputMarkupId(true);
            this.wizard = wizard;
        }

    }

}