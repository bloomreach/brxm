/*
 * Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.resourcebundle.dialogs;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.onehippo.cms7.resourcebundle.ResourceBundlePlugin;
import org.onehippo.cms7.resourcebundle.data.Resource;

public class ResourceDeleteDialog extends Dialog<Resource> {

    private ResourceBundlePlugin plugin;

    public ResourceDeleteDialog(final ResourceBundlePlugin plugin, final Resource resource) {
        this.plugin = plugin;

        // interpolate the string
        String warning = new StringResourceModel("dialog.resource.delete.warning", plugin, null).getObject();
        warning = warning.replaceAll("\\$\\{key\\}", resource.getKey());
        add(new Label("warning", warning));

        setFocusOnCancel();
    }

    public IModel<String> getTitle() {
        return new StringResourceModel("dialog.resource.delete.title", plugin, null);
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM_AUTO;
    }
}
