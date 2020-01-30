/*
 *  Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v10.content.beans.jackson;

import com.fasterxml.jackson.databind.annotation.JsonAppend;

// TODO get rid of this once  HippoGalleryImageBean are written to 'root document level' : then _links can be added
// TODO as in org.hippoecm.hst.pagemodelapi.v09.core.container.HippoBeanSerializer.addLinksToContent()
// TODO instead of this serializer
// TODO marker this as TODO but will only be changed in v10 and stay the same in this version....so only in next
// TODO API version we can get rid of this Mixin!
@JsonAppend(props = {
        @JsonAppend.Prop(
                name = "_links",
                value = HippoGalleryImageBeanLinksVirtualBeanPropertyWriter.class
        )
}
)
public interface HippoGalleryImageBeanMixin {

}
