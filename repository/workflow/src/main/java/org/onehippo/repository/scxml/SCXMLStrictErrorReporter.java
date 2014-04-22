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

import org.apache.commons.scxml2.env.SimpleErrorReporter;

/**
 * SCXMLStrictErrorReporter enforces throwing a {@link SCXMLExecutionError} exception on every reported error
 * and injects the current SCXML path into the error message.
 */
public class SCXMLStrictErrorReporter extends SimpleErrorReporter {

    private static final long serialVersionUID = 1L;

    private final SCXMLDefinition scxmlDef;

    public SCXMLStrictErrorReporter(final SCXMLDefinition scxmlDef) {
        this.scxmlDef = scxmlDef;
    }

    @Override
    public void onError(final String errorCode, final String errDetail,
            final Object errCtx) {
        super.onError(errorCode, decorateErrorDetail(errDetail), errCtx);
    }

    @Override
    protected void handleErrorMessage(final String errorCode, final String errDetail,
                                      final Object errCtx, final CharSequence errorMessage) {
        // don't support any errors and suppress SimpleErrorReporter warn logging
        throw new SCXMLExecutionError(errorCode, errDetail, errCtx, errorMessage);
    }

    /**
     * Decorates the error detail string by appending the SCXML path.
     */
    protected String decorateErrorDetail(final String errDetail) {
        StringBuilder sbDetail = new StringBuilder(128);
        sbDetail.append(errDetail);
        sbDetail.append(" in ");
        sbDetail.append(scxmlDef.getPath());
        return sbDetail.toString();
    }
}
