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

import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { VariantsService } from '../../services/variants.service';

import { VariantsComponent } from './variants.component';

describe('VariantsComponent', () => {
  let component: VariantsComponent;
  let componentEl: HTMLElement;
  let fixture: ComponentFixture<VariantsComponent>;

  const mockVariants = [
   {
     id: 'hippo-default',
     name: 'Default',
     description: null,
     group: '',
     avatar: '',
     variantName: 'Default',
     expressions: [],
     defaultVariant: true,
     abvariantId: null,
   },
   {
     id: 'dirk-1440145443062@1600075014',
     name: 'Dutch',
     description: null,
     group: 'Dutch',
     avatar: null,
     variantName: 'Dutch',
     expressions: [
       {
         type: 'persona',
         id: 'dirk-1440145443062',
         name: 'Dutch',
       },
     ],
     defaultVariant: false,
     abvariantId: '1600075014',
   },
 ];

  const mockVariantIds = [
    mockVariants[0].id,
    mockVariants[1].id,
  ];

  beforeEach(async(() => {
    const componentEditorServiceMock = {
      getComponentId: () => 'mockComponentId',
    };
    const variantsServiceMock = {
      getVariantIds: () => of(mockVariantIds),
      getVariants: () => of(mockVariants),
    };

    TestBed.configureTestingModule({
      imports: [
        MatFormFieldModule,
        MatSelectModule,
        BrowserAnimationsModule,
      ],
      declarations: [ VariantsComponent ],
      providers: [
        { provide: NG1_COMPONENT_EDITOR_SERVICE, useValue: componentEditorServiceMock },
        { provide: VariantsService, useValue: variantsServiceMock },
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(VariantsComponent);
    component = fixture.componentInstance;
    componentEl = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(componentEl).toMatchSnapshot();
  });

  it('should select the first variant by default', done => {
    component.initialSelection$.subscribe(id => {
      expect(id).toEqual(mockVariantIds[0]);
      done();
    });
  });
});
