/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.model;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;

/**
 * Interface for a DAO that knows how to retrieve and store the classification for a document Node.
 */
public interface ClassificationDao extends IClusterable {

    String SERVICE_ID = "taxonomy.classification.dao";

    Classification getClassification(IModel<Node> nodeModel);

    void save(Classification classification);

}
