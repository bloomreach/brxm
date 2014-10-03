/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;

public class LinkPathNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LinkPathNotFoundException() {
        super();
    }

    public LinkPathNotFoundException(String message) {
        super(message);
    }

    public LinkPathNotFoundException(Throwable nested) {
        super(nested);
    }

    public LinkPathNotFoundException(String msg, Throwable nested) {
        super(msg, nested);
    }

}
