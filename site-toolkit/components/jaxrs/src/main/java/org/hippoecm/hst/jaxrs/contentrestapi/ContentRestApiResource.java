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

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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
    }

    private final class SearchResultItem {
        public String name;
        public String uuid;
        public Link[] links;
    }

    private final class SearchResult {
        public long offset;
        public long max;
        public long count;
        public boolean more;
        public long total;
        public SearchResultItem[] items;

        void initialize(QueryResult queryResult, Session session) throws RepositoryException {
            offset = 0;
            max = 100;
            count = queryResult.getTotalHitCount();
            more = false;
            total = queryResult.getTotalHitCount();

            int count = (int) queryResult.getTotalHitCount();
            int current = 0;
            items = new SearchResultItem[count];
            HitIterator iterator = queryResult.getHits();
            while (iterator.hasNext()) {
                Hit hit = iterator.nextHit();
                items[current] = new SearchResultItem();
                String uuid = hit.getSearchDocument().getContentId().toIdentifier();
                Node node = session.getNodeByIdentifier(uuid);
                items[current].name = node.getName();
                items[current].uuid = uuid;
                items[current].links = new Link[1];
                items[current].links[0] = new Link();
                items[current].links[0].url = uuid;
                current++;
            }
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

    @GET
    @Path("/content/documents")
    public Response getDocuments() {
        try {
            SearchService searchService = getSearchService();
            Query query = searchService.createQuery()
                    .from("/content/documents")
                    .ofType("myhippoproject:basedocument") // TODO change to blacklisting mechanism
                    .returnParentNode()
                    .orderBy(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE)
                    .descending();
            QueryResult queryResult = searchService.search(query);
            SearchResult returnValue = new SearchResult();
            returnValue.initialize(queryResult, context.getSession());

            return Response.status(200).entity(returnValue).build();
        } catch (RepositoryException e) {
            return buildErrorResponse(500, e);
        }
    }

    @GET
    @Path("/content/documents/{uuid}")
    public Response getDocumentsByUUID(@PathParam("uuid") String uuid) {
        try {
            Session session = context.getSession();

            // check uuid validity, if not valid this throws an IllegalArgumentException
            UUID.fromString(uuid);

            // this could throw an ItemNotFoundException
            Node node = session.getNodeByIdentifier(uuid);

            HashMap<String, Object> hashMap = new HashMap<>();

            nodeToHashMap(node, hashMap);

            return Response.status(200).entity(hashMap).build();
        } catch (IllegalArgumentException iae) {
            return buildErrorResponse(400, iae, "The string '" + uuid + "' is not a valid UUID.");
        } catch (ItemNotFoundException infe) {
            return buildErrorResponse(404, infe);
        } catch (RepositoryException re) {
            return buildErrorResponse(500, re);
        }
    }

    private Response buildErrorResponse(int status, Exception exception) {
        return buildErrorResponse(status, exception, null);
    }

    private Response buildErrorResponse(int status, Exception exception, String description) {
        Error error;
        if (description == null) {
            error = new Error(status, exception.toString());
        } else {
            error = new Error(status, description);
        }
        return Response.status(status).entity(error).build();
    }

    private SearchService getSearchService() throws RepositoryException {
        final HippoJcrSearchService searchService = new HippoJcrSearchService();
        searchService.setSession(context.getSession());
        return searchService;
    }

    @SuppressWarnings("unchecked")
    private void nodeToHashMap(Node node, HashMap<String, Object> hashMap) throws RepositoryException {
        // Iterate over all properties and add those to the hashMap.
        Iterator<Property> propertyIterator = node.getProperties();
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.next();
            if (property.isMultiple()) {
                hashMap.put(property.getName(), new Object[0]); //property.getValues());
            } else {
                hashMap.put(property.getName(), "prop"); //property.getValue());
            }
        }

        // TODO discuss with Ate
        // Iterate over all nodes and add those to the hashMap.
        // In case of a property and a sub node with the same name, this overwrites the property.
        // In Hippo 10.x and up, it is not possible to create document types through the document type editor that
        // have this type of same-name-siblings. It is possible when creating document types in the console or when
        // upgrading older projects. For now, it is acceptable that in those exceptional situations there is data-loss.
        Iterator<Node> nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            Node childNode = nodeIterator.next();
            HashMap<String, Object> childHashMap = new HashMap<>();
            hashMap.put(childNode.getName(), childHashMap);
            nodeToHashMap(childNode, childHashMap);
        }
    }

}
