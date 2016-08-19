/*!
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  "use strict";

  angular.module('hippo.theme', [
    'ngAria',
    'ui.bootstrap',
    'ui.tree',
    'localytics.directives',
    'hippo-theme.templates'
  ]);
})();

angular.module('hippo-theme.templates', ['carousel/carousel-indicator.tpl.html', 'carousel/carousel.tpl.html', 'confirmation-dialog/confirmation-dialog.tpl.html', 'hippo-icon/hippo-icon.tpl.html', 'tree/tree-include.tpl.html', 'tree/tree.tpl.html']);

angular.module("carousel/carousel-indicator.tpl.html", []).run(["$templateCache", function($templateCache) {
  "use strict";
  $templateCache.put("carousel/carousel-indicator.tpl.html",
    "<img data-ng-src=\"{{imgSrc}}\">");
}]);

angular.module("carousel/carousel.tpl.html", []).run(["$templateCache", function($templateCache) {
  "use strict";
  $templateCache.put("carousel/carousel.tpl.html",
    "<div ng-mouseenter=\"pause()\" ng-mouseleave=\"play()\" class=\"carousel\" ng-swipe-right=\"prev()\" ng-swipe-left=\"next()\"><ol class=\"carousel-indicators\" ng-show=\"slides.length > 1\"><li hippo-carousel-indicator ng-repeat=\"slide in slides track by $index\" ng-class=\"{active: isActive(slide)}\" ng-click=\"select(slide)\"></li></ol><div class=\"carousel-inner\" ng-transclude></div><a class=\"left carousel-control\" ng-click=\"prev()\" ng-show=\"slides.length > 1\"><span class=\"glyphicon glyphicon-chevron-left\"></span></a> <a class=\"right carousel-control\" ng-click=\"next()\" ng-show=\"slides.length > 1\"><span class=\"glyphicon glyphicon-chevron-right\"></span></a></div>");
}]);

angular.module("confirmation-dialog/confirmation-dialog.tpl.html", []).run(["$templateCache", function($templateCache) {
  "use strict";
  $templateCache.put("confirmation-dialog/confirmation-dialog.tpl.html",
    "<div class=\"alert alert-warning confirmation-dialog\" data-ng-class=\"{'s-visible': show, 's-invisible': !show}\"><div ng-transclude></div><p class=\"text-right buttons\"><button class=\"btn btn-default\" data-ng-click=\"performConfirmation()\"><hippo-icon name=\"{{confirmIcon}}\" size=\"m\" data-ng-show=\"confirmIcon\"></hippo-icon>&nbsp;{{ confirmLabel }}</button> &nbsp;&nbsp; <button class=\"btn btn-default\" data-ng-click=\"performCancel()\">{{ cancelLabel }}</button></p></div>");
}]);

angular.module("hippo-icon/hippo-icon.tpl.html", []).run(["$templateCache", function($templateCache) {
  "use strict";
  $templateCache.put("hippo-icon/hippo-icon.tpl.html",
    "<svg ng-class=\"className\"><use xlink:href=\"{{xlink}}\"></svg>");
}]);

angular.module("tree/tree-include.tpl.html", []).run(["$templateCache", function($templateCache) {
  "use strict";
  $templateCache.put("tree/tree-include.tpl.html",
    "<div ui-tree-handle data-ng-click=\"selectItem()\"><div hippo-tree-template></div></div><ol ui-tree-nodes ng-model=\"item.items\" ng-if=\"!collapsed\"><li ng-repeat=\"item in item.items\" ui-tree-node data-hippo-node-template-url=\"tree/tree-include.tpl.html\" ng-if=\"displayTreeItem(item)\" data-ng-class=\"{active: selectedItem.id == item.id}\" scroll-to-if=\"selectedItem.id == item.id\" data-collapsed=\"item.collapsed\"></li></ol>");
}]);

angular.module("tree/tree.tpl.html", []).run(["$templateCache", function($templateCache) {
  "use strict";
  $templateCache.put("tree/tree.tpl.html",
    "<div ui-tree=\"options\" ng-class=\"{\n" +
    "       'drag-enabled': drag == true,\n" +
    "       'drag-disabled': drag == false,\n" +
    "     }\" data-drag-enabled=\"drag\"><ol ui-tree-nodes ng-model=\"treeItems\"><li ng-repeat=\"item in treeItems\" ui-tree-node data-hippo-node-template-url=\"tree/tree-include.tpl.html\" data-ng-class=\"{active: selectedItem.id == item.id}\" scroll-to-if=\"selectedItem.id == item.id\" data-collapsed=\"item.collapsed\"></li></ol></div>");
}]);

/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

(function () {
  "use strict";

  angular.module('hippo.theme')

  /**
   * @ngdoc directive
   * @name a
   * @restrict E
   *
   * @description
   * Blurs an anchor when it is clicked. This removes the default outline for clicked anchors in multiple browsers.
   */
    .directive('a', function () {
      return {
        restrict: 'E',
        link: function (scope, element) {
          element.on('click', function () {
            element.blur();
          });
        }
      };
    })

  /**
   * @ngdoc directive
   * @name button
   * @restrict E
   *
   * @description
   * Blurs a button when it is clicked. This removes the default outline for clicked buttons in multiple browsers.
   */
    .directive('button', function () {
      return {
        restrict: 'E',
        link: function (scope, element) {
          element.on('click', function () {
            element.blur();
          });
        }
      };
    });

}());

/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  'use strict';

  // Override ui.bootstrap carousel template
  angular.module('template/carousel/carousel.html', ['carousel/carousel.tpl.html']).run([
    '$templateCache',
    function ($templateCache) {
      var carouselTpl = $templateCache.get('carousel/carousel.tpl.html');
      $templateCache.put('template/carousel/carousel.html', carouselTpl);
    }
  ]);

  angular.module('hippo.theme').directive('hippoCarouselIndicator', [
      function () {
        return {
          restrict: 'A',
          templateUrl: 'carousel/carousel-indicator.tpl.html',
          link: function (scope, element, attr) {
            // Get corresponding slide object from the slide scope
            var slideObj = scope.slide.$element.scope().slides[scope.$index];
            scope.imgSrc = slideObj.image;
          }
        };
      }
    ]
  );
}());

/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  "use strict";

  angular.module('hippo.theme')

  /**
   * @ngdoc directive
   * @name hippoConfirmationDialog
   * @restrict A
   * @scope
   *
   * @param {string} confirmIcon The name of a FontAwesome icon to show, without the fa-* prefix
   * @param {string} confirmLabel The label to show for the confirmation button
   * @param {string} cancelLabel The label to show for the cancel link
   * @param {expression} performConfirmation Function to call when the confirm button is clicked.
   * @param {expression} performCancel Function to call when the cancel button is clicked.
   * @param {boolean} show Will set the .s-invisible or .s-visible class on the element.
   *
   * @description
   * Renders a confirmation dialog that can show any message and provides a confirm- and cancel button.
   * It has two states, `s-visible` and `s-invisible`, which are represented by CSS-classes.
   * The CSS property `top` will animate when the value is changed in CSS.
   *
   * *Note*: the confirmation dialog won't show or hide itself. You can do this easily by assigning the desired
   * CSS properties for the `.s-visible` and `.s-invisible` classes in your own CSS.
   */
    .directive('hippoConfirmationDialog', [
      function () {
        return {
          restrict: 'A',
          replace: true,
          transclude: true,
          templateUrl: 'confirmation-dialog/confirmation-dialog.tpl.html',
          scope: {
            confirmIcon: '@',
            confirmLabel: '@',
            cancelLabel: '@',
            performConfirmation: '&',
            performCancel: '&',
            show: '='
          }
        };
      }
    ]);
})();

/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  "use strict";

  angular.module('hippo.theme')

  /**
   * @ngdoc directive
   * @name hippoDivider
   * @restrict A
   *
   * @description
   * When passed true as value, it adds a DOM node as divider to the element.
   */
    .directive('hippoDivider', [
      function () {
        return {
          restrict: 'A',
          link: function (scope, elem, attrs) {
            var active = scope.$eval(attrs.hippoDivider);
            if (active) {
              elem.before('<li role="presentation" class="divider"></li>');
            }
          }
        };
      }
    ]);
})();

/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

(function () {
  "use strict";

  /**
   * @ngdoc directive
   * @name hippoFocusMe
   * @restrict A
   *
   * @description
   * Sets the focus on an element.
   * Credits to Mark Rajcok: http://stackoverflow.com/a/14837021/363448
   */
  angular.module('hippo.theme').directive('hippoFocusMe', [
    function () {
      return {
        restrict: 'A',
        scope: {
          trigger: '=hippoFocusMe'
        },
        link: function (scope, element) {
          scope.$watch('trigger', function (newValue, oldValue) {
            if (newValue && newValue !== oldValue) {
              element[0].focus();
            }
          });

          element.blur(function () {
            scope.trigger = false;
          });

          element.focus(function () {
            scope.trigger = true;
          });
        }
      };
    }
  ]);
})();

/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  "use strict";

  angular.module('hippo.theme')
    .filter('hippoGetByProperty', function () {
      return function (collection, propertyName, propertyValue, subCollection) {
        var itemWithProperty;

        function findPropertiesAndSubProperties (newCollection) {
          for (var i = 0; i < newCollection.length; i++) {
            if (newCollection[i][propertyName] === propertyValue) {
              itemWithProperty = newCollection[i];
            }
            if (subCollection && newCollection[i][subCollection]) {
              findPropertiesAndSubProperties(newCollection[i][subCollection]);
            }
          }
        }

        findPropertiesAndSubProperties(collection);

        return itemWithProperty;
      };
    });
}());

/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

(function () {
  'use strict';

  angular.module('hippo.theme').directive('hippoIcon', [

    function () {
      return {
        replace: true,
        restrict: 'E',
        scope: {
          name: '@',
          position: '@',
          size: '@'
        },
        templateUrl: 'hippo-icon/hippo-icon.tpl.html',
        link: function (scope) {
          var xlink,
            iconPosition = '',
            iconName = '',
            defaultSize = 'm';

          if (scope.position) {
            angular.forEach(scope.position.split(' '), function (position, i) {
              if (position === 'center') {
                if (i === 0) {
                  position = 'vcenter';
                } else {
                  position = 'hcenter';
                }
              }

              iconPosition += ' hi-' + position;
            });
          }

          scope.$watchGroup(['name', 'size'], function () {
            if (scope.size) {
              iconName = ' hi-' + scope.name + ' hi-' + scope.size;
              xlink = '#hi-' + scope.name + '-' + scope.size;
            } else {
              iconName += ' hi-' + scope.name + ' hi-' + defaultSize;
              xlink = '#hi-' + scope.name + '-' + defaultSize;
            }

            scope.className = 'hi' + iconPosition + iconName;
            scope.xlink = xlink;
          });
        }
      };
    }
  ]);
})();
/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

(function () {
  'use strict';

  angular.module('hippo.theme').directive('pre', [
    '$window',
    function ($window) {
      return {
        restrict: 'E',
        link: function (scope, element) {
          var ignoreExpression = /\s/,
            text = element.html(),
            superfluousSpaceCount = 0,
            currentChar = text.substring(0, 1),
            parts = text.split("\n"),
            reformattedText = "",
            length = parts.length;

          while (ignoreExpression.test(currentChar)) {
            currentChar = text.substring(++superfluousSpaceCount, superfluousSpaceCount + 1);
          }

          for (var i = 0; i < length; i++) {
            reformattedText += parts[i].substring(superfluousSpaceCount) + ( i == length - 1 ? "" : "\n" );
          }

          reformattedText = reformattedText.replace(/ /g, "&nbsp;");

          element.html($window.prettyPrintOne(reformattedText, undefined, true));
          element.addClass('pre-scrollable');
        }
      };
    }
  ]);
})();


/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  'use strict';

  /**
   * @ngdoc directive
   * @name scrollToIf
   *
   * @description
   * Directive to scroll to an item if an expression evaluates to true.
   */
  angular.module('hippo.theme').directive('scrollToIf', [
    '$timeout',
    function ($timeout) {
      var getParentOfScrollItem = function (element) {
        element = element.parentElement;
        while (element) {
          if (element.scrollHeight !== element.clientHeight) {
            return element;
          }
          if (element.parentElement) {
            element = element.parentElement;
          } else {
            return element;
          }
        }
        return null;
      };
      return function (scope, element, attrs) {
        scope.$watch(attrs.scrollToIf, function (value) {
          if (value) {
            $timeout(function () {
              var parent = getParentOfScrollItem(element[0]),
                topPadding = parseInt(window.getComputedStyle(parent, null).getPropertyValue('padding-top')) || 0,
                leftPadding = parseInt(window.getComputedStyle(parent, null).getPropertyValue('padding-left')) || 0,
                elemOffsetTop = element[0].offsetTop,
                elemOffsetLeft = element[0].offsetLeft,
                elemHeight = element[0].clientHeight,
                elemWidth = element[0].clientWidth;

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
            }, 0);
          }
        });
      };
    }
  ]);
}());

/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  "use strict";

  angular.module('hippo.theme')
  /**
   * @ngdoc directive
   * @name hippo.theme.directive:stopPropagation
   * @restrict A
   *
   * @description
   * Prevent event bubbling
   */
    .directive('stopPropagation', function () {
      return {
        restrict: 'A',
        link: function (scope, element) {
          element.bind('click', function (e) {
            e.stopPropagation();
          });
        }
      };
    });
}());

/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  'use strict';

  angular.module('hippo.theme')

  /**
   * @ngdoc directive
   * @name hippoTree
   * @restrict A
   *
   * @description
   * Tree component for the Hippo Theme based on [NestedSortable](https://github.com/JimLiu/Angular-NestedSortable).
   *
   * @param {Array} items The items to use for the Tree. Each item is an object with `title` (String) and `items`
   *   (Array) property.
   * @param {Object} selectedItem The item that should be marked as selected.
   * @param {callbacks} callbacks The available callbacks. A list of all available callbacks is available at
   * the [Hippo Theme demo](http://onehippo.github.io/hippo-theme-demo/) page.
   */
    .directive('hippoTree', function () {
      return {
        restrict: 'A',
        transclude: true,
        scope: {
          drag: '=',
          options: '=callbacks',
          selectedItem: '=',
          treeItems: '=items'
        },
        templateUrl: 'tree/tree.tpl.html',
        controller: 'hippo.theme.tree.TreeCtrl'
      };
    })

    .controller('hippo.theme.tree.TreeCtrl', [
      '$transclude',
      '$scope',
      '$filter',
      function ($transclude, $scope, $filter) {
        this.renderTreeTemplate = $transclude;

        function collectNodes (items, map) {
          angular.forEach(items, function (item) {
            map[item.id] = item;
            collectNodes(item.items, map);
          });
          return map;
        }

        function copyCollapsedState (srcNodes, targetNodes) {
          angular.forEach(srcNodes, function (srcNode) {
            var targetNode = targetNodes[srcNode.id];
            if (targetNode) {
              targetNode.collapsed = srcNode.collapsed;
            }
          });
        }

        $scope.toggleItem = function () {
          var item = this.$modelValue;
          this.toggle();
          item.collapsed = !item.collapsed;

          if (item.collapsed && $filter('hippoGetByProperty')(item.items, 'id', $scope.selectedItem.id, 'items')) {
            $scope.selectItem(item);
          }
          if (angular.isFunction($scope.options.toggleItem)) {
            $scope.options.toggleItem(item);
          }
        };

        $scope.selectItem = function (item) {
          if (!item) {
            item = this.$modelValue;
          }
          $scope.selectedItem = item;
          if (angular.isFunction($scope.options.selectItem)) {
            $scope.options.selectItem(item);
          }
        };

        $scope.displayTreeItem = function (item) {
          if (angular.isFunction($scope.options.displayTreeItem)) {
            return $scope.options.displayTreeItem(item);
          } else {
            return true;
          }
        };

        $scope.$watch('treeItems', function (newItems, oldItems) {
          var oldNodes = collectNodes(oldItems, {}),
            newNodes = collectNodes(newItems, {});
          copyCollapsedState(oldNodes, newNodes);
        });
      }
    ])

    .directive('hippoTreeTemplate', function () {
      return {
        require: '^hippoTree',
        link: function (scope, element, attrs, controller) {
          controller.renderTreeTemplate(scope, function (dom) {
            element.replaceWith(dom);
          });
        }
      };
    })

    .directive('hippoNodeTemplateUrl', [
      '$compile',
      '$http',
      '$templateCache',
      function ($compile, $http, $templateCache) {
        return {
          restrict: 'A',
          link: function (scope, element, attr) {
            var templateUrl = attr.hippoNodeTemplateUrl;
            var innerTemplate = $templateCache.get(templateUrl);

            if (typeof innerTemplate === 'undefined') {
              $http.get(templateUrl).then(function (response) {
                innerTemplate = response.data;
                $templateCache.put(templateUrl, innerTemplate);

                $compile(innerTemplate)(scope, function (clone) {
                  element.append(clone);
                });
              });
            } else {
              $compile(innerTemplate)(scope, function (clone) {
                element.append(clone);
              });
            }
          }
        };
      }
    ]);
})();

/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

(function () {
  "use strict";

  /**
   * @ngdoc directive
   * @name typeaheadFocus
   *
   * @description
   * Show the typeahead on focus
   * Credits to yohairosen: http://stackoverflow.com/questions/24764802/angular-js-automatically-focus-input-and-show-typeahead-dropdown-ui-bootstra#answer-27331340
   */
  angular.module('hippo.theme').directive('typeaheadFocus', function () {
    return {
      require: 'ngModel',
      link: function (scope, element, attr, ngModel) {
        element.bind('focus', function () {

          var viewValue = ngModel.$viewValue;

          //restore to null value so that the typeahead can detect a change
          if (ngModel.$viewValue == ' ') {
            ngModel.$setViewValue(null);
          }

          //force trigger the popup
          ngModel.$setViewValue(' ');

          //set the actual value in case there was already a value in the input
          ngModel.$setViewValue(viewValue || ' ');
        });

        //compare function that treats the empty space as a match
        scope.emptyOrMatch = function (actual, expected) {
          if (expected == ' ') {
            return true;
          }
          return actual.indexOf(expected) > -1;
        };
      }
    };
  });
})();

/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  "use strict";

  angular.module('hippo.theme')

  /**
   * @ngdoc service
   * @name hippo.theme.UrlParser
   *
   * @description
   * Whenever the current state changes, the URL parser service will parse the new URL and provide an array containing
   *   each part,
   *
   * The URL is divided by forward slashes, so /page/subpage/detail will result in an array containing 'page',
   *   'subpage' and 'detail'. Modified version of the [Angular App breadcrumb
   *   service](https://github.com/angular-app/angular-app/blob/master/client/src/common/services/breadcrumbs.js)
   */
    .service('hippo.theme.UrlParser', [
      '$rootScope',
      '$location',
      function ($rootScope, $location) {
        var urlParts = [];
        var urlParserService = {};

        //we want to update the parts only when a route is actually changed
        //as $location.path() will get updated immediately (even if route change fails!)
        $rootScope.$on('$stateChangeSuccess', function () {
          var pathElements = $location.path().split('/'),
            result = [],
            i;
          var partPath = function (index) {
            return '/' + (pathElements.slice(0, index + 1)).join('/');
          };

          pathElements.shift();
          for (i = 0; i < pathElements.length; i++) {
            result.push({
              name: pathElements[i],
              path: partPath(i)
            });
          }

          urlParts = result;
        });

        /**
         * @ngdoc method
         * @name getAll
         * @methodOf hippo.theme.UrlParser
         *
         * @description
         * Get all the URL parts
         *
         * @returns {Array} List of URL parts
         */
        urlParserService.getAll = function () {
          return urlParts;
        };

        /**
         * @ngdoc method
         * @name getFirst
         * @methodOf hippo.theme.UrlParser
         *
         * @description
         * Get the first part of the URL
         *
         * @returns {String} The first part of the URL
         */
        urlParserService.getFirst = function () {
          return urlParts[0] || {};
        };

        /**
         * @ngdoc method
         * @name getAllWithoutLast
         * @methodOf hippo.theme.UrlParser
         *
         * @description
         * Get all the URL parts without the last part. This can be useful when the last part is an id that you don't
         *   want to use.
         *
         * @returns {Array} List of URL parts, without the last part
         */
        urlParserService.getAllWithoutLast = function () {
          var tmp = urlParts.slice(0);
          tmp.pop();
          return tmp;
        };

        /**
         * @ngdoc method
         * @name getParent
         * @methodOf hippo.theme.UrlParser
         *
         * @description
         * Get the parent / previous state of the current view
         *
         * @returns {String} The state name for the parent / previous view
         */
        urlParserService.getParent = function () {
          return urlParts[urlParts.length - 2] || null;
        };

        return urlParserService;
      }
    ]);
}());

/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  "use strict";

  angular.module('hippo.theme')

  /**
   * @ngdoc service
   * @name hippoViewportSizes
   *
   * @description
   * Holds the different possible viewport sizes.
   * It is able to return the current viewport size and provides a method to set the current viewport size.
   */
    .service('hippoViewportSizes', [
      function () {
        var viewportSizes = {};

        var sizes = [
          {
            order: 0,
            name: 'xs',
            active: false
          },

          {
            order: 1,
            name: 'sm',
            active: false
          },

          {
            order: 2,
            name: 'md',
            active: false
          },

          {
            order: 3,
            name: 'lg',
            active: false
          }
        ];

        /**
         * @ngdoc method
         * @name getAll
         * @methodOf hippoViewportSizes
         *
         * @description
         * Returns all the possible viewport sizes
         *
         * @returns {Array} List of viewport sizes
         */
        viewportSizes.getAll = function () {
          return sizes;
        };

        /**
         * @ngdoc method
         * @name setCurrent
         * @methodOf hippoViewportSizes
         * @param {Object} viewport The viewport to set as active
         *
         * @description
         * Sets the current active viewport. It also updates the $rootScope `activeViewport` property with the active
         *   viewport;
         */
        viewportSizes.setCurrent = function (viewport) {
          angular.forEach(sizes, function (size) {
            size.active = (viewport.name == size.name);
          });
        };

        /**
         * @ngdoc method
         * @name getCurrent
         * @methodOf hippoViewportSizes
         *
         * @description
         * Fetches the current active viewport
         *
         * @returns {Object} The current active viewport
         */
        viewportSizes.getCurrent = function () {
          for (var i = 0, len = sizes.length; i < len; i++) {
            if (sizes[i].active === true) {
              return sizes[i];
            }
          }
        };

        return viewportSizes;
      }
    ])

  /**
   * @ngdoc directive
   * @name hippoViewportTest
   * @restrict A
   * @requires $window
   *
   * @description
   * Detects the current active viewport by creating an empty div-element and attaching Bootstrap 3 classes to it.
   * When the created element is hidden, the related viewport for the class given is set to active.
   *
   * When the window gets resized, the possible new viewport will automatically be detected and set as active.
   */
    .directive('hippoViewportTest', [
      '$window',
      'hippoViewportSizes',
      function ($window, ViewportSizes) {
        return {
          restrict: 'A',
          replace: true,
          template: '<div></div>',
          link: function (scope, elem) {
            // initial detection
            detectViewportSize();

            // window resize
            angular.element($window).bind('resize', function () {
              detectViewportSize();
            });

            // detect viewport size
            function detectViewportSize () {
              // optimized version of http://stackoverflow.com/a/15150381/363448
              var emptyDiv = angular.element('<div>');
              elem.append(emptyDiv);

              var sizes = ViewportSizes.getAll();

              for (var i = sizes.length - 1; i >= 0; i--) {
                var size = sizes[i];

                emptyDiv.addClass('hidden-' + size.name);
                if (emptyDiv.is(':hidden')) {
                  emptyDiv.remove();
                  ViewportSizes.setCurrent(size);
                  return;
                }
              }
            }
          }
        };
      }
    ]);
}());

//# sourceMappingURL=main.js.map