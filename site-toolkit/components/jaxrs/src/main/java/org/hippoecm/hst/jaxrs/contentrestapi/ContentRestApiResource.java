/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.jaxrs.contentrestapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.onehippo.cms7.services.search.jcr.service.HippoJcrSearchService;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.result.Hit;
import org.onehippo.cms7.services.search.result.HitIterator;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.SearchService;

@Produces("application/json")
public class ContentRestApiResource {

    private interface Context {
        Session getSession() throws RepositoryException;
    }

    private final Context context;
    private final List<String> ignoredVariantProperties = Collections.unmodifiableList(Arrays.asList("jcr:uuid"));

    public ContentRestApiResource() {
        this.context = new Context() {
            public Session getSession() throws RepositoryException {
                return RequestContextProvider.get().getSession();
            }
        };
    }

    ContentRestApiResource(Session session) {
        this.context = new Context() {
            public Session getSession() throws RepositoryException {
                return session;
            }
        };
    }

    private final class Link {
        public String url;
        public Link(String url) {
            this.url = url;
        }
    }

    private final class SearchResultItem {
        @JsonProperty("jcr:name")
        public String name;

        @JsonProperty("jcr:uuid")
        public String uuid;

        public Link[] links;

        public SearchResultItem(String name, String uuid, Link[] links) {
            this.name = name;
            this.uuid = uuid;
            this.links = links;
        }
    }

    private final class SearchResult {
        public long offset;
        public long max;
        public long count;
        public long total;
        public boolean more;
        public SearchResultItem[] items;

        void initialize(int offset, int max, QueryResult queryResult, Session session) throws RepositoryException {
            ArrayList<SearchResultItem> itemArrayList = new ArrayList<>();
            HitIterator iterator = queryResult.getHits();
            while (iterator.hasNext()) {
                Hit hit = iterator.nextHit();
                String uuid = hit.getSearchDocument().getContentId().toIdentifier();
                Node node = session.getNodeByIdentifier(uuid);
                SearchResultItem item = new SearchResultItem(node.getName(), uuid, new Link[] { new Link(uuid) });
                itemArrayList.add(item);
            }

            this.offset = offset;
            this.max = max;
            count = itemArrayList.size();
            total = queryResult.getTotalHitCount();
            more = (offset + count) < total;
            items = new SearchResultItem[(int)count];
            itemArrayList.toArray(items);
        }
    }

    private final class Error {
        public int status;
        public String description;
        Error(int status, String description) {
            this.status = status;
            this.description = description;
        }
    }

    private int parseMax(String maxString) throws IllegalArgumentException {
        int max = 100;

        if (maxString != null) {
            try {
                max = Integer.parseInt(maxString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("_max must be a number, greater than zero, it was: '" + maxString + "'");
            }
            if (max <= 0) {
                throw new IllegalArgumentException("_max must be greater than zero, it was: '" + maxString + "'");
            }
        }

        return max;
    }

    private int parseOffset(String offsetString) throws IllegalArgumentException {
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

    @GET
    @Path("/documents")
    public Response getDocuments(@QueryParam("_offset") String offsetString, @QueryParam("_max") String maxString) {
        try {
            int offset = parseOffset(offsetString);
            int max = parseMax(maxString);

            SearchService searchService = getSearchService();
            Query query = searchService.createQuery()
                    .from("/content/documents")
                    .ofType("myhippoproject:basedocument") // TODO change to blacklisting mechanism
                    .returnParentNode()
                    .orderBy(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE)
                    .descending()
                    .offsetBy(offset)
                    .limitTo(max);
            QueryResult queryResult = searchService.search(query);
            SearchResult returnValue = new SearchResult();
            returnValue.initialize(offset, max, queryResult, context.getSession());

            return Response.status(200).entity(returnValue).build();
        } catch (IllegalArgumentException iae) {
            return buildErrorResponse(400, iae);
        } catch (RepositoryException re) {
            return buildErrorResponse(500, re);
        }
    }

    private UUID parseUUID(String uuid) throws IllegalArgumentException {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The string '" + uuid + "' is not a valid UUID");
        }
    }

    @GET
    @Path("/documents/{uuid}")
    public Response getDocumentsByUUID(@PathParam("uuid") String uuidString) {
        try {
            UUID uuid = parseUUID(uuidString);

            Session session = context.getSession();

            // throws an ItemNotFoundException in case the uuid does not exist or is not readable
            Node node = session.getNodeByIdentifier(uuid.toString());

            // throws a PathNotFoundException in case there is no live variant or it is not readable
            Node variant = node.getNode(node.getName());

            HashMap<String, Object> response = new HashMap<>();

            response.put("jcr:name", node.getName());
            addToResponse(node.getProperties(), response, Collections.emptyList());
            addToResponse(variant, response, ignoredVariantProperties);

            return Response.status(200).entity(response).build();
        } catch (IllegalArgumentException iae) {
            return buildErrorResponse(400, iae);
        } catch (ItemNotFoundException|PathNotFoundException nfe) {
            return buildErrorResponse(404, nfe);
        } catch (RepositoryException re) {
            return buildErrorResponse(500, re);
        }
    }

    private Response buildErrorResponse(int status, Exception exception) {
        return buildErrorResponse(status, exception.toString());
    }

    private Response buildErrorResponse(int status, String description) {
        return Response.status(status).entity(new Error(status, description)).build();
    }

    private SearchService getSearchService() throws RepositoryException {
        final HippoJcrSearchService searchService = new HippoJcrSearchService();
        searchService.setSession(context.getSession());
        return searchService;
    }

    @SuppressWarnings("unchecked")
    private void addToResponse(Node node, HashMap<String, Object> response, List<String> ignoredProperties) throws RepositoryException {
        addToResponse(node.getProperties(), response, ignoredProperties);

        // TODO discuss with Ate
        // Iterate over all nodes and add those to the response.
        // In case of a property and a sub node with the same name, this overwrites the property.
        // In Hippo 10.x and up, it is not possible to create document types through the document type editor that
        // have this type of same-name-siblings. It is possible when creating document types in the console or when
        // upgrading older projects. For now, it is acceptable that in those exceptional situations there is data-loss.
        Iterator<Node> nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            Node childNode = nodeIterator.next();
            HashMap<String, Object> childHashMap = new HashMap<>();
            response.put(childNode.getName(), childHashMap);
            addToResponse(childNode, childHashMap, ignoredProperties);
        }
    }

    private void addToResponse(Iterator<Property> propertyIterator, HashMap<String, Object> response, List<String> ignoredProperties) throws RepositoryException {
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.next();
            boolean ignore = ignoredProperties.contains(property.getName()) || property.getType() == PropertyType.BINARY;
            if (!ignore) {
                if (property.isMultiple()) {
                    Value[] jcrValues = property.getValues();
                    String[] stringValues = new String[jcrValues.length];
                    for (int i = 0; i < jcrValues.length; i++) {
                        stringValues[i] = jcrValues[i].getString();
                    }
                    response.put(property.getName(), stringValues);
                } else {
                    response.put(property.getName(), property.getValue().getString());
                }
            }
        }
    }

}
