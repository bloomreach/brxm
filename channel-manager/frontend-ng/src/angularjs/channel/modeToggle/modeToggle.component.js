import template from './modeToggle.html';
import controller from './modeToggle.controller';

const modeToggleComponent = {
  restrict: 'E',
  template,
  controller,
  bindings: {
    mode: '=',
  },
};

export default modeToggleComponent;
