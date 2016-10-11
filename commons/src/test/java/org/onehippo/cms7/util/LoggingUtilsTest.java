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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Check if the mapping to the exposed #warn() methods is correct.
 */
public class LoggingUtilsTest {
    private static final Logger log = LoggerFactory.getLogger(LoggingUtilsTest.class);
    private static final Logger dbg = LoggerFactory.getLogger(DebugLogger.class);

    @Test
    public void variations() {
        final Exception e = new IllegalStateException("Exception message");

        LoggingUtils.warnException(log, e, "Simple");
        assertThat(LoggingAppender.message, equalTo("Simple: Exception message"));
        assertThat(LoggingAppender.throwable, equalTo(null));
        LoggingUtils.warnException(dbg, e, "Simple");
        assertThat(LoggingAppender.message, equalTo("Simple"));
        assertThat(LoggingAppender.throwable, equalTo(e));

        LoggingUtils.warnException(log, e, "Simple {}", "arg1");
        assertThat(LoggingAppender.message, equalTo("Simple arg1: Exception message"));
        assertThat(LoggingAppender.throwable, equalTo(null));
        LoggingUtils.warnException(dbg, e, "Simple {}", "arg1");
        assertThat(LoggingAppender.message, equalTo("Simple arg1"));
        assertThat(LoggingAppender.throwable, equalTo(e));

        LoggingUtils.warnException(log, e, "Simple {} {}", "arg1", "arg2");
        assertThat(LoggingAppender.message, equalTo("Simple arg1 arg2: Exception message"));
        assertThat(LoggingAppender.throwable, equalTo(null));
        LoggingUtils.warnException(dbg, e, "Simple {} {}", "arg1", "arg2");
        assertThat(LoggingAppender.message, equalTo("Simple arg1 arg2"));
        assertThat(LoggingAppender.throwable, equalTo(e));

        LoggingUtils.warnException(log, e, "Simple {} {} {}", "arg1", "arg2", "arg3");
        assertThat(LoggingAppender.message, equalTo("Simple arg1 arg2 arg3: Exception message"));
        assertThat(LoggingAppender.throwable, equalTo(null));
        LoggingUtils.warnException(dbg, e, "Simple {} {} {}", "arg1", "arg2", "arg3");
        assertThat(LoggingAppender.message, equalTo("Simple arg1 arg2 arg3"));
        assertThat(LoggingAppender.throwable, equalTo(e));
    }
}
