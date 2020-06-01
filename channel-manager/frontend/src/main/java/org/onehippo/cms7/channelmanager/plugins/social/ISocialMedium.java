/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.plugins.social;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * Provides access to a social medium.
 */
public interface ISocialMedium extends IClusterable, Comparable<ISocialMedium> {

    /**
     * @return the name of this social medium as displayed in a frontend (including the right capitalization etc.)
     */
    public String getDisplayName();

    /**
     * Shares a URL on this social medium.
     *
     * @param url the URL to share
     */
    public void shareUrl(String url);

    /**
     * Returns an icon for this social medium. The icon's size is 16x16 pixels.
     *
     * @return a reference to the icon
     */
    public ResourceReference getIcon16();

}
