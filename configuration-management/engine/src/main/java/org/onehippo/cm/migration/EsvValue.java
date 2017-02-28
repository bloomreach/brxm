/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.UUID;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.util.Base64;
import org.apache.jackrabbit.util.ISO8601;
import org.onehippo.cm.impl.model.SourceLocationImpl;

public class EsvValue {

    private final boolean resourcePath;
    private final boolean base64;
    private final SourceLocationImpl location;
    private String string;
    private final int type;

    public EsvValue(final int type, final String resourcePath, final SourceLocationImpl location) {
        this.type = type;
        this.resourcePath = true;
        this.base64 = false;
        this.location = location;
        this.string = resourcePath;
    }

    public EsvValue(final int type, final boolean base64, final SourceLocationImpl location) {
        this.type = type;
        this.resourcePath = false;
        this.base64 = base64;
        this.location = location;
    }

    public int getType() {
        return type;
    }

    public SourceLocationImpl getSourceLocation() {
        return location;
    }

    public String getString() {
        return string;
    }

    public void setString(final String string) {
        this.string = string;
    }

    public boolean isResourcePath() {
        return resourcePath;
    }

    public boolean isBase64() {
        return base64;
    }

    public Object getValue() throws EsvParseException {
        final int effectiveType = resourcePath ? PropertyType.STRING : this.type;
        String value = string;
        try {
            if (base64) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Base64.decode(string, out);
                value = new String(out.toByteArray(), "UTF-8");
            }
            switch (effectiveType) {
                case PropertyType.BINARY:
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Base64.decode(value, out);
                    return out.toByteArray();
                case PropertyType.STRING:
                case PropertyType.NAME:
                case PropertyType.PATH:
                    return value;
                case PropertyType.REFERENCE:
                case PropertyType.WEAKREFERENCE:
                    return UUID.fromString(value);
                case PropertyType.BOOLEAN:
                    return Boolean.valueOf(value);
                case PropertyType.DATE:
                    Calendar calendar = ISO8601.parse(value);
                    if (calendar == null) {
                        throw new EsvParseException("Not a valid date: "+value+" at " + location);
                    }
                    return calendar;
                case PropertyType.DECIMAL:
                    return new BigDecimal(value);
                case PropertyType.DOUBLE:
                    return Double.valueOf(value);
                case PropertyType.LONG:
                    return Long.valueOf(value);
                case PropertyType.URI:
                    return URI.create(value);
                default:
                    throw new EsvParseException("Unsupported value type " + PropertyType.nameFromValue(type) +
                            " at " + location);
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new EsvParseException("Invalid value \"" + string + "\" for value type: " +
                    PropertyType.nameFromValue(type) + " at " + location, e);
        }
    }
}
