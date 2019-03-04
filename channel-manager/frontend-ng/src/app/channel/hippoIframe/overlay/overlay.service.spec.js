/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

import hippoIframeCss from '../../../../styles/string/hippo-iframe.scss?url';

describe('OverlayService', () => {
  let $iframe;
  let $q;
  let $rootScope;
  let iframeWindow;
  let ChannelService;
  let CmsService;
  let CreateContentService;
  let DomService;
  let ExperimentStateService;
  let HstCommentsProcessorService;
  let MarkupService;
  let OverlayService;
  let PageStructureService;
  let PickerService;
  let ScrollService;
  let SvgService;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    PickerService = jasmine.createSpyObj('PickerService', ['pickPath']);

    angular.mock.module(($provide) => {
      $provide.value('PickerService', PickerService);
    });

    inject((
      _$q_,
      _$rootScope_,
      _ChannelService_,
      _CmsService_,
      _CreateContentService_,
      _DomService_,
      _ExperimentStateService_,
      _HstCommentsProcessorService_,
      _MarkupService_,
      _OverlayService_,
      _PageStructureService_,
      _ScrollService_,
      _SvgService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      CreateContentService = _CreateContentService_;
      DomService = _DomService_;
      ExperimentStateService = _ExperimentStateService_;
      HstCommentsProcessorService = _HstCommentsProcessorService_;
      MarkupService = _MarkupService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      ScrollService = _ScrollService_;
      SvgService = _SvgService_;
    });

    spyOn(CmsService, 'subscribe').and.callThrough();
    spyOn(SvgService, 'getSvg').and.callFake(() => angular.element('<svg>test</svg>'));

    jasmine.getFixtures().load('channel/hippoIframe/overlay/overlay.service.fixture.html');
    $iframe = $('.iframe');

    // initialize the overlay service only once per test so there is only one MutationObserver
    // (otherwise multiple MutationObservers will react on each other's changes and crash the browser)
    OverlayService.init($iframe);
  });

  function loadIframeFixture(callback) {
    $iframe.one('load', async () => {
      iframeWindow = $iframe[0].contentWindow;
      await DomService.addCssLinks(iframeWindow, [hippoIframeCss]);

      try {
        PageStructureService.clearParsedElements();
        HstCommentsProcessorService.run(
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

    $iframe.attr('src', `/${jasmine.getFixtures().fixturesPath}/channel/hippoIframe/overlay/overlay.service.iframe.fixture.html`); // eslint-disable-line max-len
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

  it('deletes iframe referrence on iframe unload', () => {
    loadIframeFixture(() => {
      OverlayService.iframeWindow = {};
      $iframe.triggerHandler('unload');
      expect(OverlayService.iframeWindow).toBeUndefined();
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
      // Total overlay elements
      expect(iframe('.hippo-overlay > .hippo-overlay-element').length).toBe(26);

      expect(iframe('.hippo-overlay > .hippo-overlay-element-component').length).toBe(4);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-container').length).toBe(6);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-menu-link').length).toBe(1);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-manage-content-link').length).toBe(15);
      done();
    });
  });

  describe('selected component highlighting', () => {
    const activeComponentSelector = '.hippo-overlay > .hippo-overlay-element-component-active';
    function expectActiveComponent(qaName) {
      expect(iframe(activeComponentSelector).length).toBe(1);
      expect(iframe(`${activeComponentSelector} [data-qa-name="${qaName}"]`).length).toBe(1);
    }

    function expectNoActiveComponent() {
      expect(iframe(activeComponentSelector).length).toBe(0);
    }

    it('highlights component after selecting it', (done) => {
      loadIframeFixture(() => {
        expectNoActiveComponent();

        OverlayService.selectComponent('aaaa');
        expectActiveComponent('component A');

        done();
      });
    });

    it('un-highlights component after deselecting it', (done) => {
      loadIframeFixture(() => {
        OverlayService.selectComponent('aaaa');
        expectActiveComponent('component A');

        OverlayService.deselectComponent();
        expectNoActiveComponent();

        done();
      });
    });

    it('highlights component after loading a page that contains the component', (done) => {
      OverlayService.selectComponent('aaaa');
      loadIframeFixture(() => {
        expectActiveComponent('component A');
        done();
      });
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
      const componentC = PageStructureService.getComponentById('cccc');
      const boxElement = componentC.getBoxElement();

      expect(boxElement.is(markupComponentC)).toBe(true);

      const emptyMarkup = `
        <!-- { "HST-Type": "CONTAINER_ITEM_COMPONENT", "HST-Label": "component C", "uuid": "cccc" } -->
        <!-- { "HST-End": "true", "uuid": "cccc" } -->
      `;
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: emptyMarkup }));

      PageStructureService.renderComponent(componentC);
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
      expect(iframe('.hippo-overlay > .hippo-overlay-element-container > .hippo-overlay-label').length).toBe(6);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-link > .hippo-overlay-label').length).toBe(0);

      const emptyContainer = iframe('.hippo-overlay-element-container').eq(2);
      expect(emptyContainer.find('.hippo-overlay-label-text').html()).toBe('Empty container');
      done();
    });
  });

  it('renders the name structure elements in a data-qa-name attribute', (done) => {
    loadIframeFixture(() => {
      expect(iframe('.hippo-overlay > .hippo-overlay-element-component > .hippo-overlay-label[data-qa-name]').length)
        .toBe(4);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-container > .hippo-overlay-label[data-qa-name]').length)
        .toBe(6);

      const emptyContainer = iframe('.hippo-overlay-element-container').eq(2);
      expect(emptyContainer.find('.hippo-overlay-label').attr('data-qa-name')).toBe('Empty container');
      done();
    });
  });

  it('renders icons for links', (done) => {
    loadIframeFixture(() => {
      const svg = iframe('.hippo-overlay > .hippo-overlay-element-link > svg');
      expect(svg.length).toBe(1);
      expect(svg.eq(0)).toContainText('test');

      done();
    });
  });

  it('renders a title for links', (done) => {
    loadIframeFixture(() => {
      expect(iframe('.hippo-overlay > .hippo-overlay-element-menu-link').attr('title')).toBe('EDIT_MENU');
      done();
    });
  });

  it('renders lock icons for disabled containers', (done) => {
    loadIframeFixture(() => {
      const disabledContainer = iframe('.hippo-overlay > .hippo-overlay-element-container').eq(4);
      const lock = disabledContainer.find('.hippo-overlay-lock');
      expect(lock.length).toBe(1);
      expect(lock.find('svg').length).toBe(1);
      expect(lock.attr('data-locked-by')).toBe('CONTAINER_LOCKED_BY');

      done();
    });
  });

  it('does not render lock icons for enabled containers', (done) => {
    loadIframeFixture(() => {
      const containers = iframe('.hippo-overlay > .hippo-overlay-element-container');
      expect(containers.eq(0).find('.hippo-overlay-lock').length).toBe(0);
      expect(containers.eq(1).find('.hippo-overlay-lock').length).toBe(0);
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
      const componentElementA = iframe('.hippo-overlay > .hippo-overlay-element-component').eq(0);
      const labelText = componentElementA.find('.hippo-overlay-label-text');
      expect(labelText.html()).toBe('component A');

      const componentMarkupWithExperiment = `
        <!-- {
          "HST-Type": "CONTAINER_ITEM_COMPONENT",
          "HST-Label": "component A",
          "uuid": "aaaa",
          "Targeting-experiment-id": "567",
          "Targeting-experiment-state": "CREATED"
        } -->
          <p id="markup-in-component-a">Markup in component A that just got an experiment</p>
        <!-- { "HST-End": "true", "uuid": "aaaa" } -->
      `;
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: componentMarkupWithExperiment }));

      const componentA = PageStructureService.getComponentById('aaaa');
      PageStructureService.renderComponent(componentA);
      $rootScope.$digest();

      const label = componentElementA.find('.hippo-overlay-label');
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

      const scrollBarSize = ScrollService.getScrollBarSize();
      const contentLink = iframe('.hippo-overlay > .hippo-overlay-element-manage-content-link');
      expect(contentLink.css('top')).toBe('0px');
      expect(contentLink.css('left')).toBe(`${300 - 40 - scrollBarSize}px`);
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

      const contentLink = iframe('.hippo-overlay > .hippo-overlay-element-manage-content-link');
      expect(contentLink).not.toHaveClass('hippo-overlay-element-visible');

      const componentB = components.eq(1);
      expect(componentB.css('top')).toBe(`${4 + 100}px`);
      expect(componentB.css('left')).toBe('2px');
      expect(componentB.css('width')).toBe(`${200 - 2}px`);
      expect(componentB.css('height')).toBe('200px');

      const emptyContainer = iframe('.hippo-overlay > .hippo-overlay-element-container').eq(2);
      expect(emptyContainer.css('top')).toBe(`${400 + 40 + 4}px`);
      expect(emptyContainer.css('left')).toBe('0px');
      expect(emptyContainer.css('width')).toBe('200px');
      expect(emptyContainer.css('height')).toBe('40px'); // minimum height of empty container

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

      const scrollBarSize = ScrollService.getScrollBarSize();
      const contentLink = iframe('.hippo-overlay > .hippo-overlay-element-manage-content-link');
      expect(contentLink.css('top')).toBe('0px');
      expect(contentLink.css('left')).toBe(`${300 - 40 - scrollBarSize}px`);
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

  it('can create content', (done) => {
    spyOn(CreateContentService, 'start');

    loadIframeFixture(() => {
      const overlayElementScenario2 = iframe('.hippo-overlay-element-manage-content-link')[1];
      const createContentButton = $(overlayElementScenario2).find('.hippo-fab-main');

      expectNoPropagatedClicks();
      createContentButton.click();

      const config = CreateContentService.start.calls.mostRecent().args[0];
      expect(config.documentTemplateQuery).toBe('manage-content-document-template-query');

      done();
    });
  });

  it('can select a document', (done) => {
    const selectDocumentHandler = jasmine.createSpy('selectDocumentHander');
    OverlayService.onSelectDocument(selectDocumentHandler);
    ChannelService.isEditable = () => true;
    spyOn(CmsService, 'reportUsageStatistic');

    loadIframeFixture(() => {
      const overlayElementScenario5 = iframe('.hippo-overlay-element-manage-content-link')[4];
      const pickPathButton = $(overlayElementScenario5).find('.hippo-fab-main');
      expectNoPropagatedClicks();

      pickPathButton.click();
      $rootScope.$digest();

      expect(selectDocumentHandler).toHaveBeenCalledWith(
        jasmine.any(Object), 'manage-content-component-parameter', undefined, jasmine.any(Object), '',
      );
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('PickContentButton');

      done();
    });
  });

  it('can (re)register a select-document-handler', () => {
    const selectDocumentHandler1 = jasmine.createSpy('selectDocumentHander1');
    const selectDocumentHandler2 = jasmine.createSpy('selectDocumentHander2');

    expect(OverlayService.onSelectDocument(selectDocumentHandler1)).toBe(angular.noop);
    expect(OverlayService.onSelectDocument(selectDocumentHandler2)).toBe(selectDocumentHandler1);
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
      expect(iframe('.hippo-overlay > .hippo-overlay-element').length).toBe(26);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-menu-link').length).toBe(1);

      const componentMarkupWithoutMenuLink = `
        <!-- { "HST-Type": "CONTAINER_ITEM_COMPONENT", "HST-Label": "component A", "uuid": "aaaa" } -->
          <p id="markup-in-component-a">Markup in component A without menu link</p>
        <!-- { "HST-End": "true", "uuid": "aaaa" } -->
      `;
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: componentMarkupWithoutMenuLink }));

      const componentA = PageStructureService.getComponentById('aaaa');
      PageStructureService.renderComponent(componentA);
      $rootScope.$digest();

      expect(iframe('.hippo-overlay > .hippo-overlay-element').length).toBe(25);
      expect(iframe('.hippo-overlay > .hippo-overlay-element-menu-link').length).toBe(0);

      done();
    });
  });

  describe('Manage content dial button(s)', () => {
    beforeEach(() => {
      ChannelService.isEditable = () => true;
    });

    it('returns correct configuration out of config object', () => {
      const config = { // each property should be filled with the method that will extract the data from the HST comment
        documentUuid: true,
        documentTemplateQuery: true,
        parameterName: true,
      };
      const buttons = OverlayService._getButtons(config);

      expect(buttons.length).toEqual(3);
      expect(Object.keys(buttons[0])).toEqual(['id', 'mainIcon', 'optionIcon', 'callback', 'tooltip']);
    });

    describe('_initManageContentConfig', () => {
      function mockManageContentConfig(
        uuid = false,
        documentTemplateQuery = false,
        parameterName = false,
        locked = false,
        folderTemplateQuery = false,
      ) {
        const enclosing = {
          isLocked: () => locked,
        };
        const structureElement = {
          getDefaultPath: () => null,
          getEnclosingElement: () => enclosing,
          getFolderTemplateQuery: () => folderTemplateQuery,
          getParameterName: () => parameterName,
          getParameterValue: () => null,
          getPickerConfig: () => null,
          getRootPath: () => null,
          getDocumentTemplateQuery: () => documentTemplateQuery,
          getUuid: () => uuid,
          isParameterValueRelativePath: () => false,
        };
        return OverlayService._initManageContentConfig(structureElement);
      }

      it('does not filter out config properties when channel is editable', () => {
        const config = mockManageContentConfig(true, true, true);
        expect(config.documentUuid).toBe(true);
        expect(config.documentTemplateQuery).toBe(true);
        expect(config.parameterName).toBe(true);
      });

      describe('when channel is not editable', () => {
        beforeEach(() => {
          ChannelService.isEditable = () => false;
        });

        it('always filters out property parameterName', () => {
          const config = mockManageContentConfig(false, false, true);
          expect(config.parameterName).not.toBeDefined();
        });

        it('filters out property documentTemplateQuery when documentUuid is set', () => {
          let config = mockManageContentConfig(false, true);
          expect(config.documentTemplateQuery).toBeDefined();

          config = mockManageContentConfig(true, true);
          expect(config.documentTemplateQuery).not.toBeDefined();
        });

        it('filters out property folderTemplateQuery when documentUuid is set', () => {
          let config = mockManageContentConfig(false, true, false, false, true);
          expect(config.folderTemplateQuery).toBeDefined();

          config = mockManageContentConfig(true, true, false, false, true);
          expect(config.folderTemplateQuery).not.toBeDefined();
        });

        it('filters all properties when parameterName is set but documentId is not', () => {
          const config = mockManageContentConfig(false, true, true);
          expect(config).toEqual({});
        });

        it('does not filter documentTemplateQuery when parameterName and documentId are not set', () => {
          const config = mockManageContentConfig(false, true);
          expect(config.documentTemplateQuery).toBe(true);
        });
      });
    });

    function manageContentScenario(scenarioNumber, callback) {
      loadIframeFixture(() => {
        const container = iframe('.hippo-overlay-element-manage-content-link')[scenarioNumber - 1];
        callback($(container).find('.hippo-fab-main'), $(container).find('.hippo-fab-options'));
      });
    }

    describe('Dial button scenario\'s for unlocked containers', () => {
      it('Scenario 1', (done) => {
        manageContentScenario(1, (mainButton, optionButtons) => {
          expect(mainButton.hasClass('qa-edit-content')).toBe(true);
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
          expect(optionButtons.children().length).toBe(0);
          done();
        });
      });

      it('Scenario 2', (done) => {
        manageContentScenario(2, (mainButton, optionButtons) => {
          expect(mainButton.hasClass('qa-add-content')).toBe(true);
          expect(mainButton.attr('title')).toBe('CREATE_DOCUMENT');

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('CREATE_DOCUMENT');
          expect(optionButtons.children().length).toBe(0);
          done();
        });
      });

      it('Scenario 3', (done) => {
        manageContentScenario(3, (mainButton, optionButtons) => {
          expect(mainButton.hasClass('qa-edit-content')).toBe(true);
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
          expect(optionButtons.children().length).toBe(1);
          expect(optionButtons.children()[0].getAttribute('title')).toBe('CREATE_DOCUMENT');
          done();
        });
      });

      it('Scenario 4', (done) => {
        manageContentScenario(4, (mainButton, optionButtons) => {
          expect(mainButton.hasClass('qa-edit-content')).toBe(true);
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
          expect(optionButtons.children().length).toBe(1);
          expect(optionButtons.children()[0].getAttribute('title')).toBe('SELECT_DOCUMENT');
          done();
        });
      });

      it('Scenario 5', (done) => {
        manageContentScenario(5, (mainButton, optionButtons) => {
          expect(mainButton.hasClass('qa-add-content')).toBe(true);
          expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT');

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT');
          expect(optionButtons.children().length).toBe(1);
          expect(optionButtons.children()[0].getAttribute('title')).toBe('CREATE_DOCUMENT');
          done();
        });
      });

      it('Scenario 6', (done) => {
        manageContentScenario(6, (mainButton, optionButtons) => {
          expect(mainButton.hasClass('qa-edit-content')).toBe(true);
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
          expect(optionButtons.children().length).toBe(2);
          expect(optionButtons.children()[0].getAttribute('title')).toBe('SELECT_DOCUMENT');
          expect(optionButtons.children()[1].getAttribute('title')).toBe('CREATE_DOCUMENT');
          done();
        });
      });
    });

    describe('when channel is not editable', () => {
      beforeEach(() => {
        ChannelService.isEditable = () => false;
      });

      it('Scenario 5 does not show any button(s)', (done) => {
        manageContentScenario(5, (mainButton, optionButtons) => {
          expect(mainButton.length).toBe(0);
          expect(optionButtons.length).toBe(0);
          expect(optionButtons.children().length).toBe(0);
          done();
        });
      });
    });

    describe('when container is locked', () => {
      it('always shows an edit button even when locked', (done) => {
        manageContentScenario(8, (mainButton) => {
          expect(mainButton.hasClass('qa-edit-content')).toBe(true);
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
          done();
        });
      });

      it('shows everything when locked by current user', (done) => {
        manageContentScenario(5, (mainButton, optionButtons) => {
          expect(mainButton.hasClass('qa-add-content')).toBe(true);
          expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT');

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT');
          expect(optionButtons.children().length).toBe(1);
          expect(optionButtons.children()[0].getAttribute('title')).toBe('CREATE_DOCUMENT');
          done();
        });
      });

      describe('shows disabled buttons when locked by another user', () => {
        function eventHandlerCount(jqueryElement, event) {
          const eventHandlers = $._data(jqueryElement[0], 'events');
          return eventHandlers && eventHandlers.hasOwnProperty(event) ? eventHandlers[event].length : 0;
        }

        it('Scenario 4', (done) => {
          manageContentScenario(10, (mainButton, optionButtons) => {
            expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
            expect(eventHandlerCount(mainButton, 'click')).toEqual(1);

            mainButton.trigger('mouseenter');
            expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
            expect(optionButtons.children().length).toBe(1);

            const firstOption = $(optionButtons.children()[0]);
            expect(firstOption.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');
            expect(firstOption.hasClass('hippo-fab-option-disabled')).toBe(true);
            expect(eventHandlerCount(firstOption, 'click')).toEqual(0);

            done();
          });
        });

        it('Scenario 5', (done) => {
          manageContentScenario(11, (mainButton, optionButtons) => {
            expect(mainButton.hasClass('hippo-fab-main-disabled')).toBe(true);
            expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');
            expect(eventHandlerCount(mainButton, 'click')).toEqual(0);

            mainButton.trigger('mouseenter');
            expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');
            expect(optionButtons.children().length).toBe(1);

            const firstOption = $(optionButtons.children()[0]);
            expect(firstOption.attr('title')).toBe('CREATE_DOCUMENT_LOCKED');
            expect(firstOption.hasClass('hippo-fab-option-disabled')).toBe(true);
            expect(eventHandlerCount(firstOption, 'click')).toEqual(0);

            done();
          });
        });

        it('Scenario 6', (done) => {
          manageContentScenario(12, (mainButton, optionButtons) => {
            expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

            mainButton.trigger('mouseenter');
            expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
            expect(optionButtons.children().length).toBe(2);

            const firstOption = $(optionButtons.children()[0]);
            expect(firstOption.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');
            expect(firstOption.hasClass('hippo-fab-option-disabled')).toBe(true);
            expect(eventHandlerCount(firstOption, 'click')).toEqual(0);

            const secondOption = $(optionButtons.children()[1]);
            expect(secondOption.attr('title')).toBe('CREATE_DOCUMENT_LOCKED');
            expect(secondOption.hasClass('hippo-fab-option-disabled')).toBe(true);
            expect(eventHandlerCount(secondOption, 'click')).toEqual(0);

            done();
          });
        });

        it('Scenario 7', (done) => {
          manageContentScenario(13, (mainButton, optionButtons) => {
            expect(mainButton.hasClass('hippo-fab-main-disabled')).toBe(true);
            expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');

            mainButton.trigger('mouseenter');
            expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');
            expect(optionButtons.children().length).toBe(0);

            done();
          });
        });
      });
    });

    describe('when button is on a template of a component that is not a container item', () => {
      it('does not fail on checks for locks on a surrounding element when by mistake a parameterName is used',
        (done) => {
          manageContentScenario(15, (mainButton) => {
            expect(mainButton.hasClass('qa-add-content')).toBe(true);
            done();
          });
        });
    });

    describe('order and number of buttons', () => {
      it('Scenario 1', () => {
        const config = {
          documentUuid: true,
          documentTemplateQuery: false,
          parameterName: false,
        };

        const buttons = OverlayService._getButtons(config);
        expect(buttons.length).toBe(1);
        expect(buttons[0].tooltip).toBe('EDIT_CONTENT');
      });

      it('Scenario 2', () => {
        const config = {
          documentUuid: false,
          documentTemplateQuery: true,
          parameterName: false,
        };

        const buttons = OverlayService._getButtons(config);
        expect(buttons.length).toBe(1);
        expect(buttons[0].tooltip).toBe('CREATE_DOCUMENT');
      });

      it('Scenario 3', () => {
        const config = {
          documentUuid: true,
          documentTemplateQuery: true,
          parameterName: false,
        };

        const buttons = OverlayService._getButtons(config);
        expect(buttons.length).toBe(2);
        expect(buttons[0].tooltip).toBe('EDIT_CONTENT');
        expect(buttons[1].tooltip).toBe('CREATE_DOCUMENT');
      });

      it('Scenario 4', () => {
        const config = {
          documentUuid: true,
          documentTemplateQuery: false,
          parameterName: true,
        };

        const buttons = OverlayService._getButtons(config);
        expect(buttons.length).toBe(2);
        expect(buttons[0].tooltip).toBe('EDIT_CONTENT');
        expect(buttons[1].tooltip).toBe('SELECT_DOCUMENT');
      });

      it('Scenario 5', () => {
        const config = {
          documentUuid: false,
          documentTemplateQuery: true,
          parameterName: true,
        };

        const buttons = OverlayService._getButtons(config);
        expect(buttons.length).toBe(2);
        expect(buttons[0].tooltip).toBe('SELECT_DOCUMENT');
        expect(buttons[1].tooltip).toBe('CREATE_DOCUMENT');
      });

      it('Scenario 6', () => {
        const config = {
          documentUuid: true,
          documentTemplateQuery: true,
          parameterName: true,
        };

        const buttons = OverlayService._getButtons(config);
        expect(buttons.length).toBe(3);
        expect(buttons[0].tooltip).toBe('EDIT_CONTENT');
        expect(buttons[1].tooltip).toBe('SELECT_DOCUMENT');
        expect(buttons[2].tooltip).toBe('CREATE_DOCUMENT');
      });

      it('Scenario 7', () => {
        const config = {
          documentUuid: false,
          documentTemplateQuery: false,
          parameterName: true,
        };

        const buttons = OverlayService._getButtons(config);
        expect(buttons.length).toBe(1);
        expect(buttons[0].tooltip).toBe('SELECT_DOCUMENT');
      });
    });
  });
});
