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

package org.hippoecm.hst.cmsrest.services;

import static org.hippoecm.hst.cmsrest.services.BlueprintsResourceConsts.MESSAGE_BLUEPRINTS_RESOURCE_REQUEST_PROCESSING_ERROR;
import static org.hippoecm.hst.cmsrest.services.BlueprintsResourceConsts.MESSAGE_BLUEPRINTS_RETRIEVAL_ERROR;
import static org.hippoecm.hst.cmsrest.services.BlueprintsResourceConsts.PARAM_MESSAGE_BLUEPRINTS_RESOURCE_REQUEST_PROCESSING_ERROR;

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
			if (log.isWarnEnabled()) {
				log.warn(MESSAGE_BLUEPRINTS_RESOURCE_REQUEST_PROCESSING_ERROR);
			}
			// This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
			// For now return empty list
			// throw rrve;
			return Collections.emptyList();
		} catch (ChannelException ce) {
			if (log.isErrorEnabled()) {
				log.error(MESSAGE_BLUEPRINTS_RETRIEVAL_ERROR);
			}
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
		    if (log.isWarnEnabled()) {
		        log.warn(PARAM_MESSAGE_BLUEPRINTS_RESOURCE_REQUEST_PROCESSING_ERROR, id);
		    }
			// This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
			// For now return empty list
			// throw rrve;
			// I know returning 'null' is not clean at all but thats *only* for now!
			return null;
		} catch (ChannelException ce) {
		    if (log.isErrorEnabled()) {
		        log.error("Exception while retrieving blueprint of id '" + id + "' : " + ce.getClass().getName() + " : " + ce.getMessage() + " : " + ce);
		    }
		    // - I know returning 'null' is not clean at all but thats *only* for now!
		    // - Also see the above comment
		    // throw ce
		    return null;
        }
	}

}
