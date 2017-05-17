import controller from './resizeHandle.controller';
import template from './resizeHandle.html';
import './resizeHandle.scss';

const resizeHandleComponent = {
  restrict: 'E',
  template,
  controller,
  bindings: {
    element: '=',
    onResize: '&',
  },
};

export default resizeHandleComponent;
