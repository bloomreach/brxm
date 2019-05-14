/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.util;

import java.util.Date;

import org.apache.jackrabbit.util.ISO8601;

public class DateConstants {

    public static final String EMPTY_DATE_VALUE = "0001-01-01T12:00:00.000+00:00";
    public static final Date EMPTY_DATE = ISO8601.parse(EMPTY_DATE_VALUE).getTime();

    private DateConstants() {
    }
}
