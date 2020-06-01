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

const COMPONENT_QA_CLASS = 'qa-dragula-component';
const MIRROR_WRAPPER_SELECTOR = '.channel-dragula-mirror';
const MOUSELEAVE_EVENT_NAME = 'mouseleave.dragDropService';
const MOUSEUP_EVENT_NAME = 'mouseup.dragDropService';

export default class DragDropService {
  constructor(
    $document,
    $q,
    $rootScope,
    $window,
    CommunicationService,
    DomService,
    PageStructureService,
  ) {
    'ngInject';

    this.$document = $document;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$window = $window;
    this.CommunicationService = CommunicationService;
    this.DomService = DomService;
    this.PageStructureService = PageStructureService;

    this.draggingOrClicking = false;
    this.dropping = false;
  }

  initialize() {
    this.$rootScope.$on('page:change', () => this._sync());
  }

  _sync() {
    if (this.drake) {
      this.drake.containers = this._getContainerBoxElements();
    }
  }

  async enable() {
    if (!this.dragulaPromise) {
      this.dragulaPromise = this._injectDragula();
    }

    await this.dragulaPromise;

    const containerBoxElements = this._getContainerBoxElements();
    const isParentAccessible = this.$window.parent
      && this.$window.parent !== this.$window
      && this.DomService.isFrameAccessible(this.$window.parent);
    const mirrorContainer = isParentAccessible
      ? angular.element(MIRROR_WRAPPER_SELECTOR, this.$window.parent.document)[0]
      : undefined;

    this.dragulaOptions = {
      mirrorContainer,
      ignoreInputTextSelection: false,
      moves: (el, source) => !this.dropping && this._isContainerEnabled(source),
      accepts: (el, target) => this._isContainerEnabled(target),
      invalid: () => !this.draggingOrClicking,
      dragDelay: 300,
    };

    this.drake = this.$window.dragula(containerBoxElements, this.dragulaOptions);
    this.drake.on('drag', (el, source) => this._onStartDrag(source));
    this.drake.on('cloned', (clone, original) => this._onMirrorCreated(clone, original));
    this.drake.on('over', (el, container) => this._updateDragDirection(container));
    this.drake.on('dragend', el => this._onStopDragOrClick(el));
    this.drake.on('drop', (el, target, source, sibling) => this._onDrop(el, target, source, sibling));
  }

  disable() {
    if (!this.drake) {
      return;
    }

    if (this.isDragging()) {
      this.drake.cancel(true);
    }
    this.drake.destroy();
    this.drake = null;
    this.dragulaOptions = null;
    this.draggingOrClicking = false;
    this.dropping = false;
  }

  isEnabled() {
    return !!this.drake;
  }

  async _injectDragula() {
    const asset = await this.CommunicationService.getAssetUrl('scripts/dragula.min.js');

    if (this._usesRequireJs()) {
      return this.$q(resolve => this.$window.require([asset], (dragula) => {
        this.$window.dragula = dragula;
        resolve();
      }));
    }

    return this.DomService.addScript(this.$window, asset);
  }

  _usesRequireJs() {
    return angular.isFunction(this.$window.require) && this.$window.require === this.$window.requirejs;
  }

  _isContainerEnabled(containerElement) {
    const container = this.PageStructureService.getContainerByIframeElement(containerElement);
    return container && !container.isDisabled();
  }

  _getContainerBoxElements() {
    const page = this.PageStructureService.getPage();
    if (!page) {
      return [];
    }

    return page.getContainers()
      .filter(container => !container.isDisabled())
      .map(container => container.getBoxElement()[0]);
  }

  startDragOrClick($event, component) {
    this.draggingOrClicking = true;

    this.$document.find('html').addClass('hippo-overlay-permeable');
    this._dispatchMouseDownInIframe($event, component);

    const componentBoxElement = component.getBoxElement();
    componentBoxElement.on(MOUSEUP_EVENT_NAME, (event) => {
      if (event.which === 1) {
        this._onComponentClick(component);
      }
    });
    componentBoxElement.on(MOUSELEAVE_EVENT_NAME, () => this._onComponentLeave(component));
    componentBoxElement.addClass(COMPONENT_QA_CLASS);
  }

  _onComponentClick(component) {
    if (!this.isDragging()) {
      this._onStopDragOrClick(component.getBoxElement());

      this.CommunicationService.emit('component:click', component.getId());
      this._digestIfNeeded();
    }
  }

  _digestIfNeeded() {
    if (!this.$rootScope.$$phase) {
      this.$rootScope.$digest();
    }
  }

  _onComponentLeave(component) {
    if (!this.isDragging()) {
      this._onStopDragOrClick(component.getBoxElement());
    }
  }

  isDraggingOrClicking() {
    return this.draggingOrClicking;
  }

  isDragging() {
    return this.drake && this.drake.dragging;
  }

  _onStartDrag(containerElement) {
    this.$document.find('html').addClass('hippo-dragging');
    this._updateDragDirection(containerElement);
    this.CommunicationService.emit('drag:start');
  }

  _onMirrorCreated(mirrorElement, originalElement) {
    this.DomService.copyComputedStyleExcept(
      originalElement,
      mirrorElement,
      [
        'border-[a-z]*',
        'box-shadow',
        'height',
        'margin-[a-z]*',
        'overflow',
        'opacity',
        'pointer-events',
        'position',
        '[a-z\\-]*user-select',
        'width',
      ],
    );

    this.DomService.copyComputedStyleOfDescendantsExcept(
      originalElement,
      mirrorElement,
      [
        'opacity',
        'pointer-events',
        '[a-z\\-]*user-select',
      ],
    );
  }

  _updateDragDirection(containerElement) {
    const container = this.PageStructureService.getContainerByIframeElement(containerElement);
    this.dragulaOptions.direction = container.getDragDirection();
  }

  _onStopDragOrClick(element) {
    this.$document.find('html').removeClass('hippo-dragging hippo-overlay-permeable');
    this.CommunicationService.emit('drag:stop');

    this.draggingOrClicking = false;

    angular.element(element)
      .off(MOUSEUP_EVENT_NAME)
      .off(MOUSELEAVE_EVENT_NAME)
      .removeClass(COMPONENT_QA_CLASS);
  }

  _onDrop(movedElement, targetContainerElement, sourceContainerElement, targetNextComponentElement) {
    this.dropping = true;

    const sourceContainer = this.PageStructureService.getContainerByIframeElement(sourceContainerElement);
    const movedComponent = sourceContainer.getComponentByIframeElement(movedElement);
    const targetContainer = this.PageStructureService.getContainerByIframeElement(targetContainerElement);
    const targetNextComponent = targetContainer.getComponentByIframeElement(targetNextComponentElement);

    try {
      this.PageStructureService.moveComponent(movedComponent, targetContainer, targetNextComponent);
    } finally {
      this.dropping = false;
    }
  }

  _dispatchMouseDownInIframe($event, component) {
    const mouseDownEvent = this.DomService.createMouseDownEvent(this.$window, $event.clientX, $event.clientY);
    const boxElement = component.getBoxElement();
    boxElement[0].dispatchEvent(mouseDownEvent);
  }
}
