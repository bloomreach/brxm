/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.test;

import javax.jcr.Session;

import org.onehippo.cm.model.impl.ConfigurationModelImpl;

@FunctionalInterface
public interface Validator {

    /**
     * Validator that checks nothing -- used as a stand-in when you want only a pre- or only a post-validator.
     */
    Validator NOOP = (session, configurationModel) -> {};

    void validate(final Session session, final ConfigurationModelImpl configurationModel) throws Exception;
}
