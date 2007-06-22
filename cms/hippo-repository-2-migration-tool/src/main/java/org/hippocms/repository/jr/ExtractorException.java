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
package org.hippocms.repository.jr;

public class ExtractorException extends Exception {

    /**
     * The ExtractorException class
     * 
     */
    public ExtractorException() {
        super();
    }

    /**
     * 
     * @param message
     */
    public ExtractorException(String message) {
        super(message);
    }

    /**
     * 
     * @param cause
     */
    public ExtractorException(Throwable cause) {
        super(cause);
    }

    /**
     * 
     * @param message
     * @param cause
     */
    public ExtractorException(String message, Throwable cause) {
        super(message, cause);
    }

}
