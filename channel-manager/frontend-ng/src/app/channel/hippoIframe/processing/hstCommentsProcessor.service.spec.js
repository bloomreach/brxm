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

describe('HstCommentsProcessorService', () => {
  let hstCommentsProcessorService;
  function NOOP() { }

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    inject((_hstCommentsProcessorService_) => {
      hstCommentsProcessorService = _hstCommentsProcessorService_;
    });

    jasmine.getFixtures().load('channel/hippoIframe/processing/hstCommentsProcessor.service.fixture.html');
  });

  // PhantomJS does not support XPath querying through document.evaluate (https://github.com/ariya/phantomjs/issues/10161)
  it('should use DOM-walking when XPath querying is not available', () => {
    spyOn(hstCommentsProcessorService, 'processCommentsWithDomWalking');
    hstCommentsProcessorService.run($j('#jasmine-fixtures')[0], NOOP);
    expect(hstCommentsProcessorService.processCommentsWithDomWalking).toHaveBeenCalled();
  });

  it('should process comments with DOM-walking', () => {
    const fixture = $j('#jasmine-fixtures')[0];
    const gatheredData = [];
    hstCommentsProcessorService.processCommentsWithDomWalking(fixture, (element, json) => {
      gatheredData.push(json);
    });

    expect(gatheredData).toEqual([
      { 'HST-Type': 'CONTAINER_COMPONENT', name: 'Container-1' },
      { 'HST-Type': 'CONTAINER_ITEM_COMPONENT', name: 'Container-1-Item-1' },
      { 'HST-Type': 'PAGE-META-DATA', name: 'Page-1' },
      { 'HST-Type': 'HST_UNPROCESSED_HEAD_CONTRIBUTIONS', headElements: ['<title>test</title>'] },
    ]);
  });

  it('should gracefully handle an undefined element when DOM-walking', () => {
    expect(hstCommentsProcessorService.processCommentsWithDomWalking).not.toThrow();
  });

  it('should not invoke callback when JSON data is invalid', () => {
    const fixture = $j('#qa-invalid-json')[0];
    const observer = { callback: NOOP };
    spyOn(observer, 'callback');
    hstCommentsProcessorService.processCommentsWithDomWalking(fixture, observer.callback);

    expect(observer.callback).not.toHaveBeenCalled();
  });

  it('should not do anything where there is no document', () => {
    expect(() => {
      hstCommentsProcessorService.run(undefined, () => {
        fail('callback should not have been called');
      });
    }).toThrow(new Error('DOM document is empty'));
  });
});
