/*
*  Copyright 2012 Hippo.
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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;

/**
 * JAX-RS service implementation which is responsible for interacting with {@link Channel} resources
 */
@Path("/channels/")
public interface ChannelService {

    /**
     * List all managed channels, identified by their channel IDs
     * 
     * @return {@link List} of {@link Channel} of all available channels, empty list otherwise
     */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Channel> getChannels();

    /**
     * Get a {@link Channel} given it id
     * 
     * @return {@link Channel} which has the given id
     */
    @GET
    @Path("/{id}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Channel getChannel(@PathParam("id") String id);

    /**
     * Save channel properties.  If the URL path of the new channel is not empty, all
     * path-steps except the last one should already map to an existing mount.
     * <p>
     * When invoking this method, an HstSubject context must be provided with the credentials necessary
     * to persist the channel.
     * </p>
     *
     * @param channel the channel to persist
     */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
    public void save(Channel channel);

	/**
	 * Retrieve {@link Channel} property definitions
	 * 
	 * @param id - {@link Channel} id
	 * @return {@link List} of {@link HstPropertyDefinition} for that {@link Channel}
	 */
	@GET
	@Path("/{id}#propdefs")
	@Produces(MediaType.APPLICATION_JSON)
	public List<HstPropertyDefinition> getChannelPropertyDefinitions(@PathParam("id") String id);

	/**
	 * Check whether use can modify {@link Channel}(s) or not
	 * 
	 * @return <code>true</code> if use can modify {@link Channel}, <code>false</code> otherwise
	 */
	@GET
	@Path("/#canUserModifyChannels")
	@Produces(MediaType.APPLICATION_JSON)
	public boolean canUserModifyChannels();

}
