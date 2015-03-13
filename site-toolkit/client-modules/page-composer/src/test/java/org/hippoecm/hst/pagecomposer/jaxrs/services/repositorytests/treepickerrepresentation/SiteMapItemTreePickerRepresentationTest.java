/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.treepickerrepresentation;

import org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SiteMapItemTreePickerRepresentationTest extends AbstractTreePickerRepresentationTest {

    @Test
    public void siteMapItem_treePicker_representation() throws Exception {

        TreePickerRepresentation representation = createSiteMapItemRepresentation("", getSiteMapItemIdentifier("about-us"));

        assertEquals("pages", representation.getPickerType());
        assertEquals("page", representation.getType());
    }



    @Test
    public void invisible_siteMapItem_treePicker_representation_results_siteMapItem_nonetheless() throws Exception {


    }
}
