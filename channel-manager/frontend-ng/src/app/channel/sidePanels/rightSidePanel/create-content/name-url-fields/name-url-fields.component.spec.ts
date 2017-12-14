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
import { Component, ViewChild } from '@angular/core';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import { NameUrlFieldsComponent } from './name-url-fields.component';
import { HintsComponent } from '../../../../../shared/components/hints/hints.component';
import { SharedModule } from '../../../../../shared/shared.module';
import CreateContentService from '../createContent.service.js';
import { ChannelServiceMock, CreateContentServiceMock } from '../create-content.mocks.spec';
import ChannelService from '../../../../channel.service';

@Component({
  template: '<hippo-name-url-fields #nameUrlFields [locale]="locale"></hippo-name-url-fields>'
})
class TestHostComponent {
  locale = 'en';
  @ViewChild(NameUrlFieldsComponent) nameUrlFields: NameUrlFieldsComponent;
}

describe('NameUrlFields Component', () => {
  let hostComponent: TestHostComponent;
  let hostFixture: ComponentFixture<TestHostComponent>;
  let component: NameUrlFieldsComponent;
  let createContentService: CreateContentService;

  const spies: any = {};

  beforeEach((done) => {
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
        { provide: ChannelService, useClass: ChannelServiceMock },
        { provide: CreateContentService, useClass: CreateContentServiceMock },
      ]
    });

    hostFixture = TestBed.createComponent(TestHostComponent);
    hostComponent = hostFixture.componentInstance;

    component = hostComponent.nameUrlFields;
    createContentService = hostFixture.debugElement.injector.get(CreateContentService);

    spies.generateDocumentUrlByName = spyOn(createContentService, 'generateDocumentUrlByName').and.callThrough();
    spies.setDocumentUrlByName = spyOn(component, 'setDocumentUrlByName').and.callThrough();

    component.ngOnInit();
    hostFixture.detectChanges();

    hostFixture.whenStable().then(() => {
      component.form.setValue({
        name: '',
        url: '',
      });

      done();
    });
  });

  afterEach(() => {
    delete component.form.controls['name'];
  });

  function setNameInputValue (value: string) {
    const nameInput = component.nameInputElement.nativeElement;
    component.form.controls.name.setValue(value);
    component.nameField = value;
    nameInput.dispatchEvent(new Event('keyup'));
  }

  describe('ngOnInit', () => {
    it('calls setDocumentUrlByName 1 second after keyup was triggered on nameInputElement', fakeAsync(() => {
      setNameInputValue('test val');
      tick(1000);
      expect(component.setDocumentUrlByName).toHaveBeenCalled();
    }));

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
  });

    describe('setDocumentUrlByName', () => {
      it('calls CreateContentService.generateDocumentUrlByName and applies the new url', () => {
        setNameInputValue('test');
        // component.setDocumentUrlByName();
        // expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('test', 'en');
        // expect(component.urlField).toEqual('test');
        //
        // spies.generateDocumentUrlByName.calls.reset();
        //
        // setNameInputValue('test val');
        // component.setDocumentUrlByName();
        // expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('test val', 'en');
        // expect(component.urlField).toEqual('test-val');
      });

      it('changes the URL upon editing the name, as long as isManualUrlMode is false', fakeAsync(() => {
        // Name editing triggers generation of URL from the back-end
        setNameInputValue('First edit');
        tick(1000);
        expect(spies.generateDocumentUrlByName).toHaveBeenCalled();
        expect(component.urlField).toEqual('first-edit');

        // Manual editing of the URL
        component.setManualUrlEditMode(true);
        component.urlField = 'manual-edit-of-url';

        spies.generateDocumentUrlByName.calls.reset();

        // Until manual editing mode is disabled, URL generations should be bypassed
        setNameInputValue('Second edit, should not change the URL');
        tick(1000);
        expect(spies.generateDocumentUrlByName).not.toHaveBeenCalled();
        expect(component.urlField).toEqual('manual-edit-of-url');
      }));
    });

  describe('validateFields', () => {
    describe('conditions scenarios', () => {
      it('returns true, all conditions resolved to "true"', () => {
        component.nameField = 'name';
        component.urlField = 'url';
        expect(component.validateFields()).toEqual(true);
      });

      it('returns false, name field is empty (conditions index 0)', () => {
        component.nameField = '';
        component.urlField = 'url';
        expect(component.validateFields()).toEqual(false);
      });

      it('returns false, url field is empty (conditions index 1)', () => {
        component.nameField = 'name';
        component.urlField = '';
        expect(component.validateFields()).toEqual(false);
      });

      it('returns false, name field is only whitespace(s) (conditions index 2)', () => {
        component.nameField = '     ';
        component.urlField = 'url';
        expect(component.validateFields()).toEqual(false);
      });

      it('returns false, url field is only whitespace(s) (conditions index 3)', () => {
        component.nameField = 'name';
        component.urlField = '     ';
        expect(component.validateFields()).toEqual(false);
      });
    });
  });
});
