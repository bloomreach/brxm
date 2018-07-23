/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.plugin.api.TaxonomyHelper;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.TaxonomyModel;
import org.onehippo.taxonomy.util.TaxonomyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyPalette extends Panel {

    static final Logger log = LoggerFactory.getLogger(TaxonomyPalette.class);

    private final Locale preferredLocale;

    @Deprecated
    public TaxonomyPalette(String id, final IModel<Classification> model, final TaxonomyModel taxonomyModel, String preferredLocale) {
        this(id, model, taxonomyModel, TaxonomyUtil.toLocale(preferredLocale));
    }

    public TaxonomyPalette(String id, final IModel<Classification> model, final TaxonomyModel taxonomyModel, final Locale preferredLocale) {
        super(id, model);

        this.preferredLocale = preferredLocale;

        IModel<List<String>> choices = new IModel<List<String>>() {

            public List<String> getObject() {
                return model.getObject().getKeys();
            }

            public void setObject(List<String> object) {
                List<String> keys = model.getObject().getKeys();
                keys.clear();
                keys.addAll(object);
            }

            public void detach() {
                model.detach();
            }

        };
        Map<String, String> leaves = new TreeMap<>();
        for (Category category : taxonomyModel.getObject().getCategories()) {
            findLeaves(leaves, category);
        }
        LinkedList<String> options = new LinkedList<>();
        options.addAll(leaves.values());

        add(new Palette<String>("palette", choices, new Model<>(options),
                new IChoiceRenderer<String>() {

                    @Override
                    public Object getDisplayValue(String object) {
                        final Category cat = taxonomyModel.getObject().getCategoryByKey(object);
                        return TaxonomyHelper.getCategoryName(cat, TaxonomyPalette.this.preferredLocale);
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

                }, 20/*rows*/, false/*allowOrder*/) {

            // FIXME: workaround for https://issues.apache.org/jira/browse/WICKET-2843
            @Override
            public Collection getModelCollection() {
                return new ArrayList(super.getModelCollection());
            }

            // trigger setObject on selection changed
            @Override
            protected Recorder newRecorderComponent() {
                Recorder recorder = super.newRecorderComponent();
                recorder.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                    }

                });
                return recorder;
            }
        });
    }

    static void findLeaves(Map<String, String> leaves, Category category) {
        if (category.getChildren().size() == 0) {
            String name = category.getName();
            if (leaves.containsKey(name)) {
                log.warn("Name " + name + " is used for multiple categories; ignoring key " + category.getKey());
            } else {
                leaves.put(name, category.getKey());
            }
        } else {
            for (Category child : category.getChildren()) {
                findLeaves(leaves, child);
            }
        }
    }

}
