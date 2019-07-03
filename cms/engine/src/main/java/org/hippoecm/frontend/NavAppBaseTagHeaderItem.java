/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend;

import java.net.URI;
import java.util.Collections;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.request.Response;

public class NavAppBaseTagHeaderItem extends HeaderItem {

    private final transient URI navAppLocation;

    public NavAppBaseTagHeaderItem(final URI navAppLocation) {
        this.navAppLocation = navAppLocation;
    }


    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("nav-app-base-tag-header-item");
    }

    @Override
    public void render(final Response response) {
        StringHeaderItem.forString(String.format("<base href=%s/>", navAppLocation)).render(response);

    }
}
