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
package org.onehippo.cm.model.parser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.source.ContentSourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.yaml.snakeyaml.events.AliasEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.NodeEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import static org.onehippo.cm.model.Constants.META_KEY_PREFIX;

/**
 * A ContentSource YAML Parser which only parses and loads the minimal 'head' section of a ContentDefinition:
 * <ul>
 *     <li>the first (and only) content definition root path</li>
 *     <li>all the .meta: prefixed scalar properties defined <em>before any</em> other property or child node definition</li>
 * </ul>
 * <p>Requirement/limitation: Any .meta: prefixed properties in the ContentSource YAML which are not defined at the top of
 * the definition root will <em>not</em> be seen/found by this parser, nor any invalid or illegal YAML or ContentDefinition
 * construct.</p>
 */
public class ContentSourceHeadParser extends ContentSourceParser {

    public ContentSourceHeadParser(final ResourceInputProvider resourceInputProvider) {
        super(resourceInputProvider);
    }

    public ContentSourceHeadParser(final ResourceInputProvider resourceInputProvider, final boolean explicitSequencing) {
        super(resourceInputProvider, false, explicitSequencing);
    }

    public ContentSourceHeadParser(final ResourceInputProvider resourceInputProvider, final boolean verifyOnly, final boolean explicitSequencing) {
        super(resourceInputProvider, verifyOnly, explicitSequencing);
    }

    public void parse(final InputStream inputStream, final String relativePath, final String location, final ModuleImpl parent) throws ParserException {
        final Pair<Node, List<NodeTuple>> head = composeYamlHead(inputStream, location);
        final ContentSourceImpl source = parent.addContentSource(relativePath);
        final ContentDefinitionImpl definition = source.addContentDefinition();
        final JcrPath rawPath = asPathScalar(head.getKey(), true, true);
        final JcrPath key = adjustHstRoot(rawPath, parent);
        final DefinitionNodeImpl definitionNode = new DefinitionNodeImpl(key, definition);
        definition.setNode(definitionNode);
        populateDefinitionNode(definitionNode, head.getKey(), head.getValue());
        source.markUnchanged();
    }

    public Pair<Node, List<NodeTuple>> composeYamlHead(final InputStream inputStream, final String location) throws ParserException {
        log.debug("Parsing YAML source head '{}'", location);
        final Resolver resolver = new Resolver();
        final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        final Parser parser = new ParserImpl(new StreamReader(reader));
        try {
            // Drop the STREAM-START event.
            parser.getEvent();
            // Compose the document head if the stream is not empty.
            if (!parser.checkEvent(Event.ID.StreamEnd)) {
                // Drop the DOCUMENT-START event.
                parser.getEvent();
                NodeEvent event = nextNodeEvent(parser);
                if (isExplicitSequencing()) {
                    if (event.is(Event.ID.SequenceStart)) {
                        // skip: next should be mappingStart
                        event = nextNodeEvent(parser);
                    }
                }
                if (event.is(Event.ID.MappingStart)) {
                    event = nextNodeEvent(parser);
                    if (event.is(Event.ID.Scalar)) {
                        final ScalarNode definitionKey = composeScalarNode(resolver, (ScalarEvent)event);
                        final List<NodeTuple> tuples = asScalarTuples(parser, resolver);
                        return Pair.of(definitionKey, tuples);
                    }
                }
            }

            final String message = String.format("Failed to parse YAML source head '%s'", location);
            throw new ParserException(message);

        } catch (RuntimeException e) {
            final String message = String.format("Failed to parse YAML source head '%s'", location);
            throw new ParserException(message, e);
        }
    }

    private List<NodeTuple> asScalarTuples(final Parser parser, final Resolver resolver) throws ParserException {
        final List<NodeTuple> tuples = new ArrayList<>();
        NodeEvent event = nextNodeEvent(parser);
        if (isExplicitSequencing()) {
            if (event.is(Event.ID.SequenceStart)) {
                // skip: next should be mappingStart
                event = nextNodeEvent(parser);
            }
        }
        if (event.is(Event.ID.MappingStart)) {
            while (true) {
                final NodeTuple tuple = asScalarMappingTuple(parser, resolver);
                if (tuple != null) {
                    tuples.add(tuple);
                    if (parser.checkEvent(Event.ID.MappingEnd)) {
                        break;
                    } else {
                        continue;
                    }
                }
                break;
            }
        }
        return tuples;
    }

    private NodeTuple asScalarMappingTuple(final Parser parser, final Resolver resolver) throws ParserException {
        NodeEvent event = nextNodeEvent(parser);
        if (event.is(Event.ID.Scalar)) {
            final ScalarNode keyNode = composeScalarNode(resolver, (ScalarEvent)event);
            event = nextNodeEvent(parser);
            if (event.is(Event.ID.Scalar)) {
                final ScalarNode valueNode = composeScalarNode(resolver, (ScalarEvent)event);
                final String key = asStringScalar(keyNode);
                if (key.startsWith(META_KEY_PREFIX)) {
                    return new NodeTuple(keyNode, valueNode);
                }
            }
        }
        return null;
    }

    private NodeEvent nextNodeEvent(final Parser parser) throws ParserException {
        if (parser.checkEvent(Event.ID.Alias)) {
            final AliasEvent event = (AliasEvent) parser.getEvent();
            final String anchor = event.getAnchor();
            throw new ParserException("Encounter node alias '" + anchor +
                    "' which is not supported when parsing a document head only, " + event.getStartMark().toString());
        } else {
            final NodeEvent event = (NodeEvent) parser.getEvent();
            final String anchor = event.getAnchor();
            if (anchor != null) {
                throw new ParserException("Encountered node anchor '" + anchor +
                        "' which is not supported when parsing a document head only, " + event.getStartMark().toString());
            }
            return event;
        }
    }

    private ScalarNode composeScalarNode(final Resolver resolver, final ScalarEvent ev) {
        final String tag = ev.getTag();
        boolean resolved = false;
        final Tag nodeTag;
        if (tag == null || tag.equals("!")) {
            nodeTag = resolver.resolve(NodeId.scalar, ev.getValue(),
                    ev.getImplicit().canOmitTagInPlainScalar());
            resolved = true;
        } else {
            nodeTag = new Tag(tag);
        }
        return new ScalarNode(nodeTag, resolved, ev.getValue(), ev.getStartMark(),
                ev.getEndMark(), ev.getStyle());
    }
}
