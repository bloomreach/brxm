/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceFactory;

/**
 * The dialog service factory wraps access to the dialog service to make sure that a dialog is
 * closed when the plugin that opened it is stopped.
 */
public class DialogServiceFactory implements IServiceFactory<IDialogService> {

    private static final long serialVersionUID = 1L;

    private IPluginContext context;
    private String serviceId;
    private DialogWindow rootService;

    public DialogServiceFactory(String wicketId) {
        rootService = new DialogWindow("dialog");
    }

    public void init(IPluginContext context, String serviceId) {
        this.context = context;
        this.serviceId = serviceId;
        context.registerService(this, serviceId);
    }

    public void destroy() {
        context.unregisterService(this, serviceId);
    }

    @Override
    public IDialogService getService(IPluginContext context) {
        return new DialogServiceWrapper();
    }

    @Override
    public Class<? extends IDialogService> getServiceClass() {
        return IDialogService.class;
    }

    @Override
    public void releaseService(IPluginContext context, IDialogService service) {
        ((DialogServiceWrapper) service).dispose();
    }

    public void render(PluginRequestTarget target) {
        rootService.render(target);
    }

    /**
     * Internal method to give the home page access to the dialog window.
     * @return
     */
    public Component getComponent() {
        return rootService;
    }

    /**
     * Dialog service wrapper specific to a plugin context.  This service intercepts
     * all calls from the plugin to the underlying (root) dialog service.
     */
    private class DialogServiceWrapper implements IDialogService {
        private static final long serialVersionUID = 1L;
        private int openDialogs = 0;

        private Set<DialogWrapper> dialogs = new HashSet<DialogWrapper>();

        @Override
        public void close() {
            rootService.close();
        }

        void onClose(final DialogWrapper wrapper) {
            dialogs.remove(wrapper);
            this.openDialogs--;
        }

        @Override
        public void render(final PluginRequestTarget target) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void show(final Dialog dialog) {
            final DialogWrapper wrapper = new DialogWrapper(this, dialog);
            dialogs.add(wrapper);
            rootService.show(wrapper);
            this.openDialogs++;
        }

        public void dispose() {
            for (final DialogWrapper wrapper : dialogs) {
                rootService.hide(wrapper);
                wrapper.dispose();
            }
            dialogs.clear();

            rootService.showPending();
        }

        public boolean isShowingDialog() {
            return openDialogs > 0;
        }
    }

    /**
     * Wrapper for dialogs.  The root service will only see these dialogs.
     */
    private class DialogWrapper implements Dialog {
        private static final long serialVersionUID = 1L;

        private Dialog dialog;
        private DialogServiceWrapper service;

        DialogWrapper(DialogServiceWrapper serviceWrapper, Dialog dialog) {
            this.service = serviceWrapper;
            this.dialog = dialog;
        }

        @Override
        public Component getComponent() {
            return dialog.getComponent();
        }

        @Override
        public IValueMap getProperties() {
            return dialog.getProperties();
        }

        @Override
        public IModel getTitle() {
            return dialog.getTitle();
        }

        @Override
        public void onClose() {
            dialog.onClose();
            service.onClose(this);
        }

        @Override
        public void render(PluginRequestTarget target) {
            dialog.render(target);
        }

        void dispose() {
            dialog = new Dialog() {
                @Override
                public void setDialogService(final IDialogService service) {
                }

                @Override
                public void render(final PluginRequestTarget target) {
                }

                @Override
                public Component getComponent() {
                    return new EmptyPanel("content");
                }

                @Override
                public IModel<String> getTitle() {
                    return Model.of("disposed");
                }

                @Override
                public void onClose() {
                }

                @Override
                public IValueMap getProperties() {
                    return new ValueMap();
                }
            };
        }

        @Override
        public void setDialogService(IDialogService dialogService) {
            if (dialogService == rootService) {
                dialog.setDialogService(this.service);
            } else {
                throw new IllegalStateException();
            }
        }

    }

}
