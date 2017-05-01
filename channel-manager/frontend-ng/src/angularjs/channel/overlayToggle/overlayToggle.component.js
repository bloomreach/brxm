import template from './overlayToggle.html';
import controller from './overlayToggle.controller';

const modeToggleComponent = {
  restrict: 'E',
  template,
  controller,
  bindings: {
    state: '=',
    icon: '@',
    tooltip: '@?',
  },
};

export default modeToggleComponent;
