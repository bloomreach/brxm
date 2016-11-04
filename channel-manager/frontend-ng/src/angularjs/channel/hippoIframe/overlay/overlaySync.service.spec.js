/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

describe('OverlaySyncService', () => {
  let OverlaySyncService;
  let $iframe;
  let $base;
  let $sheet;
  let $scrollX;
  let $overlay;
  let $window;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe.overlay');

    inject((_$window_, _OverlaySyncService_, _DomService_) => {
      $window = _$window_;
      OverlaySyncService = _OverlaySyncService_;

      spyOn(_DomService_, 'getScrollBarWidth').and.returnValue(15);
    });

    jasmine.getFixtures().load('channel/hippoIframe/overlay/overlaySync.service.fixture.html');

    $base = $j('.channel-iframe-base');
    $sheet = $j('.channel-iframe-sheet');
    $scrollX = $j('.channel-iframe-scroll-x');
    $iframe = $j('.iframe');
    $overlay = $j('.overlay');
  });

  function loadIframeFixture(callback) {
    OverlaySyncService.init($base, $sheet, $scrollX, $iframe, $overlay);
    $iframe.one('load', () => {
      const iframeWindow = $iframe[0].contentWindow;
      try {
        callback(iframeWindow);
      } catch (e) {
        // Karma silently swallows stack traces for synchronous tests, so log them in an explicit fail
        fail(e);
      }
    });
    $iframe.attr('src', `/${jasmine.getFixtures().fixturesPath}/channel/hippoIframe/overlay/overlaySync.service.iframe.fixture.html`);
  }

  it('should initialize using the default values', (done) => {
    spyOn(OverlaySyncService, '_onLoad');
    loadIframeFixture(() => {
      expect(OverlaySyncService._onLoad).toHaveBeenCalled();
      done();
    });
  });

  it('should not throw errors when sync is called before init', () => {
    expect(() => OverlaySyncService.syncIframe()).not.toThrow();
  });

  it('should attach an unload handler to the iframe', (done) => {
    spyOn(OverlaySyncService, '_onUnLoad');
    loadIframeFixture(() => {
      // load URL again to cause unload
      loadIframeFixture(() => {
        expect(OverlaySyncService._onUnLoad).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should sync on first load', (done) => {
    spyOn(OverlaySyncService, 'syncIframe');
    loadIframeFixture(() => {
      expect(OverlaySyncService.syncIframe).toHaveBeenCalled();
      done();
    });
  });

  it('should attach a MutationObserver on the iframe document on first load', (done) => {
    loadIframeFixture(() => {
      expect(OverlaySyncService.observer).not.toBeNull();
      done();
    });
  });

  it('should disconnect the MutationObserver on iframe unload', (done) => {
    loadIframeFixture(() => {
      const disconnect = spyOn(OverlaySyncService.observer, 'disconnect').and.callThrough();
      // load URL again to cause unload
      loadIframeFixture(() => {
        expect(disconnect).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should trigger onDOMChanged when the iframe DOM is changed', (done) => {
    spyOn(OverlaySyncService, 'onDOMChanged');
    loadIframeFixture((iframeWindow) => {
      OverlaySyncService.onDOMChanged.calls.reset();
      OverlaySyncService.onDOMChanged.and.callFake(done);
      $(iframeWindow.document.body).css('color', 'green');
    });
  });

  it('should sync when the iframe DOM is changed', (done) => {
    spyOn(OverlaySyncService, 'syncIframe');
    loadIframeFixture((iframeWindow) => {
      OverlaySyncService.syncIframe.calls.reset();
      OverlaySyncService.syncIframe.and.callFake(done);
      $(iframeWindow.document.body).css('color', 'green');
    });
  });

  it('should sync when the browser is resized', (done) => {
    spyOn(OverlaySyncService, 'syncIframe');
    loadIframeFixture(() => {
      OverlaySyncService.syncIframe.calls.reset();
      $($window).trigger('resize');
      expect(OverlaySyncService.syncIframe).toHaveBeenCalled();
      done();
    });
  });

  it('should sync the overlay when the iframe is resized', (done) => {
    spyOn(OverlaySyncService, '_syncOverlayElements');
    loadIframeFixture(() => {
      OverlaySyncService._syncOverlayElements.calls.reset();
      $($iframe[0].contentWindow).trigger('resize');
      expect(OverlaySyncService._syncOverlayElements).toHaveBeenCalled();
      done();
    });
  });

  it('should not constrain the viewport by default', (done) => {
    spyOn(OverlaySyncService, 'onDOMChanged');

    loadIframeFixture(() => {
      const $container = $iframe.contents().find('.container');

      $container.width(1200);
      $container.height(600);
      OverlaySyncService.syncIframe();

      expect($sheet).toHaveCss({
        'max-width': 'none',
      });
      expect($iframe).toHaveCss({
        'min-width': '1280px',
      });

      expect($iframe.height()).toEqual(600 + 2);
      expect($overlay.height()).toEqual(600 + 2);
      expect($scrollX.height()).toEqual(600 + 2);

      done();
    });
  });

  it('should calculate the iframe height based on the height marker element when present', (done) => {
    spyOn(OverlaySyncService, 'onDOMChanged');

    loadIframeFixture(() => {
      $iframe.contents().find('body').append(`<div class="hippo-channel-manager-page-height-marker" 
                                                   style="position: absolute; top: 1000px; margin-bottom: 42px;"/>`);
      OverlaySyncService.syncIframe();

      expect($iframe.height()).toEqual(1042);
      expect($overlay.height()).toEqual(1042);
      expect($scrollX.height()).toEqual(1042);

      done();
    });
  });

  it('should constrain the maximum width to the viewport', (done) => {
    loadIframeFixture(() => {
      OverlaySyncService.setViewPortWidth(720);
      OverlaySyncService.syncIframe();
      expect($sheet.width()).toEqual(720);
      expect($iframe.width()).toEqual(720);
      expect($overlay.width()).toEqual(720);
      done();
    });
  });

  it('should show a horizontal scrollbar when viewport is constrained and site is not responsive', (done) => {
    spyOn(OverlaySyncService, 'onDOMChanged');

    loadIframeFixture(() => {
      const $container = $iframe.contents().find('.container');
      $container.width(1200);
      $container.height(600);

      OverlaySyncService.setViewPortWidth(720);
      OverlaySyncService.syncIframe();

      expect($iframe.width()).toEqual(1200 + 4);
      expect($overlay.width()).toEqual(1200 + 4);

      expect($iframe.height()).toEqual(600 + 2);
      expect($overlay.height()).toEqual(600 + 2);
      expect($scrollX.height()).toEqual(600 + 15 + 2);
      done();
    });
  });

  it('should be able to (un)register an overlay element', () => {
    spyOn(OverlaySyncService, '_syncElement');
    const element = {};

    OverlaySyncService.registerElement(element);
    expect(OverlaySyncService.overlayElements).toContain(element);

    OverlaySyncService.unregisterElement(element);
    expect(OverlaySyncService.overlayElements).not.toContain(element);
  });

  it('should sync a registered overlay element', (done) => {
    loadIframeFixture(() => {
      const overlayEl = $('<div style="position: absolute"></div>');
      $overlay.append(overlayEl);
      const boxEl = $iframe.contents().find('.container');

      const element = jasmine.createSpyObj('element', ['getOverlayElement', 'getBoxElement']);
      element.getOverlayElement.and.returnValue(overlayEl);
      element.getBoxElement.and.returnValue(boxEl);

      OverlaySyncService.registerElement(element);

      expect(OverlaySyncService.overlayElements).toContain(element);
      expect(overlayEl.width()).toEqual(200);
      expect(overlayEl.height()).toEqual(400);
      expect(overlayEl.offset()).toEqual({ top: 2, left: 4 });

      done();
    });
  });
});
