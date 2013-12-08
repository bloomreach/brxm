/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.config;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @version "$Id$"
 */
public class RestApplication extends Application {

    private static Logger log = LoggerFactory.getLogger(RestApplication.class);

   /* @Override
    public Set<Object> getSingletons() {
        JsonProvider provider = new JsonProvider();
        provider.setIncludeRoot(false);
        HashSet<Object> set = new HashSet<>(1);
        set.add(provider);
        return set;
    }
*/
}
