import { useState } from 'react';
import { createRoot } from 'react-dom/client';
import { LocalGame } from './LocalGame.jsx';
import { invitedRoom, OnlineGame } from './OnlineGame.jsx';
import './style.css';

function App() {
  const [mode, setMode] = useState(() => invitedRoom() ? 'online' : 'local');

  return (
    <main>
      <header>
        <h1>Ludo</h1>
        <p>{mode === 'local' ? '2 à 4 joueurs sur le même écran' : '2 joueurs sur deux écrans'}</p>
      </header>
      <nav className="mode" aria-label="Mode de jeu">
        <button className="secondary" aria-pressed={mode === 'local'} onClick={() => setMode('local')}>Même écran</button>
        <button className="secondary" aria-pressed={mode === 'online'} onClick={() => setMode('online')}>En ligne à deux</button>
      </nav>
      <div hidden={mode !== 'local'}><LocalGame /></div>
      <div hidden={mode !== 'online'}><OnlineGame /></div>
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
