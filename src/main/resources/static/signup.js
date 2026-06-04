document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('signup-form');
  const msg = document.getElementById('msg');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    msg.textContent = '';
    msg.className = 'msg';
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const confirm = document.getElementById('password-confirm').value;
    if (username.length < 3) { msg.textContent = 'Username must be at least 3 characters'; return; }
    if (password.length < 8) { msg.textContent = 'Password must be at least 8 characters'; return; }
    if (password !== confirm) { msg.textContent = 'Passwords do not match'; return; }

    const btn = form.querySelector('button[type=submit]');
    const prev = btn.textContent;
    btn.disabled = true;
    btn.textContent = 'Creating...';
    try {
      const res = await fetch('/api/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const data = await res.json();
      if (!res.ok) {
        msg.textContent = data.error || 'Signup failed';
        return;
      }
      msg.className = 'msg success';
      msg.textContent = 'Account created — redirecting to login...';
      setTimeout(() => location.href = '/login.html', 900);
    } catch (err) {
      console.error(err);
      msg.textContent = 'Request failed';
    } finally {
      btn.disabled = false;
      btn.textContent = prev;
    }
  });
});
