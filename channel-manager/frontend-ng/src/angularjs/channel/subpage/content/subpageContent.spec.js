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

describe('SubpageContent', () => {
  'use strict';

  let $element;
  let $rootScope;
  let $compile;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
    });
  });

  function compileDirective() {
    $element = angular.element('<subpage-content><div id="transcluded"></div></subpage-content>');
    $compile($element)($rootScope.$new());
    $rootScope.$digest();
  }

  it('transcludes the inner HTML', () => {
    compileDirective();

    expect($element.find('#transcluded').length).toBe(1);
  });

  it('adds markup, specifying the feedback parent', () => {
    compileDirective();

    expect($element.find('.subpage-feedback-parent').length).toBe(1);
  });
});
