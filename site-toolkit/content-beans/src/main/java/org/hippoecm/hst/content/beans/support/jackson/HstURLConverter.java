/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.support.jackson;

import org.hippoecm.hst.core.component.HstURL;

import com.fasterxml.jackson.databind.util.StdConverter;

public class HstURLConverter extends StdConverter<HstURL, HstURLRepresentation> {

    @Override
    public HstURLRepresentation convert(HstURL value) {
        HstURLRepresentation representation = new HstURLRepresentation();

        representation.setType(value.getType());
        representation.setUrl(value.toString());

        return representation;
    }

}
