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

import java.util.ArrayList;
import java.util.Collections;
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

	/* (non-Javadoc)
	 * @see org.hippoecm.hst.rest.BlueprintService#getBlueprints()
	 */
	@Override
	public List<Blueprint> getBlueprints() {
		try {
			// Do required validations and throw @{link ResourceRequestValidationException} if there are violations
			// We should use a proper validation framework!
			validate();
	        return Collections.unmodifiableList(new ArrayList<Blueprint>(channelManager.getBlueprints()));
		} catch (ResourceRequestValidationException rrve) {
		    if (log.isDebugEnabled()) {
		        log.warn("Error while processing blueprints resource request", rrve);
		    } else {
		        log.warn("Error while processing blueprints resource request - {}", rrve.toString());
		    }
			// This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
			// For now return empty list
			// throw rrve;
			return Collections.emptyList();
		} catch (ChannelException ce) {
		    log.warn("Error while retrieving blueprints");
			// This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
			// For now return empty list
			// throw ce;
			return Collections.emptyList();
		}
	}

	/* (non-Javadoc)
	 * @see org.hippoecm.hst.rest.BlueprintService#getBlueprint(java.lang.String)
	 */
	@Override
	public Blueprint getBlueprint(String id) {
		try {
			// Do required validations and throw @{link ResourceRequestValidationException} if there are violations
			// We should use a proper validation framework!
			validate();
			return channelManager.getBlueprint(id);
		} catch (ResourceRequestValidationException rrve) {
		    if (log.isDebugEnabled()) {
		        log.warn("Error while processing blueprints resource request for blueprint '" + id + "'", rrve);
		    } else {
		        log.warn("Error while processing blueprints resource request for blueprint '{}' - {}", id, rrve.toString());
		    }
			// This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
			// For now return empty list
			// throw rrve;
			// I know returning 'null' is not clean at all but thats *only* for now!
			return null;
		} catch (ChannelException ce) {
		    log.error("Exception while retrieving blueprint of id '{}' : {} : {}", new String[] {id, ce.getClass().getName(), ce.toString()});
		    // - I know returning 'null' is not clean at all but thats *only* for now!
		    // - Also see the above comment
		    // throw ce
		    return null;
        }
	}

}
