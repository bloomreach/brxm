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

import java.io.IOException;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class HstLinkSerializer extends StdSerializer<HstLink> {

    private static final long serialVersionUID = 1L;

    public HstLinkSerializer() {
        this(null);
    }

    public HstLinkSerializer(Class<HstLink> type) {
        super(type);
    }

    @Override
    public void serialize(HstLink value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        final HstRequestContext requestContext = RequestContextProvider.get();

        gen.writeStartObject();

        if (value.getPath() != null) {
            gen.writeStringField("path", value.getPath());
        }
        if (value.getSubPath() != null) {
            gen.writeStringField("subPath", value.getSubPath());
        }
        if (requestContext != null) {
            gen.writeStringField("url", value.toUrlForm(requestContext, true));
        }

        gen.writeEndObject();
    }

}