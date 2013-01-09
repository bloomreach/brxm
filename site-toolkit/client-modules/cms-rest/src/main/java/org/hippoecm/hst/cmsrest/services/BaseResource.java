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

import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class represents functionality common among different RESTful resources
 */
public abstract class BaseResource {

    private static final Logger log = LoggerFactory.getLogger(BaseResource.class);

	protected ChannelManager channelManager;

    /**
     * {@link ChannelManager} setter method
     * 
     * @param channelManager
     */
    public void setChannelManager(final ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

	/**
	 * Validate some constraints before going further with Resource request processing
	 * 
	 * @throws ResourceRequestValidationException When any/all constraint(s) is/are violated
	 */
	protected void validate() throws  ResourceRequestValidationException {
        if (channelManager == null) {
        	log.warn("Cannot look up channels because the channel manager is null");
            throw new ResourceRequestValidationException("Cannot look up channels because the channel manager is null");
        }
	}

	@SuppressWarnings("serial")
	protected class ResourceRequestValidationException extends Exception {

		public ResourceRequestValidationException(String message) {
			super(message);
		}

	}

}
