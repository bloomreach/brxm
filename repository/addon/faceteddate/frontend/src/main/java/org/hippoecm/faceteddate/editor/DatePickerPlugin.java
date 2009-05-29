/*
 *  Copyright 2008 Hippo.
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.datetime.StyleDateConverter;
import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.DateFieldWidget;

public class DatePickerPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DatePickerPlugin.class);

    public DatePickerPlugin(IPluginContext context, IPluginConfig config) throws RepositoryException {
        super(context, config);

        Node dateNode = ((JcrNodeModel) getModel()).getNode();
        JcrPropertyValueModel valueModel = new JcrPropertyValueModel(new JcrPropertyModel(dateNode
                .getProperty("hippostd:date")));
        MarkupContainer panel;
        add(panel = new WebMarkupContainer("info"));

        panel.add(new Label("weekofyear", dateNode.hasProperty("hippostd:weekofyear") ? dateNode.getProperty(
                "hippostd:weekofyear").getString() : "-"));
        if (dateNode.hasProperty("hippostd:dayofweek")) {
            panel.add(new Label("dayofweek", new TypeTranslator(new JcrNodeTypeModel("hippostd:date")).getValueName(
                    "hippostd:dayofweek", new Model(dateNode.getProperty("hippostd:dayofweek").getString()))));
        } else {
            panel.add(new Label("dayofweek", "-"));
        }
        panel.add(new Label("dayofyear", dateNode.hasProperty("hippostd:dayofyear") ? dateNode.getProperty(
                "hippostd:dayofyear").getString() : "-"));

        if ("edit".equals(config.getString("mode", "view"))) {
            add(new DateFieldWidget("value", valueModel));
            panel.setVisible(false);
        } else {
            add(new DateLabel("value", valueModel, new StyleDateConverter(true)));
        }

        setOutputMarkupId(true);
    }
}
