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

public abstract class AbstractDialogPage extends WebPage {

    public AbstractDialogPage(final AbstractDialog dialog) {
        add(new AjaxLink("ok") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                ok();
                dialog.close(target);
            }
        });
        
        add(new AjaxLink("cancel") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                cancel();
                dialog.close(target);
            }
        });
    }
    
    
    protected abstract void ok();

    protected abstract void cancel();
    
}
