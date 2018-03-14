/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.restapi.content;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.restapi.AbstractResource;
import org.hippoecm.hst.restapi.NodeVisitor;
import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.hst.restapi.content.search.SearchResult;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.search.query.AndClause;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.QueryUtils;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.SearchService;
import org.onehippo.cms7.services.search.service.SearchServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.restapi.content.DocumentsResource.SortOrder.DESCENDING;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

@Produces("application/json")
public class DocumentsResource extends AbstractResource {

    private static final Logger log = LoggerFactory.getLogger(DocumentsResource.class);

    private static final int DEFAULT_MAX_SEARCH_RESULT_ITEMS = 100;
    private int maxSearchResultItems = DEFAULT_MAX_SEARCH_RESULT_ITEMS;

    public enum SortOrder { ASCENDING, ASC, DESCENDING, DESC }

    @Override
    public Logger getLogger() {
        return log;
    }


    public void setMaxSearchResultItems(final Integer maxSearchResultItems) {
        if (maxSearchResultItems != null) {
            this.maxSearchResultItems = maxSearchResultItems;
        }
    }

    private int parseMax(final String maxString) throws IllegalArgumentException {
        int max = maxSearchResultItems;

        if (maxString != null) {
            try {
                max = Integer.parseInt(maxString);
                if (max > maxSearchResultItems) {
                    log.debug("Max '{}' exceeds maximum search result items of '{}'. Reducing max to '{}'.",
                            max, maxSearchResultItems, maxSearchResultItems);
                    max = maxSearchResultItems;
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("_max must be a number, greater than zero, it was: '" + maxString + "'");
            }
            if (max <= 0) {
                throw new IllegalArgumentException("_max must be greater than zero, it was: '" + maxString + "'");
            }
        }

        return max;
    }

    private int parseOffset(final String offsetString) throws IllegalArgumentException {
        int offset = 0;

        if (offsetString != null) {
            try {
                offset = Integer.parseInt(offsetString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("_offset must be a number, greater than or equal to zero, it was: '" + offsetString + "'");
            }
            if (offset < 0) {
                throw new IllegalArgumentException("_offset must be greater than or equal to zero, it was: '" + offsetString + "'");
            }
        }

        return offset;
    }

    private String parseQuery(final String queryString) {
        return SearchInputParsingUtils.parse(queryString, true);
    }

    private String parseNodeType(final ResourceContext context, final String nodeTypeString) throws IllegalArgumentException {
        if (nodeTypeString == null) {
            return NT_DOCUMENT;
        }

        final ContentType type = context.getContentTypes().getType(nodeTypeString);
        if (type == null) {
            throw new IllegalArgumentException(String.format("_nodetype must be a known node type, it was: '%s'", nodeTypeString));
        }

        if (NT_DOCUMENT.equals(nodeTypeString) || type.getSuperTypes().contains(NT_DOCUMENT)) {
            return nodeTypeString;
        }

        throw new IllegalArgumentException(String.format("_nodetype must be of (sub)type: '%s'", NT_DOCUMENT));
    }

    private List<String> parseAttributes(final String attributeString) {
        if (attributeString == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(attributeString.split(","));
    }


    private List<String> getOrderBy(final String orderBy, final boolean preview) {
        if (StringUtils.isNotBlank(orderBy)) {
            return parseOrderBy(orderBy);
        }
        if (preview) {
            return Collections.singletonList(HIPPOSTDPUBWF_LAST_MODIFIED_DATE);
        }
        return  Collections.singletonList(HIPPOSTDPUBWF_PUBLICATION_DATE);
    }


    private List<String> parseOrderBy(final String orderBy) {
        return Arrays.asList(StringUtils.split(orderBy, ','));
    }

    private List<SortOrder> getSortOrder(final String sortOrder) {
        if (StringUtils.isNotBlank(sortOrder)) {
            return parseSortOrder(sortOrder);
        }
        return Collections.singletonList(DESCENDING);
    }

    private List<SortOrder> parseSortOrder(final String sortOrder) {
        final List<SortOrder> sortOrders = new LinkedList<>();
        try {
            final String[] sortOrderArray = StringUtils.split(sortOrder, ',');
            for (String sort : sortOrderArray) {
                sortOrders.add(SortOrder.valueOf(sort.toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("_sortOrder value must be one of: " + StringUtils.join(SortOrder.values(), ", ").toLowerCase());
        }
        return sortOrders;
    }

    private void checkOrderParameters(final List<String> orderBys, final List<SortOrder> parsedSortOrders) {
        if (orderBys.size() != parsedSortOrders.size()) {
            throw new IllegalArgumentException("Number of values for _orderBy and _sortOrder must be equal.");
        }
    }

    @GET
    @Path("/documents")
    public Response getDocuments(@QueryParam("_offset") final String offsetString,
                                 @QueryParam("_max") final String maxString,
                                 @QueryParam("_query") final String queryString,
                                 @QueryParam("_nodetype") final String nodeTypeString,
                                 @QueryParam("_orderBy") final String orderBy,
                                 @QueryParam("_sortOrder") final String sortOrder,
                                 @QueryParam("_attributes") final String attributeString) {
        try {

            final boolean preview = RequestContextProvider.get().isPreview();

            final List<String> includedAttributes = parseAttributes(attributeString);
            final ResourceContext context = getResourceContextFactory().createResourceContext(includedAttributes);
            final int offset = parseOffset(offsetString);
            final int max = parseMax(maxString);
            final String parsedQuery = parseQuery(queryString);
            final String parsedNodeType = parseNodeType(context, nodeTypeString);
            final List<String> parsedOrderBys = getOrderBy(orderBy, preview);
            final List<SortOrder> parsedSortOrders = getSortOrder(sortOrder);
            checkOrderParameters(parsedOrderBys, parsedSortOrders);

            final String availability;
            if (preview) {
                availability = "preview";
            } else {
                availability = "live";
            }
            final SearchService searchService = getSearchService(context);
            AndClause andClause = searchService.createQuery()
                    .from(RequestContextProvider.get()
                            .getResolvedMount()
                            .getMount()
                            .getContentPath())
                    .ofType(parsedNodeType)
                    .where(QueryUtils.text().contains(parsedQuery == null ? "" : parsedQuery))
                    .and(QueryUtils.text(HIPPO_AVAILABILITY).isEqualTo(availability));

            final Query query = addOrdering(andClause, parsedOrderBys, parsedSortOrders).offsetBy(offset)
                    .limitTo(max);

            final QueryResult queryResult = searchService.search(query);
            final SearchResult result = new SearchResult();
            result.populateFromDocument(offset, max, queryResult, context);

            return Response.status(200).entity(result).build();

        } catch (SearchServiceException sse) {
            logException("Exception while fetching documents", sse);
            if (sse.getCause() instanceof InvalidQueryException) {
                return buildErrorResponse(400, sse.toString());
            }
            return buildErrorResponse(500, sse);
        } catch (IllegalArgumentException iae) {
            logException("Exception while fetching documents", iae);
            return buildErrorResponse(400, iae);
        } catch (Exception e) {
            logException("Exception while fetching documents", e);
            return buildErrorResponse(500, e);
        }
    }

    private Query addOrdering(final AndClause andClause, final List<String> orderBys, final List<SortOrder> sortOrders) {
        Query query = andClause;
        for (int i = 0; i < orderBys.size(); i++) {
            final String orderBy = orderBys.get(i);
            final SortOrder sortOrder = sortOrders.get(i);
            switch (sortOrder) {
                case DESCENDING:
                case DESC:
                    query = query.orderBy(orderBy).descending();
                    break;
                default:
                    query = query.orderBy(orderBy);
            }
        }
        return query;
    }

    @GET
    @Path("/documents/{uuid}")
    public Response getDocumentsByUUID(@PathParam("uuid") final String uuidString,
                                       @QueryParam("_attributes") final String attributeString) {
        try {
            final List<String> includedAttributes = parseAttributes(attributeString);
            final ResourceContext context = getResourceContextFactory().createResourceContext(includedAttributes, true);
            final Session session = context.getRequestContext().getSession();

            // throws an IllegalArgumentException in case the uuid is not correctly formed
            final UUID uuid = parseUUID(uuidString);

            // throws an ItemNotFoundException in case the uuid does not exist or is not readable
            final Node node = session.getNodeByIdentifier(uuid.toString());

            if (!isNodePartOfApiContent(context, node)) {
                // documents not within context of the mount content path "don't exist"
                throw new ItemNotFoundException(String.format("Item '%s' not found below scope '%s'",  uuidString,
                        context.getRequestContext().getResolvedMount().getMount().getMountPath()));
            }

            if (!node.isNodeType(NT_HANDLE)) {
                throw new ItemNotFoundException(String.format("Item '%s' not found below scope '%s'",  uuidString,
                        context.getRequestContext().getResolvedMount().getMount().getMountPath()));
            }

            // throws a PathNotFoundException in case there is no live variant or it is not readable
            final Node doc = node.getNode(node.getName());
            if (!doc.isNodeType(NT_DOCUMENT)) {
                throw new ItemNotFoundException(String.format("Item '%s' not found below scope '%s'",  uuidString,
                        context.getRequestContext().getResolvedMount().getMount().getMountPath()));
            }

            final Map<String, Object> response = new LinkedHashMap<>();

            final NodeVisitor visitor = context.getVisitor(node);
            visitor.visit(context, node, response);

            return Response.status(200).entity(response).build();

        } catch (IllegalArgumentException iae) {
            return buildErrorResponse(400, iae);
        } catch (ItemNotFoundException|PathNotFoundException nfe) {
            return buildErrorResponse(404, nfe);
        } catch (RepositoryException re) {
            return buildErrorResponse(500, re);
        }
    }
}