/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.AbstractHelper;

public class ConfigurationExistsValidator implements Validator {

    private final String id;
    private final String mountId;
    private final AbstractHelper helper;

    public ConfigurationExistsValidator(final String id, final AbstractHelper helper){
        this.id = id;
        this.mountId = null;
        this.helper = helper;
    }

    public ConfigurationExistsValidator(final String id, final String mountId, final AbstractHelper helper){
        this.id = id;
        this.mountId = mountId;
        this.helper = helper;
    }

    @Override
    public void validate(HstRequestContext requestContext) throws RuntimeException {
        if (mountId == null) {
            helper.getConfigObject(id);
        } else {
            helper.getConfigObject(id, requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(mountId));
        }

    }

}
