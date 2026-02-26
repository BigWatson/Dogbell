class dogbellSetup {
  constructor(rootId = 'app') {
    this.root = document.getElementById(rootId) || document.body;
  }

  render() {
    this.root.innerHTML = '';
    

    const header = document.createElement('header');
    header.className = 'header';
    header.textContent = 'Setup Dogbell';

    const center = document.createElement('main');
    center.className = 'center';

    const row = document.createElement('div');
    row.className = 'button-row';

    const phoneInformation = {
      phone: "17607042102",
      message: "Your dog is at the door!",
      gatewayEmail: "17607042102@tmomail.net"
    };
    const setupBtn = this._makeButton('Set Up Dogbell', () =>
      fetch('api/doorbell/ring', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(phoneInformation)
      })
      .then(r => r.json())
      .then(data => alert(data.status === 'SMS_SENT' ? 'Text sent!' : 'Failed: ' + (data.error || JSON.stringify(data))))
      .catch(err => alert('Request error: ' + err))
    );

    row.appendChild(setupBtn);
    center.appendChild(row);

    const footer = document.createElement('footer');
    footer.className = 'footer';
    const img = document.createElement('img');
    img.className = 'placeholder-img';
    img.alt = 'Dogbell logo';
    img.src = '/TEAM4U.png';
    footer.appendChild(img);

    this.root.appendChild(header);
    this.root.appendChild(center);
    this.root.appendChild(footer);
  }

  _makeButton(text, onClick) {
    const b = document.createElement('button');
    b.className = 'btn';
    b.textContent = text;
    b.addEventListener('click', onClick);
    return b;
  }

  _navigate(path) {
    // For navigation use a direct location change for better UX
    window.location.href = path;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const page = new dogbellSetup();
  page.render();
});

export default dogbellSetup;
