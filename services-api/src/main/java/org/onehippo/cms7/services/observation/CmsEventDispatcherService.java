/*
 * Copyright 2017-2023 Bloomreach
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
package org.onehippo.cms7.services.observation;

import javax.jcr.Node;

/**
 * <p>
 *     Service that if present can be used to to dispatch nodes for which the CMS has to reload its wicket model
 * </p>
 * <p>
 *     Note, in case of [SITE + REPOSITORY] deployments without CMS, this service won't be present since the CMS will register
 *     it
 * </p>
 *
 */
public interface CmsEventDispatcherService {

    void events(Node... nodes);

}
