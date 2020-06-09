/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

import hippoIframeCss from '../../../styles/string/hippo-iframe.scss?url';

describe('OverlayService', () => {
  let $document;
  let $q;
  let $rootScope;
  let $window;
  let CommunicationService;
  let DomService;
  let DragDropService;
  let OverlayService;
  let PageStructureService;
  let PickerService;
  let SvgService;

  beforeEach(() => {
    angular.mock.module('hippo-cm-iframe');

    CommunicationService = jasmine.createSpyObj('CommunicationService', ['emit', 'getAssetUrl', 'isEditable']);
    DragDropService = jasmine.createSpyObj('DragDropService', [
      'enable',
      'disable',
      'isEnabled',
      'startDragOrClick',
    ]);
    PickerService = jasmine.createSpyObj('PickerService', ['pickPath']);

    angular.mock.module(($provide) => {
      $provide.value('CommunicationService', CommunicationService);
      $provide.value('DragDropService', DragDropService);
      $provide.value('PickerService', PickerService);
    });

    inject((
      _$document_,
      _$q_,
      _$rootScope_,
      _$window_,
      _DomService_,
      _OverlayService_,
      _PageStructureService_,
      _SvgService_,
    ) => {
      $document = _$document_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $window = _$window_;
      DomService = _DomService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      SvgService = _SvgService_;
    });

    spyOn(DomService, 'addCssLinks').and.returnValue($q.resolve());
    spyOn(SvgService, 'getSvg').and.callFake(() => angular.element('<svg>test</svg>'));

    jasmine.getFixtures().load('iframe/overlay/overlay.service.fixture.html');

    const { fixturesPath } = jasmine.getStyleFixtures();
    jasmine.getStyleFixtures().fixturesPath = '';
    jasmine.getStyleFixtures().load(hippoIframeCss.slice(1));
    jasmine.getStyleFixtures().fixturesPath = fixturesPath;
  });

  afterEach(() => {
    $document.find('body')
      .empty()
      .off('click');
  });

  function loadIframeFixture() {
    const promise = OverlayService.initialize()
      .then(() => PageStructureService.parseElements())
      .catch((error) => {
        fail(error);

        return $q.reject(error);
      })
      .finally(() => {
        setTimeout(() => $rootScope.$digest(), 0);
      });

    $rootScope.$digest();

    return promise;
  }

  it('does not throw errors when synced before init', () => {
    expect(() => OverlayService.sync()).not.toThrow();
  });

  it('attaches a MutationObserver on the iframe document on first load', async () => {
    await loadIframeFixture();

    expect(OverlayService.observer).not.toBeNull();
  });

  it('syncs when the page structure has changed', async () => {
    await loadIframeFixture();
    spyOn(OverlayService, 'sync');
    await OverlayService._onPageChange();

    expect(OverlayService.sync).toHaveBeenCalled();
  });

  it('syncs when the iframe DOM is changed', async () => {
    spyOn(OverlayService, 'sync');
    await loadIframeFixture();

    $document.find('body').css('color', 'green');
    expect(OverlayService.sync).toHaveBeenCalled();
  });

  it('syncs when the iframe is resized', async () => {
    spyOn(OverlayService, 'sync');
    await loadIframeFixture();
    await $document.trigger('resize');

    expect(OverlayService.sync).toHaveBeenCalled();
  });

  it('generates an empty overlay when there are no page structure elements', async () => {
    spyOn(PageStructureService, 'getPage').and.returnValue(null);
    spyOn(PageStructureService, 'getEmbeddedLinks').and.returnValue([]);
    await loadIframeFixture();

    expect($document.find('.hippo-overlay')).toBeEmpty();
  });

  it('should clean all overlay elements on sync', async () => {
    await loadIframeFixture();

    expect($document.find('.hippo-overlay').children()).not.toHaveLength(0);
    expect($document.find('.hst-fab')).not.toHaveLength(0);

    spyOn(PageStructureService, 'getPage').and.returnValue(null);
    spyOn(PageStructureService, 'getEmbeddedLinks').and.returnValue([]);

    await OverlayService._onPageChange();

    expect($document.find('.hippo-overlay').children()).toHaveLength(0);
    expect($document.find('.hst-fab')).toHaveLength(0);
  });

  it('should add iframe css', async () => {
    CommunicationService.getAssetUrl.and.returnValue('url');

    await loadIframeFixture();

    expect(CommunicationService.getAssetUrl).toHaveBeenCalledWith(jasmine.stringMatching('hippo-iframe'));
    expect(DomService.addCssLinks).toHaveBeenCalledWith($window, ['url']);
  });

  describe('toggleComponentsOverlay', () => {
    beforeEach(async () => {
      spyOn(PageStructureService, 'getPage').and.returnValue(null);
      spyOn(PageStructureService, 'getEmbeddedLinks').and.returnValue([]);

      await loadIframeFixture();
    });

    it('should add the hippo-show-components class on toggling component overlays on', () => {
      OverlayService.toggleComponentsOverlay(true);

      expect($document.find('html')).toHaveClass('hippo-show-components');
    });

    it('should remove the hippo-show-components class on toggling component overlays off', () => {
      OverlayService.toggleComponentsOverlay(true);
      OverlayService.toggleComponentsOverlay(false);

      expect($document.find('html')).not.toHaveClass('hippo-show-components');
    });

    it('should enable drag and drop on toggling component overlays on', () => {
      OverlayService.toggleComponentsOverlay(true);

      expect(DragDropService.enable).toHaveBeenCalled();
    });

    it('should disable drag and drop on toggling component overlays off', () => {
      OverlayService.toggleComponentsOverlay(false);

      expect(DragDropService.disable).toHaveBeenCalled();
    });
  });

  describe('toggleContentsOverlay', () => {
    beforeEach(async () => {
      spyOn(PageStructureService, 'getPage').and.returnValue(null);
      spyOn(PageStructureService, 'getEmbeddedLinks').and.returnValue([]);

      await loadIframeFixture();
    });

    it('should add the hippo-show-content class on toggling content overlays on', () => {
      OverlayService.toggleContentsOverlay(true);

      expect($document.find('html')).toHaveClass('hippo-show-content');
    });

    it('should remove the hippo-show-content class on toggling content overlays off', () => {
      OverlayService.toggleContentsOverlay(true);
      OverlayService.toggleContentsOverlay(false);

      expect($document.find('html')).not.toHaveClass('hippo-show-content');
    });
  });

  it('generates overlay elements', async () => {
    await loadIframeFixture();

    // Total overlay elements
    expect($document.find('.hippo-overlay > .hippo-overlay-element')).toHaveLength(27);
    expect($document.find('.hippo-overlay > .hippo-overlay-element-container')).toHaveLength(6);
    expect($document.find('.hippo-overlay > .hippo-overlay-element-component')).toHaveLength(5);
    expect($document.find('.hippo-overlay > .hippo-overlay-element-menu-link')).toHaveLength(1);
    expect($document.find('.hippo-overlay > .hippo-overlay-element-manage-content-link')).toHaveLength(15);
    expect($document.find('.hst-fab')).toHaveLength(16);
  });

  describe('selected component highlighting', () => {
    const activeComponentSelector = '.hippo-overlay > .hippo-overlay-element-component-active';
    function expectActiveComponent(qaName) {
      expect($document.find(activeComponentSelector)).toHaveLength(1);
      expect($document.find(`${activeComponentSelector} [data-qa-name="${qaName}"]`)).toHaveLength(1);
    }

    function expectNoActiveComponent() {
      expect($document.find(activeComponentSelector)).toHaveLength(0);
    }

    it('highlights component after selecting it', async () => {
      await loadIframeFixture();

      expectNoActiveComponent();

      OverlayService.selectComponent('aaaa');
      expectActiveComponent('component A');
    });

    it('un-highlights component after deselecting it', async () => {
      await loadIframeFixture();

      OverlayService.selectComponent('aaaa');
      expectActiveComponent('component A');

      OverlayService.selectComponent(null);
      expectNoActiveComponent();
    });

    it('highlights component after loading a page that contains the component', async () => {
      OverlayService.selectComponent('aaaa');
      await loadIframeFixture();

      expectActiveComponent('component A');
    });
  });

  it('sets specific CSS classes on the box- and overlay elements of containers', async () => {
    await loadIframeFixture();

    const vboxContainerBox = $document.find('#container-vbox');
    expect(vboxContainerBox).toHaveClass('hippo-overlay-box-container-filled');

    const containers = $document.find('.hippo-overlay-element-container');
    const vboxContainerOverlay = containers.eq(0);
    expect(vboxContainerOverlay).not.toHaveClass('hippo-overlay-element-container-empty');

    const nomarkupContainerBox = $document.find('#container-nomarkup');
    expect(nomarkupContainerBox).toHaveClass('hippo-overlay-box-container-filled');

    const nomarkupContainerOverlay = containers.eq(1);
    expect(nomarkupContainerOverlay).not.toHaveClass('hippo-overlay-element-container-empty');

    const emptyContainerBox = $document.find('#container-empty');
    expect(emptyContainerBox).not.toHaveClass('hippo-overlay-box-container-filled');

    const emptyContainerOverlay = containers.eq(2);
    expect(emptyContainerOverlay).toHaveClass('hippo-overlay-element-container-empty');

    const inheritedContainerOverlay = containers.eq(3);
    expect(inheritedContainerOverlay).toHaveClass('hippo-overlay-element-container-disabled');

    const sharedContainerOverlay = containers.eq(5);
    expect(sharedContainerOverlay).toHaveClass('hippo-overlay-element-container-readonly');
  });

  it('generates box elements for re-rendered components without any markup in an HST.NoMarkup container', async () => {
    await loadIframeFixture();

    const markupComponentC = $document.find('#componentC');
    const componentC = PageStructureService.getPage().getComponentById('cccc');
    const boxElement = componentC.getBoxElement();

    expect(boxElement.is(markupComponentC)).toBe(true);

    const emptyMarkup = `
      <!-- {
        "HST-Type": "CONTAINER_ITEM_COMPONENT",
        "HST-Label": "component C",
        "uuid": "cccc"
      } -->
      <!-- { "HST-End": "true", "uuid": "cccc" } -->
    `;

    await PageStructureService.updateComponent(componentC.getId(), emptyMarkup);

    const generatedBoxElement = PageStructureService.getPage().getComponentById('cccc').getBoxElement();
    expect(generatedBoxElement).toBeDefined();
    expect(generatedBoxElement).toHaveClass('hippo-overlay-box-empty');
  });

  it('only renders labels for structure elements that have a label', async () => {
    await loadIframeFixture();

    expect($document.find('.hippo-overlay > .hippo-overlay-element-component > .hippo-overlay-label')).toHaveLength(5);
    expect($document.find('.hippo-overlay > .hippo-overlay-element-container > .hippo-overlay-label')).toHaveLength(6);
    expect($document.find('.hippo-overlay > .hippo-overlay-element-link > .hippo-overlay-label')).toHaveLength(0);

    const emptyContainer = $document.find('.hippo-overlay-element-container').eq(2);
    expect(emptyContainer.find('.hippo-overlay-label-text').html()).toBe('Empty container');
  });

  it('renders "shared" labels for structure elements that are marked with HST-Shared', async () => {
    await loadIframeFixture();

    const container = $document.find('.hippo-overlay > .hippo-overlay-element-container').eq(5);
    const labelText = container.find('.hippo-overlay-label-text');

    expect(labelText.html()).toBe('vBox shared container SHARED_SUFFIX');
    expect(container.find('.hippo-overlay-label').attr('data-qa-name')).toBe('vBox shared container-shared');
  });

  it('renders the name structure elements in a data-qa-name attribute', async () => {
    await loadIframeFixture();

    expect($document.find('.hippo-overlay > .hippo-overlay-element-component > .hippo-overlay-label[data-qa-name]'))
      .toHaveLength(5);
    expect($document.find('.hippo-overlay > .hippo-overlay-element-container > .hippo-overlay-label[data-qa-name]'))
      .toHaveLength(6);
    expect($document.find('.hippo-overlay-element-container:eq(2) .hippo-overlay-label').attr('data-qa-name'))
      .toBe('Empty container');
  });

  it('renders icons for links', async () => {
    await loadIframeFixture();
    const svg = $document.find('.hippo-overlay > .hippo-overlay-element-link > svg');

    expect(svg).toHaveLength(1);
    expect(svg.eq(0)).toContainText('test');
  });

  it('renders a title for links', async () => {
    await loadIframeFixture();

    expect($document.find('.hippo-overlay > .hippo-overlay-element-menu-link').attr('title')).toBe('EDIT_MENU');
  });

  it('renders lock icons for disabled containers', async () => {
    await loadIframeFixture();
    const disabledContainer = $document.find('.hippo-overlay > .hippo-overlay-element-container').eq(4);
    const lock = disabledContainer.find('.hippo-overlay-lock');

    expect(lock).toHaveLength(1);
    expect(lock.find('svg')).toHaveLength(1);
    expect(lock.attr('data-locked-by')).toBe('CONTAINER_LOCKED_BY');
  });

  it('does not render lock icons for enabled containers', async () => {
    await loadIframeFixture();
    const containers = $document.find('.hippo-overlay > .hippo-overlay-element-container');

    expect(containers.eq(0).find('.hippo-overlay-lock')).toHaveLength(0);
    expect(containers.eq(1).find('.hippo-overlay-lock')).toHaveLength(0);
  });

  it('renders lock icons for inherited containers', async () => {
    await loadIframeFixture();

    const inheritedContainer = $document.find('.hippo-overlay > .hippo-overlay-element-container').eq(3);
    const lock = inheritedContainer.find('.hippo-overlay-lock');
    expect(lock).toHaveLength(1);
    expect(lock.find('svg')).toHaveLength(1);
    expect(lock.attr('data-locked-by')).toBe('CONTAINER_INHERITED');
  });

  it('renders the experiment state of components', async () => {
    await loadIframeFixture();
    const componentWithExperiment = $document.find('.hippo-overlay > .hippo-overlay-element-component').eq(3);
    const label = componentWithExperiment.find('.hippo-overlay-label');

    expect(label).toHaveLength(1);
    expect(label.attr('data-qa-experiment-id')).toBe('1234');
    expect(label.find('svg')).toHaveLength(1);

    const labelText = label.find('.hippo-overlay-label-text');
    expect(labelText).toHaveLength(1);
    expect(labelText.html()).toBe('EXPERIMENT_LABEL_RUNNING');
  });

  it('updates the experiment state of components', async () => {
    await loadIframeFixture();

    const componentWithExperiment = $document.find('.hippo-overlay > .hippo-overlay-element-component').eq(3);
    const labelText = componentWithExperiment.find('.hippo-overlay-label-text');
    expect(labelText.html()).toBe('EXPERIMENT_LABEL_RUNNING');
    const component = PageStructureService.getComponentByOverlayElement(componentWithExperiment);

    spyOn(component, 'getExperimentStateLabel').and.returnValue('EXPERIMENT_LABEL_COMPLETED');
    OverlayService.sync();

    expect(labelText.html()).toBe('EXPERIMENT_LABEL_COMPLETED');
  });

  it('starts showing the experiment state of a component for which an experiment was just created', async () => {
    await loadIframeFixture();
    const componentElementA = $document.find('.hippo-overlay > .hippo-overlay-element-component').eq(0);
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

    const componentA = PageStructureService.getPage().getComponentById('aaaa');
    await PageStructureService.updateComponent(componentA.getId(), componentMarkupWithExperiment);

    const label = componentElementA.find('.hippo-overlay-label');
    expect(label.attr('data-qa-experiment-id')).toBe('567');
    expect(labelText.html()).toBe('EXPERIMENT_LABEL_CREATED');
  });

  it('syncs the position of overlay elements when content overlay is active', async () => {
    OverlayService.toggleContentsOverlay(true);
    await loadIframeFixture();

    const components = $document.find('.hippo-overlay > .hippo-overlay-element-component');

    const componentA = $(components[0]);
    expect(componentA).not.toHaveClass('hippo-overlay-element-visible');

    const menuLink = $document.find('.hippo-overlay > .hippo-overlay-element-menu-link');
    expect(menuLink).not.toHaveClass('hippo-overlay-element-visible');

    const contentLink = $document.find('.hippo-overlay > .hippo-overlay-element-manage-content-link');
    expect(contentLink.css('top')).toBe('0px');
    expect(contentLink.css('left')).toBe(`${$document.width() - 40}px`);
    expect(contentLink.css('width')).toBe('40px');
    expect(contentLink.css('height')).toBe('40px');

    const componentB = $(components[1]);
    expect(componentB).not.toHaveClass('hippo-overlay-element-visible');

    const emptyContainer = $($document.find('.hippo-overlay > .hippo-overlay-element-container')[1]);
    expect(emptyContainer).not.toHaveClass('hippo-overlay-element-visible');
  });

  it('syncs the position of overlay elements in edit mode', async () => {
    const minimalOverlayBoxHeight = 50;

    OverlayService.toggleComponentsOverlay(true);
    OverlayService.toggleContentsOverlay(false);
    await loadIframeFixture();

    const components = $document.find('.hippo-overlay > .hippo-overlay-element-component');

    const componentA = components.eq(0);
    expect(componentA.css('top')).toBe('4px');
    expect(componentA.css('left')).toBe('2px');
    expect(componentA.css('width')).toBe(`${200 - 2}px`);
    expect(componentA.css('height')).toBe('100px');

    const menuLink = $document.find('.hippo-overlay > .hippo-overlay-element-menu-link');
    expect(menuLink.css('top')).toBe(`${4 + 30}px`);
    expect(menuLink.css('left')).toBe(`${200 - 40}px`);
    expect(menuLink.css('width')).toBe('40px');
    expect(menuLink.css('height')).toBe('40px');

    const contentLink = $document.find('.hippo-overlay > .hippo-overlay-element-manage-content-link');
    expect(contentLink).not.toHaveClass('hippo-overlay-element-visible');

    const componentB = components.eq(1);
    expect(componentB.css('top')).toBe(`${4 + 100}px`);
    expect(componentB.css('left')).toBe('2px');
    expect(componentB.css('width')).toBe(`${200 - 2}px`);
    expect(componentB.css('height')).toBe('200px');

    const emptyContainer = $document.find('.hippo-overlay > .hippo-overlay-element-container').eq(2);
    expect(emptyContainer.css('top')).toBe(`${400 + minimalOverlayBoxHeight + 4}px`);
    expect(emptyContainer.css('left')).toBe('0px');
    expect(emptyContainer.css('width')).toBe('200px');
    expect(emptyContainer.css('height')).toBe(`${minimalOverlayBoxHeight}px`); // minimum height of empty container
  });

  it('takes the scroll position of the iframe into account when positioning overlay elements', async () => {
    OverlayService.toggleContentsOverlay(true);
    await loadIframeFixture();
    // enlarge body so the iframe can scroll
    const body = $document.find('body');
    body.width('200%');
    body.height('200%');

    $window.scrollX = 1;
    $window.scrollY = 2;
    OverlayService.sync();

    const contentLink = $document.find('.hippo-overlay > .hippo-overlay-element-manage-content-link');
    expect(contentLink.css('top')).toBe('2px');
    expect(contentLink.css('left')).toBe(`${$document.width() / 2 - 40 + 1}px`);
    expect(contentLink.css('width')).toBe('40px');
    expect(contentLink.css('height')).toBe('40px');
  });

  function expectNoPropagatedClicks() {
    const body = $document.find('body');
    body.click(() => {
      fail('click event should not propagate to the page');
    });
  }

  describe('_onOverlayMouseDown', () => {
    let component;
    let overlayComponentElement;

    beforeEach(async () => {
      await loadIframeFixture();

      component = PageStructureService.getPage().getComponentById('aaaa');
      overlayComponentElement = $document.find('.hippo-overlay > .hippo-overlay-element-component').first();
      OverlayService.toggleComponentsOverlay(true);
    });

    it('should delegate mouse down event to the drag and drop service', () => {
      DragDropService.isEnabled.and.returnValue(true);
      overlayComponentElement.mousedown();

      expect(DragDropService.startDragOrClick).toHaveBeenCalledWith(jasmine.any(Object), component);
    });

    it('should not delegate mouse down event to the drag and drop service if it is disabled', () => {
      DragDropService.isEnabled.and.returnValue(false);
      overlayComponentElement.mousedown();

      expect(DragDropService.startDragOrClick).not.toHaveBeenCalled();
    });

    it('should not delegate mouse down event to the drag and drop service if related component was not found', () => {
      DragDropService.isEnabled.and.returnValue(true);
      spyOn(PageStructureService, 'getComponentByOverlayElement').and.returnValue(undefined);
      overlayComponentElement.mousedown();

      expect(DragDropService.startDragOrClick).not.toHaveBeenCalled();
    });
  });

  it('should trigger document:create event', async () => {
    await loadIframeFixture();
    const overlayElementScenario2 = $document.find('.hippo-overlay-element-manage-content-link')[1];
    const createContentButton = $(overlayElementScenario2).find('.hippo-fab-main');

    expectNoPropagatedClicks();
    createContentButton.click();

    expect(CommunicationService.emit).toHaveBeenCalledWith('document:create', jasmine.objectContaining({
      documentTemplateQuery: 'manage-content-document-template-query',
    }));
  });

  it('should trigger document:edit event', async () => {
    await loadIframeFixture();
    const overlayElementScenario1 = $document.find('.hippo-overlay-element-manage-content-link')[0];
    const createContentButton = $(overlayElementScenario1).find('.hippo-fab-main');

    expectNoPropagatedClicks();
    createContentButton.click();

    expect(CommunicationService.emit).toHaveBeenCalledWith('document:edit', 'manage-content-uuid');
  });

  it('can select a document', async () => {
    CommunicationService.isEditable.and.returnValue(true);
    await loadIframeFixture();
    const overlayElementScenario5 = $document.find('.hippo-overlay-element-manage-content-link')[4];
    const pickPathButton = $(overlayElementScenario5).find('.hippo-fab-main');
    expectNoPropagatedClicks();

    pickPathButton.click();

    expect(CommunicationService.emit).toHaveBeenCalledWith('document:select', jasmine.objectContaining({
      containerItemId: 'bbbb',
      isParameterValueRelativePath: false,
      parameterName: 'manage-content-component-parameter',
      parameterValue: undefined,
      pickerConfig: jasmine.any(Object),
    }));
  });

  it('does not throw an error when calling edit menu handler if not set', async () => {
    await loadIframeFixture();
    const menuLink = $document.find('.hippo-overlay > .hippo-overlay-element-menu-link');

    expectNoPropagatedClicks();
    expect(() => menuLink.click()).not.toThrow();
  });

  it('should trigger the menu:edit event', async () => {
    OverlayService.toggleComponentsOverlay(true);
    await loadIframeFixture();
    const menuLink = $document.find('.hippo-overlay > .hippo-overlay-element-menu-link');

    expectNoPropagatedClicks();
    menuLink.click();

    expect(CommunicationService.emit).toHaveBeenCalledWith('menu:edit', 'menu-in-component-a');
  });

  it('removes overlay elements when they are no longer part of the page structure', async () => {
    OverlayService.toggleComponentsOverlay(true);

    await loadIframeFixture();

    expect($document.find('.hippo-overlay > .hippo-overlay-element')).toHaveLength(27);
    expect($document.find('.hippo-overlay > .hippo-overlay-element-menu-link')).toHaveLength(1);

    const componentMarkupWithoutMenuLink = `
      <!-- {
        "HST-Type": "CONTAINER_ITEM_COMPONENT",
        "HST-Label": "component A",
        "uuid": "aaaa"
      } -->
        <p id="markup-in-component-a">Markup in component A without menu link</p>
      <!-- { "HST-End": "true", "uuid": "aaaa" } -->
    `;

    const componentA = PageStructureService.getPage().getComponentById('aaaa');
    await PageStructureService.updateComponent(componentA.getId(), componentMarkupWithoutMenuLink);

    expect($document.find('.hippo-overlay > .hippo-overlay-element')).toHaveLength(26);
    expect($document.find('.hippo-overlay > .hippo-overlay-element-menu-link')).toHaveLength(0);
  });

  describe('Manage content dial button(s)', () => {
    beforeEach(() => {
      CommunicationService.isEditable.and.returnValue(true);
      OverlayService._isEditable = true;
    });

    it('returns correct configuration out of config object', () => {
      const config = { // each property should be filled with the method that will extract the data from the HST comment
        documentUuid: true,
        documentTemplateQuery: true,
        parameterName: true,
      };
      const buttons = OverlayService._getButtons(config);

      expect(buttons).toHaveLength(3);
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
        const component = {
          getId: () => 'aaaa',
          isLocked: () => locked,
        };
        const structureElement = {
          getDefaultPath: () => null,
          getComponent: () => component,
          getFolderTemplateQuery: () => folderTemplateQuery,
          getParameterName: () => parameterName,
          getParameterValue: () => null,
          getPickerConfig: () => null,
          getRootPath: () => null,
          getDocumentTemplateQuery: () => documentTemplateQuery,
          getId: () => uuid,
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
          OverlayService._isEditable = false;
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

    async function manageContentScenario(scenarioNumber) {
      await loadIframeFixture();

      const container = $document.find('.hippo-overlay-element-manage-content-link').eq(scenarioNumber - 1);

      return {
        mainButton: container.find('.hippo-fab-main'),
        optionButtons: container.find('.hippo-fab-options'),
      };
    }

    describe('Dial button scenario\'s for unlocked containers', () => {
      it('Scenario 1', async () => {
        const { mainButton, optionButtons } = await manageContentScenario(1);

        expect(mainButton.hasClass('qa-edit-content')).toBe(true);
        expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

        mainButton.trigger('mouseenter');
        expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
        expect(optionButtons.children()).toHaveLength(0);
      });

      it('Scenario 2', async () => {
        const { mainButton, optionButtons } = await manageContentScenario(2);
        expect(mainButton.hasClass('qa-add-content')).toBe(true);
        expect(mainButton.attr('title')).toBe('CREATE_DOCUMENT');

        mainButton.trigger('mouseenter');
        expect(mainButton.attr('title')).toBe('CREATE_DOCUMENT');
        expect(optionButtons.children()).toHaveLength(0);
      });

      it('Scenario 3', async () => {
        const { mainButton, optionButtons } = await manageContentScenario(3);
        expect(mainButton.hasClass('qa-edit-content')).toBe(true);
        expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

        mainButton.trigger('mouseenter');
        expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
        expect(optionButtons.children()).toHaveLength(1);
        expect(optionButtons.children()[0].getAttribute('title')).toBe('CREATE_DOCUMENT');
      });

      it('Scenario 4', async () => {
        const { mainButton, optionButtons } = await manageContentScenario(4);
        expect(mainButton.hasClass('qa-edit-content')).toBe(true);
        expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

        mainButton.trigger('mouseenter');
        expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
        expect(optionButtons.children()).toHaveLength(1);
        expect(optionButtons.children()[0].getAttribute('title')).toBe('SELECT_DOCUMENT');
      });

      it('Scenario 5', async () => {
        const { mainButton, optionButtons } = await manageContentScenario(5);
        expect(mainButton.hasClass('qa-add-content')).toBe(true);
        expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT');

        mainButton.trigger('mouseenter');
        expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT');
        expect(optionButtons.children()).toHaveLength(1);
        expect(optionButtons.children()[0].getAttribute('title')).toBe('CREATE_DOCUMENT');
      });

      it('Scenario 6', async () => {
        const { mainButton, optionButtons } = await manageContentScenario(6);
        expect(mainButton.hasClass('qa-edit-content')).toBe(true);
        expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

        mainButton.trigger('mouseenter');
        expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
        expect(optionButtons.children()).toHaveLength(2);
        expect(optionButtons.children()[0].getAttribute('title')).toBe('SELECT_DOCUMENT');
        expect(optionButtons.children()[1].getAttribute('title')).toBe('CREATE_DOCUMENT');
      });
    });

    describe('when channel is not editable', () => {
      beforeEach(() => {
        CommunicationService.isEditable.and.returnValue(false);
      });

      it('Scenario 5 does not show any button(s)', async () => {
        const { mainButton, optionButtons } = await manageContentScenario(5);

        expect(mainButton).toHaveLength(0);
        expect(optionButtons).toHaveLength(0);
        expect(optionButtons.children()).toHaveLength(0);
      });
    });

    describe('when container is locked', () => {
      it('always shows an edit button even when locked', async () => {
        const { mainButton } = await manageContentScenario(8);
        expect(mainButton.hasClass('qa-edit-content')).toBe(true);
        expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

        mainButton.trigger('mouseenter');
        expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
      });

      it('shows everything when locked by current user', async () => {
        const { mainButton, optionButtons } = await manageContentScenario(5);
        expect(mainButton.hasClass('qa-add-content')).toBe(true);
        expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT');

        mainButton.trigger('mouseenter');
        expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT');
        expect(optionButtons.children()).toHaveLength(1);
        expect(optionButtons.children()[0].getAttribute('title')).toBe('CREATE_DOCUMENT');
      });

      describe('shows disabled buttons when locked by another user', () => {
        function eventHandlerCount(jqueryElement, event) {
          const eventHandlers = angular.element._data(jqueryElement[0], 'events');
          return eventHandlers && eventHandlers.hasOwnProperty(event) ? eventHandlers[event].length : 0;
        }

        it('Scenario 4', async () => {
          const { mainButton, optionButtons } = await manageContentScenario(10);
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
          expect(eventHandlerCount(mainButton, 'click')).toEqual(1);

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
          expect(optionButtons.children()).toHaveLength(1);

          const firstOption = $(optionButtons.children()[0]);
          expect(firstOption.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');
          expect(firstOption.hasClass('hippo-fab-option-disabled')).toBe(true);
          expect(eventHandlerCount(firstOption, 'click')).toEqual(0);
        });

        it('Scenario 5', async () => {
          const { mainButton, optionButtons } = await manageContentScenario(11);
          expect(mainButton.hasClass('hippo-fab-main-disabled')).toBe(true);
          expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');
          expect(eventHandlerCount(mainButton, 'click')).toEqual(0);

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');
          expect(optionButtons.children()).toHaveLength(1);

          const firstOption = $(optionButtons.children()[0]);
          expect(firstOption.attr('title')).toBe('CREATE_DOCUMENT_LOCKED');
          expect(firstOption.hasClass('hippo-fab-option-disabled')).toBe(true);
          expect(eventHandlerCount(firstOption, 'click')).toEqual(0);
        });

        it('Scenario 6', async () => {
          const { mainButton, optionButtons } = await manageContentScenario(12);
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('EDIT_CONTENT');
          expect(optionButtons.children()).toHaveLength(2);

          const firstOption = $(optionButtons.children()[0]);
          expect(firstOption.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');
          expect(firstOption.hasClass('hippo-fab-option-disabled')).toBe(true);
          expect(eventHandlerCount(firstOption, 'click')).toEqual(0);

          const secondOption = $(optionButtons.children()[1]);
          expect(secondOption.attr('title')).toBe('CREATE_DOCUMENT_LOCKED');
          expect(secondOption.hasClass('hippo-fab-option-disabled')).toBe(true);
          expect(eventHandlerCount(secondOption, 'click')).toEqual(0);
        });

        it('Scenario 7', async () => {
          const { mainButton, optionButtons } = await manageContentScenario(13);
          expect(mainButton.hasClass('hippo-fab-main-disabled')).toBe(true);
          expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');

          mainButton.trigger('mouseenter');
          expect(mainButton.attr('title')).toBe('SELECT_DOCUMENT_LOCKED');
          expect(optionButtons.children()).toHaveLength(0);
        });
      });
    });

    describe('when button is on a template of a component that is not a container item', () => {
      it('does not fail on checks for locks on a surrounding element when by mistake a parameterName is used',
        async () => {
          const { mainButton } = await manageContentScenario(15);

          expect(mainButton.hasClass('qa-add-content')).toBe(true);
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
        expect(buttons).toHaveLength(1);
        expect(buttons[0].tooltip).toBe('EDIT_CONTENT');
      });

      it('Scenario 2', () => {
        const config = {
          documentUuid: false,
          documentTemplateQuery: true,
          parameterName: false,
        };

        const buttons = OverlayService._getButtons(config);
        expect(buttons).toHaveLength(1);
        expect(buttons[0].tooltip).toBe('CREATE_DOCUMENT');
      });

      it('Scenario 3', () => {
        const config = {
          documentUuid: true,
          documentTemplateQuery: true,
          parameterName: false,
        };

        const buttons = OverlayService._getButtons(config);
        expect(buttons).toHaveLength(2);
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
        expect(buttons).toHaveLength(2);
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
        expect(buttons).toHaveLength(2);
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
        expect(buttons).toHaveLength(3);
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
        expect(buttons).toHaveLength(1);
        expect(buttons[0].tooltip).toBe('SELECT_DOCUMENT');
      });
    });
  });

  describe('toggleAddMode', () => {
    beforeEach(() => loadIframeFixture());

    it('should add a css class when the add mode is enabled', () => {
      OverlayService.toggleAddMode(true);

      expect($document.find('.hippo-overlay-add-mode')).toHaveLength(1);
    });

    it('should remove a css class when the add mode is disabled', () => {
      OverlayService.toggleAddMode(true);
      OverlayService.toggleAddMode(false);

      expect($document.find('.hippo-overlay-add-mode')).toHaveLength(0);
    });

    it('should reject add mode promise when the mode is disabled', (done) => {
      const promise = OverlayService.toggleAddMode(true);
      OverlayService.toggleAddMode(false);

      promise.catch((result) => {
        expect(result).toBeUndefined();
        done();
      });
    });

    it('should reject add mode promise on overlay click', (done) => {
      const promise = OverlayService.toggleAddMode(true);
      $document.find('.hippo-overlay').click();

      promise.catch((result) => {
        expect(result).toBeUndefined();
        done();
      });
    });

    it('should resolve add mode promise on the container click', (done) => {
      const promise = OverlayService.toggleAddMode(true);
      const containerOverlay = $document.find('.hippo-overlay > .hippo-overlay-element-container').eq(0);

      containerOverlay.click();
      promise.then((result) => {
        expect(result).toEqual({ container: 'container-vbox' });
        done();
      });
    });

    it('should stay in the add mode on the disabled container click', () => {
      OverlayService.toggleAddMode(true);
      const containerOverlay = $document.find('.hippo-overlay > .hippo-overlay-element-container').eq(4);

      containerOverlay.click();
      expect(OverlayService.isInAddMode).toBe(true);
    });

    it('should add a component before the clicked one', (done) => {
      const promise = OverlayService.toggleAddMode(true);
      const componentBeforeArea = $document.find(`.hippo-overlay
        > .hippo-overlay-element-component:eq(0)
        .hippo-overlay-element-component-drop-area-before`);

      componentBeforeArea.click();
      promise.then((result) => {
        expect(result).toEqual({ container: 'container-vbox', nextComponent: 'aaaa' });
        done();
      });
    });

    it('should add a component after the clicked one', (done) => {
      const promise = OverlayService.toggleAddMode(true);
      const componentAfterArea = $document.find(`.hippo-overlay
        > .hippo-overlay-element-component:eq(0)
        .hippo-overlay-element-component-drop-area-after`);

      componentAfterArea.click();
      promise.then((result) => {
        expect(result).toEqual({ container: 'container-vbox', nextComponent: 'bbbb' });
        done();
      });
    });

    it('should stay in the add mode on the disabled component click', () => {
      OverlayService.toggleAddMode(true);
      const disabledComponentOverlay = $document.find(`.hippo-overlay
        > .hippo-overlay-element-component:eq(3)`)[0];

      disabledComponentOverlay.click();
      expect(OverlayService.isInAddMode).toBe(true);
    });
  });

  describe('shared containers', () => {
    function expectSingleButton(el, txt) {
      const buttons = el.find('button');
      expect(buttons).toHaveLength(1);
      expect(buttons.eq(0).text()).toBe(txt);
    }

    function expectDoubleButton(el, txt) {
      const buttons = el.find('button');
      expect(buttons).toHaveLength(2);
      expect(buttons.eq(0).text()).toBe(txt);
      expect(buttons.eq(1).text()).toBe(txt);
    }

    beforeEach(() => loadIframeFixture());

    it('should render buttons to toggle between editing shared and local containers', () => {
      const shared = $document.find('.hippo-overlay-shared');

      expect(shared).toHaveLength(6);
      expectDoubleButton(shared.eq(0), 'TOGGLE_PAGE_CONTAINERS');
      expectDoubleButton(shared.eq(1), 'TOGGLE_PAGE_CONTAINERS');
      expectSingleButton(shared.eq(2), 'TOGGLE_PAGE_CONTAINERS');
      expectDoubleButton(shared.eq(3), 'TOGGLE_PAGE_CONTAINERS');
      expectSingleButton(shared.eq(4), 'TOGGLE_PAGE_CONTAINERS');
      expectDoubleButton(shared.eq(5), 'TOGGLE_SHARED_CONTAINERS');
    });

    it('should toggle "readonly" state between shared and local containers when clicking the toggle button', () => {
      const containers = $document.find('.hippo-overlay-element-container');
      const localContainerOverlay = containers.eq(0);
      const sharedContainerOverlay = containers.eq(5);

      expect(localContainerOverlay).not.toHaveClass('hippo-overlay-element-container-readonly');
      expect(sharedContainerOverlay).toHaveClass('hippo-overlay-element-container-readonly');

      sharedContainerOverlay.find('.hippo-overlay-shared button')[0].click();

      expect(sharedContainerOverlay).not.toHaveClass('hippo-overlay-element-container-readonly');
      expect(localContainerOverlay).toHaveClass('hippo-overlay-element-container-readonly');

      localContainerOverlay.find('.hippo-overlay-shared button')[0].click();

      expect(localContainerOverlay).not.toHaveClass('hippo-overlay-element-container-readonly');
      expect(sharedContainerOverlay).toHaveClass('hippo-overlay-element-container-readonly');
    });
  });
});
