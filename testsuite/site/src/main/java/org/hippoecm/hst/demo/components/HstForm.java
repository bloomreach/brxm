/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.component.support.forms.FormField;
import org.hippoecm.hst.component.support.forms.FormMap;
import org.hippoecm.hst.component.support.forms.FormUtils;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstForm extends BaseHstComponent {

    private static Logger log = LoggerFactory.getLogger(HstForm.class);
    private static final String[] DEFINED_FORM_FIELDS = {"name", "email", "textarea", "checkbox"};

    @Override
    public void doAction(final HstRequest request, final HstResponse response) throws HstComponentException {
        super.doAction(request, response);

        super.doAction(request, response);
        FormMap formMap = new FormMap(request, DEFINED_FORM_FIELDS);
        FormField checkbox = formMap.getField("checkbox");
        if (checkbox == null || checkbox.getValues().size() == 0) {
            formMap.addMessage("checkbox", "Please choose a color");
            formMap.addMessage("checkbox", "Please choose any color");
        }
        if (request.getParameterValues("seal") != null) {
            log.info("Sealing data. this data cannot be read within doBefore anymore");
            formMap.setSealed(true);
        }
        FormUtils.persistFormMap(request, response, formMap, null);
    }

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        FormMap formMap = new FormMap();
        FormUtils.populate(request, formMap);
        request.setAttribute("form", formMap);
    }

}



