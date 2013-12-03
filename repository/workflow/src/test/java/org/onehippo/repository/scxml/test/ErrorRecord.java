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

import org.apache.commons.scxml2.ErrorReporter;

/**
 * ErrorRecord
 * <P>
 * SCXML Error record sent to {@link ErrorReporter#onError(String, String, Object)}.
 * </P>
 */
public class ErrorRecord {

    private final String errCode;
    private final String errDetail;
    private final Object errCtx;

    public ErrorRecord(final String errCode, final String errDetail, final Object errCtx) {
        this.errCode = errCode;
        this.errDetail = errDetail;
        this.errCtx = errCtx;
    }

    public String getErrCode() {
        return errCode;
    }

    public String getErrDetail() {
        return errDetail;
    }

    public Object getErrCtx() {
        return errCtx;
    }

}
