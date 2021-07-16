// Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)

export default class MaxValuesHintCtrl {
  constructor() {
    'ngInject';
  }

  getHintStatus() {
    return {
      current: (this.values && this.values.length) || 0,
      max: this.max,
    };
  }
};
