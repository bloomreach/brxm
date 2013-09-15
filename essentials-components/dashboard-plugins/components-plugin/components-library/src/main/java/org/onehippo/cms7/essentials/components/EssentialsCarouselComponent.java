/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HST component used for HST menus.
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsCarouselComponent.class)
public class EssentialsCarouselComponent extends CommonComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsCarouselComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        // TODO implement
    }
}
