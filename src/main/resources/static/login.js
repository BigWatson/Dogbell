document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('login-form');
  const msg = document.getElementById('msg');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    msg.textContent = '';
    msg.className = 'msg';
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    if (username.length === 0 || password.length === 0) { msg.textContent = 'Enter username and password'; return; }

    const btn = form.querySelector('button[type=submit]');
    const prev = btn.textContent;
    btn.disabled = true;
    btn.textContent = 'Signing in...';
    try {
      const res = await fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const data = await res.json();
      if (!res.ok) {
        msg.textContent = data.error || 'Login failed';
        return;
      }
      if (data.twoFactor) {
        showTwoFactorPrompt();
        return;
      }
      msg.className = 'msg success';
      msg.textContent = 'Login successful — redirecting...';
      setTimeout(() => location.href = '/loggedInHomepage.html', 700);
    } catch (err) {
      console.error(err);
      msg.textContent = 'Request failed';
    } finally {
      btn.disabled = false;
      btn.textContent = prev;
    }
  });
});

function showTwoFactorPrompt() {
  const container = document.createElement('div');
  container.className = 'twofa-inline';
  container.innerHTML = `
    <label for="totp-code" class="form-label">Authentication code</label>
    <input id="totp-code" placeholder="6-digit code" autocomplete="one-time-code" inputmode="numeric" />
    <button id="verify-2fa" class="btn" type="button">Verify</button>
    <div id="2fa-msg" class="msg" role="alert" aria-live="polite"></div>
  `;
  const form = document.getElementById('login-form');
  form.appendChild(container);
  document.getElementById('verify-2fa').addEventListener('click', async () => {
    const code = document.getElementById('totp-code').value.trim();
    const m = document.getElementById('2fa-msg');
    m.textContent = '';
    if (!code) { m.textContent = 'Enter code'; return; }
    try {
      const res = await fetch('/api/2fa/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code })
      });
      const d = await res.json();
      if (!res.ok) { m.textContent = d.error || 'Verification failed'; return; }
      m.className = 'msg success';
      m.textContent = '2FA verified — redirecting...';
      setTimeout(() => location.href = '/loggedInHomepage.html', 700);
    } catch (err) {
      console.error(err);
      m.textContent = 'Request failed';
    }
  });
}
