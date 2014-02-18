/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.repository.util.DateTools;
import org.onehippo.cms7.essentials.components.info.EssentialsDocumentListComponentInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsEventsComponentInfo;
import org.onehippo.cms7.essentials.components.paging.Pageable;
import org.onehippo.cms7.essentials.components.utils.query.HstQueryBuilder;
import org.onehippo.cms7.essentials.components.utils.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HST component used for listing of Event document types
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsEventsComponentInfo.class)
public class EssentialsEventsComponent extends EssentialsListComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsEventsComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsEventsComponentInfo essentialsEventsComponentInfo = getComponentParametersInfo(request);
        final String path = getScopePath(essentialsEventsComponentInfo);
        log.debug("Getting EssentialsEventsComponentInfo for documents path:  [{}]", path);
        final HippoBean scope = getScopeBean(request, path);
        if (scope == null) {
            log.warn("Search scope was null");
            handleInvalidScope(request, response);
            return;
        }

        final Pageable<HippoBean> pageable = doSearch(request, essentialsEventsComponentInfo, scope);
        request.setAttribute(REQUEST_ATTR_PAGEABLE, pageable);
    }

    @Override
    protected <T extends EssentialsDocumentListComponentInfo> HstQuery buildQuery(final HstRequest request, final T componentInfo, final HippoBean scope) {
        final QueryBuilder builder = new HstQueryBuilder(this, request);
        final String documentTypes = componentInfo.getDocumentTypes();
        final String[] types = parseDocumentTypes(documentTypes);
        EssentialsEventsComponentInfo essentialsEventsComponentInfo = (EssentialsEventsComponentInfo) componentInfo;

        builder.scope(scope).documents(types).includeSubtypes();

        if(essentialsEventsComponentInfo.hidePastEvents()) {
            String dateField = null;
            try {
                final Session session = request.getRequestContext().getSession();
                Filter filter = new FilterImpl(session, DateTools.Resolution.DAY);
                dateField = essentialsEventsComponentInfo.getDocumentDateField();
                filter.addGreaterOrEqualThan(dateField, Calendar.getInstance(), DateTools.Resolution.DAY);
                builder.addFilter(filter);
            } catch (FilterException | RepositoryException e) {
                log.error("Error while creating query filter to hide past events using date field {}", dateField, e);
            }
        }
        return builder.build();
    }
}
