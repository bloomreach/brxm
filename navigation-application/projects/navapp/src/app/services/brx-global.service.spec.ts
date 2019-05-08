import { TestBed } from '@angular/core/testing';

import { BrxGlobalService } from './brx-global.service';

describe('BrxGlobalService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: BrxGlobalService = TestBed.get(BrxGlobalService);
    expect(service).toBeTruthy();
  });
});
