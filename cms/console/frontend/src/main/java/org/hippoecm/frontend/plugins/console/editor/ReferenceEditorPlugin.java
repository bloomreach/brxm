/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.editor;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.HippoNodeType;

public class ReferenceEditorPlugin extends Plugin implements ValueEditorFactory {

    public ReferenceEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        context.registerService(this, SERVICE_ID);
    }

    @Override
    public boolean canEdit(final JcrPropertyValueModel valueModel) {
        if (valueModel == null) {
            return false;
        }
        try {
            Property property = valueModel.getJcrPropertymodel().getProperty();
            return (property.getType() == PropertyType.REFERENCE
                    || property.getName().equals(HippoNodeType.HIPPO_DOCBASE));
        } catch (RepositoryException e) {
            NodeEditor.log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public Component createEditor(final String id, final JcrPropertyValueModel valueModel) {
        return new ReferenceEditor(id, valueModel.getJcrPropertymodel(), valueModel);
    }
}
