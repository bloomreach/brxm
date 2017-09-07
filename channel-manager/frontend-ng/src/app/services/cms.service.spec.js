/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
import 'angular-mocks';

describe('CmsService', () => {
  let $rootScope;
  let $window;
  let CmsService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _$window_, _CmsService_) => {
      $rootScope = _$rootScope_;
      $window = _$window_;
      CmsService = _CmsService_;
    });

    spyOn($window.APP_TO_CMS, 'publish').and.callThrough();
    spyOn($window.CMS_TO_APP, 'subscribe').and.callThrough();
    spyOn($window.CMS_TO_APP, 'subscribeOnce').and.callThrough();
  });

  it('publishes events to the CMS', () => {
    CmsService.publish('browseTo', '/about');
    expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('browseTo', '/about');
  });

  it('subscribes to events from the CMS', () => {
    CmsService.subscribe('test', 'callback');
    expect($window.CMS_TO_APP.subscribe).toHaveBeenCalledWith('test', 'callback');
  });

  it('subscribes to events from the CMS with a function scope', () => {
    CmsService.subscribe('test', 'callback', 'scope');
    expect($window.CMS_TO_APP.subscribe).toHaveBeenCalledWith('test', 'callback', 'scope');
  });

  it('subscribes once to events from the CMS', () => {
    CmsService.subscribeOnce('test', 'callback');
    expect($window.CMS_TO_APP.subscribeOnce).toHaveBeenCalledWith('test', 'callback');
  });

  it('subscribes once to events from the CMS with a function scope', () => {
    CmsService.subscribeOnce('test', 'callback', 'scope');
    expect($window.CMS_TO_APP.subscribeOnce).toHaveBeenCalledWith('test', 'callback', 'scope');
  });

  it('unsubscribes from events from the CMS', () => {
    const mock = jasmine.createSpyObj('mock', ['test']);

    CmsService.subscribe('test', mock.test, mock);

    $window.CMS_TO_APP.publish('test');
    expect(mock.test).toHaveBeenCalled();

    mock.test.calls.reset();
    CmsService.unsubscribe('test', mock.test, mock);
    $window.CMS_TO_APP.publish('test');
    expect(mock.test).not.toHaveBeenCalled();
  });

  it('returns the app configuration specified by the CMS', () => {
    expect(CmsService.getConfig()).toEqual($window.APP_CONFIG);
  });

  it('throws an error when the CMS does not contain an ExtJs IFramePanel with the given ID', () => {
    spyOn($window.parent.Ext, 'getCmp').and.returnValue(undefined);
    expect(() => {
      CmsService.getConfig();
    }).toThrow(new Error("Unknown iframe panel id: 'ext-42'"));
  });

  it('throws an error when the CMS\'s IFramePanel does not contain any configuration for the app', () => {
    spyOn($window.parent.Ext, 'getCmp').and.returnValue({
      initialConfig: {},
    });
    expect(() => {
      CmsService.getConfig();
    }).toThrowError(Error, 'Parent iframe panel does not contain iframe configuration');
  });

  it('throws an error when the IFrame URL does not contain request parameter \'parentExtIFramePanelId\'', () => {
    $window.location.search = '';
    expect(() => {
      CmsService.getParentIFramePanelId();
    }).toThrowError(Error, 'Request parameter \'parentExtIFramePanelId\' not found in IFrame url');
  });

  it('closes a valid document', (done) => {
    CmsService.closeDocumentWhenValid('test').then(done);

    expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('close-content', 'test');
    $window.CMS_TO_APP.publish('close-content-result', 'test', true);
    $rootScope.$digest();
  });

  it('does not close an invalid document', (done) => {
    CmsService.closeDocumentWhenValid('test').catch(done);

    expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('close-content', 'test');
    $window.CMS_TO_APP.publish('close-content-result', 'test', false);
    $rootScope.$digest();
  });

  it('closes different documents independently', () => {
    let okCount1 = 0;
    let errorCount1 = 0;
    let okCount2 = 0;
    let errorCount2 = 0;

    CmsService.closeDocumentWhenValid('one')
      .then(() => {
        okCount1 += 1;
      })
      .catch(() => {
        errorCount1 += 1;
      });
    expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('close-content', 'one');

    CmsService.closeDocumentWhenValid('two')
      .then(() => {
        okCount2 += 1;
      })
      .catch(() => {
        errorCount2 += 1;
      });
    expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('close-content', 'two');

    $window.CMS_TO_APP.publish('close-content-result', 'two', true);
    $window.CMS_TO_APP.publish('close-content-result', 'one', false);
    $rootScope.$digest();

    expect(okCount1).toBe(0);
    expect(errorCount1).toBe(1);
    expect(okCount2).toBe(1);
    expect(errorCount2).toBe(0);
  });

  it('closes the same document only once', () => {
    let okCount1 = 0;
    let errorCount1 = 0;
    let okCount2 = 0;
    let errorCount2 = 0;

    CmsService.closeDocumentWhenValid('test')
      .then(() => {
        okCount1 += 1;
      })
      .catch(() => {
        errorCount1 += 1;
      });
    expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('close-content', 'test');

    $window.APP_TO_CMS.publish.calls.reset();
    CmsService.closeDocumentWhenValid('test')
      .then(() => {
        okCount2 += 1;
      })
      .catch(() => {
        errorCount2 += 1;
      });
    expect($window.APP_TO_CMS.publish).not.toHaveBeenCalled();

    $window.CMS_TO_APP.publish('close-content-result', 'test', true);
    $rootScope.$digest();

    expect(okCount1).toBe(1);
    expect(okCount2).toBe(1);
    expect(errorCount1).toBe(0);
    expect(errorCount2).toBe(0);
  });

  it('reports a usage statistic', () => {
    spyOn($window.parent.Hippo.Events, 'publish');
    CmsService.reportUsageStatistic('something');
    expect($window.parent.Hippo.Events.publish).toHaveBeenCalledWith('something');
  });
});
