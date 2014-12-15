/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.components;

import static org.onehippo.forge.sitemap.components.util.ReflectionUtils.obtainInstanceForClass;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.forge.sitemap.components.model.news.NewsUrlset;
import org.onehippo.forge.sitemap.components.model.news.info.Publication;
import org.onehippo.forge.sitemap.generator.DefaultNewsInformationProvider;
import org.onehippo.forge.sitemap.generator.NewsSitemapGenerator;

/**
 * This component serves a news sitemap based on configured document types
 * The documents in the news sitemap cannot be older than 48 hours
 * This class uses deprecated annotations and methods to be compatible with Hippo 7.6
 *
 * @author Wouter Danes
 */
@SuppressWarnings({"unused"})
@ParametersInfo(type = NewsSitemapFeed.NewsSitemapFeedParametersInformation.class)
public class NewsSitemapFeed extends BaseHstComponent {

    private static final int MAX_AGE_IN_HOURS = 48;

    public interface NewsSitemapFeedParametersInformation {
        @Parameter(
                name = "publicationDateProperty",
                required = true
        )
        String getPublicationDateProperty();

        @Parameter(
                name = "documentTypes",
                required = true
        )
        String getDocumentTypes();

        @Parameter(
                name = "propertyCriteria",
                defaultValue = ""
        )
        String getPropertyCriteria();

        @Parameter(
                name = "timezone",
                defaultValue = "UTC"
        )
        String getTimezone();

        @Parameter(
                name = "publicationName",
                defaultValue = ""
        )
        String getPublicationName();

        @Parameter(
                name = "publicationLanguage",
                defaultValue = ""
        )
        String getPublicationLanguage();

        @Parameter(
                name = "newsInformationProvider",
                defaultValue = ""
        )
        String getNewsInformationProviderClassname();
    }

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        NewsSitemapFeedParametersInformation parameters = getComponentParametersInfo(request);
        verifyRequiredParametersAreFilled(parameters);

        NewsInformationProvider newsInformationProvider = createNewsInformationProvider(parameters);

        Map<String, String> propertyCriteria = parsePropertyCriteria(parameters.getPropertyCriteria());

        String[] documentTypes = parameters.getDocumentTypes().trim().split("[\\s]*,[\\s]*");

        HstQuery query = createQuery(
                request,
                documentTypes,
                parameters.getTimezone(),
                newsInformationProvider,
                propertyCriteria
        );

        HstRequestContext requestContext = request.getRequestContext();
        NewsSitemapGenerator sitemapGenerator =
                new NewsSitemapGenerator(requestContext, newsInformationProvider, RequestContextProvider.get().getContentBeansTool().getObjectConverter());
        NewsUrlset urlset = sitemapGenerator.createNewsSitemap(query);

        String sitemap = NewsSitemapGenerator.toString(urlset);
        request.setAttribute("sitemap", sitemap);
    }

    private HstQuery createQuery(final HstRequest request, final String[] documentTypes, final String timezone,
                                 final NewsInformationProvider newsInformationProvider,
                                 final Map<String, String> propertyCriteria) {
        HippoBean siteContentBaseBean = request.getRequestContext().getSiteContentBaseBean();

        HstQuery query;
        try {
            query = RequestContextProvider.get().getQueryManager().createQuery(siteContentBaseBean, documentTypes);

            Filter filter = query.createFilter();

            Calendar dateToObtainNewsSince = new GregorianCalendar(TimeZone.getTimeZone(timezone));
            dateToObtainNewsSince.add(Calendar.HOUR_OF_DAY, -MAX_AGE_IN_HOURS);
            filter.addGreaterThan(newsInformationProvider.getPublicationDateProperty(), dateToObtainNewsSince);

            if (!propertyCriteria.isEmpty()) {
                // Add property criteria
                for (Map.Entry<String, String> propertyCriterion : propertyCriteria.entrySet()) {
                    filter.addEqualTo(propertyCriterion.getKey(), propertyCriterion.getValue());
                }
            }

            query.setFilter(filter);
        } catch (QueryException e) {
            throw new HstComponentException("Cannot create HstQuery", e);
        }

        return query;
    }

    private static void verifyRequiredParametersAreFilled(final NewsSitemapFeedParametersInformation parameters){
        if (StringUtils.isEmpty(parameters.getDocumentTypes())) {
            throw new HstComponentException("No document types specified, please pass the parameter documentTypes");
        }
        if (StringUtils.isEmpty(parameters.getPublicationDateProperty())
                && StringUtils.isEmpty(parameters.getNewsInformationProviderClassname())
                ) {
            throw new HstComponentException("No Publication date property specified, please pass the parameter " +
                    "publicationDateProperty");
        }
    }

    private static NewsInformationProvider createNewsInformationProvider(
            final NewsSitemapFeedParametersInformation parameters) {
        boolean hasCustomProviderClass = StringUtils.isNotEmpty(parameters.getNewsInformationProviderClassname());
        boolean hasPublicationInformation = StringUtils.isNotEmpty(parameters.getPublicationName()) &&
                StringUtils.isNotEmpty(parameters.getPublicationLanguage());
        if (!hasCustomProviderClass && !hasPublicationInformation) {
            throw new HstComponentException("No publication information set and no custom NewsInformationProvider specified");
        }
        if (hasCustomProviderClass) {
            return obtainInstanceForClass(NewsInformationProvider.class,
                    parameters.getNewsInformationProviderClassname());
        } else {
            return new DefaultNewsInformationProvider(
                    new Publication(parameters.getPublicationName(), parameters.getPublicationLanguage()),
                    parameters.getPublicationDateProperty()
            );
        }
    }

    /**
     * Takes an input string with comma seperated property criteria (in the format prop1=condition1,prop2=condiion2...)
     * and returns a map with key = property and value = criterion
     *
     * @param propertyCriteria comma seperated list of property criteria
     * @return {@link Map} containing the property criteria
     */
    private static Map<String, String> parsePropertyCriteria(final String propertyCriteria) {
        if (StringUtils.isEmpty(propertyCriteria)) {
            return Collections.emptyMap();
        }
        String[] criteria = propertyCriteria.trim().split("[\\s]*,[\\s]*");
        Pattern propertyCriterionPattern = Pattern.compile("([\\w]+)[\\s]*=[\\s]*([\\w]+)");
        Map<String, String> propertiesWithCriteria = new HashMap<String, String>();
        for (final String criterion : criteria) {
            Matcher propertyCriterionMatcher = propertyCriterionPattern.matcher(criterion);
            if (!propertyCriterionMatcher.matches()) {
                throw new HstComponentException("Criterion does not match pattern \"property=condition\"");
            }
            String property = propertyCriterionMatcher.group(1);
            String condition = propertyCriterionMatcher.group(2);
            propertiesWithCriteria.put(property, condition);
        }
        return propertiesWithCriteria;
    }
}
