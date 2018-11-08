/*
* Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('EditContentIconCtrl', () => {
  let ContentEditor;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($controller) => {
      ContentEditor = jasmine.createSpyObj('ContentEditor', ['getPublicationState']);

      $ctrl = $controller('editContentIconCtrl', {
        ContentEditor,
      });
    });
  });

  describe('the publication icon name', () => {
    it('is based on the publication state of the document', () => {
      ContentEditor.getPublicationState.and.returnValue('new');
      expect($ctrl.getPublicationIconName()).toBe('mdi-minus-circle');

      ContentEditor.getPublicationState.and.returnValue('live');
      expect($ctrl.getPublicationIconName()).toBe('mdi-check-circle');

      ContentEditor.getPublicationState.and.returnValue('changed');
      expect($ctrl.getPublicationIconName()).toBe('mdi-alert');

      ContentEditor.getPublicationState.and.returnValue('unknown');
      expect($ctrl.getPublicationIconName()).toBe('');
    });
    it('is empty when there is no document', () => {
      ContentEditor.getPublicationState.and.returnValue(undefined);
      expect($ctrl.getPublicationIconName()).toBe('');
    });
  });

  describe('the publication icon tooltip', () => {
    it('is based on the publication state of the document', () => {
      ContentEditor.getPublicationState.and.returnValue('new');
      expect($ctrl.getPublicationIconTooltip()).toBe('DOCUMENT_NEW_TOOLTIP');

      ContentEditor.getPublicationState.and.returnValue('live');
      expect($ctrl.getPublicationIconTooltip()).toBe('DOCUMENT_LIVE_TOOLTIP');

      ContentEditor.getPublicationState.and.returnValue('changed');
      expect($ctrl.getPublicationIconTooltip()).toBe('DOCUMENT_CHANGED_TOOLTIP');

      ContentEditor.getPublicationState.and.returnValue('unknown');
      expect($ctrl.getPublicationIconTooltip()).toBe('');
    });
    it('is empty when there is no document', () => {
      ContentEditor.getPublicationState.and.returnValue(undefined);
      expect($ctrl.getPublicationIconTooltip()).toBe('');
    });
  });
});
