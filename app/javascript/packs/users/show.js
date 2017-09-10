import React from 'react';
import ReactDOM from 'react-dom';
import UpdateButton from '../../components/update_button';

document.addEventListener('DOMContentLoaded', function() {
  var mountNode = document.getElementById('update_button');
  if (mountNode != null) {
    var path = mountNode.getAttribute('data-path');
    var label = mountNode.getAttribute('data-label');
    ReactDOM.render(
      React.createElement(UpdateButton, {path: path, label: label}, null),
      mountNode
    );
  }
});
