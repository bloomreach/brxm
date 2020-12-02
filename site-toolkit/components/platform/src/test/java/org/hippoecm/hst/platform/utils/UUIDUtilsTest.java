/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.platform.utils;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class UUIDUtilsTest {

    @Test
    public void invalid_UUIDS() {
        Stream.of(
                "this is not a UUID",
                "0000001-0001-0001-0001-000000000001",
                "@afebabe-cafe-babe-cafe-babecafebabe"
        ).forEach(this::validateInvalid);
    }

    @Test
    public void invalid_UUIDS_that_parse_fromString() {
        Stream.of(
                "0-1-2-3-4",
                "a-b-c-d-e",
                "9a-8b-7c-6d-7e",
                "0000cafebabe-cafe-babe-cafe-babecafebabe",
                "0000cafebabe-11111cafe-babe-cafe-babecafebabe",
                "0000cafebabe-11111cafe-2222babe-cafe-babecafebabe",
                "0000cafebabe-11111cafe-2222babe-33333cafe-babecafebabe",
                "0000cafebabe-11111cafe-2222babe-33333cafe-4444babecafebabe"
        ).map(uuid ->
                // fromString does not throw exception but uuid is invalid
                UUID.fromString(uuid).toString().equals(uuid) ? null : uuid
        ).forEach(this::validateInvalid);

    }

    private void validateInvalid(String value) {
        assertFalse("Expected " + value + " to not be a valid UUID", UUIDUtils.isValidUUID(value));
    }
}
