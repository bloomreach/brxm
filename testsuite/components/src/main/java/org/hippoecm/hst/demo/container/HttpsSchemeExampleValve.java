/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.container;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.AbstractHttpsSchemeValve;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;

public class HttpsSchemeExampleValve extends AbstractHttpsSchemeValve {

    @Override
    public boolean requiresHttps(final ValveContext context) {
        final HippoBean contentBean = context.getRequestContext().getContentBean();
        if (contentBean == null ) {
            return false;
        }
        try {
            return contentBean.getNode().isNodeType("demosite:secure");
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    @Override
    protected int getRedirectStatusCode() {
        return 302;
    }
}
