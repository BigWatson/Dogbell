document.addEventListener('DOMContentLoaded', async () => {
  // Check auth — redirect to login if not authenticated
  let currentUsername;
  try {
    const meRes = await fetch('/api/me');
    if (!meRes.ok) { location.href = '/login.html'; return; }
    const meData = await meRes.json();
    currentUsername = meData.username;
  } catch (e) { location.href = '/login.html'; return; }

  // Greet the user
  const welcome = document.getElementById('welcome-name');
  if (welcome && currentUsername) welcome.textContent = `, ${currentUsername}`;

  // Sign-out
  const signOutBtn = document.getElementById('sign-out-btn');
  if (signOutBtn) {
    signOutBtn.addEventListener('click', () => {
      fetch('/api/logout', { method: 'POST' }).finally(() => { location.href = '/login.html'; });
    });
  }

  // Pre-fill the Pi registration username field (if present on this page)
  const piUsernameInput = document.getElementById('pi-username');
  if (piUsernameInput) piUsernameInput.value = currentUsername;

  // ── 2FA Enable ──
  const enableBtn = document.getElementById('enable-2fa');
  const area = document.getElementById('2fa-area');
  if (enableBtn) {
    enableBtn.addEventListener('click', async () => {
      enableBtn.disabled = true;
      try {
        const res = await fetch('/api/2fa/setup', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: '{}'
        });
        const data = await res.json();
        if (!res.ok) { alert(data.error || 'Failed to setup 2FA'); return; }
        area.innerHTML = `
          <div class="twofa-panel">
            <p>Enter the secret into your authenticator app:</p>
            <p class="twofa-secret">${data.secret}</p>
            <p><a href="${data.otpAuthUrl}" target="_blank" rel="noopener" class="twofa-otp-link">Open OTP URL</a></p>
            <div class="twofa-row">
              <input id="enable-code" placeholder="Enter code from app" autocomplete="one-time-code" inputmode="numeric" />
              <button id="confirm-2fa" class="btn" type="button">Enable</button>
            </div>
            <div id="enable-msg" class="msg" role="alert" aria-live="polite"></div>
          </div>
        `;
        document.getElementById('confirm-2fa').addEventListener('click', async () => {
          const code = document.getElementById('enable-code').value.trim();
          const msg = document.getElementById('enable-msg');
          if (!code) { msg.textContent = 'Enter code'; return; }
          try {
            const r2 = await fetch('/api/2fa/enable', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ secret: data.secret, code })
            });
            const d2 = await r2.json();
            if (!r2.ok) { msg.textContent = d2.error || 'Enable failed'; return; }
            msg.className = 'msg success';
            msg.textContent = '2FA enabled successfully';
          } catch (err) { console.error(err); msg.textContent = 'Request failed'; }
        });
      } catch (err) {
        console.error(err);
        alert('Request failed');
      } finally { enableBtn.disabled = false; }
    });
  }

  // ── Pi Registration ──
  const registerPiBtn = document.getElementById('register-pi');
  if (registerPiBtn) {
    registerPiBtn.addEventListener('click', async () => {
      const piId = document.getElementById('pi-id').value.trim();
      const username = document.getElementById('pi-username').value.trim();
      const phone = document.getElementById('pi-phone').value.trim();
      const gateway = document.getElementById('pi-gateway').value.trim();
      const msg = document.getElementById('pi-msg');

      msg.className = 'msg';

      if (!piId || !username || !phone) {
        msg.textContent = 'Pi ID, username, and phone are required';
        return;
      }

      registerPiBtn.disabled = true;
      msg.textContent = 'Registering...';

      try {
        const res = await fetch('/api/pi/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ piId, username, phone, gatewayEmail: gateway })
        });
        const data = await res.json();
        if (!res.ok) {
          msg.textContent = data.error || 'Registration failed';
          return;
        }
        msg.className = 'msg success';
        msg.textContent = `Pi registered successfully! (${data.piId})`;
        document.getElementById('pi-id').value = '';
        document.getElementById('pi-phone').value = '';
        document.getElementById('pi-gateway').value = '';
      } catch (err) {
        console.error(err);
        msg.textContent = 'Request failed: ' + err.message;
      } finally {
        registerPiBtn.disabled = false;
      }
    });
  }
});
