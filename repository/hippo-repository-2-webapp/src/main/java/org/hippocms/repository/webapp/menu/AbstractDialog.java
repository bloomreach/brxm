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
package org.hippocms.repository.webapp.menu;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public abstract class AbstractDialog extends WebPage {

    protected AjaxLink ok;
    protected AjaxLink cancel;
    protected JcrNodeModel model;
    
    public AbstractDialog(final DialogWindow dialogWindow, JcrNodeModel model) {
        this.model = model;
        
        ok = new AjaxLink("ok") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                ok();
                dialogWindow.close(target);
            }
        };
        add(ok);
        
        cancel = new AjaxLink("cancel") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                cancel();
                dialogWindow.close(target);
            }
        };
        add(cancel);
    }
    
    
    protected abstract void ok();

    protected abstract void cancel();
    
}
