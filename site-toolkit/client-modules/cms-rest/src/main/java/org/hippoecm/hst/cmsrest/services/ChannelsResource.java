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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.rest.ChannelService;
import org.hippoecm.hst.rest.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.rest.beans.HstPropertyDefinitionInfo;
import org.hippoecm.hst.rest.beans.InformationObjectsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ChannelService} for CMS to interact with {@link Channel} resources
 */
public class ChannelsResource extends BaseResource implements ChannelService {

    private static final Logger log = LoggerFactory.getLogger(ChannelsResource.class);

	/* (non-Javadoc)
	 * @see org.hippoecm.hst.rest.ChannelService#getChannels()
	 */
	@Override
	public List<Channel> getChannels() throws ChannelException {
		try {
			// Do required validations and throw @{link ResourceRequestValidationException} if there are violations
			// TODO - We should use a proper validation framework!
			validate();
	        return Collections.unmodifiableList(new ArrayList<Channel>(channelManager.getChannels().values()));
		} catch (ResourceRequestValidationException rrve) {
		    if (log.isDebugEnabled()) {
		        log.warn("Error while processing channels resource request", rrve);
		    } else {
		        log.warn("Error while processing channels resource request - {}", rrve.toString());
		    }

            throw new ChannelException("Validation error while retrieving channels. Details: " + rrve.getMessage()
                    , rrve, ChannelException.Type.SERVER_ERROR);

		} catch (ChannelException ce) {
		    log.warn("Error while retrieving channels - {} : {}", new String[] {ce.getClass().getName(), ce.toString()});
			throw ce;
		}
	}

	/* (non-Javadoc)
     * @see org.hippoecm.hst.rest.ChannelService#save(Channel channel)
     */
    @Override
    public void save(Channel channel) throws ChannelException {
        try {
            // Do required validations and throw @{link ResourceRequestValidationException} if there are violations
            // TODO - We should use a proper validation framework!
            validate();
            channelManager.save(channel);
        } catch (ResourceRequestValidationException rrve) {
            if (log.isDebugEnabled()) {
                log.warn("Error while processing channels resource request for channel '" + channel.getId() + "'", rrve);
            } else {
                log.warn("Error while processing channels resource request for channel '{}' - {}", channel.getId(), rrve.toString());
            }

            throw new ChannelException("Validation error while saving channel with id '" + channel.getId() + "'."
                    + " Details: " + rrve.getMessage()
                    , rrve, ChannelException.Type.SERVER_ERROR);

        } catch (ChannelException ce) {
            log.warn("Error while saving a channel - Channel: {} - {} : {}", new Object[] {channel, ce.getClass().getName(), ce.toString()});
            throw ce;
        }
    }

    @Override
    public String persist(String blueprintId, Channel channel) throws ChannelException {
        try {
            // Do required validations and throw @{link ResourceRequestValidationException} if there are violations
            // TODO - We should use a proper validation framework!
            validate();
            return channelManager.persist(blueprintId, channel);
        } catch (ResourceRequestValidationException rrve) {
            if (log.isDebugEnabled()) {
                log.warn("Error while processing channels resource request for channel '" + channel.getId() + "'", rrve);
            } else {
                log.warn("Error while processing channels resource request for channel '{}' - {}", channel.getId(), rrve.toString());
            }

            throw new ChannelException("Validation error while persisting channel with id '" + channel.getId() + "'"
                    + " using blueprint with id '" + blueprintId + "'."
                    + " Details: " + rrve.getMessage()
                    , rrve, ChannelException.Type.SERVER_ERROR);

        } catch (ChannelException ce) {
            log.warn("Error while persisting a new channel - Channel: {} - {} : {}", new Object[] {channel, ce.getClass().getName(), ce.toString()});
            throw ce;
        }
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.rest.ChannelService#getChannelPropertyDefinitions(String id)
     */
    @Override
    public List<HstPropertyDefinitionInfo> getChannelPropertyDefinitions(String id) {
        return InformationObjectsBuilder.buildHstPropertyDefinitionInfos(channelManager.getPropertyDefinitions(id));
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.rest.ChannelService#getChannel(String id)
     */
    @Override
    public Channel getChannel(String id) throws ChannelException {
        try {
            // Do required validations and throw @{link ResourceRequestValidationException} if there are violations
            // TODO - We should use a proper validation framework!
            validate();
            return channelManager.getChannelById(id);
        } catch (ResourceRequestValidationException rrve) {
            if (log.isDebugEnabled()) {
                log.warn("Error while processing channels resource request of retrieving a channel with id '" + id + "'", rrve);
            } else {
                log.warn("Error while processing channels resource request of retrieving a channel with id,'{}' - {}", id
                        ,  rrve.toString());

            }

            throw new ChannelException("Validation error while retrieving details of channel with id '" + id + "'."
                    + " Details: " + rrve.getMessage()
                    , rrve , ChannelException.Type.SERVER_ERROR);

        } catch (ChannelException ce) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve a channel with id '" + id + "'", ce);
            } else {
                log.warn("Failed to retrieve a channel with id '{}' - {}", id, ce.toString());
            }

            throw ce;
        }
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.rest.ChannelService#canUserModifyChannels()
     */
    @Override
    public boolean canUserModifyChannels() {
        return channelManager.canUserModifyChannels();
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.rest.ChannelService#getChannelInfoClassInfo(String id)
     */
    @Override
    public ChannelInfoClassInfo getChannelInfoClassInfo(String id) throws ChannelException {
        ChannelInfoClassInfo channelInfoClassInfo = null;

        try {
            // Do required validations and throw @{link ResourceRequestValidationException} if there are violations
            // TODO - We should use a proper validation framework!
            validate();
            Class<? extends ChannelInfo> channelInfoClass = channelManager.getChannelInfoClass(id);

            if (channelInfoClass != null) {
                channelInfoClassInfo = InformationObjectsBuilder.buildChannelInfoClassInfo(channelInfoClass);
            }

            return channelInfoClassInfo;
        } catch (ResourceRequestValidationException rrve) {
            if (log.isDebugEnabled()) {
                log.warn("Error while processing channels resource request of retrieving information of channel with id '" + id + "'", rrve);
            } else {
                log.warn("Error while processing channels resource request of retrieving information of channel with id,'{}' - {}", id
                        ,  rrve.toString());

            }

            throw new ChannelException("Validation error while retrieving information of channel with id '" + id + "'."
                    + " Details: " + rrve.getMessage()
                    , rrve , ChannelException.Type.SERVER_ERROR);

        } catch (ChannelException ce) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve channel info class for channel with id '" + id + "'", ce);
            } else {
                log.warn("Failed to retrieve channel info class for channel with id '{}' - {}", id, ce.toString());
            }

            throw ce;
        }
    }

    @Override
    public Properties getChannelResourceValues(String id, String language) throws ChannelException {
        try {
            // Do required validations and throw @{link ResourceRequestValidationException} if there are violations
            // TODO - We should use a proper validation framework!
            validate();
            Channel channel = channelManager.getChannelById(id);
            return InformationObjectsBuilder.buildResourceBundleProperties(channelManager.getResourceBundle(channel, new Locale(language)));
        } catch (ResourceRequestValidationException rrve) {
            if (log.isDebugEnabled()) {
                log.warn("Error while processing channels resource request of retrieving resource values of channel with id '" + id + "'"
                        + " for language '" + language + "'", rrve);

            } else {
                log.warn("Error while processing channels resource request of retrieving resource values of channel with id '{}' for language '{}' - {}"
                        , new String[] {id, language, rrve.toString()});

            }

            throw new ChannelException("Validation error while retrieving resource values of channel with id '" + id + "'"
                    + " for language '" + language + "'. "+ "Details: " + rrve.getMessage()
                    , rrve , ChannelException.Type.SERVER_ERROR);

        } catch (ChannelException ce) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve channel resource values for channel with id '" + id + "'"
                        + " for language '" + language + "'", ce);

            } else {
                log.warn("Failed to retrieve channel resource values for channel with id '{}' for language '{}' - {}"
                        , new String[] {id, language, ce.toString()});
            }

            throw ce;
        }
    }

}
