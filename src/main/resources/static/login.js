document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('login-form');
  const msg = document.getElementById('msg');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    msg.textContent = '';
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
        // show TOTP input
        showTwoFactorPrompt(username);
        return;
      }
      msg.style.color = '#8f8';
      msg.textContent = 'Login successful — redirecting...';
      // mark logged-in state on the window and in localStorage so other modules can read it
      window.isLoggedIn = true;
      try { localStorage.setItem('isLoggedIn', '1'); } catch (e) { /* ignore */ }
      setTimeout(() => location.href = '/dashboard.html', 700);
    } catch (err) {
      console.error(err);
      msg.textContent = 'Request failed';
    } finally {
      btn.disabled = false;
      btn.textContent = prev;
    }
  });
});

function showTwoFactorPrompt(username) {
  const container = document.createElement('div');
  container.style.marginTop = '12px';
  container.innerHTML = `
    <div style="display:flex;flex-direction:column;gap:8px;max-width:360px;margin:0 auto;">
      <input id="totp-code" placeholder="Enter 6-digit code" />
      <div style="display:flex;gap:8px;justify-content:center;">
        <button id="verify-2fa" class="btn">Verify</button>
      </div>
      <div id="2fa-msg" class="msg"></div>
    </div>
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
        body: JSON.stringify({ username, code })
      });
      const d = await res.json();
      if (!res.ok) { m.textContent = d.error || 'Verification failed'; return; }
      m.style.color = '#8f8';
      m.textContent = '2FA verified — redirecting...';
      setTimeout(() => location.href = '/dashboard.html', 700);
    } catch (err) {
      console.error(err);
      m.textContent = 'Request failed';
    }
  });
}
