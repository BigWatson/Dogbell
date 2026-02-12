document.addEventListener('DOMContentLoaded', () => {
  const enableBtn = document.getElementById('enable-2fa');
  const area = document.getElementById('2fa-area');
  if (!enableBtn) return;
  enableBtn.addEventListener('click', async () => {
    const username = prompt('Enter your username to enable 2FA:');
    if (!username) return;
    enableBtn.disabled = true;
    try {
      const res = await fetch('/api/2fa/setup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username })
      });
      const data = await res.json();
      if (!res.ok) { alert(data.error || 'Failed to setup 2FA'); return; }
      // show QR/secret and allow user to verify code
      area.innerHTML = `
        <div style="color:#fff;max-width:560px;margin:0 auto;text-align:center;">
          <p>Scan this QR or enter secret into your authenticator app:</p>
          <p style="font-family:monospace;background:#111;padding:8px;border-radius:6px;display:inline-block;">${data.secret}</p>
          <p><a href="${data.otpAuthUrl}" target="_blank" style="color:#fff">Open OTP URL</a></p>
          <div style="margin-top:12px;display:flex;gap:8px;justify-content:center;">
            <input id="enable-code" placeholder="Enter code from app" />
            <button id="confirm-2fa" class="btn">Enable</button>
          </div>
          <div id="enable-msg" class="msg"></div>
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
            body: JSON.stringify({ username, secret: data.secret, code })
          });
          const d2 = await r2.json();
          if (!r2.ok) { msg.textContent = d2.error || 'Enable failed'; return; }
          msg.style.color = '#8f8';
          msg.textContent = '2FA enabled successfully';
        } catch (err) { console.error(err); msg.textContent = 'Request failed'; }
      });
    } catch (err) {
      console.error(err);
      alert('Request failed');
    } finally { enableBtn.disabled = false; }
  });
});
