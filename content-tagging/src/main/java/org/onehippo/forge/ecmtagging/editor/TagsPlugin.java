/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.ecmtagging.editor;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.diff.LCS;
import org.hippoecm.frontend.plugins.standards.diff.LCS.Change;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.onehippo.forge.ecmtagging.TaggingNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Frontend Plugin that displays the current assigned tags of the document.
 */
public class TagsPlugin extends AbstractTagsPlugin {

    private static final Logger log = LoggerFactory.getLogger(TagsPlugin.class);

    public static final String WIDGET_ROWS = "widget.rows";
    public static final String LOWERCASE = "tolowercase";
    private static final CssResourceReference CSS = new CssResourceReference(TagsPlugin.class, "TagsPlugin.css");

    private final JcrNodeModel nodeModel;
    private String rows;
    private final boolean toLowerCase;

    public TagsPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final String defaultCaption = new ClassResourceModel("keywords", TagsPlugin.class).getObject();
        add(new Label("title", getCaptionModel("tags", defaultCaption)));

        nodeModel = (JcrNodeModel) getModel();
        toLowerCase = config.getAsBoolean(LOWERCASE, false);

        String mode = config.getString("mode", "view");
        TagsModel tagModel = createTagModel(nodeModel);
        if ("edit".equals(mode)) {
            rows = config.getString(WIDGET_ROWS, "3");
            TextAreaWidget ta = createTextArea(tagModel);
            add(ta);
        } else {
            Label label = null;
            if ("compare".equals(mode) && config.containsKey("model.compareTo")) {
                IModelReference baseRef = context.getService(config.getString("model.compareTo"),
                    IModelReference.class);
                if (baseRef != null) {
                    TagsModel baseModel = createTagModel((JcrNodeModel) baseRef.getModel());
                    Set<String> baseTags = new TreeSet<>(baseModel.getTags());
                    Set<String> currentTags = new TreeSet<>(tagModel.getTags());
                    List<Change<String>> changes = LCS.getChangeSet(
                        baseTags.toArray(new String[0]),
                        currentTags.toArray(new String[0]));
                    boolean first = true;
                    StringBuilder sb = new StringBuilder();
                    for (Change<String> change : changes) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(", ");
                        }
                        switch (change.getType()) {
                        case ADDED:
                            sb.append("<span class=\"hippo-diff-added\">");
                            sb.append(change.getValue());
                            sb.append("</span>");
                            break;
                        case REMOVED:
                            sb.append("<span class=\"hippo-diff-removed\">");
                            sb.append(change.getValue());
                            sb.append("</span>");
                            break;
                        case INVARIANT:
                            sb.append(change.getValue());
                        }
                    }
                    label = (Label) new Label("value", sb.toString()).setEscapeModelStrings(false);
                } else {
                    log.warn("no base model service available in compare mode");
                }
            }
            if (label == null) {
                label = new Label("value", tagModel);
            }
            add(label);
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CSS));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        TagsModel tagModel = createTagModel(nodeModel);
        replace(createTextArea(tagModel));
        redraw();
    }

    public TextAreaWidget createTextArea(TagsModel tagModel) {
        TextAreaWidget widget = new TextAreaWidget("value", tagModel);
        widget.setRows(rows);
        return widget;
    }

    private TagsModel createTagModel(JcrNodeModel nodeModel) {
        String path = nodeModel.getItemModel().getPath() + '/' + TaggingNodeType.PROP_TAGS;
        JcrPropertyModel propertyModel = new JcrPropertyModel(path);
        TagsModel tagModel = new TagsModel(propertyModel);
        tagModel.setToLowerCase(toLowerCase);
        log.debug("New tag model object: {}", tagModel.getObject());
        return tagModel;
    }
}
