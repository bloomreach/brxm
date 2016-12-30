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

import angular from 'angular';
import 'angular-mocks';

describe('LinkProcessorService', () => {
  let linkProcessorService;
  let $document;
  const previewUrl = ['http://localhost:8080/site'];

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    inject((_$document_, _linkProcessorService_) => {
      $document = _$document_;
      linkProcessorService = _linkProcessorService_;
    });
  });

  beforeEach(() => {
    jasmine.getFixtures().load('channel/hippoIframe/processing/linkProcessor.service.fixture.html');
  });

  function expectTargetAttrToBeBlank(selector) {
    $j(selector, $document).each(function checkTarget() {
      expect(this).toHaveAttr('target', '_blank');
    });
  }

  function expectTargetAttrNotToBeBlank(selector) {
    $j(selector, $document).each(function checkTarget() {
      expect(this).not.toHaveAttr('target', '_blank');
    });
  }

  describe('running with a valid internalLinkPrefix', () => {
    it('should set attribute target to _blank for external links', () => {
      linkProcessorService.run($document, previewUrl);
      expectTargetAttrToBeBlank('.qa-external-link');
      expectTargetAttrNotToBeBlank('.qa-internal-link, .qa-local-link');
    });
  });

  describe('running with two valid internalLinkPrefixes', () => {
    it('should set attribute target to _blank for external links', () => {
      linkProcessorService.run($document, ['http://localhost:8080/intranet', previewUrl[0]]);
      expectTargetAttrToBeBlank('.qa-external-link');
      expectTargetAttrNotToBeBlank('.qa-internal-link, .qa-local-link');
    });
  });

  describe('running with an undefined internalLinkPrefix', () => {
    it('should set attribute target to _blank for both internal and external links', () => {
      linkProcessorService.run($document);
      expectTargetAttrToBeBlank('.qa-external-link, .qa-internal-link');
      expectTargetAttrNotToBeBlank('.qa-local-link');
    });
  });

  describe('running with a null internalLinkPrefix', () => {
    it('should set attribute target to _blank for both internal and external links', () => {
      linkProcessorService.run($document, null);
      expectTargetAttrToBeBlank('.qa-external-link, .qa-internal-link');
      expectTargetAttrNotToBeBlank('.qa-local-link');
    });
  });

  it('should show a confirm when clicking an external link', () => {
    const nrOfExternalLinks = $j('.qa-external-link', $document).length;
    const confirmSpy = spyOn(window, 'confirm').and.returnValue(true);

    linkProcessorService.run($document, previewUrl);
    $j('a', $document).click();
    expect(confirmSpy.calls.count()).toEqual(nrOfExternalLinks);
  });

  it('should prevent opening an external link if confirm is cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    linkProcessorService.run($document, previewUrl);
    $j('.qa-external-link', $document).each(function checkClick() {
      const spyEvent = window.spyOnEvent(this, 'click');
      $j(this).click();
      expect('click').toHaveBeenPreventedOn(this);
      expect(spyEvent).toHaveBeenPrevented();
    });
  });

  it('should open an external link if confirm is ok', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    linkProcessorService.run($document, previewUrl);
    $j('.qa-external-link', $document).each(function checkClick() {
      const spyEvent = window.spyOnEvent(this, 'click');
      $j(this).click();
      expect('click').not.toHaveBeenPreventedOn(this);
      expect(spyEvent).not.toHaveBeenPrevented();
    });
  });
});
