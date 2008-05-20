/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.dialog;

import java.lang.reflect.Constructor;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.hippoecm.frontend.sa.core.IPluginContext;
import org.hippoecm.frontend.sa.dialog.error.ErrorDialog;
import org.hippoecm.frontend.service.IDialogService;

public class DynamicDialogFactory implements PageCreator {
    private static final long serialVersionUID = 1L;

    private IPluginContext context; 
    private IDialogService window;
    private Class dialogClass;
    
    private IDialogFactory dialogFactory;

    public DynamicDialogFactory(IPluginContext context, IDialogService window, Class dialogClass) {
        this.context = context;
        this.window = window;
        this.dialogClass = dialogClass;
    }

    public DynamicDialogFactory(IDialogService window, IDialogFactory dialogFactory) {
        this.window = window;
        this.dialogFactory = dialogFactory;
    }

    public Page createPage() {
        AbstractDialog result = null;
        if (dialogFactory != null) {
            result = dialogFactory.createDialog(window);
        } else if (dialogClass != null) {
            try {
                Class[] formalArgs = new Class[] { IPluginContext.class, IDialogService.class };
                Constructor constructor = dialogClass.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] { context, window };
                result = (AbstractDialog) constructor.newInstance(actualArgs);
            } catch (Exception e) {
                String msg = e.getClass().getName() + ": " + e.getMessage();
                result = new ErrorDialog(context, window, msg);
            }
        } else {
            String msg = "No dialog renderer specified";
            result = new ErrorDialog(context, window, msg);
        }
        
        return result;
    }
    
}
