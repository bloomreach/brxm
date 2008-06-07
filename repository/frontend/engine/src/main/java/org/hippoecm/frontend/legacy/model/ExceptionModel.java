/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.legacy.model;

import java.util.HashMap;
import java.util.Map;


@Deprecated
public class ExceptionModel implements IPluginModel {

    private static final long serialVersionUID = 1L;
    private Exception exception;
    private String displayMessage;

    public ExceptionModel(Exception e) {
        this.exception = e;
    }

    public ExceptionModel(String displayMessage) {
        this.displayMessage = displayMessage;
    }
    
    public ExceptionModel(Exception e,String displayMessage) {
        this.exception = e;
        this.displayMessage = displayMessage;
    }
    
    public Exception getException() {
        return exception;
    }
    
    public String getDisplayMessage() {
        return displayMessage;
    }

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("exception", exception);
        map.put("displayMessage", displayMessage);
        return map;
    }

    public Object getObject() {
        return exception;
    }

    public void setObject(Object object) {
        this.exception = (object instanceof Exception) ? (Exception) object : null;
    }

    public void detach() {
    }
}
