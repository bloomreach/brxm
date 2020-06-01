/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.common.base.Predicate;

public class UriValidator implements Predicate<String> {

    @Override
    public boolean apply(String input) {
        try {
            return input != null && new URI(input) != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
