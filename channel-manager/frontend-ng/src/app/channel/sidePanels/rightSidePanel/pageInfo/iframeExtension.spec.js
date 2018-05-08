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

    extension = {
      displayName: 'Test',
      id: 'test',
      urlPath: 'testUrlPath',
    };

    const uiRouterParams = angular.copy(extension);
    uiRouterParams.extensionId = extension.id;
    uiRouterParams.pageUrl = 'testPageUrl';
    const $uiRouterGlobals = { params: uiRouterParams };

    DomService = jasmine.createSpyObj('DomService', ['getIframeWindow']);
    ExtensionService = jasmine.createSpyObj('ExtensionService', ['getExtension']);

    const $scope = $rootScope.$new();
    $element = angular.element('<iframe src="about:blank"></iframe>');
    $ctrl = $componentController('iframeExtension', {
      $element,
      $scope,
      $uiRouterGlobals,
      DomService,
      ExtensionService,
    });
  });

  describe('$onInit', () => {
    it('initializes the page extension', () => {
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

  describe('on iframe load', () => {
    let iframeJQueryElement;
    let iframeWindow;

    beforeEach(() => {
      ExtensionService.getExtension.and.returnValue(extension);

      iframeJQueryElement = jasmine.createSpyObj('iframe', ['on']);
      iframeWindow = {};

      spyOn($element, 'children').and.returnValue(iframeJQueryElement);
      DomService.getIframeWindow.and.returnValue(iframeWindow);
      spyOn($log, 'warn');
    });

    function triggerIframeLoad() {
      $ctrl.$onInit();
      const onLoad = iframeJQueryElement.on.calls.mostRecent().args[1];
      onLoad();
    }

    describe('without a correct API', () => {
      it('logs a warning when the BR_EXTENSION object does not exist', () => {
        triggerIframeLoad();
        expect($log.warn).toHaveBeenCalledWith('Page info extension \'Test\' does not define a window.BR_EXTENSION object, cannot provide page context');
      });

      it('logs a warning when the BR_EXTENSION object is not an object', () => {
        iframeWindow.BR_EXTENSION = () => true;
        triggerIframeLoad();
        expect($log.warn).toHaveBeenCalledWith('Page info extension \'Test\' does not define a window.BR_EXTENSION object, cannot provide page context');
      });

      it('logs a warning when the BR_EXTENSION.onContextChanged function does not exist', () => {
        iframeWindow.BR_EXTENSION = {};
        triggerIframeLoad();
        expect($log.warn).toHaveBeenCalledWith('Page info extension \'Test\' does not define a window.BR_EXTENSION.onContextChanged function, cannot provide page context');
      });
    });

    describe('with a correct API', () => {
      beforeEach(() => {
        iframeWindow.BR_EXTENSION = jasmine.createSpyObj('BR_EXTENSION', ['onContextChanged']);
      });

      it('calls the BR_EXTENSION.onContextChanged function', () => {
        triggerIframeLoad();
        expect(iframeWindow.BR_EXTENSION.onContextChanged).toHaveBeenCalledWith({
          context: 'page',
          data: {
            pageUrl: 'testPageUrl',
          },
        });
      });

      it('logs a warning when BR_EXTENSION.onContextChanged throws an error', () => {
        const error = new Error('EEK');
        iframeWindow.BR_EXTENSION.onContextChanged.and.throwError(error);
        triggerIframeLoad();
        expect($log.warn).toHaveBeenCalledWith('Page info extension \'Test\' threw an error in window.BR_EXTENSION.onContextChanged()', error);
      });
    });
  });

  describe('uiOnParamsChanged', () => {
    beforeEach(() => {
      spyOn($ctrl, '_setPageContext');
    });

    it('updates the page context when the pageUrl parameter changed', () => {
      $ctrl.uiOnParamsChanged({
        pageUrl: '/newPageUrl',
      });
      expect($ctrl._setPageContext).toHaveBeenCalled();
    });

    it('does not update the page context when the pageUrl parameter did not change', () => {
      $ctrl.uiOnParamsChanged({});
      expect($ctrl._setPageContext).not.toHaveBeenCalled();
    });
  });
});
