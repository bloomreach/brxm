/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

import com.google.common.base.Strings;

/**
 * Component that does a redirect on the response or on the component,
 * based on the component parameters {@code 'type'} and {@code 'redirect'}.
 *
 * @version $Id$
 */
public class EssentialsRedirectComponent extends CommonComponent {

    public static final String COMPONENT_PARAM_REDIRECT = "redirect";
    public static final String COMPONENT_PARAM_TYPE = "type";

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {

        final String redirect = getComponentParameter(COMPONENT_PARAM_REDIRECT);

        if (Strings.isNullOrEmpty(redirect)) {
            throw new HstComponentException("Parameter '" + COMPONENT_PARAM_REDIRECT + "' is required for " + this.getClass().getName());
        }

        final String typeStr = getComponentParameter(COMPONENT_PARAM_TYPE);
        if (!Strings.isNullOrEmpty(typeStr)) {
            final RedirectType type = RedirectType.valueOf(typeStr);
            switch (type) {
                case response:
                    try {
                        response.sendRedirect(redirect);
                    } catch (IOException e) {
                        throw new HstComponentException("Failed to redirect to " + redirect, e);
                    }
                    break;
                case component:
                    this.sendRedirect(redirect, request, response);
            }
        }
        this.sendRedirect(redirect, request, response);
    }

    public enum RedirectType {
        component, response
    }
}
