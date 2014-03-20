/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.restservices;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.packaging.PowerpackPackage;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/restservices")
public class RestPluginResource extends BaseResource {


    @GET
    @Path("/beans")
    public RestfulList<KeyValueRestful> getHippoBeans(@Context ServletContext servletContext) {

        final RestfulList<KeyValueRestful> list = new RestfulList<>();
        final Map<String, java.nio.file.Path> hippoBeans = BeanWriterUtils.mapExitingBeanNames(getContext(servletContext), "java");
        for (Map.Entry<String, java.nio.file.Path> bean : hippoBeans.entrySet()) {
            list.add(new KeyValueRestful(bean.getKey(), bean.getValue().toString()));
        }
        return list;
    }

    /**
     * Executes REST powerpack
     */
    @POST
    @Path("/")
    public MessageRestful executePowerpack(final PostPayloadRestful payloadRestful, @Context ServletContext servletContext) {


        final MessageRestful message = new MessageRestful();

        final Map<String, String> values = payloadRestful.getValues();
        final String restName = values.get(RestPluginConst.REST_NAME);
        final String restType = values.get(RestPluginConst.REST_TYPE);
        if (Strings.isNullOrEmpty(restName) || Strings.isNullOrEmpty(restType)) {
            return new ErrorMessageRestful("REST service name / type or both were empty");
        }
        final PluginContext context = getContext(servletContext);

        final PowerpackPackage powerpack = new RestServicesPowerpack();
        // TODO: figure out injection part
        getInjector().autowireBean(powerpack);
        powerpack.setProperties(new HashMap<String, Object>(values));
        powerpack.execute(context);
        message.setValue("Please rebuild and restart your application");
        return message;
    }

}
