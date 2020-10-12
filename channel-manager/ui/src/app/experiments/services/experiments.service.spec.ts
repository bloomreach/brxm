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

import { TestBed } from '@angular/core/testing';
import { mocked } from 'ts-jest/utils';

import { TargetingApiResponse } from '../../models/targeting-api-response.model';
import { Ng1TargetingService, NG1_TARGETING_SERVICE } from '../../services/ng1/targeting.ng1service';
import { ExperimentStatus } from '../models/experiment-status.model';

import { ExperimentsService } from './experiments.service';

describe('ExperimentsService', () => {
  let service: ExperimentsService;
  let targetingService: Ng1TargetingService;

  beforeEach(() => {
    const targetingServiceMock = {
      getExperimentStatus: jest.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        ExperimentsService,
        { provide: NG1_TARGETING_SERVICE, useValue: targetingServiceMock },
      ],
    });

    service = TestBed.inject(ExperimentsService);
    targetingService = TestBed.inject(NG1_TARGETING_SERVICE);
  });

  describe('getExperimentStatus', () => {
    const mockExperimentStatusData: ExperimentStatus = [
      { 'variant-1': 3, default: 1, timestamp: 1600948800000 },
      { 'variant-1': 100, default: 10, timestamp: 1600959600000 },
      { 'variant-1': 40, default: 1000, timestamp: 1600970400000 },
      { 'variant-1': 20, default: 5, timestamp: 1600981200000 },
      { 'variant-1': 80, default: 80, timestamp: 1600992000000 },
      { 'variant-1': 11, default: 12, timestamp: 1601002800000 },
      { 'variant-1': 0, default: 0, timestamp: 1601013600000 },
      { 'variant-1': 3, default: 4, timestamp: 1601024400000 },
      { 'variant-1': 1000, default: 500, timestamp: 1601035200000 },
      { 'variant-1': 123, default: 456, timestamp: 1601046000000 },
      { 'variant-1': 234, default: 546, timestamp: 1601056800000 },
      { 'variant-1': 4635, default: 4365, timestamp: 1601067600000 },
      { 'variant-1': 345, default: 465, timestamp: 1601078400000 },
      { 'variant-1': 465, default: 467, timestamp: 1601089200000 },
    ];

    beforeEach(() => {
      const mockApiResponse: TargetingApiResponse<ExperimentStatus> = {
        success: true,
        message: 'Some message',
        errorCode: null,
        reloadRequired: false,
        data: mockExperimentStatusData,
      };

      mocked(targetingService.getExperimentStatus).mockResolvedValue(mockApiResponse);
    });

    it('should provide experiment id', () => {
      service.getExperimentStatus('experiment-id');

      expect(targetingService.getExperimentStatus).toHaveBeenCalledWith('experiment-id');
    });

    it('should return the result',  async () => {
      const result = await service.getExperimentStatus('experiment-id');

      expect(result).toBeDefined();
    });

    it('should calculate visits', async () => {
      const expected = [
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
        { 'variant-1': 234, default: 546, timestamp: 1601056800000, visits: 780 },
        { 'variant-1': 4635, default: 4365, timestamp: 1601067600000, visits: 9000 },
        { 'variant-1': 345, default: 465, timestamp: 1601078400000, visits: 810 },
        { 'variant-1': 465, default: 467, timestamp: 1601089200000, visits: 932 },
      ];

      const result = await service.getExperimentStatus('experiment-id');

      expect(result?.statusWithVisits).toEqual(expected);
    });

    it('should calculate total visits', async () => {
      const result = await service.getExperimentStatus('experiment-id');

      expect(result?.totalVisits).toBe(14970);
    });

    it('should return undefined if the data was not fetched', async () => {
      const mockErrorApiResponse: TargetingApiResponse<ExperimentStatus> = {
        success: false,
        message: 'some error message',
        errorCode: 'error-code',
        reloadRequired: false,
        data: null,
      };

      mocked(targetingService.getExperimentStatus).mockResolvedValue(mockErrorApiResponse);

      const result = await service.getExperimentStatus('experiment-id');

      expect(result).toBeUndefined();
    });
  });
});
