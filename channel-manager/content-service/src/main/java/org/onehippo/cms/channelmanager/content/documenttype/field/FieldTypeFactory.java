/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.field;

import java.util.Optional;

import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating new instances of subclasses of FieldType.
 *
 * We keep this class as small as possible, as the newInstance() call is hard to mock.
 */
public class FieldTypeFactory {
    private static final Logger log = LoggerFactory.getLogger(FieldTypeFactory.class);

    public static <T extends FieldType> Optional<T> createFieldType(Class<T> clazz) {
        try {
            return Optional.of(clazz.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            log.warn("Failed to create instance of class {}", clazz.getName(), e);
        }
        return Optional.empty();
    }
}
