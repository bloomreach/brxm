/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.diagnosis;

import org.apache.wicket.request.Request;

/**
 * Diagnostics Service.
 */
public interface DiagnosticsService {

    /**
     * Return true if diagnostics is enabled for the client with the {@code request}.
     * @param request request for the client at remote address
     * @return true if diagnostics is enabled
     */
    public boolean isEnabledFor(Request request);

    /**
     * Return threshold time to report diagnostics in milliseconds.
     * @return threshold time to report diagnostics in milliseconds
     */
    public long getThresholdMillisec();

    /**
     * Return diagnostics reporting depth.
     * @return diagnostics reporting depth
     */
    public int getDepth();

    /**
     * Return threshold time for a subtask to report diagnostics in milliseconds.
     * @return threshold time for a subtask to report diagnostics in milliseconds
     */
    public long getUnitThresholdMillisec();

}
