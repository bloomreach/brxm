/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

package {{componentsPackage}};

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.forge.selection.hst.contentbean.ValueList;
import org.onehippo.forge.selection.hst.manager.ValueListManager;
import org.onehippo.forge.selection.hst.util.SelectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultilingualComponent extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(MultilingualComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

        super.doBeforeRender(request, response);
        HippoBean doc = request.getRequestContext().getContentBean();

        if(doc != null) {
            request.setAttribute("document", doc);

            ValueListManager valueListManager = HstServices.getComponentManager().getComponent(ValueListManager.class.getName());

            // api with locale, for multilingual
            final ValueList multilingualValueList = valueListManager.getValueList(request.getRequestContext().getSiteContentBaseBean(), "valuelistMultilingual", request.getLocale());
            if (multilingualValueList != null) {
                request.setAttribute("multilingualValues", SelectionUtil.valueListAsMap(multilingualValueList));
            }
        }
    }

}
