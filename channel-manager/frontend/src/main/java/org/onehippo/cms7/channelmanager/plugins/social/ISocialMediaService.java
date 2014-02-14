/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.plugins.social;

import java.util.List;

import org.apache.wicket.util.io.IClusterable;

/**
 * Provides access to social media.
 */
public interface ISocialMediaService extends IClusterable {

    public static final String DEFAULT_SERVICE_ID = "default.social.media.service";

    /**
     * @return all social media supported by this service.
     */
    public List<ISocialMedium> getAllSocialMedia();

}
