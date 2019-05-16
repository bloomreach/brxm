export function mergeIntersecting(obj1: object, obj2: object): object {
  const intersection = Object.keys(obj1).reduce((obj, key) => {
    if (key in obj2) {
      obj[key] = obj2[key];
    }

    return obj;
  }, {});

  return { ...obj1, ...intersection };
}
