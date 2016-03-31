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

const COMPONENT_QA_CLASS = 'qa-dragula-component';
const MOUSEUP_EVENT_NAME = 'mouseup.dragDropService';

export class DragDropService {

  constructor($rootScope, $q, DomService, HstService, PageStructureService, ScalingService, ChannelService, ScrollService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$q = $q;
    this.DomService = DomService;
    this.HstService = HstService;
    this.PageStructureService = PageStructureService;
    this.ScalingService = ScalingService;
    this.ChannelService = ChannelService;
    this.ScrollService = ScrollService;

    this.draggingOrClicking = false;
  }

  init(iframeJQueryElement, baseJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
    this.baseJQueryElement = baseJQueryElement;

    this.ScrollService.init(iframeJQueryElement, baseJQueryElement);
    this.iframeJQueryElement.on('load', () => this._onLoad());
  }

  _onLoad() {
    this.iframe = this.iframeJQueryElement[0].contentWindow;
    $(this.iframe).one('unload', () => this._destroyDragula());
  }

  _destroyDragula() {
    this._destroyDrake();
    this.dragulaPromise = null;
  }

  _destroyDrake() {
    if (this.drake) {
      this.drake.destroy();
      this.drake = null;
      this.draggingOrClicking = false;
    }
  }

  enable(containers) {
    if (!this.dragulaPromise) {
      this.dragulaPromise = this._injectDragula(this.iframe);
    }

    return this.dragulaPromise.then(() => {
      const iframeContainerElements = containers.map((container) => container.getBoxElement()[0]);

      this.drake = this.iframe.dragula(iframeContainerElements, {
        ignoreInputTextSelection: false,
        mirrorContainer: this.baseJQueryElement[0],
      });
      this.drake.on('drag', () => this._onStartDrag());
      this.drake.on('cloned', (clone, original) => this._onMirrorCreated(clone, original));
      this.drake.on('dragend', (el) => this._onStopDrag(el));
      this.drake.on('drop', (el, target, source, sibling) => this._onDrop(el, target, source, sibling));

      this.ScrollService.enable(() => this.draggingOrClicking);
    });
  }

  disable() {
    this.ScrollService.disable();
    this._destroyDrake();
  }

  _injectDragula(iframe) {
    const appRootUrl = this.DomService.getAppRootUrl();

    const dragulaCss = `${appRootUrl}styles/dragula.min.css`;
    this.DomService.addCss(iframe, dragulaCss);

    const dragulaJs = `${appRootUrl}scripts/dragula.min.js`;
    return this.DomService.addScript(iframe, dragulaJs);
  }

  replaceContainer(oldContainer, newContainer) {
    return this.dragulaPromise.then(() => {
      const oldIndex = this.drake.containers.indexOf(oldContainer.getBoxElement()[0]);
      if (oldIndex >= 0 && newContainer) {
        this.drake.containers[oldIndex] = newContainer.getBoxElement()[0];
      }
    });
  }

  startDragOrClick($event, component) {
    this.draggingOrClicking = true;
    this._dispatchMouseDownInIframe($event, component);

    const componentBoxElement = component.getBoxElement();
    componentBoxElement.on(MOUSEUP_EVENT_NAME, () => this._onComponentClick(component));
    componentBoxElement.addClass(COMPONENT_QA_CLASS);
  }

  _onComponentClick(component) {
    if (!this.isDragging()) {
      this._onStopDrag(component.getBoxElement());
      this.PageStructureService.showComponentProperties(component);
    }
  }

  isDraggingOrClicking() {
    return this.draggingOrClicking;
  }

  isDragging() {
    return this.drake && this.drake.dragging;
  }

  _onStartDrag() {
    // make Angular evaluate isDragging() again
    this._digestIfNeeded();
  }

  _onMirrorCreated(mirrorElement, originalElement) {
    this.DomService.copyComputedStyleExcept(originalElement, mirrorElement, ['border-[a-z]*', 'box-shadow', 'height', 'margin-[a-z]*', 'overflow', 'opacity', 'pointer-events', 'position', '[a-z\\\-]*user-select', 'width']);
    this.DomService.copyComputedStyleOfDescendantsExcept(originalElement, mirrorElement, ['opacity', 'pointer-events', '[a-z\\\-]*user-select']);
  }

  _onStopDrag(element) {
    this.draggingOrClicking = false;
    $(element)
      .off(MOUSEUP_EVENT_NAME)
      .removeClass(COMPONENT_QA_CLASS);
    this._digestIfNeeded();
  }

  _digestIfNeeded() {
    if (!this.$rootScope.$$phase) {
      this.$rootScope.$digest();
    }
  }

  _onDrop(movedElement, targetContainerElement, sourceContainerElement, targetNextComponentElement) {
    const sourceContainer = this.PageStructureService.getContainerByIframeElement(sourceContainerElement);
    const movedComponent = sourceContainer.getComponentByIframeElement(movedElement);
    const targetContainer = this.PageStructureService.getContainerByIframeElement(targetContainerElement);
    const targetNextComponent = targetContainer.getComponentByIframeElement(targetNextComponentElement);

    // first update the page structure so the component is already 'moved' in the client-side state
    sourceContainer.removeComponent(movedComponent);
    targetContainer.addComponentBefore(movedComponent, targetNextComponent);

    const changedContainers = [sourceContainer];
    if (sourceContainer.getId() !== targetContainer.getId()) {
      changedContainers.push(targetContainer);
    }

    // next, push the updated container representation(s) to the backend
    const backendCallPromises = [];
    changedContainers.forEach((container) => backendCallPromises.push(this._updateContainer(container)));

    // last, re-render the changed container(s) so their meta-data is updated and we're sure they look right
    this.$q.all(backendCallPromises)
      .then(() => this.ChannelService.recordOwnChange())
      // TODO: handle errors (e.g. one of the containers is already locked)
      .finally(() => {
        changedContainers.forEach((container) => this._renderContainer(container));
      });
  }

  _updateContainer(container) {
    return this.HstService.doPost(container.getHstRepresentation(), container.getId(), 'update');
  }

  _renderContainer(container) {
    this.PageStructureService.renderContainer(container)
      .then((newContainer) => this.replaceContainer(container, newContainer));
  }

  _dispatchMouseDownInIframe($event, component) {
    const [clientX, clientY] = this.ScalingService.getScaleFactor() === 1.0 ? this._shiftCoordinates($event) : this._shiftAndDescaleCoordinates($event);
    const iframeMouseDownEvent = this.DomService.createMouseDownEvent(this.iframe, clientX, clientY);
    const iframeElement = component.getBoxElement();
    iframeElement[0].dispatchEvent(iframeMouseDownEvent);
  }

  _shiftCoordinates($event) {
    const iframeOffset = this.iframeJQueryElement.offset();

    const shiftedX = $event.clientX - iframeOffset.left;
    const shiftedY = $event.clientY - iframeOffset.top;

    return [shiftedX, shiftedY];
  }

  _shiftAndDescaleCoordinates($event) {
    const iframeOffset = this.iframeJQueryElement.offset();
    const baseOffset = this.baseJQueryElement.offset();
    const scale = this.ScalingService.getScaleFactor();

    // Shift horizontal using the base offset since the iframe offset is also scaled
    // and hence to far to the right.
    const shiftedX = $event.clientX - baseOffset.left;
    const shiftedY = $event.clientY - iframeOffset.top;

    // The user sees the scaled iframe, but the browser actually uses the unscaled coordinates,
    // so we have to transform the click in the scaled iframe to its 'descaled' position.
    const iframeWidth = this.iframeJQueryElement.width();
    const shiftedAndDescaledX = (shiftedX - (iframeWidth * (1 - scale))) / scale;
    const shiftedAndDescaledY = shiftedY / scale;

    return [shiftedAndDescaledX, shiftedAndDescaledY];
  }

}
