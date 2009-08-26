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

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.PluginRequestTarget;

public interface IDialogService extends IClusterable {
    final static String SVN_ID = "$Id$";

    final static String DIALOG_WICKET_ID = "content";

    interface Dialog extends IClusterable {

        void setDialogService(IDialogService service);

        void render(PluginRequestTarget target);

        /**
         * @return a component with wicket id "content"
         */
        Component getComponent();

        IModel getTitle();

        void onClose();
        
        IValueMap getProperties();
    }

    void render(PluginRequestTarget target);

    void show(Dialog dialog);

    void close();
}
