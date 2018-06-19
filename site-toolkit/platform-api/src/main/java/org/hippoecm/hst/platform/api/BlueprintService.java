/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;

public interface BlueprintService {

    /**
     * Retrieve a list of all available blueprints
     * 
     * @return {@link List} of all available {@link Blueprint}(s), empty list otherwise
     */
	List<Blueprint> getBlueprints();

    /**
     * Retrieve a blue print identified by an Id
     * 
     * @param id - The <code>id</code> of a {@link Blueprint}
     * @return A {@link Blueprint} object instance identified by <code>id</code> if available, <code>null</code> otherwise
     */
    Blueprint getBlueprint(String id) throws ChannelException;

}
