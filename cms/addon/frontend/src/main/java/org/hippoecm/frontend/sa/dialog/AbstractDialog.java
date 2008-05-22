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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.service.IDialogService;
import org.hippoecm.frontend.sa.service.ITitleDecorator;
import org.hippoecm.frontend.sa.service.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDialog extends WebPage implements ITitleDecorator {

    static final Logger log = LoggerFactory.getLogger(AbstractDialog.class);

    protected AjaxLink ok;
    protected AjaxLink cancel;
    private ServiceReference<IDialogService> windowRef;

    private String exception = "";

    public AbstractDialog(IPluginContext context, IDialogService dialogWindow) {
        this.windowRef = context.getReference(dialogWindow);

        final Label exceptionLabel = new Label("exception", new PropertyModel(this, "exception"));
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel);

        ok = new AjaxLink("ok") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    ok();
                } catch (Exception e) {
                    String msg = e.getClass().getName() + ": " + e.getMessage();
                    log.error(msg);
                    if (log.isDebugEnabled()) {
                        log.debug("Error from repository: ", e);
                    }
                    setException(msg);
                    target.addComponent(exceptionLabel);
                    e.printStackTrace();
                }
            }
        };
        add(ok);

        cancel = new AjaxLink("cancel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancel();
                closeDialog();
            }
        };
        add(cancel);
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getException() {
        return exception;
    }

    protected IDialogService getDialogService() {
        return windowRef.getService();
    }

    // override to do any cleanup.
    protected void closeDialog() {
        getDialogService().close();
    }

    protected abstract void ok() throws Exception;

    protected abstract void cancel();

}
