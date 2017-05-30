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
package org.onehippo.cm.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.model.impl.ValueImpl;

/**
 * JCR binary value representation.
 */
public class JcrBinaryValueImpl extends ValueImpl {

    public JcrBinaryValueImpl(final javax.jcr.Value value) {
        super(value, ValueType.BINARY, false, false);
    }

    @Override
    public String getString() {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(getResourceInputStream(), writer, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to convert InputStream to String for property '%s'", getParent().getName()), e);
        }
        return writer.toString();
    }

    @Override
    public InputStream getResourceInputStream() throws IOException {
        try {
            return ((Value)value).getBinary().getStream();
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }
}
