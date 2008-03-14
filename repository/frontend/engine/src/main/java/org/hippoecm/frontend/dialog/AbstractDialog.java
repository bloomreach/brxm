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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.ComponentReference;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDialog extends WebPage {

    static final Logger log = LoggerFactory.getLogger(AbstractDialog.class);

    protected AjaxLink ok;
    protected AjaxLink cancel;
    private ComponentReference windowRef;

    private String exception = "";

    public AbstractDialog(DialogWindow dialogWindow) {
        this.windowRef = new ComponentReference(dialogWindow);

        final Label exceptionLabel = new Label("exception", new PropertyModel(this, "exception"));
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel);

        ok = new AjaxLink("ok") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    ok();
                    getDialogWindow().close(target);
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
                getDialogWindow().close(target);
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

    protected DialogWindow getDialogWindow() {
        return (DialogWindow) windowRef.getObject();
    }

    protected Plugin getPlugin() {
        return (Plugin) getDialogWindow().findParent(Plugin.class);
    }
    
    protected Channel getChannel() {
        return getDialogWindow().getProxyChannel();
    }

    protected abstract void ok() throws Exception;

    protected abstract void cancel();

}
