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

describe('OverlayService', () => {
  let OverlayService;
  let PageStructureService;
  let $iframe;
  let iframeWindow;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    inject((_OverlayService_, _PageStructureService_) => {
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
    });

    jasmine.getFixtures().load('channel/hippoIframe/overlay.service.fixture.html');
    $iframe = $j('.iframe');
  });

  function loadIframeFixture(callback) {
    OverlayService.init($iframe);
    $iframe.one('load', () => {
      iframeWindow = $iframe[0].contentWindow;
      try {
        callback();
      } catch (e) {
        // Karma silently swallows stack traces for synchronous tests, so log them in an explicit fail
        fail(e);
      }
    });
    $iframe.attr('src', `/${jasmine.getFixtures().fixturesPath}/channel/hippoIframe/overlay.service.iframe.fixture.html`);
  }

  function iframe(selector) {
    return $j(selector, iframeWindow.document);
  }

  it('should initialize when the iframe is loaded', (done) => {
    spyOn(OverlayService, '_onLoad');
    loadIframeFixture(() => {
      expect(OverlayService._onLoad).toHaveBeenCalled();
      done();
    });
  });

  it('should not throw errors when the iframe is loaded but does not have a document (e.g. loaded a PDF)', () => {
    expect(() => OverlayService._onLoad()).not.toThrow();
  });

  it('should not throw errors when synced before init', () => {
    expect(() => OverlayService.sync()).not.toThrow();
  });

  it('should attach an unload handler to the iframe', (done) => {
    spyOn(OverlayService, '_onUnload');
    loadIframeFixture(() => {
      // load URL again to cause unload
      loadIframeFixture(() => {
        expect(OverlayService._onUnload).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should attach a MutationObserver on the iframe document on first load', (done) => {
    loadIframeFixture(() => {
      expect(OverlayService.observer).not.toBeNull();
      done();
    });
  });

  it('should disconnect the MutationObserver on iframe unload', (done) => {
    loadIframeFixture(() => {
      const disconnect = spyOn(OverlayService.observer, 'disconnect').and.callThrough();
      // load URL again to cause unload
      loadIframeFixture(() => {
        expect(disconnect).toHaveBeenCalled();
        done();
      });
    });
  });

  it('should sync when the iframe DOM is changed', (done) => {
    spyOn(OverlayService, 'sync');
    loadIframeFixture(() => {
      OverlayService.sync.calls.reset();
      OverlayService.sync.and.callFake(done);
      iframe('body').css('color', 'green');
    });
  });

  it('should sync when the iframe is resized', (done) => {
    spyOn(OverlayService, 'sync');
    loadIframeFixture(() => {
      OverlayService.sync.calls.reset();
      $(iframeWindow).trigger('resize');
      expect(OverlayService.sync).toHaveBeenCalled();
      done();
    });
  });

  it('should generate an empty overlay when there are no page structure elements', (done) => {
    spyOn(PageStructureService, 'getContainers').and.returnValue([]);
    spyOn(PageStructureService, 'getEmbeddedLinks').and.returnValue([]);
    loadIframeFixture(() => {
      OverlayService.toggle(true);
      expect(iframe('#hippo-overlay')).toBeEmpty();
      done();
    });
  });

  it('should set the class hippo-overlay-visible on the HTML element when the overlay is visible', (done) => {
    spyOn(PageStructureService, 'getContainers').and.returnValue([]);
    spyOn(PageStructureService, 'getEmbeddedLinks').and.returnValue([]);
    loadIframeFixture(() => {
      OverlayService.toggle(false);
      expect(iframe('html')).not.toHaveClass('hippo-overlay-visible');

      OverlayService.toggle(true);
      expect(iframe('html')).toHaveClass('hippo-overlay-visible');

      // repeat same toggle
      OverlayService.toggle(true);
      expect(iframe('html')).toHaveClass('hippo-overlay-visible');

      // hide overlay again
      OverlayService.toggle(false);
      expect(iframe('html')).not.toHaveClass('hippo-overlay-visible');
      done();
    });
  });
});
