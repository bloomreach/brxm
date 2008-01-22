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
package org.hippoecm.frontend.plugins.template.dialog;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PathDialog.class);

    class PathModel extends Model {
        private static final long serialVersionUID = 1L;

        private String path;

        public Object getObject() {
            return path;
        }

        public void setObject(Object object) {
            if (object instanceof String) {
                path = (String) object;
            }
        }
    }

    public PathDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);

        PathModel model = new PathModel();
        setModel(model);
        TextFieldWidget editor = new TextFieldWidget("path", model);
        add(editor);
    }

    @Override
    protected void cancel() {
    }

    @Override
    protected void ok() throws Exception {
        Channel channel = getIncoming();
        if (channel != null) {
            PluginModel model = new PluginModel();
            model.put("name", (String) getModelObject());
            Request request = channel.createRequest("name", model);
            channel.send(request);
        }
    }

}
