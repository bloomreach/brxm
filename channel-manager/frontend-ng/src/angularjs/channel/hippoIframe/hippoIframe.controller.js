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
  constructor($rootScope, $element, $translate, $mdDialog, linkProcessorService, hstCommentsProcessorService, CmsService, ChannelService,
              PageStructureService, OverlaySyncService) {
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

    this.iframeJQueryElement = $element.find('iframe');
    this.iframeJQueryElement.on('load', () => this.onLoad());

    OverlaySyncService.init(this.iframeJQueryElement, $element.find('.overlay'));

    CmsService.subscribe('delete-component', (componentId) => {
      this.deleteComponent(componentId);
    });
  }

  onLoad() {
    this.$rootScope.$apply(() => {

      this._parseHstComments();
      this._parseLinks();

      this.OverlaySyncService.startObserving();
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
    return this.selectMode ? this.PageStructureService.containers : [];
  }

}
