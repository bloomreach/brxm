/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

import CatalogService from './catalog.service';
import CmsService from './cms.service';
import ConfigService from './config.service';
import DialogService from './dialog.service';
import HstService from './hst.service';
import SessionService from './session.service';
import SiteMapService from './siteMap.service';
import SiteMapItemService from './siteMapItem.service';
import SiteMenuService from './siteMenu.service';
import HstConstants from './hst.constants';

const channelManagerApi = angular
  .module('hippo-cm-api', [])
  .service('CatalogService', CatalogService)
  .service('CmsService', CmsService)
  .service('ConfigService', ConfigService)
  .service('DialogService', DialogService)
  .service('HstService', HstService)
  .service('SessionService', SessionService)
  .service('SiteMapService', SiteMapService)
  .service('SiteMapItemService', SiteMapItemService)
  .service('SiteMenuService', SiteMenuService)
  .constant('HstConstants', HstConstants);

export default channelManagerApi;
