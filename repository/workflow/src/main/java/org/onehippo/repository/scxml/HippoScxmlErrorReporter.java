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

import org.apache.commons.scxml2.env.SimpleErrorReporter;

/**
 * HippoScxmlErrorReporter
 * <P>
 * Decorates error information with more useful information in context.
 * </P>
 */
public class HippoScxmlErrorReporter extends SimpleErrorReporter {

    private static final long serialVersionUID = 1L;

    private final SCXMLDefinition scxmlDef;
    private List<SCXMLExecutionError> scxmlExecutionErrors;
    private boolean recordingScxmlExecutionErrors;

    public HippoScxmlErrorReporter(final SCXMLDefinition scxmlDef) {
        this.scxmlDef = scxmlDef;
    }

    @Override
    public void onError(final String errorCode, final String errDetail,
            final Object errCtx) {
        String decoratedErrorDetail = decorateErrorDetail(errDetail);

        if (isRecordingScxmlExecutionErrors()) {
            if (scxmlExecutionErrors == null) {
                scxmlExecutionErrors = new LinkedList<SCXMLExecutionError>();
            }

            scxmlExecutionErrors.add(new SCXMLExecutionError(errorCode, decoratedErrorDetail, errCtx));
        }

        super.onError(errorCode, decoratedErrorDetail, errCtx);
    }

    public boolean isRecordingScxmlExecutionErrors() {
        return recordingScxmlExecutionErrors;
    }

    public void setRecordingScxmlExecutionErrors(boolean recordingScxmlExecutionErrors) {
        this.recordingScxmlExecutionErrors = recordingScxmlExecutionErrors;
    }

    public void clearExecutionErrors() {
        if (scxmlExecutionErrors != null) {
            scxmlExecutionErrors.clear();
        }
    }

    public List<SCXMLExecutionError> getSCXMLExecutionErrors() {
        if (scxmlExecutionErrors != null) {
            return Collections.unmodifiableList(scxmlExecutionErrors);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Decorates the error detail string by appending the SCXML path.
     * @param errDetail
     * @return
     */
    protected String decorateErrorDetail(final String errDetail) {
        StringBuilder sbDetail = new StringBuilder(128);
        sbDetail.append(errDetail);
        sbDetail.append(" in ");
        sbDetail.append(scxmlDef.getPath());
        return sbDetail.toString();
    }
}
