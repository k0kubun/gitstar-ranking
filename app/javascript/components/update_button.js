import React from 'react';
import PropTypes from 'prop-types';

export default class UpdateButton extends React.Component {
  static propTypes = {
    label: PropTypes.string.isRequired,
    login: PropTypes.string.isRequired,
    path: PropTypes.string.isRequired
  };

  constructor(props) {
    super(props);
    this.state = {};
    this.updateStatus();
  }

  updateStatus() {
    var query = `
      query {
        user(login:"${this.props.login}") {
          updateStatus
        }
      }
    `;
    $.post('/graphql', { query: query }, function(data) {
      var updateStatus = data.data.user.updateStatus;
      this.setState({ updateStatus: updateStatus });
      if (updateStatus == 'UPDATING') {
        setTimeout(function() { this.updateStatus() }.bind(this), 3000);
      }
    }.bind(this));
  }

  render() {
    switch (this.state.updateStatus) {
      case 'UPDATING':
        return React.createElement('span',
          { className: 'btn btn-default disabled col-xs-12' },
          'Updating stars ...'
        );
      case 'UPDATED':
        return React.createElement('span',
          { className: 'btn btn-default disabled col-xs-12' },
          'Up to date'
        );
      case 'OUTDATED':
        return React.createElement('a',
          { className: 'btn btn-info col-xs-12', href: this.props.path, 'data-method': 'post' },
          this.props.label
        );
      default:
        return React.createElement('span',
          { className: 'btn btn-default disabled col-xs-12' },
          'Loading status ...'
        );
    }
  }
}
