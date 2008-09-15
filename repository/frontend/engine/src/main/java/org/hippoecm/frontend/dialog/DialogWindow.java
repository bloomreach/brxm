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

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.hippoecm.frontend.service.ITitleDecorator;

public class DialogWindow extends ModalWindow implements PageCreator, IDialogService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected Page page;

    public DialogWindow(String id) {
        super(id);
        setCookieName(id);
        setPageCreator(this);
    }

    public Page createPage() {
        return page;
    }

    public void show(Page aPage) {
        this.page = aPage;

        if (page instanceof ITitleDecorator) {
            setTitle(((ITitleDecorator) page).getTitle());
        }

        IRequestTarget target = RequestCycle.get().getRequestTarget();
        if (AjaxRequestTarget.class.isAssignableFrom(target.getClass())) {
            show((AjaxRequestTarget) target);
        }
    }

    public void close() {
        IRequestTarget target = RequestCycle.get().getRequestTarget();
        if (AjaxRequestTarget.class.isAssignableFrom(target.getClass())) {
            close((AjaxRequestTarget) target);
        }
    }
}
