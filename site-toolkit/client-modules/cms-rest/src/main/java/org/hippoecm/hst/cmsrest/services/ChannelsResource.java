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

import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.MESSAGE_CHANNELS_RETRIEVAL_ERROR;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.MESSAGE_CHANNEL_MANAGER_IS_NULL;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.MESSAGE_CHEANNELS_RESOURCE_REQUEST_PROCESSING_ERROR;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.MESSAGE_HST_LINK_CREATOR_IS_NULL;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.PARAM_MESSAGE_CHEANNELS_RESOURCE_REQUEST_PROCESSING_ERROR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.rest.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ChannelService} for CMS to interact with {@link Channel} resources
 */
public class ChannelsResource implements ChannelService {

    private static final Logger log = LoggerFactory.getLogger(ChannelsResource.class);

    private ChannelManager channelManager;
    private HstLinkCreator hstLinkCreator;

    public void setChannelManager(final ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public void setHstLinkCreator(final HstLinkCreator hstLinkCreator) {
        this.hstLinkCreator = hstLinkCreator;
    }

	@Override
	public List<Channel> getChannels() {
		try {
			// Do required validations and throw @{link ResourceRequestValidationException} if there are violations
			// COMMENT - MNour: We should use a proper validation framework!
			validate();
	        return Collections.unmodifiableList(new ArrayList<Channel>(channelManager.getChannels().values()));
		} catch (ResourceRequestValidationException rrve) {
			if (log.isWarnEnabled()) {
				log.warn(MESSAGE_CHEANNELS_RESOURCE_REQUEST_PROCESSING_ERROR);
			}
			// COMMENT - MNour: This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
			//                  For now return empty list
			// throw rrve;
			return Collections.emptyList();
		} catch (ChannelException ce) {
			if (log.isErrorEnabled()) {
				log.error(MESSAGE_CHANNELS_RETRIEVAL_ERROR);
			}
			// COMMENT - MNour: This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
			//                  For now return empty list
			// throw ce;
			return Collections.emptyList();
		}
	}

	@Override
	public Channel getChannel(String uuid) {
		try {
			// Do required validations and throw @{link ResourceRequestValidationException} if there are violations
			// COMMENT - MNour: We should use a proper validation framework!
			validate();
			// COMMENT - MNour: Channel manager retrieves channels based on path! We need to retrieve them based on UUID(s)/Id(s).
			// return channelManager.getChannel(channelPath);
			// COMMENT - MNour: This is only test data
			return new Channel("this-is-a-test-channel");
		} catch (ResourceRequestValidationException rrve) {
			log.warn(PARAM_MESSAGE_CHEANNELS_RESOURCE_REQUEST_PROCESSING_ERROR, uuid);
			// COMMENT - MNour: This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
			//                  For now return empty list
			// throw rrve;
			// COMMENT - MNour: I know returning 'null' is not clean at all but thats *only* for now!
			return null;
		}
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
	
	
//	protected <KEY_TYPE, VALUE_TYPE> List<VALUE_TYPE> values(Map<KEY_TYPE, VALUE_TYPE> map) {
//		if ((map != null) && (!map.isEmpty())) {
//			List<VALUE_TYPE> values = new ArrayList<VALUE_TYPE>(map.size());
//			for (VALUE_TYPE value : map.values()
//			return values;
//		}
//		return Collections.emptyList();
//	}

	@SuppressWarnings("serial")
	private class ResourceRequestValidationException extends Exception {

		public ResourceRequestValidationException(String message) {
			super(message);
		}

	}

}
