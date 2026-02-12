class HomePage {
  constructor(rootId = 'app') {
    this.root = document.getElementById(rootId) || document.body;
  }

  render() {
    this.root.innerHTML = '';

    const header = document.createElement('header');
    header.className = 'header';
    header.textContent = 'Team 4 U';

    const center = document.createElement('main');
    center.className = 'center';

    const row = document.createElement('div');
    row.className = 'button-row';

    const signupBtn = this._makeButton('Sign Up', () => this._navigate('/signup.html'));
    const loginBtn = this._makeButton('Login', () => this._navigate('/login.html'));
    const aboutBtn = this._makeButton('About Us', () => this._navigate('/about.html'));

    row.appendChild(signupBtn);
    row.appendChild(loginBtn);
    row.appendChild(aboutBtn);

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
  const page = new HomePage();
  page.render();
});

export default HomePage;
