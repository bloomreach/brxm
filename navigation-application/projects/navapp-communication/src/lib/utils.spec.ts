import { mergeIntersecting } from './utils';

describe('mergeIntersecting', () => {
  it('creates a new object from 2 other objects', () => {
    const obj1 = {
      test1(): void {},
    };
    const obj2 = {
      test1(): void {},
    };

    expect(mergeIntersecting(obj1, obj2)).not.toBe(obj1 || obj2);
  });

  it('merges intersecting enumerable own properties on 2 objects', () => {
    const test = () => 1 + 1;
    const test2 = () => 2 + 2;
    const obj1 = {
      myMethod: () => {},
      test,
    };
    const obj2 = {
      myOtherMethod: () => {},
      test: test2,
    };

    const merged = mergeIntersecting(obj1, obj2);

    expect((merged as any).test).toBe(test2);
    expect('myMethod' in merged).toBe(true);
    expect('myOtherMethod' in merged).toBe(false);
  });
});
