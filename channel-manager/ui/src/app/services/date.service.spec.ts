// Copyright 2021 Bloomreach B.V. (https://www.bloomreach.com/)

import { TestBed } from '@angular/core/testing';

import { DateService } from './date.service';

describe('DateService', () => {
  let dateService: DateService;
  const RealDate = Date.now;
  const mockCurrentDate = '2019-04-07T10:20:30.000Z';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [DateService] });
    dateService = TestBed.inject(DateService);
  });

  beforeAll(() => {
    window.Date.now = jest.fn(() => new Date(mockCurrentDate).getTime());
  });

  afterAll(() => {
    window.Date.now = RealDate;
  });

  it('should get the current date', () => {
    const currentDate = dateService.getCurrentDate();

    expect(currentDate.toISOString()).toEqual(mockCurrentDate);
  });

  it(`should give the future date in 3 days`, () => {
    const currentDate = dateService.getCurrentDate();
    const futureDate = dateService.getFutureDate(currentDate, 3);

    expect(futureDate.toISOString()).toEqual('2019-04-10T10:20:30.000Z');
  });
});
