/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.intranet.components;

import org.hippoecm.hst.intranet.componentsinfo.ListViewInfo;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ParametersInfo(type = ListViewInfo.class)
public class ListView extends BaseComponent {

    public static final Logger log = LoggerFactory.getLogger(ListView.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {

       ListViewInfo info = getComponentParametersInfo(request);
       HippoBean scopeBean = null;

       String scope = info.getScope();
       if (scope == null) {
           throw new HstComponentException("Scope is not allowed to be null for a List component. Cannot create a list");
       }

       scopeBean = request.getRequestContext().getSiteContentBaseBean();
       if ("".equals(scope) || "/".equals(scope)) {
           // the scope is the root content bean of this site, scopeBean is already ok.
       } else {
           // strip leading and trailing slashes
           scope = PathUtils.normalizePath(scope);
           scopeBean = scopeBean.getBean(scope);
           if(scopeBean == null) {
               throw new HstComponentException("Scope '" + scope
                       + "' does not point to a bean for Mount with content path '"
                       + request.getRequestContext().getResolvedMount().getMount().getContentPath()
                       + "'. Cannot create a list");
           }
       }

       if (scope == null) {
           response.setStatus(404);
           throw new HstComponentException("For an Overview component there must be a content bean available to search below. Cannot create an overview");
       }
       createAndExecuteSearch(request, info, scopeBean, null);
    }

}
