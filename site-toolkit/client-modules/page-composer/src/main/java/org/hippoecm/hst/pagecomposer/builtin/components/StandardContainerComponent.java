/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.builtin.components;

import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

public class StandardContainerComponent extends GenericHstComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        if(getComponentConfiguration().getRenderPath() == null || "".equals(getComponentConfiguration().getRenderPath())) {
            // set default builtin template. This is hardcoded. The freemarker templates are relative to the current package name
            String xtype = getComponentConfiguration().getXType();
            if ("HST.Table".equalsIgnoreCase(xtype)) {
                response.setRenderPath("classpath:table.ftl");
            } else if ("HST.UnorderedList".equalsIgnoreCase(xtype)) {
                response.setRenderPath("classpath:ul.ftl");
            } else if ("HST.OrderedList".equalsIgnoreCase(xtype)) {
                response.setRenderPath("classpath:ol.ftl");
            } else if ("HST.Span".equalsIgnoreCase(xtype)) {
                response.setRenderPath("classpath:span.ftl");
            } else if ("HST.vBox".equalsIgnoreCase(xtype)) {
                response.setRenderPath("classpath:vbox.ftl");
            } else {
                response.setRenderPath("classpath:vbox.ftl");
            }
        }
        super.doBeforeRender(request, response);
    }

}
