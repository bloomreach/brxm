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
package org.onehippo.cm.engine.parser;

import org.yaml.snakeyaml.nodes.Node;

public class ParserException extends Exception {

    private final Node node;

    ParserException(final String message) {
        this(message, null, null);
    }

    ParserException(final String message, final Node node) {
        this(message, node, null);
    }

    ParserException(final String message, final Throwable cause) {
        this(message, null, cause);
    }

    ParserException(final String message, final Node node, final Throwable cause) {
        super(message, cause);
        this.node = node;
    }

    Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        if (node == null) {
            return getClass().getName() + ": " + getMessage();
        } else {
            return getClass().getName() + ": " + getMessage() + node.getStartMark().toString();
        }
    }

}
