/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.jquery.upload.multiple;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadWidgetSettings;

public class FileUploadBar extends Panel {
    private static final long serialVersionUID = 1L;

    public FileUploadBar(final String id, final FileUploadWidgetSettings settings) {
        super(id);
        setOutputMarkupId(true);

        add(new GalleryFileUploadBehavior(settings));

        final StringResourceModel message = new StringResourceModel("select.files.caption", this)
                .setModel(Model.of(settings));
        add(new Label("select-files-message", message));
    }
}
