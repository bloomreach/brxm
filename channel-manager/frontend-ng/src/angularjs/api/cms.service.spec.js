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

describe('CmsService', function () {
  'use strict';

  var $window;
  var CmsService;

  beforeEach(function () {
    module('hippo-cm-api');

    inject(function (_$window_, _CmsService_) {
      $window = _$window_;
      CmsService = _CmsService_;
    });
  });

  describe('in production mode', function () {
    it('should publish events to the CMS', function () {
      CmsService.publish('browseTo', '/about');
      expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('browseTo', '/about');
    });

    it('should subscribe to events from the CMS', function () {
      CmsService.subscribe('test', 'callback', 'scope');
      expect($window.CMS_TO_APP.subscribe).toHaveBeenCalledWith('test', 'callback', 'scope');
    });

    it('should return the app configuration specified by the CMS', function () {
      expect(CmsService.getConfig()).toEqual(window.APP_CONFIG);
    });

    it('should throw an error when the CMS does not contain an ExtJs IFramePanel with the given ID', function () {
      spyOn($window.parent.Ext, 'getCmp').and.returnValue(undefined);
      expect(function () {
        CmsService.getConfig();
      }).toThrow(new Error("Unknown iframe panel id: 'ext-42'"));
    });

    it('should throw an error when the CMS\'s IFramePanel does not contain any configuration for the app', function () {
      spyOn($window.parent.Ext, 'getCmp').and.returnValue({
        initialConfig: {},
      });
      expect(function () {
        CmsService.getConfig();
      }).toThrowError(Error, 'Parent iframe panel does not contain iframe configuration');
    });

    it('should throw an error when the IFrame URL does not contain request parameter \'parentExtIFramePanelId\'', function () {
      window.history.replaceState({}, document.title, '/');
      expect(function () {
        CmsService.getParentIFramePanelId();
      }).toThrowError(Error, 'Request parameter \'parentExtIFramePanelId\' not found in IFrame url');
    });
  });
});
