window.addEventListener('load', () => {
  if (!document.querySelector('.users_controller.show_action'))
    return;

  const userUpdate = document.getElementById('user-update');
  if (userUpdate) {
    userUpdate.classList.remove('disabled');

    const updateButton = () => {
      const query = `
        query {
          user(login:"${userUpdate.dataset['login']}") {
            updateStatus
          }
        }
      `;
      $.post('/graphql',  { query: query }, (data) => {
        const updateStatus = data.data.user.updateStatus;
        if (updateStatus == 'UPDATED') {
          userUpdate.innerHTML = 'Up to date';
        } else if (updateStatus == 'UPDATING') {
          setTimeout(function() { updateButton() }.bind(this), 3000);
        }
      });
    };

    userUpdate.addEventListener('click', (event) => {
      event.preventDefault();

      userUpdate.innerHTML = 'Updating your stars...';
      userUpdate.classList.add('disabled');
      userUpdate.classList.remove('btn-info');
      userUpdate.classList.add('btn-default');

      updateButton();
    });
  }
});
