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

import autoScrollerFactory from 'dom-autoscroller';

export class ComponentAdderCtrl {
  constructor($scope, $log, $element, ComponentAdderService, PageStructureService, CatalogService, DragDropService,
              HippoIframeService) {
    'ngInject';

    this.PageStructureService = PageStructureService;

    const drake = window.dragula({
      ignoreInputTextSelection: false,
      isContainer: (el) => this._isEnabledOverlayContainer(el) || ComponentAdderService.isCatalogContainer(el),
      copy: true,
      moves: (el) => ComponentAdderService.isCatalogContainerItem(el),
      accepts: (el, target) => this._isEnabledOverlayContainer(target),
    });

    drake.on('cloned', (clone, original) => {
      $scope.$apply(() => {
        this.selectedCatalogItem = CatalogService.getComponentByDomElement(original);
        $element.addClass('add-mode');

        // prevent IE11 from dragging the image
        this.selectedCatalogItem.catalogJQueryElement.find('img').on('dragstart', (event) => event.preventDefault());
      });
    });

    drake.on('dragend', () => {
      $scope.$apply(() => {
        $element.removeClass('add-mode');
        this.isAddingComponent = false;
      });

      this.selectedCatalogItem.catalogJQueryElement.find('img').off('dragstart');
    });

    drake.on('over', (el, container) => {
      $scope.$apply(() => {
        $(container).addClass('has-shadow'); // CSS :hover didn't work, use the over and out events instead.
      });
    });

    drake.on('out', (el, container) => {
      $scope.$apply(() => {
        $(container).removeClass('has-shadow');
      });
    });

    drake.on('shadow', (el) => {
      $scope.$apply(() => {
        $(el).addClass('gu-hide'); // never show the shadow when adding a component
      });
    });

    drake.on('drop', (el, target) => {
      if (target !== null) {
        $scope.$apply(() => {
          $(target).removeClass('has-shadow');
          $(el).detach(); // delete the (hidden) dropped DOM element.

          const container = PageStructureService.getContainerByOverlayElement(target);
          if (container) {
            PageStructureService.addComponentToContainer(this.selectedCatalogItem, container)
              .then((newComponent) => {
                if (PageStructureService.containsNewHeadContributions(newComponent.getContainer())) {
                  $log.info(`New '${newComponent.getLabel()}' component needs additional head contributions, reloading page`);
                  HippoIframeService.reload().then(() => {
                    PageStructureService.showComponentProperties(newComponent);
                  });
                } else {
                  DragDropService.replaceContainer(container, newComponent.getContainer());
                  PageStructureService.showComponentProperties(newComponent);
                }
              });
          } else {
            $log.debug(`Cannot add catalog item ${this.selectedCatalogItem.id} because container cannot be found for the overlay element or has been locked by a different user`, target);
          }
        });
      }
    });

    const autoScroll = autoScrollerFactory($('.channel-iframe-base'), {
      margin: 20,
      pixels: 15,
      scrollWhenOutside: true,
      autoScroll: function autoScroll() {
        return this.down && drake.dragging;
      },
    });

    $scope.$on('$destroy', () => {
      drake.destroy();
      autoScroll.destroy();
    });
  }

  _isEnabledOverlayContainer(el) {
    const container = this.PageStructureService.getContainerByOverlayElement(el);
    return container && !container.isDisabled();
  }
}
