/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.repository;

import java.util.Locale;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.plugin.sorting.IListItemComparator;
import org.onehippo.forge.selection.repository.utils.SortUtils;
import org.onehippo.forge.selection.repository.valuelist.ValueListService;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces("application/json")
@Path("/")
public class ValueListResource {

    private static final Logger log = LoggerFactory.getLogger(ValueListResource.class);

    private static final CacheControl NO_CACHE = new CacheControl();

    static {
        NO_CACHE.setNoCache(true);
    }

    private final SessionRequestContextProvider sessionRequestContextProvider;

    public ValueListResource(final SessionRequestContextProvider userSessionProvider) {
        this.sessionRequestContextProvider = userSessionProvider;
    }

    @GET
    @Path("{source:.*}")
    public Response getDocument(
            @PathParam("source") final String source,
            @QueryParam("locale") final String locale,
            @QueryParam("sortComparator") final String sortComparator,
            @QueryParam("sortBy") final String sortBy,
            @QueryParam("sortOrder") final String sortOrder,
            @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest,
                (session) -> {
                    final String checkedSource = checkSource(source);
                    final Locale checkedLocale = checkLocale(locale);
                    final ValueList valueList = 
                            ValueListService.get().getValueList(checkedSource, checkedLocale, session);
                    if (StringUtils.isNotBlank(sortComparator)) {
                        IListItemComparator comparator = SortUtils.getComparator(sortComparator, sortBy, sortOrder);
                        if (comparator != null) {
                            valueList.sort(comparator);
                        }
                    }
                    return valueList;
                }
        );
    }

    /**
     * Prepend a source that is a path with a starting slash. Source can also be a node identifier.
     */
    private String checkSource(final String source) {
        if (StringUtils.contains(source, '/')) {
            return "/" + source;
        }
        return source;
    }

    private Locale checkLocale(final String locale) {
        if (StringUtils.isBlank(locale)) {
            return null;
        }
        try {
            return LocaleUtils.toLocale(locale);
        } catch (IllegalArgumentException e) {
            log.debug("Locale {} is not valid.", locale);
        }
        return null;
    }
    
    /**
     * Shared logic for providing the EndPointTask with contextual input and handling the packaging of its response
     * (which may be an error, encapsulated in an Exception).
     *
     * @param servletRequest current HTTP servlet request to derive contextual input
     * @param task           the EndPointTask to execute
     * @return a JAX-RS response towards the client
     */
    private Response executeTask(final HttpServletRequest servletRequest,
                                 final EndPointTask task) {
        final Session session = sessionRequestContextProvider.getJcrSession(servletRequest);
        try {
            final Object result = task.execute(session);
            return Response.status(Response.Status.OK).cacheControl(NO_CACHE).entity(result).build();
        } catch (final ErrorWithPayloadException e) {
            return Response.status(e.getStatus()).cacheControl(NO_CACHE).entity(e.getPayload()).build();
        }
    }

    @FunctionalInterface
    private interface EndPointTask {
        Object execute(Session session) throws ErrorWithPayloadException;
    }

}
