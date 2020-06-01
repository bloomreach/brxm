/*
 * Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.poll.component;

import javax.jcr.RepositoryException;
import javax.servlet.http.Cookie;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.annotations.Persistable;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;

/**
 * Standard HST poll component that has a poll folder picker as parameter.
 */
@ParametersInfo(type = PollComponentInfo.class)
public class PollComponent extends BaseHstComponent {

    private final PollProvider pollProvider = new PollProvider();

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response)
            throws HstComponentException {
        super.doBeforeRender(request, response);
        // set a cookie that is totally not important but is needed to avoid that IF a visitor does not yet have
        // any cookie, that org.onehippo.forge.poll.component.PollProvider.getPersistentValue() returns
        // org.onehippo.forge.poll.component.PollProvider.NO_COOKIE_SUPPORT
        response.addCookie(new Cookie("cookiesAllowed", "true"));
        PollComponentInfo pollComponentInfo = getComponentParametersInfo(request);
        pollProvider.doBeforeRender(request, response, pollComponentInfo);
    }

    @Persistable
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        try {
            PollComponentInfo pollComponentInfo = getComponentParametersInfo(request);
            pollProvider.doAction(request, response, pollComponentInfo);
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        }
    }

}
