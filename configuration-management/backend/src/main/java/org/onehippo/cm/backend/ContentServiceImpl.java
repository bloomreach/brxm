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
package org.onehippo.cm.backend;

import org.onehippo.cm.api.ContentService;
import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionType;
import org.onehippo.cm.api.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * Saves content definitions per source
 */
public class ContentServiceImpl implements ContentService {

    private static final Logger log = LoggerFactory.getLogger(ContentServiceImpl.class);

    private final Session session;

    public ContentServiceImpl(final Session session) {
        this.session = session;
    }

    @Override
    public void apply(final MergedModel mergedModel, final EnumSet<DefinitionType> includeDefinitionTypes)
            throws Exception {
        try {
            final Collection<ContentDefinition> contentDefinitions = mergedModel.getContentDefinitions();
            final Map<Source, List<ContentDefinition>> contentMap = contentDefinitions.stream()
                    .collect(Collectors.groupingBy(Definition::getSource, toSortedList(comparing(e -> e.getNode().getPath()))));
            for (Source source : contentMap.keySet()) {

                final ContentProcessingService contentProcessingService = new JcrContentProcessingService(session, mergedModel.getResourceInputProviders());
                contentProcessingService.apply(source, contentMap.get(source));

                session.save();
            }
        } catch (Exception e) {
            log.warn("Failed to apply configuration", e);
            throw e;
        }
    }

    private static <T> Collector<T,?,List<T>> toSortedList(Comparator<? super T> c) {
        return Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), l->{ l.sort(c); return l; } );
    }
}
