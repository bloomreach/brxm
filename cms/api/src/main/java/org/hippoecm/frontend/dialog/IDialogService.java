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

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.service.IRenderService;

/**
 * A service that displays modal dialogs.  An instance of this service is provided
 * under the name IDialogService.class.getName().
 */
public interface IDialogService extends IClusterable {

    final static String DIALOG_WICKET_ID = "content";

    /**
     * Interface that must be implemented by clients of the dialog service.
     */
    interface Dialog extends IClusterable {

        /**
         * When the dialog is shown ({@link IDialogService#show(Dialog)}), the
         * dialog service is injected into the dialog.
         */
        void setDialogService(IDialogService service);

        /**
         * Part of the pre-rendering registration.  When the dialog has components
         * that should be rendered, they can be registered with the target.
         * <p>
         * Implementations must invoke {@link IRenderService#render(PluginRequestTarget)}
         * on render services that contribute to the wicket component hierarchy.
         */
        void render(PluginRequestTarget target);

        /**
         * @return a component with wicket id "content"
         */
        Component getComponent();

        /**
         * The title of the dialog.  Will be displayed in the dialog border.
         */
        IModel getTitle();

        /**
         * Invoked when the dialog is closed.  Since the dialog will be decorated
         * with a close link (X), this method may be called even when the dialog
         * is not explicitly closed via {@link IDialogService#close()}.
         */
        void onClose();

        /**
         * Properties that determine how the dialog is rendered.  In particular,
         * the width and height properties are used for the size (in pixels).
         */
        IValueMap getProperties();

    }

    /**
     * Invoked during the pre-rendering registration.
     */
    void render(PluginRequestTarget target);

    /**
     * Show a dialog.  When a dialog is already shown, the dialog is enqueued to be
     * displayed when the current dialog is closed.
     */
    void show(Dialog dialog);

    /**
     * Close the current dialog.
     */
    void close();

}
