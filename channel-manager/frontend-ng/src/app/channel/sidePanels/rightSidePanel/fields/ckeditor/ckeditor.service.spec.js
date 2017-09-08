/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

describe('CKEditor Service', () => {
  let $log;
  let $q;
  let $rootScope;
  let $timeout;
  let $window;
  let CKEditorService;
  let ConfigService;
  let DomService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    $log = jasmine.createSpyObj('$log', ['info']);
    ConfigService = jasmine.createSpyObj('ConfigService', ['getCmsContextPath']);
    DomService = jasmine.createSpyObj('DomService', ['addScript']);

    angular.mock.module(($provide) => {
      $provide.value('$log', $log);
      $provide.value('ConfigService', ConfigService);
      $provide.value('DomService', DomService);
    });

    inject((_$q_, _$rootScope_, _$window_, _$timeout_, _CKEditorService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $window = _$window_;
      $timeout = _$timeout_;
      CKEditorService = _CKEditorService_;
    });

    ConfigService.getCmsContextPath.and.returnValue('/cms/');
    ConfigService.ckeditorUrl = 'ckeditor.js';
    ConfigService.ckeditorTimestamp = '42';
  });

  function expectLoaded(CKEDITOR) {
    expect(DomService.addScript).toHaveBeenCalledWith($window, '/cms/ckeditor.js');
    expect(CKEDITOR).toBeDefined();
    expect(CKEDITOR.timestamp).toBe('42');
  }

  it('loads CKEditor instantly', (done) => {
    DomService.addScript.and.callFake(() => {
      $window.CKEDITOR = {
        on: () => {},
        status: 'loaded',
      };
      return $q.resolve();
    });

    CKEditorService.loadCKEditor().then((CKEDITOR) => {
      expectLoaded(CKEDITOR);
      done();
    });

    $rootScope.$digest();
  });

  it('loads CKEditor by waiting for the "loaded" event', (done) => {
    DomService.addScript.and.callFake(() => {
      $window.CKEDITOR = {
        on: jasmine.createSpy('on'),
        status: 'unloaded',
      };
      return $q.resolve();
    });

    CKEditorService.loadCKEditor().then((CKEDITOR) => {
      expectLoaded(CKEDITOR);
      done();
    });

    $rootScope.$digest();

    expect($window.CKEDITOR.on).toHaveBeenCalledWith('loaded', jasmine.any(Function));
    const onLoaded = $window.CKEDITOR.on.calls.mostRecent().args[1];
    onLoaded();

    $rootScope.$digest();
  });

  it('loads CKEditor by polling until the editor has been loaded', (done) => {
    DomService.addScript.and.callFake(() => {
      $window.CKEDITOR = {};
      return $q.resolve();
    });

    CKEditorService.loadCKEditor().then((CKEDITOR) => {
      expectLoaded(CKEDITOR);
      done();
    });

    $rootScope.$digest();
    expect($log.info).toHaveBeenCalledWith('Waiting 4 ms for CKEditor\'s event mechanism to load...');

    $timeout.flush();
    expect($log.info).toHaveBeenCalledWith('Waiting 8 ms for CKEditor\'s event mechanism to load...');

    $timeout.flush();
    expect($log.info).toHaveBeenCalledWith('Waiting 16 ms for CKEditor\'s event mechanism to load...');

    $window.CKEDITOR.on = () => {};
    $window.CKEDITOR.status = 'loaded';
    $timeout.flush();
    $rootScope.$digest();
  });

  it('loads CKEditor just once', (done) => {
    DomService.addScript.and.callFake(() => {
      $window.CKEDITOR = {
        on: () => {},
        status: 'loaded',
      };
      return $q.resolve();
    });

    CKEditorService.loadCKEditor().then((CKEDITOR1) => {
      CKEditorService.loadCKEditor().then((CKEDITOR2) => {
        expect(CKEDITOR1 === CKEDITOR2);
        done();
      });
    });

    $rootScope.$digest();
  });
});
