/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
  let $rootScope;
  let context;
  let extension;
  let DomService;
  let ExtensionService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$log_, _$rootScope_) => {
      $componentController = _$componentController_;
      $log = _$log_;
      $rootScope = _$rootScope_;
    });

    context = {
      foo: 1,
    };

    extension = {
      id: 'test',
      displayName: 'Test',
      context: 'testContext',
      urlPath: 'testUrlPath',
    };

    DomService = jasmine.createSpyObj('DomService', ['getIframeWindow']);
    ExtensionService = jasmine.createSpyObj('ExtensionService', ['getExtension']);

    const $scope = $rootScope.$new();
    $element = angular.element('<iframe src="about:blank"></iframe>');
    $ctrl = $componentController('iframeExtension', {
      $element,
      $scope,
      DomService,
      ExtensionService,
    }, {
      extensionId: extension.id,
      context,
    });
  });

  describe('$onInit', () => {
    it('initializes the extension', () => {
      ExtensionService.getExtension.and.returnValue(extension);

      $ctrl.$onInit();

      expect(ExtensionService.getExtension).toHaveBeenCalledWith('test');
      expect($ctrl.extension).toEqual(extension);
    });

    it('listens to the iframe load event', () => {
      const iframeJQueryElement = jasmine.createSpyObj('iframe', ['on']);
      const iframeWindow = {};

      spyOn($element, 'children').and.returnValue(iframeJQueryElement);
      DomService.getIframeWindow.and.returnValue(iframeWindow);

      $ctrl.$onInit();

      expect($element.children).toHaveBeenCalledWith('.iframe-extension');
      expect(DomService.getIframeWindow).toHaveBeenCalledWith(iframeJQueryElement);
      expect($ctrl.iframeWindow).toBe(iframeWindow);
      expect(iframeJQueryElement.on).toHaveBeenCalledWith('load', jasmine.any(Function));
    });
  });

  describe('when initialized', () => {
    let iframeJQueryElement;
    let iframeWindow;

    beforeEach(() => {
      ExtensionService.getExtension.and.returnValue(extension);

      iframeJQueryElement = jasmine.createSpyObj('iframe', ['on']);
      iframeWindow = {};

      spyOn($element, 'children').and.returnValue(iframeJQueryElement);
      DomService.getIframeWindow.and.returnValue(iframeWindow);
    });

    function triggerIframeLoad() {
      $ctrl.$onInit();
      const onLoad = iframeJQueryElement.on.calls.mostRecent().args[1];
      onLoad();
    }

    describe('on iframe load', () => {
      beforeEach(() => {
        spyOn($log, 'warn');
      });

      describe('without a correct API', () => {
        it('logs a warning when the BR_EXTENSION object does not exist', () => {
          triggerIframeLoad();
          expect($log.warn).toHaveBeenCalledWith('Extension \'Test\' does not define a window.BR_EXTENSION object, cannot provide context');
        });

        it('logs a warning when the BR_EXTENSION object is not an object', () => {
          iframeWindow.BR_EXTENSION = () => true;
          triggerIframeLoad();
          expect($log.warn).toHaveBeenCalledWith('Extension \'Test\' does not define a window.BR_EXTENSION object, cannot provide context');
        });

        it('logs a warning when the BR_EXTENSION.onContextChanged function does not exist', () => {
          iframeWindow.BR_EXTENSION = {};
          triggerIframeLoad();
          expect($log.warn).toHaveBeenCalledWith('Extension \'Test\' does not define a window.BR_EXTENSION.onContextChanged function, cannot provide context');
        });
      });

      describe('with a correct API', () => {
        beforeEach(() => {
          iframeWindow.BR_EXTENSION = jasmine.createSpyObj('BR_EXTENSION', ['onContextChanged']);
        });

        it('calls the BR_EXTENSION.onContextChanged function', () => {
          triggerIframeLoad();
          expect(iframeWindow.BR_EXTENSION.onContextChanged).toHaveBeenCalledWith({
            context: 'testContext',
            data: {
              foo: 1,
            },
          });
        });

        it('logs a warning when BR_EXTENSION.onContextChanged throws an error', () => {
          const error = new Error('EEK');
          iframeWindow.BR_EXTENSION.onContextChanged.and.throwError(error);
          triggerIframeLoad();
          expect($log.warn).toHaveBeenCalledWith('Extension \'Test\' threw an error in window.BR_EXTENSION.onContextChanged()', error);
        });
      });
    });

    describe('$onChanges', () => {
      let changedContext;
      let isFirstChange;

      beforeEach(() => {
        iframeWindow.BR_EXTENSION = jasmine.createSpyObj('BR_EXTENSION', ['onContextChanged']);
        changedContext = {
          currentValue: {
            foo: 2,
          },
          isFirstChange: () => isFirstChange,
        };
      });

      it('does not update the context when it did not change', () => {
        $ctrl.$onChanges({});
        expect(iframeWindow.BR_EXTENSION.onContextChanged).not.toHaveBeenCalled();
      });

      describe('before the iframe is loaded', () => {
        it('does not update the context for the first change', () => {
          isFirstChange = true;
          $ctrl.$onChanges({ context: changedContext });
          expect(iframeWindow.BR_EXTENSION.onContextChanged).not.toHaveBeenCalled();
        });

        it('does not update the context for subsequent changes', () => {
          isFirstChange = false;
          $ctrl.$onChanges({ context: changedContext });
          expect(iframeWindow.BR_EXTENSION.onContextChanged).not.toHaveBeenCalled();
        });
      });

      describe('after the iframe is loaded', () => {
        beforeEach(() => {
          triggerIframeLoad();
          iframeWindow.BR_EXTENSION.onContextChanged.calls.reset();
        });

        it('does not update the context for the first change', () => {
          isFirstChange = true;
          $ctrl.$onChanges({ context: changedContext });
          expect(iframeWindow.BR_EXTENSION.onContextChanged).not.toHaveBeenCalled();
        });

        it('updates the context for subsequent changes', () => {
          isFirstChange = false;
          $ctrl.$onChanges({ context: changedContext });
          expect(iframeWindow.BR_EXTENSION.onContextChanged).toHaveBeenCalledWith({
            context: 'testContext',
            data: {
              foo: 2,
            },
          });
        });
      });
    });
  });
});
