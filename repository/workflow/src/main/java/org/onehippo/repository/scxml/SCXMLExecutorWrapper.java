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
package org.onehippo.repository.scxml;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;

/**
 * SCXMLExecutorWrapper wrapping {@link SCXMLExecutor} for convenience.
 * <P>
 * {@link #tryGo()}, {@link #tryReset()}, {@link #tryTriggerEvent(TriggerEvent)} and {@link #tryTriggerEvents(TriggerEvent[])} methods
 * catch {@link ModelException} while invoking {@link SCXMLExecutor#go()}, {@link SCXMLExecutor#reset()}, {@link SCXMLExecutor#triggerEvent(TriggerEvent)} and {@link SCXMLExecutor#triggerEvents(TriggerEvent[])}
 * and capture all the SCXML execution errors reported to {@link ErrorReporter}.
 * Each of those methods return true if there's no exception and no SCXML execution errors. Otherwise it returns false.
 * </P>
 */
public class SCXMLExecutorWrapper {

    private final SCXMLExecutor executor;
    private HippoScxmlErrorReporter hippoScxmlErrorReporter;

    private ModelException modelException;
    private List<SCXMLExecutionError> scxmlExecutionErrors;

    public SCXMLExecutorWrapper(final SCXMLExecutor executor) {
        this.executor = executor;

        if (executor.getErrorReporter() instanceof HippoScxmlErrorReporter) {
            hippoScxmlErrorReporter = (HippoScxmlErrorReporter) executor.getErrorReporter();
        }
    }

    public SCXMLExecutor getSCXMLExecutor() {
        return executor;
    }

    /**
     * Invokes {@link SCXMLExecutor#go()} on the wrapping SCXMLExecutor without throwing {@link ModelException}.
     * Returns true if there's no exception and no SCXML errors.
     * @return
     */
    public boolean tryGo() {
        try {
            clearExceptionAndErrors();

            if (hippoScxmlErrorReporter != null) {
                hippoScxmlErrorReporter.setRecordingScxmlExecutionErrors(true);
            }

            executor.go();
        } catch (ModelException e) {
            modelException = e;
        } finally {
            if (hippoScxmlErrorReporter != null) {
                hippoScxmlErrorReporter.setRecordingScxmlExecutionErrors(false);
                scxmlExecutionErrors = new LinkedList<SCXMLExecutionError>(hippoScxmlErrorReporter.getSCXMLExecutionErrors());
                hippoScxmlErrorReporter.clearExecutionErrors();
            }
        }

        return !hasAnyExceptionOrError();
    }

    /**
     * Invokes {@link SCXMLExecutor#reset()} on the wrapping SCXMLExecutor without throwing {@link ModelException}.
     * Returns true if there's no exception and no SCXML errors.
     * @return
     */
    public boolean tryReset() {
        try {
            clearExceptionAndErrors();

            if (hippoScxmlErrorReporter != null) {
                hippoScxmlErrorReporter.setRecordingScxmlExecutionErrors(true);
            }

            executor.reset();
        } catch (ModelException e) {
            modelException = e;
        } finally {
            if (hippoScxmlErrorReporter != null) {
                hippoScxmlErrorReporter.setRecordingScxmlExecutionErrors(false);
                scxmlExecutionErrors = new LinkedList<SCXMLExecutionError>(hippoScxmlErrorReporter.getSCXMLExecutionErrors());
                hippoScxmlErrorReporter.clearExecutionErrors();
            }
        }

        return !hasAnyExceptionOrError();
    }

    /**
     * Invokes {@link SCXMLExecutor#triggerEvent(TriggerEvent)} on the wrapping SCXMLExecutor without throwing {@link ModelException}.
     * Returns true if there's no exception and no SCXML errors.
     * @return
     */
    public boolean tryTriggerEvent(final TriggerEvent evt) {
        try {
            clearExceptionAndErrors();

            if (hippoScxmlErrorReporter != null) {
                hippoScxmlErrorReporter.setRecordingScxmlExecutionErrors(true);
            }

            executor.triggerEvent(evt);
        } catch (ModelException e) {
            modelException = e;
        } finally {
            if (hippoScxmlErrorReporter != null) {
                hippoScxmlErrorReporter.setRecordingScxmlExecutionErrors(false);
                scxmlExecutionErrors = new LinkedList<SCXMLExecutionError>(hippoScxmlErrorReporter.getSCXMLExecutionErrors());
                hippoScxmlErrorReporter.clearExecutionErrors();
            }
        }

        return !hasAnyExceptionOrError();
    }

    /**
     * Invokes {@link SCXMLExecutor#triggerEvents(TriggerEvent[])} on the wrapping SCXMLExecutor without throwing {@link ModelException}.
     * Returns true if there's no exception and no SCXML errors.
     * @return
     */
    public boolean tryTriggerEvents(final TriggerEvent[] evts) {
        try {
            clearExceptionAndErrors();

            if (hippoScxmlErrorReporter != null) {
                hippoScxmlErrorReporter.setRecordingScxmlExecutionErrors(true);
            }

            executor.triggerEvents(evts);
        } catch (ModelException e) {
            modelException = e;
        } finally {
            if (hippoScxmlErrorReporter != null) {
                hippoScxmlErrorReporter.setRecordingScxmlExecutionErrors(false);
                scxmlExecutionErrors = new LinkedList<SCXMLExecutionError>(hippoScxmlErrorReporter.getSCXMLExecutionErrors());
                hippoScxmlErrorReporter.clearExecutionErrors();
            }
        }

        return !hasAnyExceptionOrError();
    }

    /**
     * Returns the {@link ModelException} while invoking the underlying SCXMLExecutor if any captured.
     * Otherwise returns null.
     * @return
     */
    public ModelException getModelException() {
        return modelException;
    }

    /**
     * Returns the first {@link SCXMLExecutionError} while invoking the underlying SCXMLExecutor if any captured.
     * @return
     */
    public SCXMLExecutionError getSCXMLExecutionError() {
        if (scxmlExecutionErrors != null && !scxmlExecutionErrors.isEmpty()) {
            return scxmlExecutionErrors.get(0);
        }

        return null;
    }

    /**
     * Returns the list of {@link SCXMLExecutionError}s while invoking the underlying SCXMLExecutor if any captured.
     * @return
     */
    public List<SCXMLExecutionError> getSCXMLExecutionErrors() {
        if (scxmlExecutionErrors != null) {
            return Collections.unmodifiableList(scxmlExecutionErrors);
        }

        return Collections.emptyList();
    }

    private void clearExceptionAndErrors() {
        modelException = null;

        if (scxmlExecutionErrors != null) {
            scxmlExecutionErrors.clear();
        }
    }

    private boolean hasAnyExceptionOrError() {
        if (modelException != null) {
            return true;
        }

        if (scxmlExecutionErrors != null && !scxmlExecutionErrors.isEmpty()) {
            return true;
        }

        return false;
    }
}
