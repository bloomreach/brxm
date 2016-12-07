import template from './rightSidePanel.html';
import RightSidePanelCtrl from './rightSidePanel.controller';

const rightSidePanelComponent = {
  bindings: {
    editMode: '=',
  },
  controller: RightSidePanelCtrl,
  template,
};

export default rightSidePanelComponent;
