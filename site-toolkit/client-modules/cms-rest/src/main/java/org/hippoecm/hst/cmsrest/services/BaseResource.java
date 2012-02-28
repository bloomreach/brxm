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

import static org.hippoecm.hst.cmsrest.services.BaseResourceConsts.MESSAGE_CHANNEL_MANAGER_IS_NULL;
import static org.hippoecm.hst.cmsrest.services.BaseResourceConsts.MESSAGE_HST_LINK_CREATOR_IS_NULL;

import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class represents functionality common among different RESTful resources
 */
public abstract class BaseResource {

    private static final Logger log = LoggerFactory.getLogger(BaseResource.class);

	protected ChannelManager channelManager;
    protected HstLinkCreator hstLinkCreator;

    /**
     * {@link ChannelManager} setter method
     * 
     * @param channelManager
     */
    public void setChannelManager(final ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    /**
     * {@link HstLinkCreator
     * 
     * @param hstLinkCreator
     */
    public void setHstLinkCreator(final HstLinkCreator hstLinkCreator) {
        this.hstLinkCreator = hstLinkCreator;
    }

	/**
	 * Validate some constraints before going further with Resource request processing
	 * 
	 * @throws ResourceRequestValidationException When any/all constraint(s) is/are violated
	 */
	protected void validate() throws  ResourceRequestValidationException {
        if (channelManager == null) {
        	log.warn(MESSAGE_CHANNEL_MANAGER_IS_NULL);
            throw new ResourceRequestValidationException(MESSAGE_CHANNEL_MANAGER_IS_NULL);
        }

        if (hstLinkCreator == null) {
            log.warn(MESSAGE_HST_LINK_CREATOR_IS_NULL);
            throw new ResourceRequestValidationException(MESSAGE_HST_LINK_CREATOR_IS_NULL);
        }
	}

	@SuppressWarnings("serial")
	protected class ResourceRequestValidationException extends Exception {

		public ResourceRequestValidationException(String message) {
			super(message);
		}

	}

}
