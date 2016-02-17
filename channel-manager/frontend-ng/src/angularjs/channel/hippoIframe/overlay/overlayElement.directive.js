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

/*
// ensure that containers and components are always visible
function ensureVisibilityOfIframeElement(domElement, scope) {
  const minHeight = domElement.style.minHeight || 'auto';
  domElement.style.minHeight = '40px';

  // reset styling when element is destroyed
  scope.$on('$destroy', () => {
    domElement.style.minHeight = minHeight;
  });
}
*/

export function overlayElementDirective(OverlaySyncService) {
  'ngInject';

  return {
    restrict: 'E',
    scope: {
      structureElement: '=',
    },
    link: (scope, element) => {
//      ensureVisibilityOfIframeElement(scope.structureElement.getJQueryElement('iframe')[0], scope);

      scope.structureElement.setJQueryElement('overlay', element);
      OverlaySyncService.registerElement(scope.structureElement);
    },
  };
}
