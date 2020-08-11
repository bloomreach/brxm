/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.concurrent.action;

public final class ActionFailure {
    
    private final Throwable throwable;
    private final Action action;
    private final String path;
    
    public ActionFailure(Throwable throwable, Action action, String path) {
        this.throwable = throwable;
        this.action = action;
        this.path = path;
    }

    public Action getAction() {
        return action;
    }
    
    public String getPath() {
        return path;
    }

    public void printFailure() {
        System.err.println("Action: " + action);
        System.err.println("Path: " + path);
        System.err.println("Stacktrace");
        throwable.printStackTrace();
    }

}
