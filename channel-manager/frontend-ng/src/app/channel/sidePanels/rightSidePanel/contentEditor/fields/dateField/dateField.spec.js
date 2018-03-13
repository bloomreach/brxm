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

import {DateValue} from "./dateField.controller";

describe('DateField', () => {
  let $componentController;
  let $ctrl;
  let FieldService;
  let onFieldFocus;
  let onFieldBlur;
  let form;

  const fieldType = {id: 'field:type'};
  const fieldValues = [
    {value: '2015-08-24T06:53:00.000Z'},
    {value: '2018-03-12T12:11:50.041+01:00'},
    {value: ''},
  ];

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$rootScope_, _FieldService_) => {
      $componentController = _$componentController_;
      FieldService = _FieldService_;
    });

    onFieldFocus = jasmine.createSpy('onFieldFocus');
    onFieldBlur = jasmine.createSpy('onFieldBlur');
    form = jasmine.createSpyObj('form', ['$setDirty']);

    $ctrl = $componentController('dateField', {}, {
      fieldType,
      fieldValues,
      name: 'test-name',
      onFieldFocus,
      onFieldBlur,
      form,
    });

    $ctrl.$onInit();
  });

  it('initializes the component', () => {
    expect($ctrl.fieldType).toBe(fieldType);
    expect($ctrl.fieldValues).toBe(fieldValues);
    expect($ctrl.name).toBe('test-name');
    expect($ctrl.onFieldFocus).toBe(onFieldFocus);
    expect($ctrl.onFieldBlur).toBe(onFieldBlur);
    expect($ctrl.dateValues.length).toBe($ctrl.fieldValues.length);
  });

  it('helps composing unique form field names', () => {
    expect($ctrl.getFieldName(0)).toBe('test-name/field:type');
    expect($ctrl.getFieldName(1)).toBe('test-name/field:type[2]');
    expect($ctrl.getFieldName(2)).toBe('test-name/field:type[3]');

    const stubbed = $componentController('primitiveField', {}, {
      fieldType,
      fieldValues,
    });
    expect(stubbed.getFieldName(0)).toBe('field:type');
    expect(stubbed.getFieldName(1)).toBe('field:type[2]');
    expect(stubbed.getFieldName(2)).toBe('field:type[3]');
  });

  it('handles $event object if supplied upon focus', () => {
    const mockTargetElement = $('<input type="text">');
    const $event = {
      target: mockTargetElement,
      customFocus: null,
    };
    spyOn(FieldService, 'unsetFocusedInput');
    spyOn(FieldService, 'setFocusedInput');

    $ctrl.focusDateField($event);
    expect(FieldService.unsetFocusedInput).toHaveBeenCalledWith();
    expect(FieldService.setFocusedInput).toHaveBeenCalledWith($event.target, $event.customFocus);
  });

  it('handles $event object if supplied upon blur', () => {
    const $event = {
      relatedTarget: angular.element('<input type="text">'),
    };
    spyOn(FieldService, 'shouldPreserveFocus').and.returnValue(true);
    spyOn(FieldService, 'triggerInputFocus');

    $ctrl.blurDateField($event);
    expect(FieldService.shouldPreserveFocus).toHaveBeenCalledWith($event.target);
    expect(FieldService.triggerInputFocus).toHaveBeenCalled();

    // if should not preserve focus
    FieldService.shouldPreserveFocus.and.returnValue(false);
    $ctrl.blurDateField($event);
  });

  it('keeps track of the focused state', () => {
    expect($ctrl.hasFocus).toBeFalsy();

    $ctrl.focusDateField();

    expect($ctrl.hasFocus).toBeTruthy();
    expect(onFieldFocus).toHaveBeenCalled();
    expect(onFieldBlur).not.toHaveBeenCalled();
    onFieldFocus.calls.reset();

    $ctrl.blurDateField();

    expect($ctrl.hasFocus).toBeFalsy();
    expect(onFieldFocus).not.toHaveBeenCalled();
    expect(onFieldBlur).toHaveBeenCalled();
  });

  it('starts a draft timer when the value changed', () => {
    spyOn(FieldService, 'startDraftTimer');

    $ctrl.valueChanged();

    expect(FieldService.startDraftTimer).toHaveBeenCalledWith('test-name/field:type', fieldValues);
  });

  it('drafts the field on blur when the value has changed', () => {
    spyOn(FieldService, 'draftField');

    $ctrl.focusDateField();
    fieldValues[1].value = '2018-01-01T12:11:50.041+01:00';
    $ctrl.blurDateField();

    expect(FieldService.draftField).toHaveBeenCalledWith('test-name/field:type', fieldValues);
  });

  it('does not draft the field on blur when the value has not changed', () => {
    spyOn(FieldService, 'draftField');

    $ctrl.focusDateField();
    $ctrl.blurDateField();

    expect(FieldService.draftField).not.toHaveBeenCalled();
  });

  it('sets the value of a date to the current date and time when set to now is called', () => {
    spyOn($ctrl, 'valueChanged');
    const oldValue = $ctrl.dateValues[0].date;

    $ctrl.setToNow(0);

    expect($ctrl.valueChanged).toHaveBeenCalled();
    const newValue = $ctrl.dateValues[0].date;
    expect(oldValue).toBeLessThan(newValue);
    expect(form.$setDirty).toHaveBeenCalled();
  });

});

describe('DateValue', () => {
  let dateValue;

  describe('when populated', () => {
    beforeEach(() => {
      dateValue = new DateValue('2018-03-12T12:01:50.041+01:00');
    });

    it('contains data', () => {
      expect(dateValue.date).not.toBe(null);
      expect(dateValue.hours).not.toBe(null);
      expect(dateValue.minutes).not.toBe(null);
      expect(dateValue.toDateString()).not.toBeEmpty();
    });

    it('with focus on minutes field gives minutes not zero padded', () => {
      dateValue.focusMinutes();
      expect(dateValue.minutes).toBe(1);
      dateValue.blurMinutes();
      expect(dateValue.minutes).toBe('01');
    });

    it('without focus on minutes field gives minutes zero padded', () => {
      expect(dateValue.minutes).toBe('01');
    });

    it('when a new date is set, existing hours and minutes are maintained', () => {
      const newDate = new Date(2017, 2, 2, 0, 0, 0, 0); // date with hours and minutes = 0
      dateValue.date = newDate;
      expect(dateValue.hours).toBe(12);
      expect(dateValue.minutes).toBe('01');
    });
  });

  describe('when empty', () => {
    beforeEach(() => {
      dateValue = new DateValue('');
    });

    it('does not contain data', () => {
      expect(dateValue.date).toBe(null);
      expect(dateValue.hours).toBe(null);
      expect(dateValue.minutes).toBe(null);
      expect(dateValue.toDateString()).toBe('');
    });

    it('gets populated when set data is called', () => {
      dateValue.date = new Date();
      expect(dateValue.date).not.toBe(null);
    });

    it('gets populated when set hours is called', () => {
      dateValue.hours = 1;
      expect(dateValue.date).not.toBe(null);
    });

    it('sets hours to 23 if set with a higher number', () => {
      dateValue.hours = 24;
      expect(dateValue.hours).toBe(23);
    });

    it('gets populated when set minutes is called', () => {
      dateValue.minutes = 1;
      expect(dateValue.date).not.toBe(null);
    });
  });

});