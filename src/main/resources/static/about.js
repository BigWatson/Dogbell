class about {

    static isLoggedIn = false; // fallback read at render time from localStorage

    constructor(rootId = 'app') {
    this.root = document.getElementById(rootId) || document.body;
    }
    render() {
        this.root.innerHTML = '';
        
        const main = document.createElement('main');

        let returnBtn;
        // determine logged-in state: prefer module flag, fallback to localStorage
        let loggedIn = about.isLoggedIn;
        try {
            if (!loggedIn && localStorage && localStorage.getItem('isLoggedIn') === '1') loggedIn = true;
        } catch (e) { /* ignore when localStorage unavailable */ }

        if (loggedIn) {
            returnBtn = this._makeButton('Return to Home', () => this._navigate('/loggedInHomepage.html'));
        } else {
            returnBtn = this._makeButton('Return to Home', () => this._navigate('/homepage.html'));
        }
       
        main.appendChild(returnBtn);
        this.root.appendChild(main);
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
  const page = new about();
  page.render();
});

export default about;