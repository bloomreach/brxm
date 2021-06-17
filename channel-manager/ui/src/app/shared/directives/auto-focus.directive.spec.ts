/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { Component, ElementRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AutoFocusDirective } from './auto-focus.directive';

describe('AutoFocusDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  let componentEl: ElementRef<HTMLInputElement>;

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      declarations: [ AutoFocusDirective, TestComponent ],
    })
      .createComponent(TestComponent);

    // element with an attached AutoFocusDirective
    componentEl = fixture.debugElement.query(By.directive(AutoFocusDirective));

    fixture.detectChanges(); // initial binding
  });

  it('should create an instance', () => {
    const directive = new AutoFocusDirective(componentEl);
    expect(directive).toBeTruthy();
  });
});

@Component({
  template: `<input emAutoFocus/>`,
})
class TestComponent { }
