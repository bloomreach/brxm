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
package org.onehippo.repository.scxml.test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.onehippo.repository.scxml.HippoScxmlErrorReporter;
import org.onehippo.repository.scxml.SCXMLDefinition;

/**
 * ErrorRecordingErrorReporterWrapper
 * <P>
 * Wrapper class to capture errors during unit tests for verification.
 * </P>
 */
public class ErrorRecordingErrorReporterWrapper extends HippoScxmlErrorReporter {

    private static final long serialVersionUID = 1L;

    private List<ErrorRecord> errorRecords = Collections.synchronizedList(new LinkedList<ErrorRecord>());

    public ErrorRecordingErrorReporterWrapper(SCXMLDefinition scxmlDef) {
        super(scxmlDef);
    }

    public List<ErrorRecord> getErrorRecords() {
        return Collections.unmodifiableList(errorRecords);
    }

    public void clearErrorRecords() {
        errorRecords.clear();
    }

    @Override
    public void onError(String errCode, String errDetail, Object errCtx) {
        super.onError(errCode, errDetail, errCtx);
        errorRecords.add(new ErrorRecord(errCode, super.decorateErrorDetail(errDetail), errCtx));
    }
}
