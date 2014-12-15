/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Session;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;

/**
 * Standard HST poll component.
 */
@ParametersInfo(type = PollComponentInfo.class)
public class PollComponent extends BaseHstComponent {

    private final PollProvider pollProvider = new PollProvider(this);
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response)
            throws HstComponentException {      
        super.doBeforeRender(request, response);

        pollProvider.doBeforeRender(request, response);
    }
    
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        Session persistableSession = null;
        try {
            persistableSession = getPersistableSession(request);
            pollProvider.doAction(request, response, persistableSession);
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        } finally {
            if (persistableSession != null) {
                persistableSession.logout();
            }
        }
    }

    /**
     * In order to obtain the benefits from the parameterInfo annotation (i.e. the parameter configuration pop-up
     * in the CMS), we override the getParameter function. Parameter names are kept internal to the PollProvider.
     *
     * @param name    the name of the parameter
     * @param request the HST request, representing the poll component instance (including parameters) of interest.
     * @return        the value of the parameter
     * @deprecated  use PollComponentInfo pi = getComponentParametersInfo(request) instead and then pi.getPollDataPath(), etc
     */
    @Deprecated
    @Override
    public String getParameter(String name, HstRequest request) {
        return PollProvider.getParameter(name, (PollComponentInfo)getComponentParametersInfo(request));
    }

}
