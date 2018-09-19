/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.internal.HstMutableRequestContext;

/**
 * InitializationValve
 */
public class InitializationValve extends AbstractBaseOrderableValve {

    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HstMutableRequestContext requestContext = (HstMutableRequestContext)context.getRequestContext();
        // because the requestContext can already have a jcr session (for example fetched during a SiteMapItemHandler or
        // during HstLinkProcessor pre processing, we explicitly set it to null in the HstMutableRequestContext to be
        // sure it gets a new one when requestContext#getSession is called : Namely, it can result in a different
        // jcr session (for example because the SecurityValve kicks in or because of a custom ContextCredentialsProvider
        // which inspects some state being provided by some custom valve
        requestContext.setSession(null);

        // continue
        context.invokeNext();
    }
}
