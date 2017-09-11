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

function illegalCharactersDirective() {
  'ngInject';

  return {
    restrict: 'A',
    require: 'ngModel',
    scope: {
      illegalCharacters: '@',
    },
    link: (scope, elem, attrs, ngModel) => {
      const validator = (value) => {
        let isValid = true;
        value = value || '';

        angular.forEach(scope.illegalCharacters, (character) => {
          if (value.indexOf(character) >= 0) {
            isValid = false;
          }
        });

        ngModel.$setValidity('illegalCharacters', isValid);

        return value;
      };

      ngModel.$parsers.unshift(validator);
      ngModel.$formatters.push(validator);
    },
  };
}

export default illegalCharactersDirective;
