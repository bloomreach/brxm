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
  let $q;
  let $rootScope;
  let context;
  let extension;
  let ChannelService;
  let DomService;
  let HippoIframeService;
  let OpenUiService;
  let connection;
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
    DomService = jasmine.createSpyObj('DomService', ['getIframeWindow']);
    HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);
    OpenUiService = jasmine.createSpyObj('OpenUiService', ['initialize']);

    child = jasmine.createSpyObj('child', ['emitEvent']);

    connection = {
      promise: $q.resolve(child),
      destroy: jasmine.createSpy('destroy'),
    };

    OpenUiService.initialize.and.returnValue(connection);

    $element = angular.element('<div></div>');
    $ctrl = $componentController('iframeExtension', {
      $element,
      ChannelService,
      DomService,
      HippoIframeService,
      OpenUiService,
    }, {
      extensionId: extension.id,
      context,
    });
  });

  describe('$onInit', () => {
    it('connects to the child', () => {
      $ctrl.$onInit();
      $rootScope.$digest();

      expect(OpenUiService.initialize).toHaveBeenCalledWith(extension.id, {
        appendTo: $element[0],
        methods: jasmine.any(Object),
      });
      expect($ctrl.child).toBe(child);
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
    });
  });

  describe('API for client library', () => {
    let methods;

    beforeEach(() => {
      $ctrl.$onInit();
      const [, args] = OpenUiService.initialize.calls.mostRecent().args;
      ({ methods } = args);
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

  describe('$onDestroy', () => {
    describe('without a connected child', () => {
      it('does nothing', () => {
        expect(() => {
          $ctrl.$onDestroy();
        }).not.toThrow();
      });
    });

    describe('with a connected child', () => {
      beforeEach(() => {
        $ctrl.$onInit();
        $rootScope.$digest();
      });

      it('destroys the connection', () => {
        $ctrl.$onDestroy();
        expect(connection.destroy).toHaveBeenCalled();
      });

      it('unsubscribes from events', () => {
        $ctrl.$onDestroy();
        $rootScope.$emit('channel:changes:publish');
        $rootScope.$emit('channel:changes:discard');
        expect(child.emitEvent).not.toHaveBeenCalled();
      });
    });
  });
});
