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

import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { Component, DebugElement, Injector, SimpleChange, ViewChild } from '@angular/core';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import { NameUrlFieldsComponent } from "./name-url-fields.component";
import { HintsComponent } from "../../../../../shared/components/hints/hints.component";
import { SharedModule } from "../../../../../shared/shared.module";
import { CreateContentService } from '../create-content.service';
import { CreateContentServiceMock } from '../create-content.mocks.spec';

@Component({
  template: '<hippo-name-url-fields #nameUrlFields [locale]="locale"></hippo-name-url-fields>'
})
class TestHostComponent {
  locale: string = 'en';
  @ViewChild(NameUrlFieldsComponent) nameUrlFields: NameUrlFieldsComponent;
}

fdescribe('NameUrlFields Component', () => {
  let hostComponent: TestHostComponent;
  let hostFixture: ComponentFixture<TestHostComponent>;
  let component: NameUrlFieldsComponent;
  let createContentService: CreateContentService;

  const spies: any = {};

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        HintsComponent,
        NameUrlFieldsComponent,
        TestHostComponent
      ],
      imports: [
        BrowserAnimationsModule,
        FormsModule,
        SharedModule
      ],
      providers: [
        { provide: CreateContentService, useClass: CreateContentServiceMock },
      ]
    });

    hostFixture = TestBed.createComponent(TestHostComponent);
    hostComponent = hostFixture.componentInstance;

    component = hostComponent.nameUrlFields;
    component.form.controls['name'] = { value: '' };
    createContentService = hostFixture.debugElement.injector.get(CreateContentService);

    spies.generateDocumentUrlByName = spyOn(createContentService, 'generateDocumentUrlByName').and.callThrough();
    spies.setDocumentUrlByName = spyOn(component, 'setDocumentUrlByName').and.callThrough();
    spies.setDocumentUrlEditable = spyOn(component, 'setDocumentUrlEditable').and.callThrough();

    component.ngOnInit();
    hostFixture.detectChanges();
  });

  function setNameInputValue(value: string) {
    const nameInput = component.nameInputElement.nativeElement;
    component.form.controls.name.value = value;
    nameInput.dispatchEvent(new Event('keyup'));
  }

  describe('ngOnInit', () => {
    it('calls setDocumentUrlByName 1 second after keyup was triggered on nameInputElement', fakeAsync(() => {
      setNameInputValue('test val');
      tick(1000);
      expect(component.setDocumentUrlByName).toHaveBeenCalled();
    }));
  });

  describe('setDocumentUrlByName', () => {
    it('sets the url field with lowercase and replace spaces with dashes', () => {
      setNameInputValue('test');
      component.setDocumentUrlByName();
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('test', 'en');
      expect(component.urlField).toEqual('test');

      spies.generateDocumentUrlByName.calls.reset();

      setNameInputValue('test val');
      component.setDocumentUrlByName();
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('test val', 'en');
      expect(component.urlField).toEqual('test-val');
    });

    it('sets the url with locale automatically after locale has been changed', fakeAsync(() => {
      setNameInputValue('some val');
      tick(1000);
      expect(component.setDocumentUrlByName).toHaveBeenCalled();
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('some val', 'en');

      hostComponent.locale = 'de';
      hostFixture.detectChanges();
      expect(component.setDocumentUrlByName).toHaveBeenCalled();
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('some val', 'de');
    }));

    it('changes the URL upon editing the name, if url.editMode.touched is true', fakeAsync(() => {
      // Name editing triggers generation of URL from the back-end
      setNameInputValue('First edit');
      tick(1000);
      expect(spies.generateDocumentUrlByName).toHaveBeenCalled();
      expect(component.urlField).toEqual('first-edit');

      // Manual editing of the URL
      component.setDocumentUrlEditable(true);
      component.urlField = 'manual-edit-of-url';
      component.editDone();
      expect(component.urlEditMode.touched).toEqual(true);

      spies.generateDocumentUrlByName.calls.reset();

      // From now on, URL generations should be bypassed
      setNameInputValue('Second edit, should not change the URL');
      tick(1000);
      expect(spies.generateDocumentUrlByName).not.toHaveBeenCalled();
      expect(component.urlField).toEqual('manual-edit-of-url');
    }));
  });

  describe('setDocumentUrlEditable', () => {
    it('sets the urlEditMode state', () => {
      component.setDocumentUrlEditable(true);
      expect(component.urlEditMode.state).toEqual(true);
    });

    it('sets the urlEditMode oldValue to the current urlField if passed true', () => {
      setNameInputValue('test val');
      component.setDocumentUrlByName();
      component.setDocumentUrlEditable(true);
      expect(component.urlEditMode.oldValue).toEqual('test-val');
    });

    it('does not set the urlEditMode oldValue to the current urlField if passed false', () => {
      setNameInputValue('test val');
      component.setDocumentUrlByName();
      component.setDocumentUrlEditable(false);
      expect(component.urlEditMode.oldValue).not.toEqual('test-val');
    });
  });

  describe('editDone', () => {
    it('saves the document URL, sets touched state to true', () => {
      expect(component.urlEditMode.touched).toEqual(false);
      component.setDocumentUrlEditable(true);
      setNameInputValue('Some input value');
      component.editDone();
      expect(component.urlEditMode.touched).toEqual(true);
      expect(component.setDocumentUrlEditable).toHaveBeenCalledWith(false);
    });
  });

  describe('regenerateUrl', () => {
    it('regenerates document URL by name and sets it to the url field', fakeAsync(() => {
      setNameInputValue('My document name');

      // As if the URL was manually edited previously
      component.urlField = 'manually-edited-url';
      component.urlEditMode.touched = true;

      component.regenerateUrl();
      tick(1000);

      expect(component.urlEditMode.touched).toEqual(false);
      expect(component.setDocumentUrlEditable).toHaveBeenCalledWith(false);
      expect(spies.setDocumentUrlByName).toHaveBeenCalled();
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('My document name', 'en');
      expect(component.urlField).toEqual('my-document-name');
    }));
  });
});
