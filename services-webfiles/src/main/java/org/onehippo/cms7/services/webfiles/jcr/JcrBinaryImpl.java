/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.jcr;

import java.io.InputStream;

import javax.jcr.RepositoryException;

import org.onehippo.cms7.services.webfiles.Binary;
import org.onehippo.cms7.services.webfiles.WebFileException;


public class JcrBinaryImpl implements Binary {

    final javax.jcr.Binary delegatee;

    public JcrBinaryImpl(final javax.jcr.Binary binary) {
        delegatee = binary;
    }

    @Override
    public InputStream getStream(){
        try {
            return delegatee.getStream();
        } catch (RepositoryException e) {
            throw new WebFileException(e);
        }
    }

    @Override
    public long getSize() {
        try {
            return delegatee.getSize();
        } catch (RepositoryException e) {
            throw new WebFileException(e);
        }
    }

    @Override
    public void dispose() {
        delegatee.dispose();
    }
}
