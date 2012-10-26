/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.core.jcr;

import javax.jcr.RepositoryException;

/**
 * The unchecked equivalent of the checked {@link javax.jcr.RepositoryException} : This class can be used to 
 * delegate a {@link RepositoryException} that cannot be handled into a {@link RuntimeException}
 */
public class RuntimeRepositoryException extends RuntimeException {
    
    public RuntimeRepositoryException (RepositoryException e) {
        super(e);
    }
}
