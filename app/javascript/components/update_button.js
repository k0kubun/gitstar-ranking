import React, { PropTypes } from 'react';

export default class UpdateButton extends React.Component {
  static propTypes = {
    label: PropTypes.string.isRequired,
    path: PropTypes.string.isRequired
  };

  constructor(props) {
    super(props);
  }

  render() {
    return React.createElement('a',
      {className: 'btn btn-info col-xs-12', href: this.props.path, method: 'post'},
      this.props.label
    );
  }
}
