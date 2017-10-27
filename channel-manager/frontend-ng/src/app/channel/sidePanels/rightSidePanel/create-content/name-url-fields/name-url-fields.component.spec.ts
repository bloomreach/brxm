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
import { DebugElement } from '@angular/core';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import { NameUrlFieldsComponent } from "./name-url-fields.component";
import { HintsComponent } from "../../../../../shared/components/hints/hints.component";
import { SharedModule } from "../../../../../shared/shared.module";

describe('NameUrlFields Component', () => {

  let component: NameUrlFieldsComponent;
  let fixture: ComponentFixture<NameUrlFieldsComponent>;
  let de: DebugElement;
  let el: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        HintsComponent,
        NameUrlFieldsComponent
      ],
      imports: [
        BrowserAnimationsModule,
        FormsModule,
        SharedModule
      ]
    });

    fixture = TestBed.createComponent(NameUrlFieldsComponent);
    component = fixture.componentInstance;

    component.ngOnInit();
    fixture.detectChanges();
  });

  describe('ngOnInit', () => {
    it('calls setDocumentUrlByName 1 second after keyup was triggered on nameInputElement', fakeAsync(() => {
      spyOn(component, 'setDocumentUrlByName').and.callThrough();

      const nameInput = component.nameInputElement.nativeElement;
      component.form.controls['name'] = { value: 'test val' };
      nameInput.dispatchEvent(new Event('keyup'));
      tick(1000);
      expect(component.setDocumentUrlByName).toHaveBeenCalledWith('test val');
    }));
  });

  describe('setDocumentUrlByName', () => {
    it('should set the url field with lowercase and replace spaces with dashes', fakeAsync(() => {
      component.setDocumentUrlByName('test');
      expect(component.urlField).toEqual('test');

      component.setDocumentUrlByName('test val');
      expect(component.urlField).toEqual('test-val');
    }));
  });

  describe('setDocumentUrlEditable', () => {
    it('should set the urlEditMode state', () => {
      component.setDocumentUrlEditable(true);
      expect(component.urlEditMode.state).toEqual(true);
    });

    it('should set the urlEditMode oldValue to the current urlField if the passed TRUE', () => {
      component.setDocumentUrlByName('test val');
      component.setDocumentUrlEditable(true);
      expect(component.urlEditMode.oldValue).toEqual('test-val');
    });

    it('should NOT set the urlEditMode oldValue to the current urlField if the passed FALSE', () => {
      component.setDocumentUrlByName('test val');
      component.setDocumentUrlEditable(false);
      expect(component.urlEditMode.oldValue).not.toEqual('test-val');
    });
  });

  describe('cancelUrlEditing', () => {
    it('should set the urlField to the old value (in urlEditMode.oldValue) and call setDocumentUrlEditable', () => {
      spyOn(component, 'setDocumentUrlEditable');
      component.urlEditMode.oldValue = 'test-val';
      component.cancelUrlEditing();
      expect(component.urlField).toEqual('test-val');
      expect(component.setDocumentUrlEditable).toHaveBeenCalledWith(false);
    });
  });
});
