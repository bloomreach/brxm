/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

// TODO: Move some of these toplevel modules into functional specific folders/modules
import BrowserService from './browser.service';
import CatalogService from './catalog.service';
import CmsService from './cms.service';
import ConfigService from './config.service';
import ContentService from './content.service';
import DialogService from './dialog.service';
import DomService from './dom.service';
import FeedbackService from './feedback.service';
import HippoGlobal from './hippoGlobal.service';
import HstComponentService from './hstComponent.service';
import HstService from './hst.service';
import PathService from './path.service';
import ProjectService from './project.service';
import SessionService from './session.service';
import SiteMapItemService from './siteMapItem.service';
import SiteMapService from './siteMap.service';
import SiteMenuService from './siteMenu.service';
import WorkflowService from './workflow.service';

const servicesModule = angular
  .module('hippo-cm.services', [])
  .service('BrowserService', BrowserService)
  .service('CatalogService', CatalogService)
  .service('CmsService', CmsService)
  .service('ConfigService', ConfigService)
  .service('ContentService', ContentService)
  .service('DialogService', DialogService)
  .service('DomService', DomService)
  .service('FeedbackService', FeedbackService)
  .service('HippoGlobal', HippoGlobal)
  .service('HstComponentService', HstComponentService)
  .service('HstService', HstService)
  .service('PathService', PathService)
  .service('ProjectService', ProjectService)
  .service('SessionService', SessionService)
  .service('SiteMapItemService', SiteMapItemService)
  .service('SiteMapService', SiteMapService)
  .service('SiteMenuService', SiteMenuService)
  .service('WorkflowService', WorkflowService);

export default servicesModule.name;
