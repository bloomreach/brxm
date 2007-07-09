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
package org.hippocms.repository.webapp.menu.reset;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippocms.repository.webapp.Main;
import org.hippocms.repository.webapp.menu.AbstractDialogPage;

public class ResetDialogPage extends AbstractDialogPage {
    private static final long serialVersionUID = 1L;

    public ResetDialogPage(final ResetDialog dialog) {
        super(dialog);
        Label label;
        try {
            boolean changes = Main.getSession().hasPendingChanges();
            if (changes) {
                label = new Label("changes", "There are pending changes");
            } else {
                label = new Label("changes", "There are no pending changes");
            }
        } catch (RepositoryException e) {
            label = new Label("changes", "exception: " + e.getMessage());
        }
        add(label);
    }
    
    public void ok() {
        try {
            Main.getSession().refresh(false);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void cancel() {
    }

}
