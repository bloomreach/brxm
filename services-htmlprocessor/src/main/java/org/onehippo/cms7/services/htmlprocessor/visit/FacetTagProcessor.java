/*
 *  Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.visit;

import java.io.Serializable;

import org.onehippo.cms7.services.htmlprocessor.Tag;
import org.onehippo.cms7.services.htmlprocessor.service.FacetService;

public interface FacetTagProcessor extends Serializable {

    String ATTRIBUTE_DATA_FRAGMENT_ID = "data-fragment-id";
    String ATTRIBUTE_DATA_UUID = "data-uuid";

    void onRead(Tag tag, final FacetService facetService);

    void onWrite(Tag tag, final FacetService facetService);
}
