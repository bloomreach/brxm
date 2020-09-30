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

import { Component, Input } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';

import { NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { Ng1StateService, NG1_STATE_SERVICE } from '../../../services/ng1/state.ng1.service';
import { Variant } from '../../models/variant.model';
import { VariantsService } from '../../services/variants.service';

import { VariantsComponent } from './variants.component';

describe('VariantsComponent', () => {
  let component: VariantsComponent;
  let componentEl: HTMLElement;
  let fixture: ComponentFixture<VariantsComponent>;
  let stateService: Ng1StateService;
  let variantsService: VariantsService;

  @Component({
    // tslint:disable-next-line:component-selector
    selector: 'mat-icon',
    template: '{{ svgIcon }}',
  })
  class MatIconMockComponent {
    @Input()
    svgIcon!: string;
  }

  const mockExpressions = [
    {
      country: 'thenetherlands-1440145311193',
    },
    {
      continent: 'africa-AF',
    },
  ];

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
     id: 'dirk-1440145443062',
     name: 'Dutch',
     description: null,
     group: 'Dutch',
     avatar: null,
     variantName: 'Dutch',
     expressions: [
       {type: 'persona', id: 'dirk-1440145443062', name: 'Dutch'},
       {type: 'rule', id: `${Object.keys(mockExpressions[0])[0]}/${Object.values(mockExpressions[0])[0]}`, name: 'The Netherlands'},
       {type: 'rule', id: `${Object.keys(mockExpressions[1])[0]}/${Object.values(mockExpressions[1])[0]}`, name: 'Africa'},
     ],
     defaultVariant: false,
     abvariantId: '1600075014',
   },
 ] as Variant[];

  const mockComponent = {
    getId: () => 'mockComponentId',
    getRenderVariant: () => 'hippo-default',
  };

  const mockFormData = {
    field1: 'value1',
    field2: 'value2',
  };

  beforeEach(() => {
    const componentEditorServiceMock = {
      getComponent: () => mockComponent,
      propertiesAsFormData: () => mockFormData,
    };
    const variantsServiceMock = {
      addVariant: () => Promise.resolve(),
      getVariants: () => Promise.resolve(mockVariants),
    };
    const stateServiceMock = {
      params: {
        variantId: mockVariants[0].id,
      },
      go: jest.fn(() => Promise.resolve()),
    };

    TestBed.configureTestingModule({
      imports: [
        MatFormFieldModule,
        MatSelectModule,
        BrowserAnimationsModule,
        TranslateModule.forRoot(),
        ReactiveFormsModule,
      ],
      declarations: [ VariantsComponent, MatIconMockComponent ],
      providers: [
        { provide: NG1_COMPONENT_EDITOR_SERVICE, useValue: componentEditorServiceMock },
        { provide: NG1_STATE_SERVICE, useValue: stateServiceMock },
        { provide: VariantsService, useValue: variantsServiceMock },
      ],
    });

    stateService = TestBed.inject(NG1_STATE_SERVICE);
    variantsService = TestBed.inject(VariantsService);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(VariantsComponent);
    component = fixture.componentInstance;
    componentEl = fixture.nativeElement;
    component.ngOnInit();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(componentEl).toMatchSnapshot();
  });

  it('should set initial selected variant', () => {
    expect(component.variantSelect.value).toBe(mockVariants[0]);
  });

  it('should go to edit component state of selected variant', fakeAsync(() => {
    component.selectVariant(mockVariants[1]);

    expect(stateService.go).toHaveBeenCalledWith('hippo-cm.channel.edit-component', {
      componentId: mockComponent.getId(),
      variantId: mockVariants[1].id,
    });
  }));

  describe('adding variant', () => {
    it('should select the newly added variant', async () => {
      jest.spyOn(variantsService, 'addVariant');
      component.variantSelect.setValue(mockVariants[1]);

      await component.addVariant();

      expect(variantsService.addVariant).toHaveBeenCalledWith(
        mockComponent.getId(),
        mockFormData,
        mockVariants[1].expressions[0].id,
        mockExpressions,
      );
    });

    it('should navigate to edit the newly created variant', async () => {
      const newVariant = {
        id: 'newVariant',
      } as Variant;
      jest.spyOn(variantsService, 'getVariants').mockResolvedValue(mockVariants.concat(newVariant));

      await component.addVariant();

      expect(stateService.go).toHaveBeenCalledWith('hippo-cm.channel.edit-component', {
        componentId: mockComponent.getId(),
        variantId: newVariant.id,
      });
    });
  });
});
