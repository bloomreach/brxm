/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.editor.compare;

import java.io.InputStream;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueComparer extends TypedComparer<Value> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueComparer.class);

    private StreamComparer streamComparer;
    
    public ValueComparer(ITypeDescriptor type) {
        super(type);
        if (type.isNode()) {
            throw new RuntimeException("type corresponds to a node type");
        }
    }

    protected IComparer<InputStream> getStreamComparer() {
        if (streamComparer == null) {
            streamComparer = new StreamComparer();
        }
        return streamComparer;
    }
    
    public boolean areEqual(Value base, Value target) {
        if (base.getType() != target.getType()) {
            return false;
        }
        try {
            switch (base.getType()) {
            case PropertyType.BOOLEAN:
                return base.getBoolean() == target.getBoolean();
            case PropertyType.LONG:
                return base.getLong() == target.getLong();
            case PropertyType.BINARY:
                return getStreamComparer().areEqual(base.getStream(), target.getStream());
            }
            return base.getString().equals(target.getString());
        } catch (RepositoryException e) {
            log.error("Could not compare values", e);
            return false;
        }
    }

    public int getHashCode(Value value) {
        try {
            switch (value.getType()) {
            case PropertyType.BOOLEAN:
                return Boolean.valueOf(value.getBoolean()).hashCode();
            case PropertyType.LONG:
                return Long.valueOf(value.getLong()).hashCode();
            case PropertyType.BINARY:
                return getStreamComparer().getHashCode(value.getStream());
            }
            return value.getString().hashCode();
        } catch (RepositoryException e) {
            log.error("Could not calculate hash code", e);
            return 0;
        }
    }

}
