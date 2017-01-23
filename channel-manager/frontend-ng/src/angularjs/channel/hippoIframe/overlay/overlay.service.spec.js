/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
  let $q;
  let $rootScope;
  let CmsService;
  let hstCommentsProcessorService;
  let OverlayService;
  let PageStructureService;
  let RenderingService;
  let $iframe;
  let iframeWindow;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    inject((_$q_, _$rootScope_, _CmsService_, _hstCommentsProcessorService_, _OverlayService_,
            _PageStructureService_, _RenderingService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      CmsService = _CmsService_;
      hstCommentsProcessorService = _hstCommentsProcessorService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      RenderingService = _RenderingService_;
    });

    jasmine.getFixtures().load('channel/hippoIframe/overlay/overlay.service.fixture.html');
    $iframe = $('.iframe');
  });

  function loadIframeFixture(callback) {
    OverlayService.init($iframe);
    $iframe.one('load', () => {
      iframeWindow = $iframe[0].contentWindow;
      try {
        hstCommentsProcessorService.run(
          iframeWindow.document,
          PageStructureService.registerParsedElement.bind(PageStructureService)
        );
        PageStructureService.attachEmbeddedLinks();
        callback();
      } catch (e) {
        // Karma silently swallows stack traces for synchronous tests, so log them in an explicit fail
        fail(e);
      }
    });
    $iframe.attr('src', `/${jasmine.getFixtures().fixturesPath}/channel/hippoIframe/overlay/overlay.service.iframe.fixture.html`);
  }

  function iframe(selector) {
    return $(selector, iframeWindow.document);
  }

  it('initializes when the iframe is loaded', (done) => {
    spyOn(OverlayService, '_onLoad');
    loadIframeFixture(() => {
      expect(OverlayService._onLoad).toHaveBeenCalled();
      done();
    });
  });

  it('does not throw errors when synced before init', () => {
    expect(() => OverlayService.sync()).not.toThrow();
  });

  it('attaches an unload handler to the iframe', (done) => {
    spyOn(OverlayService, '_onUnload');
    loadIframeFixture(() => {
      // load URL again to cause unload
      loadIframeFixture(() => {
        expect(OverlayService._onUnload).toHaveBeenCalled();
        done();
      });
    });
  });

  it('attaches a MutationObserver on the iframe document on first load', (done) => {
    loadIframeFixture(() => {
      expect(OverlayService.observer).not.toBeNull();
      done();
    });
  });

  it('disconnects the MutationObserver on iframe unload', (done) => {
    loadIframeFixture(() => {
      const disconnect = spyOn(OverlayService.observer, 'disconnect').and.callThrough();
      // load URL again to cause unload
      loadIframeFixture(() => {
        expect(disconnect).toHaveBeenCalled();
        done();
      });
    });
  });

  it('syncs when the iframe DOM is changed', (done) => {
    spyOn(OverlayService, 'sync');
    loadIframeFixture(() => {
      OverlayService.sync.calls.reset();
      OverlayService.sync.and.callFake(done);
      iframe('body').css('color', 'green');
    });
  });

  it('syncs when the iframe is resized', (done) => {
    spyOn(OverlayService, 'sync');
    loadIframeFixture(() => {
      OverlayService.sync.calls.reset();
      $(iframeWindow).trigger('resize');
      expect(OverlayService.sync).toHaveBeenCalled();
      done();
    });
  });

  it('generates an empty overlay when there are no page structure elements', (done) => {
    spyOn(PageStructureService, 'getContainers').and.returnValue([]);
    spyOn(PageStructureService, 'getEmbeddedLinks').and.returnValue([]);
    loadIframeFixture(() => {
      expect(iframe('#hippo-overlay')).toBeEmpty();
      done();
    });
  });

  it('sets the class hippo-mode-edit on the HTML element when edit mode is active', (done) => {
    spyOn(PageStructureService, 'getContainers').and.returnValue([]);
    spyOn(PageStructureService, 'getEmbeddedLinks').and.returnValue([]);
    loadIframeFixture(() => {
      OverlayService.setMode('view');
      expect(iframe('html')).not.toHaveClass('hippo-mode-edit');

      OverlayService.setMode('edit');
      expect(iframe('html')).toHaveClass('hippo-mode-edit');

      // repeat same mode
      OverlayService.setMode('edit');
      expect(iframe('html')).toHaveClass('hippo-mode-edit');

      // change mode again
      OverlayService.setMode('view');
      expect(iframe('html')).not.toHaveClass('hippo-mode-edit');
      done();
    });
  });

  it('generates overlay elements', (done) => {
    loadIframeFixture(() => {
      expect(iframe('#hippo-overlay > .hippo-overlay-element').length).toBe(7);
      expect(iframe('#hippo-overlay > .hippo-overlay-element-component').length).toBe(2);
      expect(iframe('#hippo-overlay > .hippo-overlay-element-container').length).toBe(3);
      expect(iframe('#hippo-overlay > .hippo-overlay-element-content-link').length).toBe(1);
      expect(iframe('#hippo-overlay > .hippo-overlay-element-menu-link').length).toBe(1);
      done();
    });
  });

  it('only renders labels for structure elements that have a label', (done) => {
    loadIframeFixture(() => {
      expect(iframe('#hippo-overlay > .hippo-overlay-element-component > .hippo-overlay-label').length).toBe(2);
      expect(iframe('#hippo-overlay > .hippo-overlay-element-container > .hippo-overlay-label').length).toBe(3);
      expect(iframe('#hippo-overlay > .hippo-overlay-element-link > .hippo-overlay-label').length).toBe(0);
      done();
    });
  });

  it('renders icons for links', (done) => {
    loadIframeFixture(() => {
      expect(iframe('#hippo-overlay > .hippo-overlay-element-link > svg').length).toBe(2);
      done();
    });
  });

  it('renders a title for links', (done) => {
    loadIframeFixture(() => {
      expect(iframe('#hippo-overlay > .hippo-overlay-element-content-link').attr('title')).toBe('IFRAME_OPEN_DOCUMENT');
      expect(iframe('#hippo-overlay > .hippo-overlay-element-menu-link').attr('title')).toBe('IFRAME_EDIT_MENU');
      done();
    });
  });

  it('renders lock icons for disabled containers', (done) => {
    loadIframeFixture(() => {
      const disabledContainer = iframe('#hippo-overlay > .hippo-overlay-element-container').eq(0);
      const lock = disabledContainer.find('.hippo-overlay-lock');
      expect(lock.length).toBe(1);
      expect(lock.find('svg').length).toBe(1);
      expect(lock.attr('title')).toBe('CONTAINER_LOCKED_BY');

      done();
    });
  });

  it('does not render lock icons for enabled containers', (done) => {
    loadIframeFixture(() => {
      const enabledContainer = iframe('#hippo-overlay > .hippo-overlay-element-container').eq(1);
      expect(enabledContainer.find('.hippo-overlay-lock').length).toBe(0);
      done();
    });
  });

  it('renders lock icons for inherited containers', (done) => {
    loadIframeFixture(() => {
      const inheritedContainer = iframe('#hippo-overlay > .hippo-overlay-element-container').eq(2);
      const lock = inheritedContainer.find('.hippo-overlay-lock');
      expect(lock.length).toBe(1);
      expect(lock.find('svg').length).toBe(1);
      expect(lock.attr('title')).toBe('CONTAINER_INHERITED');

      done();
    });
  });

  it('syncs the position of overlay elements in view mode', (done) => {
    OverlayService.setMode('view');
    loadIframeFixture(() => {
      const components = iframe('#hippo-overlay > .hippo-overlay-element-component');

      const componentA = $(components[0]);
      expect(componentA).not.toHaveClass('hippo-overlay-element-visible');

      const menuLink = iframe('#hippo-overlay > .hippo-overlay-element-menu-link');
      expect(menuLink).not.toHaveClass('hippo-overlay-element-visible');

      const contentLink = iframe('#hippo-overlay > .hippo-overlay-element-content-link');
      expect(contentLink.css('top')).toBe(`${4 + 100}px`);
      expect(contentLink.css('left')).toBe(`${200 - 40}px`);
      expect(contentLink.css('width')).toBe('40px');
      expect(contentLink.css('height')).toBe('40px');

      const componentB = $(components[1]);
      expect(componentB).not.toHaveClass('hippo-overlay-element-visible');

      const emptyContainer = $(iframe('#hippo-overlay > .hippo-overlay-element-container')[1]);
      expect(emptyContainer).not.toHaveClass('hippo-overlay-element-visible');

      done();
    });
  });

  it('syncs the position of overlay elements in edit mode', (done) => {
    OverlayService.setMode('edit');
    loadIframeFixture(() => {
      const components = iframe('#hippo-overlay > .hippo-overlay-element-component');

      const componentA = $(components[0]);
      expect(componentA.css('top')).toBe('4px');
      expect(componentA.css('left')).toBe('2px');
      expect(componentA.css('width')).toBe(`${200 - 2}px`);
      expect(componentA.css('height')).toBe('100px');

      const menuLink = iframe('#hippo-overlay > .hippo-overlay-element-menu-link');
      expect(menuLink.css('top')).toBe(`${4 + 30}px`);
      expect(menuLink.css('left')).toBe(`${200 - 40}px`);
      expect(menuLink.css('width')).toBe('40px');
      expect(menuLink.css('height')).toBe('40px');

      const contentLink = iframe('#hippo-overlay > .hippo-overlay-element-content-link');
      expect(contentLink).not.toHaveClass('hippo-overlay-element-visible');

      const componentB = $(components[1]);
      expect(componentB.css('top')).toBe(`${4 + 100 + 60}px`);
      expect(componentB.css('left')).toBe('2px');
      expect(componentB.css('width')).toBe(`${200 - 2}px`);
      expect(componentB.css('height')).toBe('200px');

      const emptyContainer = $(iframe('#hippo-overlay > .hippo-overlay-element-container')[1]);
      expect(emptyContainer.css('top')).toBe(`${400 + 4}px`);
      expect(emptyContainer.css('left')).toBe('0px');
      expect(emptyContainer.css('width')).toBe('200px');
      expect(emptyContainer.css('height')).toBe('40px');  // minimum height of empty container

      done();
    });
  });

  it('takes the scroll position of the iframe into account when positioning overlay elements', (done) => {
    OverlayService.setMode('view');
    loadIframeFixture(() => {
      // enlarge body so the iframe can scroll
      const body = iframe('body');
      body.width('200%');
      body.height('200%');

      iframeWindow.scrollTo(1, 2);
      OverlayService.sync();

      const contentLink = iframe('#hippo-overlay > .hippo-overlay-element-content-link');
      expect(contentLink.css('top')).toBe(`${4 + 100}px`);
      expect(contentLink.css('left')).toBe(`${200 - 40}px`);
      expect(contentLink.css('width')).toBe('40px');
      expect(contentLink.css('height')).toBe('40px');

      done();
    });
  });

  function expectNoPropagatedClicks() {
    const body = iframe('body');
    body.click(() => {
      fail('click event should not propagate to the page');
    });
  }

  it('mousedown event on component-overlay calls attached callback with component reference', (done) => {
    const mousedownSpy = jasmine.createSpy('mousedown');
    OverlayService.attachComponentMouseDown(mousedownSpy);

    loadIframeFixture(() => {
      const component = PageStructureService.getComponentById('aaaa');
      const overlayComponentElement = iframe('#hippo-overlay > .hippo-overlay-element-component').first();

      overlayComponentElement.mousedown();
      expect(mousedownSpy).toHaveBeenCalledWith(jasmine.anything(), component);
      mousedownSpy.calls.reset();

      OverlayService.detachComponentMouseDown();
      overlayComponentElement.mousedown();
      expect(mousedownSpy).not.toHaveBeenCalled();

      done();
    });
  });

  it('mousedown event on component-overlay only calls attached callback if related component is found', (done) => {
    const mousedownSpy = jasmine.createSpy('mousedown');
    OverlayService.attachComponentMouseDown(mousedownSpy);

    spyOn(PageStructureService, 'getComponentByOverlayElement').and.returnValue(false);

    loadIframeFixture(() => {
      const overlayComponentElement = iframe('#hippo-overlay > .hippo-overlay-element-component').first();

      overlayComponentElement.mousedown();
      expect(mousedownSpy).not.toHaveBeenCalled();

      done();
    });
  });

  it('sends an "open-content" event to the CMS to open content', (done) => {
    spyOn(CmsService, 'publish');
    OverlayService.setMode('view');
    loadIframeFixture(() => {
      const contentLink = iframe('#hippo-overlay > .hippo-overlay-element-content-link');

      expectNoPropagatedClicks();
      contentLink.click();

      expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'content-in-container-vbox');

      done();
    });
  });

  it('calls the edit menu handler to edit a menu', (done) => {
    const editMenuHandler = jasmine.createSpy('editMenuHandler');

    OverlayService.onEditMenu(editMenuHandler);
    OverlayService.setMode('edit');
    loadIframeFixture(() => {
      const menuLink = iframe('#hippo-overlay > .hippo-overlay-element-menu-link');

      expectNoPropagatedClicks();
      menuLink.click();

      expect(editMenuHandler).toHaveBeenCalledWith('menu-in-component-a');

      done();
    });
  });

  it('removes overlay elements when they are no longer part of the page structure', (done) => {
    OverlayService.setMode('edit');

    loadIframeFixture(() => {
      expect(iframe('#hippo-overlay > .hippo-overlay-element').length).toBe(7);
      expect(iframe('#hippo-overlay > .hippo-overlay-element-menu-link').length).toBe(1);

      const componentMarkupWithoutMenuLink = `
        <!-- { "HST-Type": "CONTAINER_ITEM_COMPONENT", "HST-Label": "component A", "uuid": "aaaa" } -->
          <p id="markup-in-component-a">Markup in component A without menu link</p>
        <!-- { "HST-End": "true", "uuid": "aaaa" } -->
      `;
      spyOn(RenderingService, 'fetchComponentMarkup').and.returnValue($q.when({ data: componentMarkupWithoutMenuLink }));

      PageStructureService.renderComponent('aaaa');
      $rootScope.$digest();

      expect(iframe('#hippo-overlay > .hippo-overlay-element').length).toBe(6);
      expect(iframe('#hippo-overlay > .hippo-overlay-element-menu-link').length).toBe(0);

      done();
    });
  });
});
