document.addEventListener('DOMContentLoaded', () => {
  const signOutBtn = document.getElementById('sign-out-btn');
  if (signOutBtn) {
    signOutBtn.addEventListener('click', () => {
      fetch('/api/logout', { method: 'POST' }).finally(() => { location.href = '/login.html'; });
    });
  }

  const piIdInput     = document.getElementById('pi-id');
  const usernameInput = document.getElementById('pi-username');
  const contactsList  = document.getElementById('contacts-list');
  const addContactBtn = document.getElementById('add-contact-btn');
  const registerBtn   = document.getElementById('register-btn');
  const registerMsg   = document.getElementById('register-msg');

  const registerSection = document.getElementById('register-section');
  const successSection  = document.getElementById('success-section');
  const successDetail   = document.getElementById('success-detail');
  const testRingBtn     = document.getElementById('test-ring-btn');
  const ringMsg         = document.getElementById('ring-msg');
  const registerAnother = document.getElementById('register-another');

  let contactCounter = 0;
  let lastRegisteredContacts = [];

  // ── Add a contact row ──
  function addContactRow() {
    contactCounter++;
    const num = contactCounter;
    const row = document.createElement('div');
    row.className = 'contact-row';
    row.dataset.index = num;
    row.innerHTML = `
      <div class="contact-header">
        <span class="contact-label">Person ${num}</span>
        ${num > 1 ? '<button type="button" class="remove-contact">Remove</button>' : ''}
      </div>
      <div class="form-group contact-field">
        <input type="tel" class="contact-phone" placeholder="Phone number (e.g. 17607042102)" />
      </div>
      <div class="form-group contact-field">
        <input type="email" class="contact-gateway" placeholder="Gateway email (e.g. 17607042102@tmomail.net)" />
      </div>
    `;

    const removeBtn = row.querySelector('.remove-contact');
    if (removeBtn) {
      removeBtn.addEventListener('click', () => {
        row.remove();
        renumberContacts();
      });
    }

    contactsList.appendChild(row);
  }

  function renumberContacts() {
    const rows = contactsList.querySelectorAll('.contact-row');
    rows.forEach((row, i) => {
      row.querySelector('.contact-label').textContent = 'Person ' + (i + 1);
      // Show remove button on all except first
      const removeBtn = row.querySelector('.remove-contact');
      if (removeBtn) removeBtn.classList.toggle('is-hidden', i === 0);
    });
  }

  // Gather contacts from the form
  function getContacts() {
    const rows = contactsList.querySelectorAll('.contact-row');
    const contacts = [];
    rows.forEach(row => {
      const phone = row.querySelector('.contact-phone').value.trim();
      const gateway = row.querySelector('.contact-gateway').value.trim();
      if (phone) {
        contacts.push({ phone, gatewayEmail: gateway });
      }
    });
    return contacts;
  }

  // Start with one contact row
  addContactRow();

  addContactBtn.addEventListener('click', () => addContactRow());

  // ── Register ──
  registerBtn.addEventListener('click', async () => {
    const piId     = piIdInput.value.trim();
    const username = usernameInput.value.trim();
    const contacts = getContacts();

    registerMsg.className = 'msg';
    registerMsg.textContent = '';

    if (!piId) { registerMsg.textContent = 'Please enter your Pi device ID.'; return; }
    if (!username) { registerMsg.textContent = 'Please enter your username.'; return; }
    if (contacts.length === 0) { registerMsg.textContent = 'Please add at least one phone number.'; return; }

    registerBtn.disabled = true;
    registerMsg.textContent = 'Registering...';

    try {
      const res = await fetch('/api/pi/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ piId, username, contacts })
      });
      const data = await res.json();

      if (!res.ok) {
        registerMsg.textContent = data.error || 'Registration failed.';
        return;
      }

      // Save contacts for test ring
      lastRegisteredContacts = contacts;

      const numLabel = contacts.length === 1 ? '1 phone number' : contacts.length + ' phone numbers';
      successDetail.textContent =
        'Pi "' + data.piId + '" registered to ' + data.username + ' with ' + numLabel + '. Credentials sent to the device.';
      registerSection.classList.add('is-hidden');
      successSection.classList.remove('is-hidden');
      ringMsg.textContent = '';
    } catch (err) {
      registerMsg.textContent = 'Request failed: ' + err.message;
    } finally {
      registerBtn.disabled = false;
      registerMsg.className = 'msg';
    }
  });

  // ── Test Ring (sends to all contacts) ──
  testRingBtn.addEventListener('click', async () => {
    if (lastRegisteredContacts.length === 0) return;

    testRingBtn.disabled = true;
    testRingBtn.textContent = 'Sending...';
    ringMsg.textContent = '';
    ringMsg.className = 'msg';

    let sent = 0;
    let failed = 0;

    for (const contact of lastRegisteredContacts) {
      try {
        const res = await fetch('/api/doorbell/ring', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            phone: contact.phone,
            message: 'Your dog is at the door!',
            gatewayEmail: contact.gatewayEmail
          })
        });
        const data = await res.json();
        if (data.status === 'SMS_SENT') sent++;
        else failed++;
      } catch (err) {
        failed++;
      }
    }

    if (failed === 0) {
      ringMsg.className = 'msg success';
      ringMsg.textContent = 'Test texts sent to all ' + sent + ' number(s)! Check your phones.';
    } else {
      ringMsg.textContent = sent + ' sent, ' + failed + ' failed.';
    }

    testRingBtn.textContent = 'Send Test Ring to All';
    testRingBtn.disabled = false;
  });

  // ── Register Another ──
  registerAnother.addEventListener('click', (e) => {
    e.preventDefault();
    successSection.classList.add('is-hidden');
    registerSection.classList.remove('is-hidden');
    registerMsg.textContent = '';
    // Reset form
    piIdInput.value = '';
    usernameInput.value = '';
    contactsList.innerHTML = '';
    contactCounter = 0;
    addContactRow();
  });
});
