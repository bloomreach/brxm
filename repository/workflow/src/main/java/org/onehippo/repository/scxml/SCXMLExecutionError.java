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

import java.io.Serializable;

import org.apache.commons.scxml2.ErrorReporter;

/**
 * SCXMLExecutionError capturing an error reported to {@link ErrorReporter} during SCXML execution.
 */
public class SCXMLExecutionError extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String errorDetail;
    private final Object errorContext;

    public SCXMLExecutionError(final String errorCode, final String errorDetail, final Object errorContext, final CharSequence errorMessage) {
        super(errorMessage.toString());
        this.errorCode = errorCode;
        this.errorDetail = errorDetail;
        this.errorContext = errorContext;
    }

    @SuppressWarnings("unused")
    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    @SuppressWarnings("unused")
    public Object getErrorContext() {
        return errorContext;
    }

}
