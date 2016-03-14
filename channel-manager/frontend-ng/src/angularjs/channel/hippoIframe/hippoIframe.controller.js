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

export class HippoIframeCtrl {
  constructor($rootScope, $scope, $element, linkProcessorService, hstCommentsProcessorService, CmsService, ChannelService,
              PageStructureService, OverlaySyncService, ScalingService, $translate, $mdDialog, DragDropService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$translate = $translate;
    this.$mdDialog = $mdDialog;
    this.linkProcessorService = linkProcessorService;
    this.hstCommentsProcessorService = hstCommentsProcessorService;
    this.CmsService = CmsService;
    this.ChannelService = ChannelService;
    this.PageStructureService = PageStructureService;
    this.OverlaySyncService = OverlaySyncService;
    this.DragDropService = DragDropService;

    this.iframeJQueryElement = $element.find('iframe');
    this.iframeJQueryElement.on('load', () => this.onLoad());

    OverlaySyncService.init(this.iframeJQueryElement, $element.find('.overlay'));

    CmsService.subscribe('delete-component', (componentId) => {
      this.deleteComponent(componentId);
    });

    ScalingService.init($element);
    DragDropService.init(this.iframeJQueryElement, $element.find('.channel-iframe-base'));

    $scope.$watch('iframe.editMode', () => this._enableDragDrop());
  }

  onLoad() {
    this.$rootScope.$apply(() => {
      this._parseHstComments();
      this._parseLinks();
    });
  }

  showComponentProperties(structureElement) {
    this.selectedComponent = structureElement;
    this.PageStructureService.showComponentProperties(this.selectedComponent);
  }

  deleteComponent(componentId) {
    console.log('Delete component ' + componentId + ' from AngularJS');

    this._confirmDelete()
      .then(() => {
        this.PageStructureService.removeComponent(componentId);
      },
      () => {
        this.PageStructureService.showComponentProperties(this.selectedComponent);
      });
  }

  _confirmDelete() {
    var confirm = this.$mdDialog.confirm()
      .title(this.$translate.instant('CONFIRM_DELETE_COMPONENT_TITLE'))
      .textContent(this.$translate.instant('CONFIRM_DELETE_COMPONENT_MESSAGE', { component: this.selectedComponent.getLabel() }))
      .ok(this.$translate.instant('BUTTON_YES'))
      .cancel(this.$translate.instant('BUTTON_NO'));

    return this.$mdDialog.show(confirm);
  }

  _enableDragDrop() {
    if (this.editMode) {
      this.DragDropService.enable(this.PageStructureService.containers);
    } else {
      this.DragDropService.disable();
    }
  }

  startDrag($event, structureElement) {
    this.DragDropService.startDrag($event, structureElement);
  }

  isDragging() {
    return this.DragDropService.isDragging();
  }

  _parseHstComments() {
    const iframeDom = this._getIframeDOM(this);

    this.PageStructureService.clearParsedElements();
    this.hstCommentsProcessorService.run(iframeDom,
      this.PageStructureService.registerParsedElement.bind(this.PageStructureService));
    this.PageStructureService.printParsedElements();
  }

  _getIframeDOM() {
    return this.iframeJQueryElement.contents()[0];
  }

  _parseLinks() {
    const iframeDom = this._getIframeDOM();
    const internalLinkPrefix = `${iframeDom.location.protocol}//${iframeDom.location.host}${this.ChannelService.getUrl()}`;

    this.linkProcessorService.run(iframeDom, internalLinkPrefix);
  }

  getContainers() {
    return this.editMode ? this.PageStructureService.containers : [];
  }
}
