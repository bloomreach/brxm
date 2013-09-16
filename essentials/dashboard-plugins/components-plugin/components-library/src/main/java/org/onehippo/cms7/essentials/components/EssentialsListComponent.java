/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsDocumentListComponentInfo;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;
import org.onehippo.cms7.essentials.components.paging.Pageable;
import org.onehippo.cms7.essentials.components.utils.query.HstQueryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

/**
 * HST component used for listing of documents
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsDocumentListComponentInfo.class)
public class EssentialsListComponent extends CommonComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsListComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsDocumentListComponentInfo paramInfo = getComponentParametersInfo(request);
        final String path = paramInfo.getPath();
        log.debug("Calling EssentialsListComponent for documents path:  [{}]", path);
        final HippoBean scope = getScopeBean(request, path);
        if (scope == null) {
            log.warn("Search scope was null");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            if (log.isDebugEnabled()) {
                throw new HstComponentException("EssentialsListComponent needs a valid scope to display documents");
            }
            return;
        }
        //############################################
        // DO SEARCH
        //############################################
        try {
            final HstQueryBuilder builder = new HstQueryBuilder(this, request);
            final String documentTypes = paramInfo.getDocumentTypes();
            final String[] types = parseDocumentTypes(documentTypes);
            final HstQuery build = builder.scope(scope).documents(types).includeSubtypes().build();
            if (build != null) {
                final HstQueryResult execute = build.execute();
                final int pageSize = 4;// NPE??? paramInfo.pageSize();
                final Pageable<HippoBean> pageable = new IterablePagination<>(
                        execute.getHippoBeans(),
                        execute.getTotalSize(),
                        pageSize,
                        getIntParameter(request, "page", 1));
                // todo configure
                pageable.setShowPagination(true);
                request.setAttribute("pageable", pageable);
            }

        } catch (QueryException e) {
            log.error("Error running query", e);
        }


    }


    /**
     * For given string, comma separate it and convert to array
     *
     * @param documentTypes comma separated document types
     * @return empty array if empty
     */
    private String[] parseDocumentTypes(final String documentTypes) {
        if (Strings.isNullOrEmpty(documentTypes)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        final Iterable<String> iterable = Splitter.on(",").trimResults().omitEmptyStrings().split(documentTypes);
        return Iterables.toArray(iterable, String.class);
    }

}
