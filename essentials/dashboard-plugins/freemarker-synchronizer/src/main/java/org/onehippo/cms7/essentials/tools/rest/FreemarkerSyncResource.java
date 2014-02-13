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

package org.onehippo.cms7.essentials.tools.rest;

import java.io.File;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/freemarkersync/")
public class FreemarkerSyncResource extends BaseResource {

    private static final Logger log = LoggerFactory.getLogger(FreemarkerSyncResource.class);

    /**
     * Returns list of all freemarker templates stored on file system
     */
    @GET
    @Path("/")
    public RestfulList<MessageRestful> getTemplateList(@Context ServletContext servletContext) {

        final PluginContext context = getContext(servletContext);
        final RestfulList<MessageRestful> list = new RestfulList<>();
        list.add(new MessageRestful("test"));

        final File freemarkerDirectory = new File((String) context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_SITE_FREEMARKER_ROOT));
        if (!freemarkerDirectory.exists()) {
            return list;
        }
        final Collection<File> files = FileUtils.listFiles(freemarkerDirectory, EssentialConst.FTL_FILTER, true);
        for (File file : files) {
            list.add(new MessageRestful(file.getPath()));
        }
        return list;


    }

    /**
     * Writes nodes to files
     *
     * @param paths   list of XPATH entries
     * @param servletContext
     */

    @POST
    @Path("/file")
    public RestfulList<KeyValueRestful> writeToFileSystem(final RestfulList<KeyValueRestful> paths, @Context ServletContext servletContext) {

        log.info("paths {}", paths.getItems());
        final RestfulList<KeyValueRestful> list = new RestfulList<>();
        list.add(new KeyValueRestful("test", "valyue"));
        return list;
    }


}
