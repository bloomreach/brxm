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

export class ComponentAdderCtrl {
  constructor($scope, $element, ComponentAdderService, PageStructureService, CatalogService) {
    'ngInject';

    const drake = window.dragula({
      ignoreInputTextSelection: false,
      isContainer(el) {
        return el.classList.contains('overlay-element-container') || ComponentAdderService.isContainer(el);
      },
      copy: true,
      moves(el) {
        return ComponentAdderService.isContainerItem(el);
      },
      accepts(el, target) {
        return target.classList.contains('overlay-element-container');
      },
    });
    drake.on('cloned', (clone, original) => {
      $scope.$apply(() => {
        this.newComponent = CatalogService.getComponentByDomElement(original); // remember the to-be-added component
        $element.addClass('add-mode');
      });
    });
    drake.on('dragend', () => {
      $scope.$apply(() => {
        $element.removeClass('add-mode');
        this.isAddingComponent = false;
      });
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

          PageStructureService.addComponentToContainer(this.newComponent, target);
        });
      }
    });

    $scope.$on('$destroy', () => drake.destroy());
  }
}
