/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.testutils.slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Level;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

/**
 * LogRecord containing all the logging level, message, arguments, etc. in SLF4j logging invocations,
 * such as marker, log level, log message (or format), arguments and throwable instance.
 * <P>
 * {@link #getFormattedMessage()} can be used to get a formatted message from the message (or format)
 * and other logging arguments array.
 * </P>
 * <P>
 * Also, {@link #getStackTrace()} can be used to get a formatted stack trace string from the throwable if exists.
 * </P>
 */
public class LogRecord {

    private final Marker marker;
    private final int level;
    private final String message;
    private final Object[] argArray;
    private final Throwable throwable;

    private final String stringified;
    private final String formattedMessage;
    private String stackTrace;

    public LogRecord(final Marker marker, final int level, final String message, final Object[] argArray,
            final Throwable throwable) {
        this.marker = marker;
        this.level = level;
        this.message = message;
        this.argArray = argArray;
        this.throwable = throwable;

        StringBuilder sb = new StringBuilder(256);
        sb.append(super.toString()).append(" - ");
        sb.append(Level.toLevel(level)).append(" ");
        formattedMessage = MessageFormatter.arrayFormat(message, argArray).getMessage();
        sb.append(formattedMessage);

        if (throwable != null) {
            StringWriter sw = new StringWriter(256);
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            pw.flush();
            stackTrace = sw.toString();
            sb.append('\n').append(stackTrace);
            pw.close();
        }

        stringified = sb.toString();
    }

    public Marker getMarker() {
        return marker;
    }

    public int getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public Object[] getArgArray() {
        return argArray;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }

    @Override
    public String toString() {
        return stringified;
    }
}
