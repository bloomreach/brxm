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

package org.hippoecm.frontend.service;

import java.net.URI;

/**
 * Represents a Navigation Application resource. There are two types of resources:
 * - A REST resource which is accessible via an XHR call
 * - An IFRAME resource which is HTML + javascript that should be rendered in an iframe and once
 * loaded will connect to it's parent via the communication library.
 */
public interface NavAppResource {

    URI getUrl();

    ResourceType getResourceType();
}
