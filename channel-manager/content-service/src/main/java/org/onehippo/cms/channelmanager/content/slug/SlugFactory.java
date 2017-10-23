/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.slug;

import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecService;
import org.onehippo.cms7.services.HippoServiceRegistry;

public class SlugFactory {

    /**
     * Creates a slug for a piece of content (a document, a folder, etc) in a certain locale.
     *
     * @param name the name of the piece of content
     * @param locale the locale of the content
     * @return
     */
    public static String createSlug(final String name, final String locale) {
        final StringCodecService service = HippoServiceRegistry.getService(StringCodecService.class);
        final StringCodec codec = service.getStringCodec(StringCodecService.Encoding.NODE_NAME, locale);
        return codec.encode(name);
    }
}
