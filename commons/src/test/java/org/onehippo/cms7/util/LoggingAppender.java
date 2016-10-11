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

package org.onehippo.cms7.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class LoggingAppender extends AppenderSkeleton {
    public static String message;
    public static Throwable throwable;

    protected void append(LoggingEvent event) {
        message = (String)event.getMessage();
        throwable = event.getThrowableInformation() != null ? event.getThrowableInformation().getThrowable() : null;
    }

    public void close() {}
    public boolean requiresLayout() { return false; }
}
