/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.mapper;

import org.onehippo.cm.api.model.Value;

/**
 * Value to File mapper. Maps JCR value to filename using smart name resolvers
 */
public interface ValueFileMapper {

    /**
     * Generates filename and path relative to the module root
     * @param value Property's value
     * @return Path with filename, or null if the mapper rejects the supplied value
     */
    // TODO HCM-101: add appropriate annotation to indicate potential null return value
    String apply(Value value);
}
