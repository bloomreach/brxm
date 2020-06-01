/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.Locale;
import java.util.ResourceBundle;

import org.hippoecm.hst.pagecomposer.jaxrs.property.SwitchTemplatePropertyRepresentationFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SwitchTemplatePropertyRepresentationFactoryBundleTest {

    @Test
    public void get_parameter_info_default_resource_bundle() {
        final ResourceBundle resourceBundle = ParametersInfoProcessor.getResourceBundle(SwitchTemplatePropertyRepresentationFactory.class, null);
        assertEquals("Choose a template", resourceBundle.getString("choose.template"));
    }

    @Test
    public void get_parameter_info_dutch_resource_bundle() {
        final ResourceBundle resourceBundle = ParametersInfoProcessor.getResourceBundle(SwitchTemplatePropertyRepresentationFactory.class, new Locale("nl"));
        assertEquals("Kies een template", resourceBundle.getString("choose.template"));
    }



}
