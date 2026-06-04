document.addEventListener('DOMContentLoaded', () => {
  const btn = document.getElementById('sign-out-btn');
  if (!btn) return;
  btn.addEventListener('click', () => {
    fetch('/api/logout', { method: 'POST' })
      .finally(() => { window.location.href = '/login.html'; });
  });
});
