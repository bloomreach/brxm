/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components;

import java.util.Calendar;

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
import org.onehippo.cms7.essentials.components.info.EssentialsNewsComponentInfo;
import org.onehippo.cms7.essentials.components.paging.Pageable;
import org.onehippo.cms7.essentials.components.utils.SiteUtils;
import org.onehippo.cms7.essentials.components.utils.query.HstQueryBuilder;
import org.onehippo.cms7.essentials.components.utils.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HST component used for listing of News document types
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsNewsComponentInfo.class)
public class EssentialsNewsComponent extends EssentialsListComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsNewsComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsNewsComponentInfo paramInfo = getComponentParametersInfo(request);
        final String path = getScopePath(paramInfo);
        log.debug("Calling EssentialsNewsComponentInfo for documents path:  [{}]", path);
        final HippoBean scope = getScopeBean(request, path);
        if (scope == null) {
            log.warn("Search scope was null");
            handleInvalidScope(request, response);
            return;
        }

        final Pageable<HippoBean> pageable = doSearch(request, paramInfo, scope);
        request.setAttribute(REQUEST_PARAM_PAGEABLE, pageable);
    }

    @Override
    protected <T extends EssentialsDocumentListComponentInfo> HstQuery buildQuery(final HstRequest request, final T paramInfo, final HippoBean scope) {
        final QueryBuilder builder = new HstQueryBuilder(this, request);
        EssentialsNewsComponentInfo newsComponentInfo = (EssentialsNewsComponentInfo) paramInfo;
        final String documentTypes = paramInfo.getDocumentTypes();
        final String[] types = SiteUtils.parseCommaSeparatedValue(documentTypes);

        builder.scope(scope).documents(types).includeSubtypes();

        if (newsComponentInfo.isHideFutureItems()) {
            try {
                final Session session = request.getRequestContext().getSession();
                Filter filter = new FilterImpl(session, DateTools.Resolution.DAY);
                filter.addLessOrEqualThan(newsComponentInfo.getDocumentDateField(), Calendar.getInstance(), DateTools.Resolution.DAY);
                builder.addFilter(filter);
            } catch (RepositoryException | FilterException e) {
                log.error("An exception occurred while trying to create a query filter for hiding future items: {}", e);
            }
        }
        return builder.build();
    }

}
