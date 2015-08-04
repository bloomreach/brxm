/*
 *  Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.advancedsearch.plugin;

import com.onehippo.cms7.search.frontend.ISearchContext;
import com.onehippo.cms7.search.frontend.constraints.IConstraintProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.onehippo.cms7.services.search.query.QueryUtils;
import org.onehippo.cms7.services.search.query.constraint.Constraint;
import org.onehippo.cms7.services.search.query.constraint.OrConstraint;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.plugin.ITaxonomyService;
import org.onehippo.taxonomy.plugin.TaxonomyPickerDialog;
import org.onehippo.taxonomy.plugin.TaxonomyPickerPlugin;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Custom plugin for filtering taxonomy documents for advanced search
 *
 * @version "\$Id$" kenan
 */
public class TaxonomyFilterPlugin extends RenderPlugin implements IConstraintProvider {

    private static Logger log = LoggerFactory.getLogger(TaxonomyFilterPlugin.class);

    private static final String PARAM_FILTER_PROPERTY_NAME = "filterPropertyName";
    private static final String PARAM_FILTER_PROPERTY_DEFAULT_VALUE = "filterPropertyDefaultValue";

    private String filterPropertyName;
    private String filterPropertyDefaultValue;
    private String filterPropertyValue;

    private List<String> taxonomyKeys = new ArrayList<>();

    public TaxonomyFilterPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        filterPropertyName = config.getString(PARAM_FILTER_PROPERTY_NAME);

        filterPropertyDefaultValue = config.getString(PARAM_FILTER_PROPERTY_DEFAULT_VALUE);
        filterPropertyValue = filterPropertyDefaultValue;

        final Form form = new Form<>("form", new CompoundPropertyModel<>(this));
        final Model<Classification> model = new Model<Classification>() {

            @Override
            public Classification getObject() {
                return new Classification(taxonomyKeys, () -> {
                });
            }

            @Override
            public void setObject(Classification object) {
                super.setObject(object);
                taxonomyKeys = object.getKeys();
                updateSearchResults();
                redraw();

            }
        };
        IDialogFactory dialogFactory = new IDialogFactory() {

            private static final long serialVersionUID = 1L;

            public AbstractDialog<Classification> createDialog() {
                return createPickerDialog(model, getPreferredLocale());
            }
        };
        form.add(new DialogLink("edit", new ResourceModel("edit"), dialogFactory, getDialogService())).setEnabled(getTaxonomy() != null);
        add(form);

        form.add(new CategoryListView("key"));

        setOutputMarkupId(true);
    }

    private void updateSearchResults() {
        ISearchContext searcher = getPluginContext().getService(ISearchContext.class.getName(), ISearchContext.class);
        searcher.updateSearchResults();
    }

    @Override
    public void clearConstraints() {
        filterPropertyValue = null;
        taxonomyKeys = new ArrayList<>();
        redraw();
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(new CssResourceReference(TaxonomyPickerPlugin.class, "res/style.css")));
    }

    /**
     * Implementing the advanced search API for constraints. This method executes an OR query for taxonomy terms.
     *
     * @return list of Constraints
     */
    @Override
    public List<Constraint> getConstraints() {
        List<Constraint> constraints = new LinkedList<>();

        OrConstraint constraint = null;
        if (!taxonomyKeys.isEmpty()) {
            for (String key : taxonomyKeys) {
                if (constraint == null) {
                    constraint = QueryUtils.either(
                            QueryUtils.text(getFilterPropertyName()).contains(key)
                    );
                } else {
                    constraint.or(QueryUtils.text(getFilterPropertyName()).contains(key));
                }

            }
        }
        if (constraint != null) {
            constraints.add(constraint);
        }
        return constraints;
    }

    public String getFilterPropertyName() {
        return filterPropertyName;
    }

    public void setFilterPropertyName(String filterPropertyName) {
        this.filterPropertyName = filterPropertyName;
    }


    public String getFilterPropertyDefaultValue() {
        return filterPropertyDefaultValue;
    }

    public void setFilterPropertyDefaultValue(String filterPropertyDefaultValue) {
        this.filterPropertyDefaultValue = filterPropertyDefaultValue;
    }

    public String getFilterPropertyValue() {
        return filterPropertyValue;
    }

    public void setFilterPropertyValue(String filterPropertyValue) {
        this.filterPropertyValue = filterPropertyValue;
    }


    /**
     * Creates and returns taxonomy picker dialog instance. <p> If you want to provide a custom taxonomy picker plugin, you might want to override this method.
     * </p>
     */
    protected AbstractDialog<Classification> createPickerDialog(Model<Classification> model, String preferredLocale) {
        return new TaxonomyPickerDialog(getPluginContext(), getPluginConfig(), model, preferredLocale);
    }

    protected Taxonomy getTaxonomy() {
        IPluginConfig config = getPluginConfig();
        ITaxonomyService service = getPluginContext()
                .getService(config.getString(ITaxonomyService.SERVICE_ID, ITaxonomyService.DEFAULT_SERVICE_TAXONOMY_ID), ITaxonomyService.class);

        final String taxonomyName = config.getString(ITaxonomyService.TAXONOMY_NAME);

        if (StringUtils.isBlank(taxonomyName)) {
            log.info("No configured/chosen taxonomy name. Found '{}'", taxonomyName);
            return null;
        }

        return service.getTaxonomy(taxonomyName);
    }

    /**
     * Returns the translation locale of the document if exists. Otherwise, returns the user's UI locale as a fallback.
     */
    protected String getPreferredLocale() {
        return getLocale().getLanguage();
    }

    private class CategoryListView extends RefreshingView<String>{

        public CategoryListView(String id) {
            super(id);
        }

        @Override
        protected Iterator<IModel<String>> getItemModels() {
            List<IModel<String>> items = new ArrayList<>();
            for(String key:taxonomyKeys){
                items.add(new Model<>(key));
            }
            return items.iterator();
        }

        @Override
        protected void populateItem(Item<String> item) {
            final String key = item.getModelObject();
            item.add(new Label("label", key));
        }
    }
}
