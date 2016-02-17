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

describe('LinkProcessorService', function () {
  'use strict';

  var linkProcessorService;
  var $document;
  var previewUrl = 'http://localhost:8080/site';

  beforeEach(function () {
    module('hippo-cm.channel.hippoIframe');

    inject(function (_$document_, _linkProcessorService_) {
      $document = _$document_;
      linkProcessorService = _linkProcessorService_;
    });

  });

  beforeEach(function () {
    jasmine.getFixtures().load('channel/hippoIframe/linkProcessor.service.fixture.html');
  });

  function expectTargetAttrToBeBlank(selector) {
    $j(selector, $document).each(function () {
      expect(this).toHaveAttr('target', '_blank');
    });
  }

  function expectTargetAttrNotToBeBlank(selector) {
    $j(selector, $document).each(function () {
      expect(this).not.toHaveAttr('target', '_blank');
    });
  }

  describe('running with a valid internalLinkPrefix', function () {
    it('should set attribute target to _blank for external links', function () {
      linkProcessorService.run($document, previewUrl);
      expectTargetAttrToBeBlank('.qa-external-link');
      expectTargetAttrNotToBeBlank('.qa-internal-link, .qa-local-link');
    });
  });

  describe('running with an undefined internalLinkPrefix', function () {
    it('should set attribute target to _blank for both internal and external links', function () {
      linkProcessorService.run($document);
      expectTargetAttrToBeBlank('.qa-external-link, .qa-internal-link');
      expectTargetAttrNotToBeBlank('.qa-local-link');
    });
  });

  describe('running with a null internalLinkPrefix', function () {
    it('should set attribute target to _blank for both internal and external links', function () {
      linkProcessorService.run($document, null);
      expectTargetAttrToBeBlank('.qa-external-link, .qa-internal-link');
      expectTargetAttrNotToBeBlank('.qa-local-link');
    });
  });

  it('should show a confirm when clicking an external link', function () {
    var nrOfExternalLinks = $j('.qa-external-link', $document).length;
    var confirmSpy = spyOn(window, 'confirm').and.returnValue(true);

    linkProcessorService.run($document, previewUrl);
    $j('a', $document).click();
    expect(confirmSpy.calls.count()).toEqual(nrOfExternalLinks);
  });

  it('should prevent opening an external link if confirm is cancelled', function () {
    var spyEvent;
    spyOn(window, 'confirm').and.returnValue(false);

    linkProcessorService.run($document, previewUrl);
    $j('.qa-external-link', $document).each(function () {
      spyEvent = window.spyOnEvent(this, 'click');
      $j(this).click();
      expect('click').toHaveBeenPreventedOn(this);
      expect(spyEvent).toHaveBeenPrevented();
    });
  });

  it('should open an external link if confirm is ok', function () {
    var spyEvent;
    spyOn(window, 'confirm').and.returnValue(true);

    linkProcessorService.run($document, previewUrl);
    $j('.qa-external-link', $document).each(function () {
      spyEvent = window.spyOnEvent(this, 'click');
      $j(this).click();
      expect('click').not.toHaveBeenPreventedOn(this);
      expect(spyEvent).not.toHaveBeenPrevented();
    });
  });

});
