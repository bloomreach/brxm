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
package org.hippoecm.frontend.dialog;

import java.lang.reflect.Constructor;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.hippoecm.frontend.dialog.error.ErrorDialog;

/**
 * Class for on-the-fly page creation based on a dynamic className attribute.
 */
public class DynamicDialogFactory implements PageCreator {
    private static final long serialVersionUID = 1L;

    public static final String ERROR = "org.hippoecm.frontend.dialog.error.ErrorDialog";

    private DialogWindow window;
    private String className;

    public DynamicDialogFactory(DialogWindow window, String className) {
        this.window = window;
        this.className = className;
    }
    
    
    public Page createPage() {
        Page result = null;
        if (className == null || ERROR.equals(className)) {
            String msg = "No dialog renderer found";
            result = new ErrorDialog(window, msg);
        } else {
            try {
                Class clazz = Class.forName(className);
                Class[] formalArgs = new Class[] { window.getClass() };
                Constructor constructor = clazz.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] { window };
                result = (AbstractDialog) constructor.newInstance(actualArgs);
            } catch (Exception e) {
                String msg = e.getClass().getName() + ": " + e.getMessage();
                result = new ErrorDialog(window, msg);
            }
        }
        return result;
    }
    

    public void setClassName(String className) {
        this.className = className;
    }
    

}
