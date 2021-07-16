// Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)

import template from './maxValuesHint.html';
import controller from './maxValuesHint.controller';
import './maxValuesHint.scss';

const maxValuesHintComponent = {
  bindings: {
    max: '<',
    values: '<'
  },
  controller,
  template,
};

export default maxValuesHintComponent;
