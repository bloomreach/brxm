/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HintsComponent } from './hints.component';
import { By } from '@angular/platform-browser';

@Component({
  template: `
   <hippo-hints [data]="testHintsData">
     <div hint="required" id="test-hint-1" class="info test">Required test hint</div>
     <div hint="minlength" id="test-hint-2" class="alert">Minlength test hint</div>
   </hippo-hints> 
  `
})
class HintsComponentTestWrapper {
  testHintsData = {
    required: false,
    minlength: false
  };
}

describe('HintsComponent', () => {
  let wrapperComponent: HintsComponentTestWrapper;
  let fixture: ComponentFixture<HintsComponentTestWrapper>;
  let hintsElement: any;
  let hintsComponent;
  let originalHintElements: Array<HTMLElement>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [HintsComponentTestWrapper, HintsComponent]
    });

    fixture = TestBed.createComponent(HintsComponentTestWrapper);
    wrapperComponent = fixture.componentInstance;
    hintsElement = fixture.debugElement.query(By.css('hippo-hints'));
    hintsComponent = hintsElement.injector.get(HintsComponent);

    originalHintElements = [
      hintsElement.nativeElement.children[0],
      hintsElement.nativeElement.children[1]
    ];
  });

  it('initializes HintsComponent', () => {
    spyOn(originalHintElements[0], 'remove');
    spyOn(originalHintElements[1], 'remove');

    hintsComponent.ngOnInit();

    expect(hintsComponent.el).toBeDefined();
    expect(hintsComponent.hints).toEqual([
      { key: 'required', content: 'Required test hint', classList: ['info', 'test'] },
      { key: 'minlength', content: 'Minlength test hint', classList: ['alert'] }
    ]);

    // Expect original (transcluded) HTML elements to be removed
    expect(originalHintElements[0].remove).toHaveBeenCalled();
    expect(originalHintElements[1].remove).toHaveBeenCalled();
  });

  it('extracts class names from original child elementsby any white space', () => {
    const testHint1 = fixture.nativeElement.querySelector('#test-hint-1');
    const testHint2 = fixture.nativeElement.querySelector('#test-hint-2');
    testHint1.className = 'test       tab       whitespace';
    testHint2.className = 'whitespace       tab       test';

    fixture.whenRenderingDone().then(() => {
      hintsComponent.ngOnInit();
      expect(hintsComponent.hints).toEqual([
        { key: 'required', content: 'Required test hint', classList: ['test', 'tab', 'whitespace'] },
        { key: 'minlength', content: 'Minlength test hint', classList: ['whitespace', 'tab', 'test'] }
      ]);
    });
  });
});
