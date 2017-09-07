/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.api.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Common Binary representation interface, reflecting any binary content.
 * <P>
 * After use, {@link #dispose()} must be invoked.
 * </P>
 */
public interface Binary extends Serializable {

    /**
     * Return input stream of this binary.
     * @return input stream of this binary
     * @throws IOException if IO exception occurs
     */
    InputStream getInputStream() throws IOException;

    /**
     * Clean up any resources opened in this binary.
     */
    void dispose();

}
