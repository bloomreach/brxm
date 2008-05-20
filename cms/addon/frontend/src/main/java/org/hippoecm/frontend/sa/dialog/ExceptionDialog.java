/*
 * Copyright 2008 Hippo
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.sa.core.IPluginContext;
import org.hippoecm.frontend.sa.core.ServiceReference;
import org.hippoecm.frontend.sa.service.IDialogService;

public class ExceptionDialog extends WebPage {

    private ServiceReference<IDialogService> windowRef;

    public ExceptionDialog(IPluginContext context, IDialogService dialogService, String exception) {
        windowRef = context.getReference(dialogService);

        add(new Label("exception", exception));
        add(new AjaxLink("ok") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    getDialogWindow().close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected IDialogService getDialogWindow() {
        return windowRef.getService();
    }
}
