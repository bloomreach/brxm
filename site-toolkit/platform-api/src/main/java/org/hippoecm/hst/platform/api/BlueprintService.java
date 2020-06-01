/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.platform.api;

import java.util.List;

import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;

public interface BlueprintService {

    /**
     * Retrieve a list of all available blueprints for which {@code session} is allowed to create new a new channel
     *
     *
     * @param userSession - the {@link Session} that will use the blueprint
     * @return {@link List} of all available {@link Blueprint}(s) for {@code userSession}, empty list otherwise.
     * Note that a cloned version of the {@link Blueprint} is returned and not the instances available
     * via {@link VirtualHosts#getBlueprints()} to avoid that invoking a setter on a {@link Blueprint} object
     * modifies the backing blueprints from the hst model
     */
	List<Blueprint> getBlueprints(Session userSession);

    /**
     * Retrieve a blue print identified by {@code id}. If the {@code session} is not allowed to create a channel for the
     * {@link Blueprint} belonging to {@code id} or there is not {@link Blueprint} for {@code id}, then {@code null} is
     * returned
     * 
     * @param id - The {@code id} of a {@link Blueprint}
     * @param userSession - the {@link Session} that will use the blueprint
     * @return A {@link Blueprint} object instance identified by {@code id} if available and allowed to be used by
     * {@code userSession}, {@code null} otherwise.
     * Note that a cloned version of the {@link Blueprint} is returned and not an instance of the backing
     * {@link org.hippoecm.hst.platform.model.HstModel} to avoid that invoking a setter on a {@link Blueprint} object
     * modifies the backing blueprint from the hst model
     */
    Blueprint getBlueprint(Session userSession, String id) throws ChannelException;

}
