/*
*  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.rest;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.rest.beans.ChannelDataset;
import org.onehippo.cms7.services.hst.Channel;

/**
 * JAX-RS service implementation which is responsible for interacting with {@link Channel} resources
 */
@Path("/channels/")
public interface ChannelService {

    /**
     * List all managed channels, identified by their channel IDs
     * 
     * @return {@link List} of {@link Channel}s of all available channels, empty list otherwise. Note that if for
     * a {@link Channel} there is both a live <b>and</b> preview version, the <b>preview</b> version is returned as
     * that is typically the version that is needed to work with through this {@link ChannelService}
     */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public ChannelDataset getChannels();

	/**
	 * Persist a new {@link Channel} object instance based on {@link Blueprint} identified by an Id
	 * 
	 * @param blueprintId - The {@link Blueprint} id
	 * @param channel - {@link Channel} object instance
	 * @return The new {@link Channel}'s id
	 */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String persist(@QueryParam("blueprint") String blueprintId, Channel channel) throws ChannelException;

	/**
	 * Check whether use can modify {@link Channel}(s) or not
	 * 
	 * @return <code>true</code> if use can modify {@link Channel}, <code>false</code> otherwise
	 */
	@GET
	@Path("/#canUserModifyChannels")
	@Produces(MediaType.APPLICATION_JSON)
	public boolean canUserModifyChannels();

    /**
     * Retrieve a {@link ResourceBundle} converted to {@link Properties} of {@link Channel} identified by an Id
     * 
     * @param id - {@link Channel} id
     * @param language - {@link Locale} language
     * @return {@link Properties} equivalent of a {@link Channel}'s {@link ResourceBundle}
     */
	@GET
    @Path("/{id}#resourcevalue")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Properties getChannelResourceValues(@PathParam("id") String id, @QueryParam("language") String language) throws ChannelException;

}
