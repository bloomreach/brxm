/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.update;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;

/**
 * Base {@link NodeUpdateVisitor} class adding support for logging.
 */
public abstract class BaseNodeUpdateVisitor implements NodeUpdateVisitor {

    protected Logger log;
    protected Map<String, Object> parametersMap;

    public void setLogger(Logger log) {
        this.log = log;
    }

    public void setParametersMap(Map<String, Object> parametersMap) {
        this.parametersMap = parametersMap;
    }

    @Override
    public void initialize(Session session) throws RepositoryException {
    }

    @Override
    public void destroy() {
    }

}
