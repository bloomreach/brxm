/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package {{componentsPackage}};

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.base.Strings;

import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.builder.HstQueryBuilder;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.site.HstServices;

import org.onehippo.cms7.essentials.components.CommonComponent;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyManager;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.impl.CategoryImpl;

import org.example.beans.NewsDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.constraint;

@ParametersInfo(type = TaxonomyCategoryResultInfo.class)
public class TaxonomyCategoryResult extends CommonComponent {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyCategoryResult.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        final TaxonomyManager taxonomyManager = HstServices.getComponentManager().getComponent(TaxonomyManager.class.getName());
        final TaxonomyCategoryResultInfo componentParametersInfo = getComponentParametersInfo(request);
        final String root = componentParametersInfo.getRoot();
        final String paramCategory = getAnyParameter(request, "category");
        final String relativePath = Strings.isNullOrEmpty(paramCategory) ? componentParametersInfo.getPath() : paramCategory;
        final String stringLocale = componentParametersInfo.getLocale();


        request.setAttribute("locale", stringLocale);
        final Locale locale = new Locale(stringLocale);
        if (root == null || taxonomyManager.getTaxonomies().getTaxonomy(root) == null) {
            request.setAttribute("error", "Cannot find taxonomy " + root);
        } else {
            final Taxonomy taxonomy = taxonomyManager.getTaxonomies().getTaxonomy(root);
            if (relativePath == null || taxonomy.getCategory(relativePath) == null) {
                // try to find

                request.setAttribute("error", "Cannot find taxonomy term for relative path: " + relativePath);
            } else {

                final Category category = taxonomy.getCategory(relativePath);
                final CategoryInfo myInfo = category.getInfo(locale);
                request.setAttribute("categoryInfo", myInfo);
                // add categories
                final List<? extends Category> children = category.getChildren();
                final List<CategoryWrapper> categories = new ArrayList<>();
                for (Category child : children) {
                    categories.add(new CategoryWrapper((CategoryImpl) child, locale));
                }
                request.setAttribute("subCategories", categories);
                final String key = category.getKey();
                final HippoBean baseBean = request.getRequestContext().getSiteContentBaseBean();
                try {

                    @SuppressWarnings("unchecked")
                    HstQuery query = HstQueryBuilder.create(baseBean)
                                                    .ofTypes(NewsDocument.class)
                                                    .where(constraint(TaxonomyNodeTypes.HIPPOTAXONOMY_KEYS).contains(key))
                                                    .build();

                    final HstQueryResult queryResult = query.execute();
                    final HippoBeanIterator beans = queryResult.getHippoBeans();
                    final List<NewsDocument> documents = new ArrayList<>();
                    while (beans.hasNext()) {
                        final NewsDocument bean = (NewsDocument) beans.nextHippoBean();
                        // in case document was depublished / removed in the meantime...
                        if (bean != null) {
                            documents.add(bean);
                        }
                    }
                    request.setAttribute("category", category);
                    request.setAttribute("documents", documents);
                } catch (QueryException e) {
                    log.error("Invalid query", e);
                }
            }
        }
    }

    public class CategoryWrapper implements CategoryInfo {
        private final Locale locale;
        private  final CategoryImpl category;
        private final CategoryInfo categoryInfo;


        public CategoryWrapper(final CategoryImpl category, final Locale locale) {
            this.category = category;
            this.categoryInfo = category.getInfo(locale);;
            this.locale = locale;
        }

        public String getLink() {
            Category parent = category.getParent();
            String link = getName();
            while (parent != null) {
                link = parent.getName() + '/' + link;
                parent = parent.getParent();
            }
            return link;
        }

        @Override
        public String getName() {
            return category.getName();
        }

        @Override
        public String getLanguage() {
            return categoryInfo.getLocale().getLanguage();
        }

        @Override
        public Locale getLocale() {
            return locale;
        }

        @Override
        public String getDescription() {
            return categoryInfo.getDescription();
        }

        @Override
        public String[] getSynonyms() {
            return categoryInfo.getSynonyms();
        }

        @Override
        public Map<String, Object> getProperties() {
            return categoryInfo.getProperties();
        }

        @Override
        public String getString(final String property) {
            return categoryInfo.getString(property);
        }

        @Override
        public String getString(final String property, final String defaultValue) {
            return categoryInfo.getString(property, defaultValue);
        }

        @Override
        public String[] getStringArray(final String property) {
            return categoryInfo.getStringArray(property);
        }
    }
}
