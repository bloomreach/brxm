/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Collections;

import javax.jcr.Node;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.JcrModel;
import org.onehippo.cms7.essentials.dashboard.model.PersistentCollectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes collecton of JCR Node items
 *
 * @version "$Id$"
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface PersistentCollection {

    enum ProcessAnnotation implements PersistentCollectionHandler<PersistentCollection, Collection<Node>> {
        NODE_COLLECTION_WRITER {
            @Override
            public Collection<Node> execute(final PluginContext context, final Collection<JcrModel> model, final PersistentCollection annotation) {
                return Collections.emptyList();
            }
        };
        private static final Logger log = LoggerFactory.getLogger(ProcessAnnotation.class);


    }
}
