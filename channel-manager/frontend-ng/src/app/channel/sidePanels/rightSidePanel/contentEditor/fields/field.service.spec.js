/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
describe('field service', () => {
  let $timeout;
  let FieldService;
  let ContentService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$timeout_, _FieldService_, _ContentService_) => {
      $timeout = _$timeout_;
      FieldService = _FieldService_;
      ContentService = _ContentService_;
    });
  });

  it('should start draft timer for field', () => {
    spyOn(FieldService, '_clearFieldTimer');
    spyOn(FieldService, 'draftField');
    FieldService.setDocumentId('mockDocumentId');

    expect(FieldService.activeDraftTimers).toEqual({});

    FieldService.startDraftTimer('mockFieldName', 'mockValue');

    expect(FieldService._clearFieldTimer).toHaveBeenCalled();

    $timeout.flush();
    expect(FieldService.draftField).toHaveBeenCalled();
  });

  it('should draft field', () => {
    spyOn(ContentService, 'draftField');
    spyOn(FieldService, '_clearFieldTimer');
    spyOn(FieldService, '_cleanupTimers');
    FieldService.setDocumentId('mockDocumentId');

    FieldService.draftField('mockFieldName', 'mockValue');

    expect(FieldService._clearFieldTimer).toHaveBeenCalled();
    expect(FieldService._cleanupTimers).toHaveBeenCalled();
    expect(ContentService.draftField).toHaveBeenCalledWith('mockDocumentId', 'mockFieldName', 'mockValue');
  });

  it('should clear field timer if exists', () => {
    FieldService.activeDraftTimers.mockDocumentId = {};
    FieldService.activeDraftTimers.mockDocumentId.mockFieldName = () => { angular.noop(); };

    spyOn($timeout, 'cancel');

    FieldService._clearFieldTimer('mockDocumentId', 'mockFieldName');

    expect($timeout.cancel).toHaveBeenCalled();
    expect(FieldService.activeDraftTimers.mockDocumentId).toEqual({});
  });

  it('should clean up timers', () => {
    FieldService.activeDraftTimers.mockDocumentId = {};
    FieldService._cleanupTimers('mockDocumentId');
    expect(FieldService.activeDraftTimers.mockDocumentId).not.toBeDefined();
  });

  it('should set document id', () => {
    expect(FieldService.documentId).toEqual(null);
    FieldService.setDocumentId('mockDocumentId');
    expect(FieldService.documentId).toEqual('mockDocumentId');
  });

  it('should return document id', () => {
    expect(FieldService.getDocumentId()).toEqual(null);
    FieldService.setDocumentId('mockDocumentId');
    expect(FieldService.getDocumentId()).toEqual('mockDocumentId');
  });

  describe('input focus preserve feature', () => {
    it('shouldPreserveFocus should return true when at least one expression is true and false otherwise', () => {
      const relatedTarget = $('<button></button>');
      relatedTarget.addClass('btn-fullwidth'); // selector is now valid (.btn-fullwidth)
      expect(FieldService.shouldPreserveFocus(relatedTarget)).toEqual(true);
      relatedTarget.removeClass('btn-fullwidth'); // remove valid selector
      expect(FieldService.shouldPreserveFocus(relatedTarget)).toEqual(false);
    });

    it('should set focused input', () => {
      expect(FieldService._focusedInput).toEqual(null);
      expect(FieldService._customFocusCallback).toEqual(null);

      const mockElement = $('<button></button>');
      const mockCustomCallback = () => { angular.noop('mock'); };

      FieldService.setFocusedInput(mockElement, mockCustomCallback);
      expect(FieldService._focusedInput).toEqual(mockElement);
      expect(FieldService._customFocusCallback).toEqual(mockCustomCallback);

      FieldService.setFocusedInput(mockElement);
      expect(FieldService._focusedInput).toEqual(mockElement);
      expect(FieldService._customFocusCallback).toEqual(null);
    });

    it('should unset focused input', () => {
      FieldService._focusedInput = 'someFocusedInput';
      FieldService._customFocusCallback = 'someCustomCallback';

      FieldService.unsetFocusedInput();
      expect(FieldService._focusedInput).toEqual(null);
      expect(FieldService._customFocusCallback).toEqual(null);
    });

    describe('triggerInputFocus', () => {
      it('should not do anything if there is no focused input', () => {
        spyOn(FieldService, '_customFocusCallback');

        FieldService._focusedInput = null;
        FieldService.triggerInputFocus();
        expect(FieldService._customFocusCallback).not.toHaveBeenCalled();
      });

      it('should call the element native focus method if there is no custom focus callback', () => {
        const mockInputElement = $('<input type="text">');
        FieldService.setFocusedInput(mockInputElement);
        spyOn(FieldService._focusedInput, 'focus');

        jasmine.clock().install();
        FieldService.triggerInputFocus();
        jasmine.clock().tick(10);
        expect(FieldService._focusedInput.focus).toHaveBeenCalled();
        jasmine.clock().uninstall();
      });

      it('should call the custom focus callback if one was specified', () => {
        const mockInputElement = $('<input type="text">');
        const mockCustomCallback = () => { angular.noop('custom callback'); };
        FieldService.setFocusedInput(mockInputElement, mockCustomCallback);
        spyOn(FieldService, '_customFocusCallback');

        FieldService.triggerInputFocus();
        expect(FieldService._customFocusCallback).toHaveBeenCalled();
      });
    });
  });
});
