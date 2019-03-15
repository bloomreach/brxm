/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

describe('iframeExtension', () => {
  let $componentController;
  let $ctrl;
  let $element;
  let $log;
  let $q;
  let $rootScope;
  let context;
  let extension;
  let ChannelService;
  let ConfigService;
  let DomService;
  let ExtensionService;
  let HippoIframeService;
  let OpenUiService;
  let child;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$q_, _$rootScope_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
    });

    context = {
      id: 1,
    };

    extension = {
      id: 'test',
      displayName: 'Test',
      extensionPoint: 'testExtensionPoint',
      url: '/testUrl',
      config: 'testConfig',
    };

    ChannelService = jasmine.createSpyObj('ChannelService', ['reload']);
    ConfigService = jasmine.createSpyObj('ConfigService', ['getCmsContextPath', 'getCmsOrigin']);
    DomService = jasmine.createSpyObj('DomService', ['getIframeWindow']);
    ExtensionService = jasmine.createSpyObj('ExtensionService', ['getExtension', 'getExtensionUrl']);
    HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);
    OpenUiService = jasmine.createSpyObj('OpenUiService', ['connect']);
    $log = jasmine.createSpyObj('$log', ['warn']);

    ExtensionService.getExtension.and.returnValue(extension);

    child = jasmine.createSpyObj('child', ['emitEvent']);

    OpenUiService.connect.and.returnValue($q.resolve(child));

    $element = angular.element('<div></div>');
    $ctrl = $componentController('iframeExtension', {
      $element,
      $log,
      ChannelService,
      ConfigService,
      DomService,
      ExtensionService,
      HippoIframeService,
      OpenUiService,
    }, {
      extensionId: extension.id,
      context,
    });
  });

  describe('$onInit', () => {
    it('initializes the extension', () => {
      $ctrl.$onInit();

      expect(ExtensionService.getExtension).toHaveBeenCalledWith('test');
      expect($ctrl.extension).toEqual(extension);
    });

    it('connects to the child', () => {
      ExtensionService.getExtensionUrl.and.returnValue('some-url');

      $ctrl.$onInit();
      $rootScope.$digest();

      expect(OpenUiService.connect).toHaveBeenCalledWith({
        url: 'some-url',
        appendTo: $element[0],
        methods: jasmine.any(Object),
      });
      expect($ctrl.child).toBe(child);
    });

    it('logs a warning when connecting to the child failed', () => {
      ExtensionService.getExtensionUrl.and.returnValue('some-url');

      const error = new Error('Connection destroyed');
      OpenUiService.connect.and.returnValue($q.reject(error));

      $ctrl.$onInit();
      $rootScope.$digest();

      expect(OpenUiService.connect).toHaveBeenCalled();
      expect($log.warn).toHaveBeenCalledWith("Extension 'Test' failed to connect with the client library.", error);
    });

    describe('channel events', () => {
      beforeEach(() => {
        $ctrl.$onInit();
        $rootScope.$digest();
      });

      it('reacts on channel:changes:publish events', () => {
        $rootScope.$emit('channel:changes:publish');
        expect(child.emitEvent).toHaveBeenCalledWith('channel.changes.publish');
      });

      it('reacts on channel:changes:discard events', () => {
        $rootScope.$emit('channel:changes:discard');
        expect(child.emitEvent).toHaveBeenCalledWith('channel.changes.discard');
      });

      it('unsubscribes from event', () => {
        $ctrl.$onDestroy();
        $rootScope.$emit('channel:changes:publish');
        $rootScope.$emit('channel:changes:discard');
        expect(child.emitEvent).not.toHaveBeenCalled();
      });
    });
  });

  describe('API for client library', () => {
    let methods;

    beforeEach(() => {
      $ctrl.$onInit();
      const [args] = OpenUiService.connect.calls.mostRecent().args;
      ({ methods } = args);
    });

    describe('getProperties', () => {
      describe('baseUrl', () => {
        it('is set to the base URL of a CMS on localhost', () => {
          ConfigService.getCmsContextPath.and.returnValue('/cms/');
          ConfigService.getCmsOrigin.and.returnValue('http://localhost:8080');
          expect(methods.getProperties().baseUrl).toBe('http://localhost:8080/cms/');
        });

        it('is set to the base URL of a CMS in production', () => {
          ConfigService.getCmsContextPath.and.returnValue('/');
          ConfigService.getCmsOrigin.and.returnValue('https://cms.example.com');
          expect(methods.getProperties().baseUrl).toBe('https://cms.example.com/');
        });
      });

      describe('extension config', () => {
        it('is set to the config string of the extension', () => {
          expect(methods.getProperties().extension.config).toBe('testConfig');
        });
      });

      describe('locale', () => {
        it('is set to the current CMS locale', () => {
          ConfigService.locale = 'fr';
          expect(methods.getProperties().locale).toBe('fr');
        });
      });

      describe('timeZone', () => {
        it('is set to the current CMS time zone', () => {
          ConfigService.timeZone = 'Europe/Amsterdam';
          expect(methods.getProperties().timeZone).toBe('Europe/Amsterdam');
        });
      });

      describe('in user data', () => {
        it('sets the id to the current CMS user name', () => {
          ConfigService.cmsUser = 'editor';
          expect(methods.getProperties().user.id).toBe('editor');
        });

        it('sets the firstName to the current CMS users first name', () => {
          ConfigService.cmsUserFirstName = 'Ed';
          expect(methods.getProperties().user.firstName).toBe('Ed');
        });

        it('sets the lastName to the current CMS users last name', () => {
          ConfigService.cmsUserLastName = 'Itor';
          expect(methods.getProperties().user.lastName).toBe('Itor');
        });

        it('sets the displayName to the current CMS users display name', () => {
          ConfigService.cmsUserDisplayName = 'Ed Itor';
          expect(methods.getProperties().user.displayName).toBe('Ed Itor');
        });
      });

      describe('version', () => {
        it('is set to the current CMS version', () => {
          ConfigService.cmsVersion = '13.0.0';
          expect(methods.getProperties().version).toBe('13.0.0');
        });
      });
    });

    describe('getPage', () => {
      it('returns the current context', () => {
        expect(methods.getPage()).toBe(context);
      });
    });

    describe('refreshChannel', () => {
      it('reloads the current channel meta-data', () => {
        methods.refreshChannel();
        expect(ChannelService.reload).toHaveBeenCalled();
      });
    });

    describe('refreshPage', () => {
      it('reloads the page', () => {
        methods.refreshPage();
        expect(HippoIframeService.reload).toHaveBeenCalled();
      });
    });
  });

  describe('$onChanges', () => {
    let newContext;

    beforeEach(() => {
      newContext = {
        id: '2',
      };
    });

    describe('without a connected child', () => {
      it('ignores changes without a context', () => {
        $ctrl.$onChanges({});
        expect($ctrl.context).toBe(context);
      });

      it('remembers a copy of the new context', () => {
        $ctrl.$onChanges({
          context: {
            currentValue: newContext,
          },
        });

        expect($ctrl.context).toEqual(newContext);
        expect($ctrl.context).not.toBe(newContext);
      });
    });

    describe('with a connected child', () => {
      beforeEach(() => {
        $ctrl.$onInit();
        $rootScope.$digest();
      });

      it('ignores changes without a context', () => {
        $ctrl.$onChanges({});
        expect($ctrl.context).toBe(context);
      });

      it('remembers a copy of the new context', () => {
        $ctrl.$onChanges({
          context: {
            currentValue: newContext,
          },
        });

        expect($ctrl.context).toEqual(newContext);
        expect($ctrl.context).not.toBe(newContext);
      });

      it('emits a "channel.page.navigate" event in the child with the new page properties', () => {
        $ctrl.$onChanges({
          context: {
            currentValue: newContext,
          },
        });

        expect(child.emitEvent).toHaveBeenCalledWith('channel.page.navigate', newContext);
      });
    });
  });
});
