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

package org.hippoecm.hst.jaxrs;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.search.jcr.service.HippoJcrSearchService;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.QueryPersistService;
import org.onehippo.cms7.services.search.service.SearchService;
import org.onehippo.cms7.services.search.service.SearchServiceException;
import org.onehippo.cms7.services.search.service.SearchServiceFactory;

@Produces("application/json")
public class AutoRestApi {

    public interface SearchSessionProvider {
        Session getSession() throws RepositoryException;
    }

    private SearchSessionProvider searchSessionProvider;
    private SearchServiceFactory searchServiceFactory;

    public AutoRestApi() {
        this.searchSessionProvider = new SearchSessionProvider() {
            public Session getSession() throws RepositoryException {
                return RequestContextProvider.get().getSession();
            }
        };

        registerSearchServiceFactory();
    }

    public void setSearchSessionProvider(SearchSessionProvider provider) {
        this.searchSessionProvider = provider;
    }

    /* TODO
     * Refactor the code in SearchService some how to load this factory by simply including a dependency. Advanced
     * search contains a module that initialized this module, but that is part of the enterprise stack, see
     * com.onehippo.cms7.search.service.SearchModule
     */
    private void registerSearchServiceFactory() {
        if (HippoServiceRegistry.getService(SearchServiceFactory.class, Session.class.getName()) == null) {
            searchServiceFactory = new SearchServiceFactory() {
                @Override
                public SearchService createSearchService(final Object clientObject) throws SearchServiceException {
                    if (!(clientObject instanceof Session)) {
                        throw new SearchServiceException("Search service argument must be of type javax.jcr.Session");
                    }

                    final HippoJcrSearchService searchService = new HippoJcrSearchService();
                    searchService.setSession((Session) clientObject);
                    return searchService;
                }

                @Override
                public QueryPersistService createQueryPersistService(final Object clientObject) throws SearchServiceException {
                    if (!(clientObject instanceof Session)) {
                        throw new SearchServiceException("Search service argument must be of type javax.jcr.Session");
                    }

                    final HippoJcrSearchService searchService = new HippoJcrSearchService();
                    searchService.setSession((Session) clientObject);
                    return searchService;
                }
            };

            HippoServiceRegistry.registerService(searchServiceFactory, SearchServiceFactory.class, Session.class.getName());
        }
    }

    private SearchService getSearchService() throws RepositoryException {
        SearchServiceFactory searchServiceFactory = HippoServiceRegistry.getService(SearchServiceFactory.class,
                Session.class.getName());
        if (searchServiceFactory == null) {
            throw new RepositoryException("Cannot get SearchServiceFactory from HippoServiceRegistry");
        }
        return searchServiceFactory.createSearchService(searchSessionProvider.getSession());
    }

    @GET
    @Path("/content/documents")
    public Response getDocuments() {
        try {
            SearchService searchService = getSearchService();
            Query query = searchService.createQuery()
                    .from("/content")
                    .ofType("myhippoproject:basedocument")
                    .returnParentNode()
                    .orderBy(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE)
                    .descending();
            QueryResult result = searchService.search(query);

            // TODO use Jackson here
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            builder.append("  \"offset\": \"0\",");
            builder.append("  \"max\":    \"20\",");
            builder.append("  \"count\":  \"").append(result.getTotalHitCount()).append("\",");
            builder.append("  \"more\":   \"false\",");
            builder.append("  \"total\":  \"").append(result.getTotalHitCount()).append("\"");
            builder.append("}");

            return Response.status(200).entity(builder.toString()).build();
        } catch (RepositoryException e) {
            return Response.status(500).entity(e.toString()).build();
        }
    }

    @GET
    @Path("/content/documents/{uuid}")
    public Response getDocumentsByUUID(@PathParam("uuid") String uuid) {
        return Response.status(500).entity("{\"message\":\"TODO - another TODO is to agree on error response guidelines\"}").build();
    }

    @GET
    @Path("/content/{type}")
    public Response getNews(@PathParam("type") String type) {
        return Response.status(500).entity("{\"message\":\"TODO - another TODO is to agree on error response guidelines\"}").build();
    }

    @GET
    @Path("/content/{type}/{uuid}")
    public Response getNewsByUUID(@PathParam("type") String type, @PathParam("uuid") String uuid) {
        return Response.status(500).entity("{\"message\":\"TODO - another TODO is to agree on error response guidelines\"}").build();
    }

    @GET
    @Path("/types")
    public Response getTypes() {
        return Response.status(500).entity("{\"message\":\"TODO - another TODO is to agree on error response guidelines\"}").build();
    }

    @GET
    @Path("/types/{name}")
    public Response getTypesByName(@PathParam("name") String name) {
        return Response.status(500).entity("{\"message\":\"TODO - another TODO is to agree on error response guidelines\"}").build();
    }

    @GET
    @Path("/content/[{path}]")
    public Response getFolderContentByPath(@PathParam("path") String path) {
        return Response.status(200).entity("{\"method\":\"getContentByPath(" + path + ")\"}").build();
    }

}
