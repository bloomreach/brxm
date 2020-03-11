/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('LinkProcessorService', () => {
  let LinkProcessorService;
  let $document;
  let $rootScope;
  let $window;

  beforeEach(() => {
    angular.mock.module('hippo-cm-iframe');

    inject((_$document_, _$rootScope_, _$window_, _LinkProcessorService_) => {
      $document = _$document_;
      $rootScope = _$rootScope_;
      $window = _$window_;
      LinkProcessorService = _LinkProcessorService_;
    });
  });

  beforeEach(() => {
    jasmine.getFixtures().load('iframe/overlay/link-processor.service.fixture.html');
    LinkProcessorService.initialize();
  });

  it('does not set the attribute "target" for internal links', () => {
    $rootScope.$emit('page:change');

    $j('.qa-internal-link', $document).each(function checkTarget() {
      expect(this).not.toHaveAttr('target', '_blank');
    });
  });

  it('does not set the attribute "target" for local links', () => {
    $rootScope.$emit('page:change');

    $j('.qa-local-link', $document).each(function checkTarget() {
      expect(this).not.toHaveAttr('target', '_blank');
    });
  });

  it('sets attribute "target" to _blank for external links', () => {
    $rootScope.$emit('page:change');

    $j('.qa-external-link', $document).each(function checkTarget() {
      expect(this).toHaveAttr('target', '_blank');
    });
  });

  it('should show a confirm when clicking an external link', () => {
    const nrOfExternalLinks = $j('.qa-external-link', $document).length;
    const confirmSpy = spyOn($window, 'confirm').and.returnValue(true);

    $rootScope.$emit('page:change');
    $j('a', $document).click();
    expect(confirmSpy.calls.count()).toEqual(nrOfExternalLinks);
  });

  it('should prevent opening an external link if confirm is cancelled', () => {
    spyOn($window, 'confirm').and.returnValue(false);

    $rootScope.$emit('page:change');
    $j('.qa-external-link', $document).each(function checkClick() {
      const spyEvent = window.spyOnEvent(this, 'click');
      $j(this).click();
      expect('click').toHaveBeenPreventedOn(this);
      expect(spyEvent).toHaveBeenPrevented();
    });
  });

  it('should open an external link if confirm is ok', () => {
    spyOn($window, 'confirm').and.returnValue(true);

    $rootScope.$emit('page:change');
    $j('.qa-external-link', $document).each(function checkClick() {
      const spyEvent = window.spyOnEvent(this, 'click');
      $j(this).click();
      expect('click').not.toHaveBeenPreventedOn(this);
      expect(spyEvent).not.toHaveBeenPrevented();
    });
  });
});
