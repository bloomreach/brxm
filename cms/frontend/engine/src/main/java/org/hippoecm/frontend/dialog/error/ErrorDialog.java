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
package org.hippoecm.frontend.dialog.error;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.plugin.JcrEvent;

public class ErrorDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;
        
    public ErrorDialog(DialogWindow dialogWindow, String message) {
        super(dialogWindow);
        add(new Label("message", message));
        dialogWindow.setTitle("Error");
    }
 

    @Override
    public JcrEvent ok() throws RepositoryException {
        return new JcrEvent(dialogWindow.getNodeModel(), false);
    }

    @Override
    public void cancel() {
    }

}
