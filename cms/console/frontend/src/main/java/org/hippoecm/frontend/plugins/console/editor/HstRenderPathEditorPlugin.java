/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

/**
 * @version $Id: $
 */
public class HstRenderPathEditorPlugin extends Plugin implements ValueEditorFactory {

    private static List<String> referenceProperties = new ArrayList<>(1);

    static {
        referenceProperties.add(HstRenderPathEditor.PROPERTY_HST_RENDERPATH);
    }

    public HstRenderPathEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        context.registerService(this, SERVICE_ID);
    }

    /**
     * Determines if a JcrPropertyValueModel represents a hst reference property.
     *
     * @param valueModel the model to inspect
     * @return true if the property references another hst configuration node
     */
    @Override
    public boolean canEdit(final JcrPropertyValueModel valueModel) {
        if (valueModel == null) {
            return false;
        }
        try {
            Property property = valueModel.getJcrPropertymodel().getProperty();
            return referenceProperties.contains(property.getName());
        } catch (RepositoryException e) {
            NodeEditor.log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public Component createEditor(final String id, final JcrPropertyValueModel valueModel) {
        return new HstRenderPathEditor(id, valueModel);
    }
}
