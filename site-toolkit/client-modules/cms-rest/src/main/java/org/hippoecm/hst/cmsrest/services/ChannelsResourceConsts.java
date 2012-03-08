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

/**
 * Constants class used by {@link ChannelsResource}
 */
public class ChannelsResourceConsts {

	public static final String MESSAGE_CHEANNELS_RESOURCE_REQUEST_PROCESSING_ERROR = "Error while processing channels resource request";
	public static final String MESSAGE_CHANNELS_RETRIEVAL_ERROR = "Error while retrieving channels - %s : %s : %s";
	public static final String MESSAGE_CHANNEL_SAVING_ERROR = "Error while saving a channel - Channel: %s - %s : %s : %s";
	public static final String PARAM_MESSAGE_ERROR_WHILE_CREATING_CHANNEL = "Error while creating channel: {}";
	public static final String PARAM_MESSAGE_CHANNELS_RESOURCE_REQUEST_PROCESSING_ERROR = "Error while processing channels resource request for channel '{}'";
	public static final String WARNING_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL = "Failed to retrieve a channel with id '%s'";
	public static final String WARNING_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL_INFO_CLASS = "Failed to retrieve channel info class for channel with id '%s'";
	public static final String PARAM_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL = "Failed to retrieve a channel with id '{}' - {}";
	public static final String PARAM_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL_INFO_CLASS = "Failed to retrieve channel info class for channel with id '{}' - {}";

}
