/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import * as HstConstants from './constants';
import ModelFactoryService from './model-factory.service';
import {
  Component,
  Container,
  EndMarker,
  HeadContributions,
  MenuLink,
  ManageContentLink,
  PageMeta,
} from './entities';

const modelModule = angular
  .module('hippo-cm-model', [])
  .service('ModelFactoryService', ModelFactoryService)

  // eslint-disable-next-line no-shadow
  .run((ModelFactoryService) => {
    'ngInject';

    ModelFactoryService
      .register(HstConstants.TYPE_COMPONENT, meta => new Component(meta))
      .register(HstConstants.TYPE_CONTAINER, meta => new Container(meta))
      .register(HstConstants.TYPE_EDIT_MENU_LINK, meta => new MenuLink(meta))
      .register(HstConstants.END_MARKER, meta => new EndMarker(meta))
      .register(HstConstants.TYPE_MANAGE_CONTENT_LINK, meta => new ManageContentLink(meta))
      .register(HstConstants.TYPE_PROCESSED_HEAD_CONTRIBUTIONS, meta => new HeadContributions(meta))
      .register(HstConstants.TYPE_UNPROCESSED_HEAD_CONTRIBUTIONS, meta => new HeadContributions(meta))
      .register(HstConstants.TYPE_PAGE_META, meta => new PageMeta(meta));
  });

export default modelModule;
