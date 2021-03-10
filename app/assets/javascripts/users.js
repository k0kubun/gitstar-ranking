if (!document.querySelector('.users_controller.show_action'))
  return;

const userUpdate = document.getElementById('user-update');
if (userUpdate) {
  userUpdate.addEventListener('click', (event) => {
    event.preventDefault();

    userUpdate.innerHTML = 'Updating your stars...';
    userUpdate.classList.add('disabled');
    userUpdate.classList.remove('btn-info');
    userUpdate.classList.add('btn-default');
  });
}
