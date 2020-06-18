/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { BehaviorSubject, Subject, Subscription } from 'rxjs';

import { distinctUntilAccumulatorIsEmpty } from './distinct-until-equal-number-of-values';

describe('distinctUntilAccumulatorIsEmpty operator', () => {
  let subject: Subject<boolean>;
  let subscription: Subscription;
  let currentValue: boolean;

  beforeEach(() => {
    subject = new BehaviorSubject<boolean>(false);
    subscription = subject.pipe(
      distinctUntilAccumulatorIsEmpty(),
    ).subscribe(x => currentValue = x);
  });

  afterEach(() => {
    subscription.unsubscribe();
  });

  it('should emit the initial value', () => {
    expect(currentValue).toBe(false);
  });

  it('should emit next "true" value', () => {
    subject.next(true);

    expect(currentValue).toBe(true);
  });

  describe('when the first "true" value is emitted', () => {
    beforeEach(() => {
      subject.next(true);
      currentValue = undefined;
    });

    it('should not emit the next "true" value', () => {
      subject.next(true);

      expect(currentValue).toBe(undefined);
    });

    it('should emit the next "false" value', () => {
      subject.next(false);

      expect(currentValue).toBe(false);
    });

    describe('when the second "true" value is emitted', () => {
      beforeEach(() => {
        subject.next(true);
        currentValue = undefined;
      });

      it('should not emit the next "true" value', () => {
        subject.next(true);

        expect(currentValue).toBe(undefined);
      });

      it('should not emit the next "false" value', () => {
        subject.next(false);

        expect(currentValue).toBe(undefined);
      });

      describe('when the first "false" value is emitted', () => {
        beforeEach(() => {
          subject.next(false);
          currentValue = undefined;
        });

        it('should not emit the next "true" value', () => {
          subject.next(true);

          expect(currentValue).toBe(undefined);
        });

        it('should emit the next "false" value', () => {
          subject.next(false);

          expect(currentValue).toBe(false);
        });

        describe('when the second "false" value is emitted', () => {
          beforeEach(() => {
            subject.next(false);
            currentValue = undefined;
          });

          it('should emit the next "true" value', () => {
            subject.next(true);

            expect(currentValue).toBe(true);
          });

          it('should not emit the next "false" value', () => {
            subject.next(false);

            expect(currentValue).toBe(undefined);
          });

          describe('when the third "false" value is emitted', () => {
            beforeEach(() => {
              subject.next(false);
              currentValue = undefined;
            });

            it('should emit the next "true" value', () => {
              subject.next(true);

              expect(currentValue).toBe(true);
            });

            it('should not emit the next "false" value', () => {
              subject.next(false);

              expect(currentValue).toBe(undefined);
            });
          });
        });
      });
    });
  });
});
