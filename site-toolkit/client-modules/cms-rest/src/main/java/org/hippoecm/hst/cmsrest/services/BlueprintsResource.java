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

package org.hippoecm.hst.cmsrest.services;

import java.util.List;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.rest.BlueprintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link BlueprintService} for CMS to interact with {@link Blueprint} resources
 */
public class BlueprintsResource extends BaseResource implements BlueprintService {

    private static final Logger log = LoggerFactory.getLogger(BlueprintsResource.class);

    @Override
    public List<Blueprint> getBlueprints() {
        return getVirtualHosts().getBlueprints();
    }

    @Override
    public Blueprint getBlueprint(String id) throws ChannelException {
        final Blueprint blueprint = getVirtualHosts().getBlueprint(id);
        if (blueprint == null) {
            log.warn("Cannot find blueprint of id '{}'", id);
            throw new ChannelException("Cannot find blueprint of id '" + id + "'");
        }
        return blueprint;
    }

}
