/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.faceteddate.editor;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.datetime.StyleDateConverter;
import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.datetime.DateFieldWidget;
import org.hippoecm.frontend.model.properties.MapEmptyDateToNullModel;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_DATE;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_DAYOFWEEK;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_DAYOFYEAR;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_WEEKOFYEAR;

public class DatePickerPlugin extends RenderPlugin<Date> {

    private static final long serialVersionUID = 1L;

    public DatePickerPlugin(IPluginContext context, IPluginConfig config) throws RepositoryException {
        super(context, config);

        setOutputMarkupId(true);

        final Node dateNode = ((JcrNodeModel) getDefaultModel()).getNode();
        final IModel<Date> valueModel = new MapEmptyDateToNullModel(new JcrPropertyValueModel<>(
                new JcrPropertyModel(dateNode.getProperty(HIPPOSTD_DATE))));

        final IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.VIEW);
        if (mode == IEditor.Mode.VIEW) {
            add(new View("value", dateNode, valueModel));
        } else {
            add(new DateFieldWidget("value", valueModel, context, config));
        }
    }

    class View extends Fragment {

        public View(String id, Node dateNode, IModel<Date> valueModel) throws RepositoryException {
            super(id, "view", DatePickerPlugin.this);

            add(new DateLabel("label", valueModel, new StyleDateConverter(true)));

            String weekOfYear = dateNode.hasProperty(HIPPOSTD_WEEKOFYEAR) ? dateNode.getProperty(HIPPOSTD_WEEKOFYEAR).getString() : "-";
            add(new Label("weekofyear", weekOfYear));

            if (dateNode.hasProperty(HIPPOSTD_DAYOFWEEK)) {
                add(new Label("dayofweek", new TypeTranslator(new JcrNodeTypeModel(HIPPOSTD_DATE)).getValueName(
                        HIPPOSTD_DAYOFWEEK, new Model<String>(dateNode.getProperty(HIPPOSTD_DAYOFWEEK).getString()))));
            } else {
                add(new Label("dayofweek", "-"));
            }

            String dayOfYear = dateNode.hasProperty(HIPPOSTD_DAYOFYEAR) ? dateNode.getProperty(HIPPOSTD_DAYOFYEAR).getString() : "-";
            add(new Label("dayofyear", dayOfYear));

        }
    }
}
