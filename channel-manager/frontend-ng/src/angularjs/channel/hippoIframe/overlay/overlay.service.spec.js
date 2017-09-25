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

import hippoIframeCss from '../../../../styles/string/hippo-iframe.scss';

describe('OverlayService', () => {
  let $q;
  let $rootScope;
  let DomService;
  let ExperimentStateService;
  let hstCommentsProcessorService;
  let OverlayService;
  let PageStructureService;
  let RenderingService;
  let $iframe;
  let iframeWindow;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    inject((_$q_,
            _$rootScope_,
            _DomService_,
            _ExperimentStateService_,
            _hstCommentsProcessorService_,
            _OverlayService_,
            _PageStructureService_,
            _RenderingService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      DomService = _DomService_;
      ExperimentStateService = _ExperimentStateService_;
      hstCommentsProcessorService = _hstCommentsProcessorService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      RenderingService = _RenderingService_;
    });

    jasmine.getFixtures().load('channel/hippoIframe/overlay/overlay.service.fixture.html');
    $iframe = $('.iframe');

    // initialize the overlay service only once per test so there is only one MutationObserver
    // (otherwise multiple MutationObservers will react on each other's changes and crash the browser)
    OverlayService.init($iframe);
  });

  function loadIframeFixture(callback) {
    $iframe.one('load', () => {
      iframeWindow = $iframe[0].contentWindow;
      DomService.addCss(iframeWindow, hippoIframeCss);

      try {
        PageStructureService.clearParsedElements();
        hstCommentsProcessorService.run(
          iframeWindow.document,
          PageStructureService.registerParsedElement.bind(PageStructureService),
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

  it('initially, content overlay is toggled on, component overlay is toggled off', () => {
    expect(OverlayService.isContentOverlayDisplayed).toEqual(true);
    expect(OverlayService.isComponentsOverlayDisplayed).toEqual(false);
  });

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
    // call through so the mutation observer is disconnected before the second one is started
    spyOn(OverlayService, '_onUnload').and.callThrough();
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
      expect(iframe('.hippo-overlay')).toBeEmpty();
      done();
    });
  });

  it('sets the class hippo overlay classes on the HTML element', (done) => {
    spyOn(PageStructureService, 'getContainers').and.returnValue([]);
    spyOn(PageStructureService, 'getEmbeddedLinks').and.returnValue([]);
    loadIframeFixture(() => {
      // Components overlay
      OverlayService.showComponentsOverlay(true);
      expect(iframe('html')).toHaveClass('hippo-show-components');

      OverlayService.showComponentsOverlay(false);
      expect(iframe('html')).not.toHaveClass('hippo-show-components');

      // Content overlay
      OverlayService.showContentOverlay(true);
      expect(iframe('html')).toHaveClass('hippo-show-content');

      OverlayService.showContentOverlay(false);
      expect(iframe('html')).not.toHaveClass('hippo-show-content');

      // Combined
      OverlayService.showComponentsOverlay(true);
      OverlayService.showContentOverlay(true);
      expect(iframe('html')).toHaveClass('hippo-show-components');
      expect(iframe('html')).toHaveClass('hippo-show-content');

      OverlayService.showComponentsOverlay(false);
      OverlayService.showContentOverlay(false);
      expect(iframe('html')).not.toHaveClass('hippo-show-components');
      expect(iframe('html')).not.toHaveClass('hippo-show-content');

      done();
    });
  });

  it('generates overlay elements', (done) => {
    loadIframeFixture(() => {
      expect(iframe('.hippo-overlay > .hippo-overlay-element').length).toBe(10);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-component').length).toBe(4);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-container').length).toBe(4);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-content-link').length).toBe(1);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-menu-link').length).toBe(1);
      done();
    });
  });

  it('sets specific CSS classes on the box- and overlay elements of containers', (done) => {
    loadIframeFixture(() => {
      const vboxContainerBox = iframe('#container-vbox');
      expect(vboxContainerBox).toHaveClass('hippo-overlay-box-container-filled');

      const vboxContainerOverlay = iframe('.hippo-overlay-element-container').eq(0);
      expect(vboxContainerOverlay).not.toHaveClass('hippo-overlay-element-container-empty');

      const nomarkupContainerBox = iframe('#container-nomarkup');
      expect(nomarkupContainerBox).toHaveClass('hippo-overlay-box-container-filled');

      const nomarkupContainerOverlay = iframe('.hippo-overlay-element-container').eq(1);
      expect(nomarkupContainerOverlay).not.toHaveClass('hippo-overlay-element-container-empty');

      const emptyContainerBox = iframe('#container-empty');
      expect(emptyContainerBox).not.toHaveClass('hippo-overlay-box-container-filled');

      const emptyContainerOverlay = iframe('.hippo-overlay-element-container').eq(2);
      expect(emptyContainerOverlay).toHaveClass('hippo-overlay-element-container-empty');

      const inheritedContainerOverlay = iframe('.hippo-overlay-element-container').eq(3);
      expect(inheritedContainerOverlay).toHaveClass('hippo-overlay-element-container-disabled');

      done();
    });
  });

  it('generates box elements for re-rendered components without any markup in an HST.NoMarkup container', (done) => {
    loadIframeFixture(() => {
      const markupComponentC = iframe('#componentC');
      const boxElement = PageStructureService.getComponentById('cccc').getBoxElement();

      expect(boxElement.is(markupComponentC)).toBe(true);

      const emptyMarkup = `
        <!-- { "HST-Type": "CONTAINER_ITEM_COMPONENT", "HST-Label": "component C", "uuid": "cccc" } -->
        <!-- { "HST-End": "true", "uuid": "cccc" } -->
      `;
      spyOn(RenderingService, 'fetchComponentMarkup').and.returnValue($q.when({ data: emptyMarkup }));

      PageStructureService.renderComponent('cccc');
      $rootScope.$digest();

      const generatedBoxElement = PageStructureService.getComponentById('cccc').getBoxElement();
      expect(generatedBoxElement).toBeDefined();
      expect(generatedBoxElement).toHaveClass('hippo-overlay-box-empty');

      done();
    });
  });

  it('only renders labels for structure elements that have a label', (done) => {
    loadIframeFixture(() => {
      expect(iframe('.hippo-overlay > .hippo-overlay-element-component > .hippo-overlay-label').length).toBe(4);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-container > .hippo-overlay-label').length).toBe(4);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-link > .hippo-overlay-label').length).toBe(0);

      const emptyContainer = iframe('.hippo-overlay-element-container').eq(2);
      expect(emptyContainer.find('.hippo-overlay-label-text').html()).toBe('Empty container');
      done();
    });
  });

  it('renders the name structure elements in a data-qa-name attribute', (done) => {
    loadIframeFixture(() => {
      expect(iframe('.hippo-overlay > .hippo-overlay-element-component > .hippo-overlay-label[data-qa-name]').length).toBe(4);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-container > .hippo-overlay-label[data-qa-name]').length).toBe(4);

      const emptyContainer = iframe('.hippo-overlay-element-container').eq(2);
      expect(emptyContainer.find('.hippo-overlay-label').attr('data-qa-name')).toBe('Empty container');
      done();
    });
  });

  it('renders icons for links', (done) => {
    loadIframeFixture(() => {
      expect(iframe('.hippo-overlay > .hippo-overlay-element-link > svg').length).toBe(2);
      done();
    });
  });

  it('renders a title for links', (done) => {
    loadIframeFixture(() => {
      expect(iframe('.hippo-overlay > .hippo-overlay-element-content-link').attr('title')).toBe('EDIT_CONTENT');
      expect(iframe('.hippo-overlay > .hippo-overlay-element-menu-link').attr('title')).toBe('EDIT_MENU');
      done();
    });
  });

  it('renders lock icons for disabled containers', (done) => {
    loadIframeFixture(() => {
      const disabledContainer = iframe('.hippo-overlay > .hippo-overlay-element-container').eq(0);
      const lock = disabledContainer.find('.hippo-overlay-lock');
      expect(lock.length).toBe(1);
      expect(lock.find('svg').length).toBe(1);
      expect(lock.attr('data-locked-by')).toBe('CONTAINER_LOCKED_BY');

      done();
    });
  });

  it('does not render lock icons for enabled containers', (done) => {
    loadIframeFixture(() => {
      const enabledContainer = iframe('.hippo-overlay > .hippo-overlay-element-container').eq(1);
      expect(enabledContainer.find('.hippo-overlay-lock').length).toBe(0);
      done();
    });
  });

  it('renders lock icons for inherited containers', (done) => {
    loadIframeFixture(() => {
      const inheritedContainer = iframe('.hippo-overlay > .hippo-overlay-element-container').eq(3);
      const lock = inheritedContainer.find('.hippo-overlay-lock');
      expect(lock.length).toBe(1);
      expect(lock.find('svg').length).toBe(1);
      expect(lock.attr('data-locked-by')).toBe('CONTAINER_INHERITED');

      done();
    });
  });

  it('renders the experiment state of components', (done) => {
    loadIframeFixture(() => {
      const componentWithExperiment = iframe('.hippo-overlay > .hippo-overlay-element-component').eq(3);
      const label = componentWithExperiment.find('.hippo-overlay-label');
      expect(label.length).toBe(1);
      expect(label.attr('data-qa-experiment-id')).toBe('1234');
      expect(label.find('svg').length).toBe(1);

      const labelText = label.find('.hippo-overlay-label-text');
      expect(labelText.length).toBe(1);
      expect(labelText.html()).toBe('EXPERIMENT_LABEL_RUNNING');

      done();
    });
  });

  it('updates the experiment state of components', (done) => {
    loadIframeFixture(() => {
      const componentWithExperiment = iframe('.hippo-overlay > .hippo-overlay-element-component').eq(3);
      const labelText = componentWithExperiment.find('.hippo-overlay-label-text');
      expect(labelText.html()).toBe('EXPERIMENT_LABEL_RUNNING');

      spyOn(ExperimentStateService, 'getExperimentStateLabel').and.returnValue('EXPERIMENT_LABEL_COMPLETED');
      OverlayService.sync();

      expect(labelText.html()).toBe('EXPERIMENT_LABEL_COMPLETED');

      done();
    });
  });

  it('starts showing the experiment state of a component for which an experiment was just created', (done) => {
    loadIframeFixture(() => {
      const componentA = iframe('.hippo-overlay > .hippo-overlay-element-component').eq(0);
      const labelText = componentA.find('.hippo-overlay-label-text');
      expect(labelText.html()).toBe('component A');

      const componentMarkupWithExperiment = `
        <!-- { "HST-Type": "CONTAINER_ITEM_COMPONENT", "HST-Label": "component A", "uuid": "aaaa", "Targeting-experiment-id": "567", "Targeting-experiment-state": "CREATED" } -->
          <p id="markup-in-component-a">Markup in component A that just got an experiment</p>
        <!-- { "HST-End": "true", "uuid": "aaaa" } -->
      `;
      spyOn(RenderingService, 'fetchComponentMarkup').and.returnValue($q.when({ data: componentMarkupWithExperiment }));

      PageStructureService.renderComponent('aaaa');
      $rootScope.$digest();

      const label = componentA.find('.hippo-overlay-label');
      expect(label.attr('data-qa-experiment-id')).toBe('567');
      expect(labelText.html()).toBe('EXPERIMENT_LABEL_CREATED');

      done();
    });
  });

  it('syncs the position of overlay elements when content overlay is active', (done) => {
    OverlayService.showContentOverlay(true);
    loadIframeFixture(() => {
      const components = iframe('.hippo-overlay > .hippo-overlay-element-component');

      const componentA = $(components[0]);
      expect(componentA).not.toHaveClass('hippo-overlay-element-visible');

      const menuLink = iframe('.hippo-overlay > .hippo-overlay-element-menu-link');
      expect(menuLink).not.toHaveClass('hippo-overlay-element-visible');

      const contentLink = iframe('.hippo-overlay > .hippo-overlay-element-content-link');
      expect(contentLink.css('top')).toBe(`${4 + 100}px`);
      expect(contentLink.css('left')).toBe(`${200 - 40}px`);
      expect(contentLink.css('width')).toBe('40px');
      expect(contentLink.css('height')).toBe('40px');

      const componentB = $(components[1]);
      expect(componentB).not.toHaveClass('hippo-overlay-element-visible');

      const emptyContainer = $(iframe('.hippo-overlay > .hippo-overlay-element-container')[1]);
      expect(emptyContainer).not.toHaveClass('hippo-overlay-element-visible');

      done();
    });
  });

  it('syncs the position of overlay elements in edit mode', (done) => {
    OverlayService.showComponentsOverlay(true);
    OverlayService.showContentOverlay(false);
    loadIframeFixture(() => {
      const components = iframe('.hippo-overlay > .hippo-overlay-element-component');

      const componentA = components.eq(0);
      expect(componentA.css('top')).toBe('4px');
      expect(componentA.css('left')).toBe('2px');
      expect(componentA.css('width')).toBe(`${200 - 2}px`);
      expect(componentA.css('height')).toBe('100px');

      const menuLink = iframe('.hippo-overlay > .hippo-overlay-element-menu-link');
      expect(menuLink.css('top')).toBe(`${4 + 30}px`);
      expect(menuLink.css('left')).toBe(`${200 - 40}px`);
      expect(menuLink.css('width')).toBe('40px');
      expect(menuLink.css('height')).toBe('40px');

      const contentLink = iframe('.hippo-overlay > .hippo-overlay-element-content-link');
      expect(contentLink).not.toHaveClass('hippo-overlay-element-visible');

      const componentB = components.eq(1);
      expect(componentB.css('top')).toBe(`${4 + 100 + 60}px`);
      expect(componentB.css('left')).toBe('2px');
      expect(componentB.css('width')).toBe(`${200 - 2}px`);
      expect(componentB.css('height')).toBe('200px');

      const emptyContainer = iframe('.hippo-overlay > .hippo-overlay-element-container').eq(2);
      expect(emptyContainer.css('top')).toBe(`${400 + 40 + 4}px`);
      expect(emptyContainer.css('left')).toBe('0px');
      expect(emptyContainer.css('width')).toBe('200px');
      expect(emptyContainer.css('height')).toBe('40px');  // minimum height of empty container

      done();
    });
  });

  it('takes the scroll position of the iframe into account when positioning overlay elements', (done) => {
    OverlayService.showContentOverlay(true);
    loadIframeFixture(() => {
      // enlarge body so the iframe can scroll
      const body = iframe('body');
      body.width('200%');
      body.height('200%');

      iframeWindow.scrollTo(1, 2);
      OverlayService.sync();

      const contentLink = iframe('.hippo-overlay > .hippo-overlay-element-content-link');
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
      const overlayComponentElement = iframe('.hippo-overlay > .hippo-overlay-element-component').first();

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
      const overlayComponentElement = iframe('.hippo-overlay > .hippo-overlay-element-component').first();

      overlayComponentElement.mousedown();
      expect(mousedownSpy).not.toHaveBeenCalled();

      done();
    });
  });

  it('does not throw an error when calling edit content handler if not set', (done) => {
    loadIframeFixture(() => {
      const contentLink = iframe('.hippo-overlay > .hippo-overlay-element-content-link');

      expectNoPropagatedClicks();
      expect(() => contentLink.click()).not.toThrow();

      done();
    });
  });

  it('calls the edit content handler to edit a document', (done) => {
    const editContentHandler = jasmine.createSpy('editContentHandler');

    OverlayService.onEditContent(editContentHandler);
    loadIframeFixture(() => {
      const contentLink = iframe('.hippo-overlay > .hippo-overlay-element-content-link');

      expectNoPropagatedClicks();
      contentLink.click();

      expect(editContentHandler).toHaveBeenCalledWith('content-in-container-vbox');

      done();
    });
  });

  it('does not throw an error when calling edit menu handler if not set', (done) => {
    loadIframeFixture(() => {
      const menuLink = iframe('.hippo-overlay > .hippo-overlay-element-menu-link');

      expectNoPropagatedClicks();
      expect(() => menuLink.click()).not.toThrow();

      done();
    });
  });

  it('calls the edit menu handler to edit a menu', (done) => {
    const editMenuHandler = jasmine.createSpy('editMenuHandler');

    OverlayService.onEditMenu(editMenuHandler);
    OverlayService.showComponentsOverlay(true);
    loadIframeFixture(() => {
      const menuLink = iframe('.hippo-overlay > .hippo-overlay-element-menu-link');

      expectNoPropagatedClicks();
      menuLink.click();

      expect(editMenuHandler).toHaveBeenCalledWith('menu-in-component-a');

      done();
    });
  });

  it('removes overlay elements when they are no longer part of the page structure', (done) => {
    OverlayService.showComponentsOverlay(true);

    loadIframeFixture(() => {
      expect(iframe('.hippo-overlay > .hippo-overlay-element').length).toBe(10);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-menu-link').length).toBe(1);

      const componentMarkupWithoutMenuLink = `
        <!-- { "HST-Type": "CONTAINER_ITEM_COMPONENT", "HST-Label": "component A", "uuid": "aaaa" } -->
          <p id="markup-in-component-a">Markup in component A without menu link</p>
        <!-- { "HST-End": "true", "uuid": "aaaa" } -->
      `;
      spyOn(RenderingService, 'fetchComponentMarkup').and.returnValue($q.when({ data: componentMarkupWithoutMenuLink }));

      PageStructureService.renderComponent('aaaa');
      $rootScope.$digest();

      expect(iframe('.hippo-overlay > .hippo-overlay-element').length).toBe(9);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-menu-link').length).toBe(0);

      done();
    });
  });
});
