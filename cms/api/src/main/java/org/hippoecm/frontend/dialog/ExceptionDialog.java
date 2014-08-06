/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

public class ExceptionDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private String title;
    private String message;

    public ExceptionDialog() {
        this.title = getString("dialog-title", null, "Exception");

        add(new Label("message", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return message;
            }

        }) {
            @Override
            public boolean isVisible() {
                return !Strings.isEmpty(message);
            }
        });
        setCancelVisible(false);
    }

    public ExceptionDialog(Exception exception) {
        this();
        IModel<String> messageModel = getExceptionTranslation(exception);
        message = messageModel.getObject();
    }

    public ExceptionDialog(final String msg) {
        this();
        this.message = msg;
    }

    @Override
    public IModel getTitle() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return title;
            }
        };
    }

    /**
     * Set the title of the exception dialog, by default it will use a translated value from
     * the ExceptionDialog.properties file(s).
     *
     * @param title  The title of the exception dialog
     */
    public void setTitle(final String title) {
        this.title = title;
    }

}
