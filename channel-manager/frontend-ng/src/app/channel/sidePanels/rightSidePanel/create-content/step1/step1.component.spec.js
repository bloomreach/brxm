/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

describe('Create content step 1 component', () => {
  let $componentController;
  let CreateContentService;
  let FeedbackService;

  let component;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    inject((
      _CreateContentService_,
      _FeedbackService_,
    ) => {
      CreateContentService = _CreateContentService_;
      FeedbackService = _FeedbackService_;
    });

    component = $componentController('createContentStep1');

    angular.noop(CreateContentService);
    angular.noop(FeedbackService);
  });

  fdescribe('DocumentType', () => {
    it('throws an error if options are not set', () => {
      expect(() => {
        component.options = null;
      }).toThrowError('Input "options" is required');
    });

    it('true', () => {
      expect(true).toBe(true);
    });
  });
});
