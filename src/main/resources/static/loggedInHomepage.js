import about from "./about.js";

class HomePage {

  static dogbellSetup = false; //might want to move location of this variable

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

    if (HomePage.dogbellSetup === false) {
      const dogbellSetupBtn = this._makeButton('Set Up Dogbell', () => this._navigate('/dogbellSetup.html'), false);
      row.appendChild(dogbellSetupBtn);
    }
    const aboutBtn = this._makeButton('About Us', () => this._navigate('/about.html'), false);
    const signOutBtn = this._makeButton('Sign Out', () => this._navigate('/login.html'), true);
    

    row.appendChild(aboutBtn);
    row.appendChild(signOutBtn);

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

  _makeButton(text, onClick, isSignOut) {
    const b = document.createElement('button');
    b.className = 'btn';
    b.textContent = text;
    if (!isSignOut) {
    b.addEventListener('click', onClick);
    } 
    else {
      b.addEventListener('click', () => {
      // clear logged-in state
      try { about.isLoggedIn = false; } catch (e) { /* ignore */ }
      try { window.isLoggedIn = false; } catch (e) { /* ignore */ }
      try { localStorage.removeItem('isLoggedIn'); } catch (e) { /* ignore */ }
      this._navigate('/login.html');
      });
    }
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
