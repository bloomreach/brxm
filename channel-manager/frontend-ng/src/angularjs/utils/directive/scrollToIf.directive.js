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

const findAncestor = (elem, className) => {
  while (elem && !elem.classList.contains(className)) {
    elem = elem.parentElement;
  }
  return elem;
};

const getParentOfScrollItem = (elem) => {
  const scrollToContainer = findAncestor(elem, 'scroll-to-container');

  if (scrollToContainer) {
    return scrollToContainer;
  }
  elem = elem.parentElement;
  while (elem) {
    if (elem.scrollHeight !== elem.clientHeight) {
      return elem;
    }
    if (elem.parentElement) {
      elem = elem.parentElement;
    } else {
      return elem;
    }
  }
  return null;
};


export function scrollToIfDirective($timeout) {
  'ngInject';

  return {
    restrict: 'A',
    link: (scope, elem, attrs) => {
      scope.$watch(attrs.scrollToIf, (value) => {
        if (value) {
          $timeout(() => {
            const parent = getParentOfScrollItem(elem[0]);
            if (parent) {
              const topPadding = parseInt(window.getComputedStyle(parent, null).getPropertyValue('padding-top'), 10) || 0;
              const leftPadding = parseInt(window.getComputedStyle(parent, null).getPropertyValue('padding-left'), 10) || 0;
              const elemOffsetTop = elem[0].offsetTop;
              const elemOffsetLeft = elem[0].offsetLeft;
              let elemHeight = elem[0].clientHeight;
              const elemWidth = elem[0].clientWidth;

              if (elemOffsetTop < parent.scrollTop) {
                parent.scrollTop = elemOffsetTop + topPadding;
              } else if (elemOffsetTop + elemHeight > parent.scrollTop + parent.clientHeight) {
                if (elemHeight > parent.clientHeight) {
                  elemHeight = elemHeight - (elemHeight - parent.clientHeight);
                }
                parent.scrollTop = elemOffsetTop + topPadding + elemHeight - parent.clientHeight;
              }
              if (elemOffsetLeft + elemWidth > parent.scrollLeft + parent.clientWidth) {
                parent.scrollLeft = elemOffsetLeft + leftPadding;
              }
            }
          }, 0);
        }
      });
    },
  };
}
