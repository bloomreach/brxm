/*
 *  Copyright 2020 Bloomreach
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.state;

import javax.jcr.RepositoryException;

public class StateServiceImpl implements StateService {

    private final StateProviderContextFactory contextFactory;

    public StateServiceImpl(final StateProviderContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    @Override
    public XPageState getXPageState(final StateContext stateContext) throws RepositoryException {
        final StateProviderContext context = contextFactory.make(stateContext);

        if (!context.isExperiencePageRequest()) {
            return null;
        }

        final XPageStateContext xPageStateContext = context.getXPageStateContext();

        final XPageState xPageState = new XPageState();
        xPageState.setBranchId(xPageStateContext.getBranchId());
        xPageState.setId(xPageStateContext.getXPageId());
        xPageState.setName(xPageStateContext.getXPageName());
        xPageState.setState(xPageStateContext.getXPageState());
        xPageState.setWorkflowRequest(xPageStateContext.getWorkflowRequest());
        xPageState.setScheduledRequest(xPageStateContext.getScheduledRequest());

        return xPageState;
    }
}
