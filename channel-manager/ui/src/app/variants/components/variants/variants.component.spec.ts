/*!
 * Copyright 2020-2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';

import { NG1_CMS_SERVICE } from '../../../services/ng1/cms.ng1.service';
import { Ng1ComponentEditorService, NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { NG1_ROOT_SCOPE } from '../../../services/ng1/root-scope.ng1.service';
import { Ng1StateService, NG1_STATE_SERVICE } from '../../../services/ng1/state.ng1.service';
import { Variant, VariantCharacteristicData } from '../../models/variant.model';
import { VariantsService } from '../../services/variants.service';

import { VariantsComponent } from './variants.component';

describe('VariantsComponent', () => {
  let component: VariantsComponent;
  let componentEl: HTMLElement;
  let fixture: ComponentFixture<VariantsComponent>;
  let stateService: Ng1StateService;
  let variantsService: VariantsService;
  let componentEditorService: Ng1ComponentEditorService;

  const mockDefaultVariantId = 'test-default';

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
  ] as VariantCharacteristicData[];

  const mockVariants = [
   {
     id: mockDefaultVariantId,
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
    getRenderVariant: () => mockDefaultVariantId,
  };

  const mockFormData = {
    field1: 'value1',
    field2: 'value2',
  };

  let mockExpressionsVisible = true;

  beforeEach(() => {
    const componentEditorServiceMock = {
      getComponent: () => mockComponent,
      propertiesAsFormData: () => mockFormData,
      isReadOnly: () => false,
    };
    const variantsServiceMock = {
      extractExpressions: jest.fn(),
      addVariant: () => Promise.resolve(),
      getVariants: () => Promise.resolve(mockVariants),
      defaultVariantId: mockDefaultVariantId,
      getExpressionsVisible: jest.fn(() => mockExpressionsVisible),
      setExpressionsVisible: jest.fn(value => mockExpressionsVisible = value),
    };
    const stateServiceMock = {
      params: {
        variantId: mockVariants[0].id,
      },
      go: jest.fn(() => Promise.resolve()),
    };
    const cmsServiceMock = {
      publish: jest.fn(),
    };
    const $rootScopeMock = {
      $on: jest.fn(() => jest.fn()),
    };

    TestBed.configureTestingModule({
      imports: [
        MatDialogModule,
        MatFormFieldModule,
        MatSelectModule,
        MatTooltipModule,
        BrowserAnimationsModule,
        TranslateModule.forRoot(),
        ReactiveFormsModule,
      ],
      declarations: [ VariantsComponent, MatIconMockComponent ],
      providers: [
        { provide: NG1_CMS_SERVICE, useValue: cmsServiceMock },
        { provide: NG1_COMPONENT_EDITOR_SERVICE, useValue: componentEditorServiceMock },
        { provide: NG1_ROOT_SCOPE, useValue: $rootScopeMock },
        { provide: NG1_STATE_SERVICE, useValue: stateServiceMock },
        { provide: VariantsService, useValue: variantsServiceMock },
      ],
    });

    stateService = TestBed.inject(NG1_STATE_SERVICE);
    variantsService = TestBed.inject(VariantsService);
    componentEditorService = TestBed.inject(NG1_COMPONENT_EDITOR_SERVICE);
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

    expect(stateService.go).toHaveBeenCalledWith('hippo-cm.channel.edit-component.properties', {
      componentId: mockComponent.getId(),
      variantId: mockVariants[1].id,
    });
  }));

  describe('adding variant', () => {
    it('should select the newly added variant', async () => {
      jest.spyOn(variantsService, 'addVariant');
      component.variantSelect.setValue(mockVariants[1]);
      jest.spyOn(variantsService, 'extractExpressions').mockReturnValue({
        persona: mockVariants[1].expressions[0].id,
        characteristics: mockExpressions,
      });

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
      jest.spyOn(variantsService, 'extractExpressions').mockReturnValue({
        persona: mockVariants[1].expressions[0].id,
        characteristics: mockExpressions,
      });

      await component.addVariant();

      expect(stateService.go).toHaveBeenCalledWith('hippo-cm.channel.edit-component.properties', {
        componentId: mockComponent.getId(),
        variantId: newVariant.id,
      });
    });
  });

  describe('when the component editor is readonly', () => {
    it('should disable the action buttons', () => {
      jest.spyOn(componentEditorService, 'isReadOnly').mockReturnValue(true);

      fixture.detectChanges();

      expect(componentEl).toMatchSnapshot();
    });
  });

  describe('collapsing of variant expressions', () => {
    async function setNonDefaultVariant(): Promise<void> {
      stateService.params.variantId = mockVariants[1].id;
      await component.ngOnInit();
      fixture.detectChanges();
    }

    function clickCollapseButton(): void {
      const button = componentEl.querySelector<HTMLButtonElement>('.qa-variant-expressions-toggle-button');
      button?.click();
      fixture.detectChanges();
    }

    it('should not show expressions for default variant', () => {
      expect(component.isDefaultVariant()).toBe(true);
      expect(componentEl).toMatchSnapshot();
    });

    it('should show expressions for non-default variant', async () => {
      await setNonDefaultVariant();
      expect(component.isDefaultVariant()).toBe(false);
      expect(componentEl).toMatchSnapshot();
    });

    it('should hide expressions for non-default variant if toggle is clicked', async () => {
      await setNonDefaultVariant();
      expect(component.isDefaultVariant()).toBe(false);

      clickCollapseButton();

      expect(component.expressionsVisible).toBe(false);
      expect(componentEl).toMatchSnapshot();
    });

    it('should show expressions for non-default variant if toggle is clicked again', async () => {
      await setNonDefaultVariant();
      expect(component.isDefaultVariant()).toBe(false);
      component.expressionsVisible = false;
      fixture.detectChanges();

      clickCollapseButton();

      expect(component.expressionsVisible).toBe(true);
      expect(componentEl).toMatchSnapshot();
    });
  });
});
