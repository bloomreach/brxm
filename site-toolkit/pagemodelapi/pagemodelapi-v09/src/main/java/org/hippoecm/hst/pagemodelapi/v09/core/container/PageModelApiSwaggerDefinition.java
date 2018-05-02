/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.pagemodelapi.v09.core.container;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;

/**
 * Page Model JSON API Swagger Definition.
 * <p>
 * Note: As Swagger UI doesn't and won't support a path parameter input value containing slash(es) without escaping,
 *       this API definition includes some additional helper operations which allow developers to test with up to
 *       7 path segments for the relative path info path parameter in Swagger UI.
 *       ref) https://github.com/OAI/OpenAPI-Specification/issues/892
 * </p>
 */
@SwaggerDefinition(
        info = @Info(
                description = "HST Page Model JSON API",
                version = "v0.9",
                title = "HST Page Model JSON API"),
        produces = { "application/json" },
        schemes = { SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS },
        externalDocs = @ExternalDocs(value = "Hippo CMS", url = "https://www.onehippo.org/")
)
@Path("/")
@Api(
        value = "/",
        produces = "application/json",
        protocols = "http,https",
        tags = { "Page Model JSON API" }
)
public interface PageModelApiSwaggerDefinition {

    String GET_AGGREGATED_PAGE_MODEL_DESC = "Retrieve a aggregated page model for the resolved SiteMapItem.";

    String GET_AGGREGATED_PAGE_MODEL_HELPER_DESC = "Helper to retrieve a aggregated page model for the resolved SiteMapItem.";

    String API_HELPER_OPERATION_TAG = "API Helpers";

    String PATH_INFO_SEGMENT_DESC = "A path segment in the pathInfo to be used in HST SiteMapItem matching";

    String PATH_SEGMENT_PARAM_NAME = "pathSegment";

    String PATH_SEGMENT_PARAM_NAME_1 = PATH_SEGMENT_PARAM_NAME + "1";

    String PATH_SEGMENT_PARAM_NAME_2 = PATH_SEGMENT_PARAM_NAME + "2";

    String PATH_SEGMENT_PARAM_NAME_3 = PATH_SEGMENT_PARAM_NAME + "3";

    String PATH_SEGMENT_PARAM_NAME_4 = PATH_SEGMENT_PARAM_NAME + "4";

    String PATH_SEGMENT_PARAM_NAME_5 = PATH_SEGMENT_PARAM_NAME + "5";

    String PATH_SEGMENT_PARAM_NAME_6 = PATH_SEGMENT_PARAM_NAME + "6";

    String PATH_SEGMENT_PARAM_NAME_7 = PATH_SEGMENT_PARAM_NAME + "7";

    String PATH_PARAM_PATH_INFO = "/{pathInfo: .+}";

    String PATH_PARAM_WITH_0_PATH_SEGMENT = "/";

    String PATH_PARAM_WITH_1_PATH_SEGMENT = "/{pathSegment1}";

    String PATH_PARAM_WITH_2_PATH_SEGMENTS = "/{pathSegment1}/{pathSegment2}";

    String PATH_PARAM_WITH_3_PATH_SEGMENTS = "/{pathSegment1}/{pathSegment2}/{pathSegment3}";

    String PATH_PARAM_WITH_4_PATH_SEGMENTS = "/{pathSegment1}/{pathSegment2}/{pathSegment3}/{pathSegment4}";

    String PATH_PARAM_WITH_5_PATH_SEGMENTS = "/{pathSegment1}/{pathSegment2}/{pathSegment3}/{pathSegment4}/{pathSegment5}";

    String PATH_PARAM_WITH_6_PATH_SEGMENTS = "/{pathSegment1}/{pathSegment2}/{pathSegment3}/{pathSegment4}/{pathSegment5}/{pathSegment6}";

    String PATH_PARAM_WITH_7_PATH_SEGMENTS = "/{pathSegment1}/{pathSegment2}/{pathSegment3}/{pathSegment4}/{pathSegment5}/{pathSegment6}/{pathSegment7}";

    @GET
    @Path(PATH_PARAM_PATH_INFO)
    @ApiOperation(value=GET_AGGREGATED_PAGE_MODEL_DESC, httpMethod=HttpMethod.GET, response=AggregatedPageModel.class)
    AggregatedPageModel getAggregatedPageModelByPathInfo(
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam("pathInfo") String pathInfo);

    @GET
    @Path(PATH_PARAM_WITH_0_PATH_SEGMENT)
    @ApiOperation(value=GET_AGGREGATED_PAGE_MODEL_HELPER_DESC, tags={ API_HELPER_OPERATION_TAG }, httpMethod=HttpMethod.GET, response=AggregatedPageModel.class)
    AggregatedPageModel getAggregatedPageModel();

    @GET
    @Path(PATH_PARAM_WITH_1_PATH_SEGMENT)
    @ApiOperation(value=GET_AGGREGATED_PAGE_MODEL_HELPER_DESC, tags={ API_HELPER_OPERATION_TAG }, httpMethod=HttpMethod.GET, response=AggregatedPageModel.class)
    AggregatedPageModel getAggregatedPageModel(
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_1) String pathSegment1);

    @GET
    @Path(PATH_PARAM_WITH_2_PATH_SEGMENTS)
    @ApiOperation(value=GET_AGGREGATED_PAGE_MODEL_HELPER_DESC, tags={ API_HELPER_OPERATION_TAG }, httpMethod=HttpMethod.GET, response=AggregatedPageModel.class)
    AggregatedPageModel getAggregatedPageModel(
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_1) String pathSegment1,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_2) String pathSegment2);

    @GET
    @Path(PATH_PARAM_WITH_3_PATH_SEGMENTS)
    @ApiOperation(value=GET_AGGREGATED_PAGE_MODEL_HELPER_DESC, tags={ API_HELPER_OPERATION_TAG }, httpMethod=HttpMethod.GET, response=AggregatedPageModel.class)
    AggregatedPageModel getAggregatedPageModel(
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_1) String pathSegment1,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_2) String pathSegment2,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_3) String pathSegment3);

    @GET
    @Path(PATH_PARAM_WITH_4_PATH_SEGMENTS)
    @ApiOperation(value=GET_AGGREGATED_PAGE_MODEL_HELPER_DESC, tags={ API_HELPER_OPERATION_TAG }, httpMethod=HttpMethod.GET, response=AggregatedPageModel.class)
    AggregatedPageModel getAggregatedPageModel(
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_1) String pathSegment1,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_2) String pathSegment2,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_3) String pathSegment3,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_4) String pathSegment4);

    @GET
    @Path(PATH_PARAM_WITH_5_PATH_SEGMENTS)
    @ApiOperation(value=GET_AGGREGATED_PAGE_MODEL_HELPER_DESC, tags={ API_HELPER_OPERATION_TAG }, httpMethod=HttpMethod.GET, response=AggregatedPageModel.class)
    AggregatedPageModel getAggregatedPageModel(
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_1) String pathSegment1,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_2) String pathSegment2,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_3) String pathSegment3,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_4) String pathSegment4,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_5) String pathSegment5);

    @GET
    @Path(PATH_PARAM_WITH_6_PATH_SEGMENTS)
    @ApiOperation(value=GET_AGGREGATED_PAGE_MODEL_HELPER_DESC, tags={ API_HELPER_OPERATION_TAG }, httpMethod=HttpMethod.GET, response=AggregatedPageModel.class)
    AggregatedPageModel getAggregatedPageModel(
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_1) String pathSegment1,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_2) String pathSegment2,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_3) String pathSegment3,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_4) String pathSegment4,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_5) String pathSegment5,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_6) String pathSegment6);

    @GET
    @Path(PATH_PARAM_WITH_7_PATH_SEGMENTS)
    @ApiOperation(value=GET_AGGREGATED_PAGE_MODEL_HELPER_DESC, tags={ API_HELPER_OPERATION_TAG }, httpMethod=HttpMethod.GET, response=AggregatedPageModel.class)
    AggregatedPageModel getAggregatedPageModel(
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_1) String pathSegment1,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_2) String pathSegment2,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_3) String pathSegment3,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_4) String pathSegment4,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_5) String pathSegment5,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_6) String pathSegment6,
            @ApiParam(PATH_INFO_SEGMENT_DESC) @PathParam(PATH_SEGMENT_PARAM_NAME_7) String pathSegment7);

}
