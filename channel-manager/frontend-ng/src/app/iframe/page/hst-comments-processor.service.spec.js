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

describe('HstCommentsProcessorService', () => {
  let HstCommentsProcessorService;

  beforeEach(() => {
    angular.mock.module('hippo-cm-iframe');

    inject((_HstCommentsProcessorService_) => {
      HstCommentsProcessorService = _HstCommentsProcessorService_;
    });

    jasmine.getFixtures().load('iframe/page/hst-comments-processor.service.fixture.html');
  });

  describe('run', () => {
    it('should process a document using XPath and DFS', () => {
      const data = Array.from(HstCommentsProcessorService.run(document));

      expect(data.map(({ json }) => json)).toEqual([
        { 'HST-Type': 'CONTAINER_COMPONENT', name: 'Container-1' },
        { 'HST-Type': 'CONTAINER_ITEM_COMPONENT', name: 'Container-1-Item-1' },
        { 'HST-End': 'true', uuid: 'CONTAINER_ITEM_COMPONENT-END' },
        { 'HST-End': 'true', uuid: 'CONTAINER_COMPONENT-END' },
        { 'HST-Type': 'PAGE-META-DATA', name: 'Page-1' },
        { 'HST-Type': 'HST_UNPROCESSED_HEAD_CONTRIBUTIONS', headElements: ['<title>test</title>'] },
      ]);
    });

    it('should throw an error when there is no document', () => {
      expect(() => {
        HstCommentsProcessorService.run(undefined);
      }).toThrow(new Error('DOM document is empty'));
    });
  });

  describe('processFragment', () => {
    it('should process a fragment using XPath and DFS', () => {
      const fixture = $j('#jasmine-fixtures');
      const data = Array.from(HstCommentsProcessorService.processFragment(fixture));

      expect(data.map(({ json }) => json)).toEqual([
        { 'HST-Type': 'CONTAINER_COMPONENT', name: 'Container-1' },
        { 'HST-Type': 'CONTAINER_ITEM_COMPONENT', name: 'Container-1-Item-1' },
        { 'HST-End': 'true', uuid: 'CONTAINER_ITEM_COMPONENT-END' },
        { 'HST-End': 'true', uuid: 'CONTAINER_COMPONENT-END' },
        { 'HST-Type': 'PAGE-META-DATA', name: 'Page-1' },
        { 'HST-Type': 'HST_UNPROCESSED_HEAD_CONTRIBUTIONS', headElements: ['<title>test</title>'] },
      ]);
    });

    it('should gracefully handle an undefined element when processing a fragment', () => {
      expect(HstCommentsProcessorService.processFragment).not.toThrow();
    });

    it('should not invoke callback when JSON data is invalid', () => {
      const fixture = $j('#qa-invalid-json')[0];
      const data = Array.from(HstCommentsProcessorService.processFragment(fixture));
      expect(data).toEqual([]);
    });
  });

  describe('locateComponent', () => {
    it('should locate containers', () => {
      const fixture = $j('#jasmine-fixtures');
      const data = Array.from(HstCommentsProcessorService.processFragment(fixture));

      const [{ element: startComment }] = data;
      const [
        boxElement,
        endComment,
      ] = HstCommentsProcessorService.locateComponent('CONTAINER_COMPONENT-END', startComment);

      expect(boxElement).toBe($j('#qa-container1')[0]);
      expect(endComment).toBe(data[3].element);
    });

    it('should locate components', () => {
      const fixture = $j('#jasmine-fixtures');
      const data = Array.from(HstCommentsProcessorService.processFragment(fixture));

      const [, { element: startComment }] = data;
      const [
        boxElement,
        endComment,
      ] = HstCommentsProcessorService.locateComponent('CONTAINER_ITEM_COMPONENT-END', startComment);

      expect(boxElement).toBe($j('#qa-item1')[0]);
      expect(endComment).toBe(data[2].element);
    });

    it('should throw an error if it can not find the END marker', () => {
      const fixture = $j('#jasmine-fixtures');
      const data = Array.from(HstCommentsProcessorService.processFragment(fixture));
      const startComment = data[4].element;

      expect(() => HstCommentsProcessorService.locateComponent('UNKNOWN_END_MARKER_ID', startComment))
        .toThrowError('No component end marker found for \'UNKNOWN_END_MARKER_ID\'.');
    });
  });
});
