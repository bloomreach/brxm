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

import { MonoTypeOperatorFunction, Observable, Operator, Subscriber, TeardownLogic } from 'rxjs';

class DistinctUntilAccumulatorIsEmptySubscriber extends Subscriber<boolean> {
  private value: boolean;
  private accumulator = 0;

  constructor(destination: Subscriber<boolean>) {
    super(destination);
  }

  protected _next(value: boolean): void {
    const increment = value ? 1 : -1;

    this.accumulator += increment;

    if (this.accumulator < 0) {
      this.accumulator = 0;
    }

    const newValue = this.accumulator > 0;

    if (this.value !== newValue) {
      this.value = newValue;
      this.destination.next(newValue);
    }
  }
}

class DistinctUntilAccumulatorIsEmptyOperator implements Operator<boolean, boolean> {
  call(subscriber: Subscriber<boolean>, source: any): TeardownLogic {
    return source.subscribe(new DistinctUntilAccumulatorIsEmptySubscriber(subscriber));
  }
}

export function distinctUntilAccumulatorIsEmpty(): MonoTypeOperatorFunction<boolean> {
  return (source: Observable<boolean>) => source.lift(new DistinctUntilAccumulatorIsEmptyOperator());
}
