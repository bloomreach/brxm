/*
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

import java.util.Map;

/**
 * SCXMLDataModel to be provided in a SCXML root Context under the predefined and reserved {@link #CONTEXT_KEY} ("dm") key.
 * <p>
 * This predefined datamodel object can be used to both set and evaluate supported actions which can be 'executed' as SCXML events.
 * </p>
 * <p>
 * Such actions can be set from within a SCXML document using the {@link ActionAction} custom Action.
 * </p>
 * <p>
 * The datamodel also provides access to a {@link #getResult()} which can be {@link #setResult(Object) set} from within the SCXML document
 * to return some result after an action event, for which the ResultAction custom Action can be used.
 * </p>
 * <p>
 * The result object should be reset to null before every SCXML 'execution'.
 * </p>
 */
public interface SCXMLDataModel {

    String CONTEXT_KEY = "dm";

    String FINAL_RESET_STATE_ID = "reset";

    Map<String,Boolean> getActions();
    Object getResult();
    void setResult(Object result);
    void reset();
}
