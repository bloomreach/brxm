/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.openui;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.onehippo.cms.json.Json;
import org.onehippo.cms7.openui.extensions.UiExtension;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class OpenUiDialog extends Dialog<String> implements OpenUiPlugin {
    final static IValueMap SMALL = new ValueMap("width=60%,height=" + DialogConstants.SMALL.getString("height")).makeImmutable();
    final static IValueMap MEDIUM = new ValueMap("width=75%,height=" + DialogConstants.MEDIUM.getString("height")).makeImmutable();
    final static IValueMap LARGE = new ValueMap("width=90%,height=" + DialogConstants.LARGE.getString("height")).makeImmutable();

    private final CloseDialogBehavior closeDialogBehavior;
    private final Map<String, String> parameters;

    OpenUiDialog(final UiExtension extension, final Map<String, String> parameters) {
        this.parameters = parameters;

        setTitle(Model.of(parameters.getOrDefault("title", StringUtils.EMPTY)));
        setSize(parseSize(parameters));
        setCssClass("openui-dialog");

        // Invoked when the uses presses the (X)
        setCancelAction(model -> "OpenUi.cancelDialog();");

        final Panel openUiPanel = new EmptyPanel("dialog-body");
        openUiPanel.add(new OpenUiBehavior(this, extension));
        add(openUiPanel);

        add(closeDialogBehavior = new CloseDialogBehavior());
    }

    private static IValueMap parseSize(final Map<String, String> parameters) {
        final String size = parameters.getOrDefault("size", "large");
        switch (size.toLowerCase()) {
            case "small":
                return SMALL;
            case "medium":
                return MEDIUM;
            case "large":
                return LARGE;
            default:
                return LARGE;
        }
    }

    @Override
    public ObjectNode getJavaScriptParameters() {
        final ObjectNode javascriptParameters = Json.object();
        javascriptParameters.put("closeUrl", closeDialogBehavior.getCallbackUrl().toString());

        final ObjectNode dialogOptions = Json.object();
        parameters.forEach(dialogOptions::put);
        javascriptParameters.set("dialogOptions", dialogOptions);

        return javascriptParameters;
    }

    private class CloseDialogBehavior extends AbstractDefaultAjaxBehavior {
        @Override
        protected void respond(final AjaxRequestTarget target) {
            closeDialog();
        }
    }
}
