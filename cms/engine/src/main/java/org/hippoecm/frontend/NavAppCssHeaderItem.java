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

import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Response;

/**
 * Contains the CSS needed to start the Navigation Application
 */
public class NavAppCssHeaderItem extends HeaderItem {

    private final transient Function<String, CssHeaderItem> mapper;

    public NavAppCssHeaderItem(Function<String, CssHeaderItem> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("nav-app-css-header-item");
    }

    @Override
    public void render(final Response response) {
        getCssSrcTagNames()
                .map(mapper)
                .forEach(item -> item.render(response));
    }

    private Stream<String> getCssSrcTagNames() {
        if (WebApplication.get().usesDevelopmentConfig()) {
            return Stream.empty();
        } else {
            return Stream.of("styles.css");
        }
    }

}
