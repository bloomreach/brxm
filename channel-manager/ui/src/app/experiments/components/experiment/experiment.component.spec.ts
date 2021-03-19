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

import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { mocked } from 'ts-jest/utils';

import { NG1_CHANNEL_SERVICE } from '../../../services/ng1/channel.ng1service';
import { NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { NotificationService } from '../../../services/notification.service';
import { VariantsService } from '../../../variants/services/variants.service';
import { ExperimentState } from '../../models/experiment-state.enum';
import { ExperimentWithStatusData } from '../../models/experiment-with-status-data.model';
import { ExperimentsService } from '../../services/experiments.service';

import { ExperimentComponent } from './experiment.component';

@Pipe({name: 'translate'})
export class TranslateMockPipe implements PipeTransform {
  transform(value: string): string {
    return value;
  }
}

describe('ExperimentComponent', () => {
  let experimentsService: ExperimentsService;
  let notificationService: NotificationService;

  const mockExperiment: ExperimentWithStatusData = {
    id: 'experiment-1',
    state: ExperimentState.Running,
    type: 'PAGE',
    startTime: 1600868196063,
    winnerVariant: null,
    goal: {
      id: 'goal-1',
      name: 'Goal 1',
      type: 'PAGE',
      readOnly: false,
      targetPage: '/target-page',
      mountId: 'some-mount-id',
    },
    variants: [
      {
        variantId: 'default',
        variantName: 'Default',
        confidence: 0.3,
        mean: 0.2,
        variance: 0.4,
        visitorSegment: 'segment-1',
      },
      {
        variantId: 'some-variant',
        variantName: 'Some variant',
        confidence: 0.3,
        mean: 0.2,
        variance: 0.4,
        visitorSegment: 'segment-1',
      },
    ],
    statusWithVisits: [
      { 'variant-1': 3, default: 1, timestamp: 1600948800000, visits: 4 },
      { 'variant-1': 100, default: 10, timestamp: 1600959600000, visits: 110 },
      { 'variant-1': 40, default: 1000, timestamp: 1600970400000, visits: 1040 },
      { 'variant-1': 20, default: 5, timestamp: 1600981200000, visits: 25 },
      { 'variant-1': 80, default: 80, timestamp: 1600992000000, visits: 160 },
      { 'variant-1': 11, default: 12, timestamp: 1601002800000, visits: 23 },
      { 'variant-1': 0, default: 0, timestamp: 1601013600000, visits: 0 },
      { 'variant-1': 3, default: 4, timestamp: 1601024400000, visits: 7 },
      { 'variant-1': 1000, default: 500, timestamp: 1601035200000, visits: 1500 },
      { 'variant-1': 123, default: 456, timestamp: 1601046000000, visits: 579 },
      { 'variant-1': 234, default: 546, timestamp: 1601056800000, visits: 779 },
      { 'variant-1': 4635, default: 4365, timestamp: 1601067600000, visits: 9000 },
      { 'variant-1': 345, default: 465, timestamp: 1601078400000, visits: 810 },
      { 'variant-1': 465, default: 467, timestamp: 1601089200000, visits: 922 },
    ],
  };

  const mockMountId = 'some-mount-id';

  const mockGoals = [
    {
      id: 'goal-1',
      name: 'Goal 1',
      type: 'PAGE',
      readOnly: false,
      targetPage: '/target-page',
      mountId: mockMountId,
    },
    {
      id: 'goal-2',
      name: 'Goal 2',
      type: 'PAGE',
      readOnly: false,
      targetPage: '/target-page',
      mountId: mockMountId,
    },
    {
      id: 'goal-3',
      name: 'Goal 3',
      type: 'PAGE',
      readOnly: false,
      targetPage: '/some-target-page',
      mountId: 'some-other-mount-id',
    },
  ];

  const mockVariants = [
    {
      id: 'variant-1',
      variantName: 'Variant 1',
    },
    {
      id: 'variant-2',
      variantName: 'Variant 2',
    },
  ];

  const mockComponent = {
    getId: () => 'mockComponentId',
    isXPageComponent: () => false,
  };

  beforeEach(() => {
    const componentEditorServiceMock = {
      getComponent: () => mockComponent,
    };

    const experimentsServiceMock = {
      getExperiment: jest.fn(() => Promise.resolve(mockExperiment)),
      getGoals: jest.fn().mockResolvedValue(mockGoals),
      saveExperiment: jest.fn(),
      completeExperiment: jest.fn(),
    };

    const variantsServiceMock = {
      defaultVariantId: 'hippo-default',
      getVariants: jest.fn().mockResolvedValue(mockVariants),
    };

    const notificationServiceMock = {
      showNotification: jest.fn(),
      showErrorNotification: jest.fn(),
    };

    const translateServiceMock = {
      instant: jest.fn(x => x),
    };

    const channelServiceMock = {
      getChannel: jest.fn(() => ({ mountId: mockMountId })),
    };

    TestBed.configureTestingModule({
      declarations: [
        ExperimentComponent,
        TranslateMockPipe,
      ],
      providers: [
        { provide: NG1_CHANNEL_SERVICE, useValue: channelServiceMock },
        { provide: NG1_COMPONENT_EDITOR_SERVICE, useValue: componentEditorServiceMock },
        { provide: ExperimentsService, useValue: experimentsServiceMock },
        { provide: VariantsService, useValue: variantsServiceMock },
        { provide: NotificationService, useValue: notificationServiceMock },
        { provide: TranslateService, useValue: translateServiceMock },
      ],
      schemas: [
        NO_ERRORS_SCHEMA,
      ],
    });

    experimentsService = TestBed.inject(ExperimentsService);
    notificationService = TestBed.inject(NotificationService);
  });

  describe('if experiment was not saved', () => {
    let fixture: ComponentFixture<ExperimentComponent>;
    let component: ExperimentComponent;

    beforeEach(fakeAsync(() => {
      fixture = TestBed.createComponent(ExperimentComponent);

      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      component = fixture.componentInstance;
    }));

    it('should show the experiment form', fakeAsync(() => {
      expect(fixture.nativeElement).toMatchSnapshot();
    }));

    it('should only show goals from current channel', async () => {
      const filteredGoals = mockGoals.filter(g => g.mountId === mockMountId);
      const availableGoals = await component.availableGoals$;
      expect(availableGoals).toEqual(filteredGoals);
    });

    describe('onCancelExperiment', () => {
      it('should cancel the experiment', () => {
        component.onCancelExperiment();

        expect(experimentsService.saveExperiment).toHaveBeenCalledWith('mockComponentId');
      });

      it('should load the saved experiment', async () => {
        const mockSavedExperiment = { ...mockExperiment , state: ExperimentState.Created };

        mocked(experimentsService.saveExperiment).mockResolvedValue();
        mocked(experimentsService.getExperiment).mockResolvedValue(mockSavedExperiment);

        await component.onCancelExperiment();
        const savedExperiment = component.experiment;

        expect(experimentsService.getExperiment).toHaveBeenCalledWith('mockComponentId');
        expect(savedExperiment).toBe(mockSavedExperiment);
      });

      it('should show a notification after successful saving', async () => {
        mocked(experimentsService.saveExperiment).mockResolvedValue();

        await component.onCancelExperiment();

        expect(notificationService.showNotification).toHaveBeenCalledWith('EXPERIMENT_CANCELED');
      });

      it('should show an error notification after unsuccessful saving', async () => {
        mocked(experimentsService.saveExperiment).mockRejectedValue(new Error());

        await component.onCancelExperiment();

        expect(notificationService.showErrorNotification).toHaveBeenCalledWith('EXPERIMENT_CANCEL_ERROR');
      });
    });

    describe('onVariantAndGoalSelected', () => {
      const variantAndGoal = {
        variantId: 'variant-1',
        goalId: 'goal-1',
      };

      it('should save the experiment', () => {
        component.onVariantAndGoalSelected(variantAndGoal);

        expect(experimentsService.saveExperiment).toHaveBeenCalledWith('mockComponentId', 'variant-1', 'goal-1');
      });

      it('should load the saved experiment', async () => {
        const mockSavedExperiment = { ...mockExperiment , state: ExperimentState.Created };

        mocked(experimentsService.saveExperiment).mockResolvedValue();
        mocked(experimentsService.getExperiment).mockResolvedValue(mockSavedExperiment);

        await component.onVariantAndGoalSelected(variantAndGoal);
        const savedExperiment = component.experiment;

        expect(experimentsService.getExperiment).toHaveBeenCalledWith('mockComponentId');
        expect(savedExperiment).toBe(mockSavedExperiment);
      });

      it('should show a notification after successful saving', async () => {
        mocked(experimentsService.saveExperiment).mockResolvedValue();

        await component.onVariantAndGoalSelected(variantAndGoal);

        expect(notificationService.showNotification).toHaveBeenCalledWith('EXPERIMENT_SAVED');
      });

      it('should show an error notification after unsuccessful saving', async () => {
        mocked(experimentsService.saveExperiment).mockRejectedValue(new Error());

        await component.onVariantAndGoalSelected(variantAndGoal);

        expect(notificationService.showErrorNotification).toHaveBeenCalledWith('EXPERIMENT_SAVE_ERROR');
      });
    });

    describe('onCompleteExperiment', () => {
      it('should complete the experiment', () => {
        component.onCompleteExperiment('variant-1');

        expect(experimentsService.completeExperiment).toHaveBeenCalledWith('mockComponentId', 'variant-1');
      });

      it('should load the completed experiment', async () => {
        const mockSavedExperiment = { ...mockExperiment , state: ExperimentState.Created };

        mocked(experimentsService.completeExperiment).mockResolvedValue();
        mocked(experimentsService.getExperiment).mockResolvedValue(mockSavedExperiment);

        await component.onCompleteExperiment();
        const savedExperiment = component.experiment;

        expect(experimentsService.getExperiment).toHaveBeenCalledWith('mockComponentId');
        expect(savedExperiment).toBe(mockSavedExperiment);
      });

      it('should show a notification after successful completion', async () => {
        mocked(experimentsService.completeExperiment).mockResolvedValue();

        await component.onCompleteExperiment();

        expect(notificationService.showNotification).toHaveBeenCalledWith('EXPERIMENT_COMPLETED');
      });

      it('should show an error notification after unsuccessful completion', async () => {
        mocked(experimentsService.completeExperiment).mockRejectedValue(new Error());

        await component.onCompleteExperiment();

        expect(notificationService.showErrorNotification).toHaveBeenCalledWith('EXPERIMENT_COMPLETION_ERROR');
      });
    });

    describe('requestInProgress', () => {
      const variantAndGoal = {
        variantId: 'variant-1',
        goalId: 'goal-1',
      };

      it('should be set to true', () => {
        component.onVariantAndGoalSelected(variantAndGoal);

        expect(component.requestInProgress).toBeTruthy();
      });

      it('should be set to false on successful experiment saving', async () => {
        await component.onVariantAndGoalSelected(variantAndGoal);

        expect(component.requestInProgress).toBeFalsy();
      });

      it('should be set to false on unsuccessful experiment saving', async () => {
        mocked(experimentsService.saveExperiment).mockRejectedValue(new Error());

        await component.onVariantAndGoalSelected(variantAndGoal);

        expect(component.requestInProgress).toBeFalsy();
      });
    });
  });

  describe('if experiment was saved', () => {
    let fixture: ComponentFixture<ExperimentComponent>;

    beforeEach(fakeAsync(() => {
      fixture = TestBed.createComponent(ExperimentComponent);

      fixture.detectChanges();
      tick();
      fixture.detectChanges();
    }));

    it('should show the experiment', fakeAsync(() => {
      expect(fixture.nativeElement).toMatchSnapshot();
    }));
  });
});
