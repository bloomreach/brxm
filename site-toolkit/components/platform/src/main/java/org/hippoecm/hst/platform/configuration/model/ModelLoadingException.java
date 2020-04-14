/*
 *  Copyright 2012-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.model;


public class ModelLoadingException extends RuntimeException {

    private boolean missingEnvironmentVariable;

    public ModelLoadingException(String message) {
        super(message);
    }

    public ModelLoadingException(String message, Throwable cause) {
        super(message, cause);
    }


    public ModelLoadingException(String message, boolean missingEnvironmentVariable) {
        super(message);
        this.missingEnvironmentVariable = missingEnvironmentVariable;
    }

    /**
     * @return {@code true} if this {@link ModelLoadingException} is the result of a missing environment variable, for
     * example there is not system property ${env} while a host nodename contains ${evn}
     */
    public boolean isMissingEnvironmentVariable() {
        return missingEnvironmentVariable;
    }
}

