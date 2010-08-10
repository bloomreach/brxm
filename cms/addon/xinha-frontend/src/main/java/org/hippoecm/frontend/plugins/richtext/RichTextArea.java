/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.richtext;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;

public class RichTextArea extends TextArea<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private AbstractAjaxBehavior callback;

    private String width = "1px";
    private String height = "1px";
    
    public RichTextArea(String id, IModel<String> model) {
        super(id, model);

        setConvertEmptyInputStringToNull(false);
        setOutputMarkupId(true);
        setVisible(true);
        setMarkupId("xinha" + Integer.valueOf(Session.get().nextSequenceValue()));

        // Auto-save callback 
        add(callback = new AbstractDefaultAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            public void respond(AjaxRequestTarget target) {
                processInput();
            }
        });
    }

    public String getCallbackUrl() {
        return callback.getCallbackUrl().toString();
    }

    @Override
    protected void onComponentTag(final ComponentTag tag) {
        StringBuilder sb = new StringBuilder();
        sb.append("width: ");
        sb.append(width);
        sb.append(";");

        sb.append("height: ");
        sb.append(height);
        sb.append(";");

        sb.append("display: none;");
        tag.put("style", sb.toString());
        super.onComponentTag(tag);
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getHeight() {
        return height;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getWidth() {
        return width;
    }

}
