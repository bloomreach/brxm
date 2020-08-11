/*
 * Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.frontend.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.TextDiffModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticDropdownPlugin extends RenderPlugin<String> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(StaticDropdownPlugin.class);

    private static final CssResourceReference CSS = new CssResourceReference(StaticDropdownPlugin.class, "StaticDropdownPlugin.css");

    public StaticDropdownPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String mode = config.getString("mode", "view");
        Fragment fragment = new Fragment("fragment", mode, this);
        add(fragment);

        if ("edit".equals(mode)) {
            final Map<String, String> optionsMap = getOptionsMap();

            DropDownChoice<String> choice = new DropDownChoice<String>("select", getModel(), new ArrayList<String>(optionsMap.keySet()),
                    new IChoiceRenderer<String>() {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public Object getDisplayValue(String object) {
                            return optionsMap.get(object);
                        }

                        @Override
                        public String getIdValue(String object, int index) {
                            return object;
                        }

                        @Override
                        public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
                            final List<? extends String> choices = choicesModel.getObject();
                            return choices.contains(id) ? id : null;
                        }
                    });

            choice.add(new OnChangeAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                }
            });

            fragment.add(choice);
        } else {
            Label label = null;
            if ("compare".equals(mode)) {
                if (config.containsKey("model.compareTo")) {
                    @SuppressWarnings("unchecked")
                    IModelReference<String> baseRef = context.getService(config.getString("model.compareTo"),
                            IModelReference.class);
                    if (baseRef != null) {
                        IModel<String> baseOptionIdModel = baseRef.getModel();
                        if (baseOptionIdModel == null) {
                            log.info("base model service provides null model");
                        }
                        label = (Label) new Label("selectLabel", new TextDiffModel(
                                getOptionTextModel(baseOptionIdModel), getOptionTextModel(getModel())))
                                .setEscapeModelStrings(false);
                    } else {
                        log.warn("opened in compare mode, but no base model service is available");
                    }
                } else {
                    log.warn("opened in compare mode, but no base model was configured");
                }
            }
            if (label == null) {
                label = new Label("selectLabel", getOptionTextModel(getModel()));
            }
            fragment.add(label);
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CSS));
    }

    /**
     * Returns a map of static value options, keyed by option IDs and valued by option texts.
     * @return
     */
    protected Map<String, String> getOptionsMap() {
        String [] tokens = StringUtils.splitPreserveAllTokens(getPluginConfig().getString("selectable.options", null), ",");

        if (tokens == null) {
            tokens = ArrayUtils.EMPTY_STRING_ARRAY;
        }

        Map<String, String> optionsMap = new LinkedHashMap<String, String>();

        int offset;
        String optionId;
        String optionText;

        for (int i = 0; i < tokens.length; i++) {
            offset = tokens[i].indexOf('=');

            if (offset == -1) {
                optionId = tokens[i];
                optionText = optionId;
            } else {
                optionId = tokens[i].substring(0, offset);
                optionText = tokens[i].substring(offset + 1);
            }

            optionsMap.put(optionId, optionText);
        }

        return optionsMap;
    }

    /**
     * Returns option text mapped by the given <code>optionId</code>.
     * If no option text found from the map, it should return the <code>optionId</code> instead.
     * @param optionId
     * @return
     */
    protected String getOptionText(String optionId) {
        if (optionId == null) {
            return "";
        }

        final Map<String, String> optionsMap = getOptionsMap();

        if (optionsMap.containsKey(optionId)) {
            return optionsMap.get(optionId);
        } else {
            return optionId;
        }
    }

    /**
     * Returns option text model mapped by the given <code>optionIdModel</code>.
     * @param optionIdModel
     * @return
     */
    protected IModel<String> getOptionTextModel(final IModel<String> optionIdModel) {
        if (optionIdModel == null) {
            return new Model<String>("");
        }

        return new Model<String>(getOptionText(optionIdModel.getObject()));
    }
}
