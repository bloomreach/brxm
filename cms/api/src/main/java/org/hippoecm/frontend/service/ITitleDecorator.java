/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service;

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Title decorator.  This service can be added to another service to provide (optional)
 * presentational information to consumers.
 * <p>
 * Implementations should register it at the other service's id.  {@link IServiceReference#getServiceId}
 * Consumers can find it then at the same location.
 */
public interface ITitleDecorator extends IClusterable {

    static final Logger log = LoggerFactory.getLogger(ITitleDecorator.class);

    IModel<String> getTitle();

    /**
     * Retrieve an icon to represent the decorated object.  Implementations should return null
     * when no icon is available.  When no icon is available of the specified size, a larger
     * sized icon can be returned.
     * <p>
     * Consumers should use a default icon when none is returned.  They should handle resizing
     * icons when these are not of the specified size.
     * 
     * @param type
     * @return
     */
    ResourceReference getIcon(IconSize type);

}
