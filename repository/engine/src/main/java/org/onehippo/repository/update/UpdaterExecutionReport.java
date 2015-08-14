/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.utilities.io.CircularBufferOutputStream;
import org.onehippo.cms7.utilities.logging.PrintStreamLogger;
import org.slf4j.Logger;

class UpdaterExecutionReport {

    private long startTime = -1l;
    private boolean started = false;
    private long finishTime = -1l;
    private boolean finished = false;

    private int updatedCount = 0;
    private int failedCount = 0;
    private int skippedCount = 0;

    private final List<String> updated;

    private final File logFile;
    private final File updatedFile;
    private final File failedFile;
    private final File skippedFile;
    private final CircularBufferOutputStream cbos;
    private final PrintStream logStream;
    private final PrintStream updatedStream;
    private final PrintStream failedStream;
    private final PrintStream skippedStream;
    private final PrintStreamLogger logger;

    private static class TimestampPrintStreamLogger extends PrintStreamLogger {
        private static final DateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public TimestampPrintStreamLogger(final String name, final int level, final PrintStream... out) throws IllegalArgumentException {
            super(name, level, out);
        }

        @Override
        protected String getMessageString(final String level, final String message) {
            final String currentTimestamp = timestampFormatter.format(new Date());
            return level + " " + currentTimestamp + " " + message;
        }
    }

    UpdaterExecutionReport() throws IOException {
        logFile = File.createTempFile("updater-execution", ".log.tmp", null);
        logStream = new PrintStream(logFile);
        cbos = new CircularBufferOutputStream(4096);
        logger = new TimestampPrintStreamLogger("repository", PrintStreamLogger.DEBUG_LEVEL, logStream, new PrintStream(cbos));
        updatedFile = File.createTempFile("updater-updated", "txt.tmp", null);
        updatedStream = new PrintStream(updatedFile);
        failedFile = File.createTempFile("updater-failed", "txt.tmp", null);
        failedStream = new PrintStream(failedFile);
        skippedFile = File.createTempFile("updater-skipped", "txt.tmp", null);
        skippedStream = new PrintStream(skippedFile);
        updated = new ArrayList<>();
    }

    void start() {
        this.started = true;
        this.startTime = System.currentTimeMillis();
    }

    void finish() {
        this.finished = true;
        this.finishTime = System.currentTimeMillis();
    }

    Calendar getStartTime() {
        final Calendar result = Calendar.getInstance();
        result.setTimeInMillis(startTime);
        return result;
    }

    boolean isStarted() {
        return started;
    }

    Calendar getFinishTime() {
        final Calendar result = Calendar.getInstance();
        result.setTimeInMillis(finishTime);
        return result;
    }

    boolean isFinished() {
        return finished;
    }

    void startBatch() {
        updated.clear();
    }

    void batchFailed() {
        for (String path : updated) {
            failed(path);
        }
    }

    int getUpdateCount() {
        return updatedCount;
    }

    File getUpdatedFile() {
        return updatedFile;
    }

    void updated(String path) {
        updated.add(path);
        updatedStream.println(path);
        updatedCount++;
    }

    int getFailedCount() {
        return failedCount;
    }

    File getFailedFile() {
        return failedFile;
    }

    void failed(String path) {
        failedStream.println(path);
        failedCount++;
    }

    int getSkippedCount() {
        return skippedCount;
    }

    File getSkippedFile() {
        return skippedFile;
    }

    void skipped(String path) {
        skippedStream.println(path);
        skippedCount++;
    }

    int getVisitedCount() {
        return getUpdateCount() + getFailedCount() + getSkippedCount();
    }

    Logger getLogger() {
        return logger;
    }

    File getLogFile() {
        return logFile;
    }

    String getLogTail() {
        return new String(cbos.toByteArray());
    }

    void close() {
        IOUtils.closeQuietly(cbos);
        IOUtils.closeQuietly(logStream);
        IOUtils.closeQuietly(updatedStream);
        IOUtils.closeQuietly(failedStream);
        IOUtils.closeQuietly(skippedStream);
        logFile.delete();
        updatedFile.delete();
        failedFile.delete();
        skippedFile.delete();
    }
}
