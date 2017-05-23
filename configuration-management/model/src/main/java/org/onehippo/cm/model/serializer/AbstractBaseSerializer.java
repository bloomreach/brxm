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
package org.onehippo.cm.engine.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

public abstract class AbstractBaseSerializer {

    private final boolean explicitSequencing;

    public AbstractBaseSerializer(final boolean explicitSequencing) {
        this.explicitSequencing = explicitSequencing;
    }

    public void serializeNode(final OutputStream outputStream, final Node node) throws IOException {
        final Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        final DumperOptions dumperOptions = new DumperOptions();
        if (explicitSequencing) {
            dumperOptions.setIndicatorIndent(2);
            dumperOptions.setIndent(4);
        } else {
            dumperOptions.setIndicatorIndent(0);
            dumperOptions.setIndent(2);
        }
        final Resolver resolver = new Resolver();
        final Serializer serializer = new Serializer(new Emitter(writer, dumperOptions), resolver, dumperOptions, null);

        serializer.open();
        serializer.serialize(node);
        serializer.close();
    }

    protected static NodeTuple createStrStrTuple(final String key, final String value) {
        return new NodeTuple(createStrScalar(key), createStrScalar(value));
    }

    protected static NodeTuple createStrSeqTuple(final String key, final List<Node> value) {
        return createStrSeqTuple(key, value, false);
    }

    protected static NodeTuple createStrSeqTuple(final String key, final List<Node> value, final boolean flowStyle) {
        return new NodeTuple(createStrScalar(key), new SequenceNode(Tag.SEQ, value, flowStyle));
    }

    protected NodeTuple createStrMapTuple(final String key, final List<NodeTuple> value) {
        return new NodeTuple(createStrScalar(key), new MappingNode(Tag.MAP, value, false));
    }

    protected NodeTuple createStrOptionalSequenceTuple(final String string, final List<NodeTuple> tuples) {
        if (explicitSequencing) {
            final List<Node> nodes = new ArrayList<>(tuples.size());
            for (NodeTuple tuple : tuples) {
                final List<NodeTuple> list = new ArrayList<>(1);
                list.add(tuple);
                nodes.add(new MappingNode(Tag.MAP, list, false));
            }
            return createStrSeqTuple(string, nodes);
        } else {
            return new NodeTuple(createStrScalar(string), new MappingNode(Tag.MAP, tuples, false));
        }
    }

    protected static ScalarNode createStrScalar(final String str) {
        return new ScalarNode(Tag.STR, str, null, null, null);
    }

    protected static ScalarNode createStrScalar(final String str, final Character style) {
        return new ScalarNode(Tag.STR, str, null, null, style);
    }

}
