/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.util;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

public class InitializationPayload {

    private Map<String,Serializable> cmsSessionContextPayload;

    @SuppressWarnings({"unchecked"})
    public InitializationPayload() {
        cmsSessionContextPayload = Collections.emptyMap();
        final RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle==null){
            return;
        }
        final ServletWebRequest request = (ServletWebRequest) requestCycle.getRequest();
        final HttpServletRequest containerRequest = request.getContainerRequest();
        final HttpSession session = containerRequest.getSession();
        final CmsSessionContext context = CmsSessionContext.getContext(session);
        final Object attribute = context.getAttribute(CmsSessionContext.CMS_SESSION_CONTEXT_PAYLOAD_KEY);
        if (attribute!=null){
            cmsSessionContextPayload = (Map<String, Serializable>) attribute;
        }
    }

    public static Map<String,Serializable> get(){
        return new InitializationPayload().cmsSessionContextPayload;
    }
}
