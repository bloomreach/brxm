/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.model.impl;

import org.onehippo.cm.model.SourceLocation;

public class SourceLocationImpl implements SourceLocation {

    private String path = "?";
    private int lineNumber = -1;
    private int columnNumber = -1;

    public SourceLocationImpl() {
    }

    public SourceLocationImpl(final String path, final int lineNumber, final int columnNumber) {
        this.path = path;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    @Override
    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public int getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(final int columnNumber) {
        this.columnNumber = columnNumber;
    }

    public void copy(SourceLocationImpl sourceLocation) {
        this.path = sourceLocation.path;
        this.lineNumber = sourceLocation.lineNumber;
        this.columnNumber = sourceLocation.columnNumber;
    }

    public String toString() {
        return path + " [" + lineNumber + "," + columnNumber + "]";
    }
}
