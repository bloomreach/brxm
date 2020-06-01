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

import java.util.List;
import java.util.Map;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.forge.selection.hst.contentbean.ValueList;
import org.onehippo.forge.selection.hst.manager.ValueListManager;
import org.onehippo.forge.selection.hst.util.SelectionUtil;

public class HomeComponent extends BaseHstComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

        super.doBeforeRender(request, response);
        HippoBean doc = request.getRequestContext().getContentBean();

        if(doc != null) {
            request.setAttribute("document", doc);

            ValueListManager valueListManager = HstServices.getComponentManager().getComponent(ValueListManager.class.getName());

            Map<String, String> staticOptions1 = valueListManager.getStaticOptionsMap("staticOptions1");
            request.setAttribute("staticOptions1", staticOptions1);

            Map<String, String> carOptions1 = valueListManager.getStaticOptionsMap("carOptions1");
            request.setAttribute("carOptions1", carOptions1);

            // get the available identifiers (just for demo)
            List<String> valueListIdentifiers = valueListManager.getValueListIdentifiers();
            request.setAttribute("valueListIdentifiers", valueListIdentifiers);

            // api without i18n
            final ValueList valueList = valueListManager.getValueList(request.getRequestContext().getSiteContentBaseBean(), "valuelist1");
            if (valueList != null) {
                request.setAttribute("valueList1Values", SelectionUtil.valueListAsMap(valueList));
            }

            final ValueList continents = valueListManager.getValueList( request.getRequestContext().getSiteContentBaseBean(), "continents");
            if (valueList != null) {
                request.setAttribute("continentValues", SelectionUtil.valueListAsMap(continents));
            }

            final ValueList africaCountries = valueListManager.getValueList( request.getRequestContext().getSiteContentBaseBean(), "africa");
            final ValueList europeCountries = valueListManager.getValueList( request.getRequestContext().getSiteContentBaseBean(), "europe");
            Map<String, String> chained = SelectionUtil.valueListAsMap(africaCountries);
            chained.putAll(SelectionUtil.valueListAsMap(europeCountries));
            if (valueList != null) {
                request.setAttribute("chainedValues", chained);
            }
        }
    }
}
