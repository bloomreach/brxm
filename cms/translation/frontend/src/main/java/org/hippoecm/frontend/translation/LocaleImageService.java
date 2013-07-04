/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceRequestHandler;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;

public final class LocaleImageService extends AbstractAjaxBehavior {

    private static final long serialVersionUID = 1L;
    
    private ILocaleProvider provider;

    public LocaleImageService(ILocaleProvider provider) {
        this.provider = provider;
    }

    @Override
    public void onRequest() {
        if (provider == null) {
            throw new WicketRuntimeException("No locale provider available");
        }
        RequestCycle rc = RequestCycle.get();
        StringValue language = rc.getRequest().getRequestParameters().getParameterValue("lang");
        ResourceReference resourceRef = provider.getLocale(language.toString()).getIcon(IconSize.TINY, LocaleState.EXISTS);
        rc.scheduleRequestHandlerAfterCurrent(new ResourceRequestHandler(resourceRef.getResource(), null));
    }

}
