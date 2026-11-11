import { useRef, useState } from 'react';
import { Board } from './Board.jsx';
import { colors } from './board.js';

const api = '/api/games';

export function LocalGame() {
  const [count, setCount] = useState(2);
  const [session, setSession] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const inFlight = useRef(false);
  const game = session?.state;

  async function post(path, body) {
    if (inFlight.current) return;
    inFlight.current = true;
    setBusy(true);
    setError('');
    try {
      const response = await fetch(api + path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        const details = await response.json().catch(() => ({}));
        if (session && response.status === 409) {
          const current = await fetch(api + '/' + session.id);
          if (current.ok) setSession(await current.json());
        }
        throw new Error(details.message || 'Le serveur a renvoyé une erreur.');
      }
      setSession(await response.json());
    } catch (failure) {
      setError(failure instanceof TypeError ? 'Connexion au serveur impossible.' : failure.message);
    } finally {
      inFlight.current = false;
      setBusy(false);
    }
  }

  function action(name, extra = {}) {
    return post('/' + session.id + '/' + name, { revision: game.revision, ...extra });
  }

  function newGame() {
    if (game && game.winner === null && !window.confirm('Recommencer la partie en cours ?')) return;
    post('', { players: count });
  }

  const current = game ? colors[game.winner ?? game.turn] : null;
  return (
    <div className="layout">
      <Board game={game} onMove={token => action('move', { token })} busy={busy} />
      <aside>
        <section className="panel">
          <h2>Partie</h2>
          <div className="setup">
            <label htmlFor="players">Joueurs</label>
            <select id="players" value={count} disabled={busy} onChange={event => setCount(Number(event.target.value))}>
              {[2, 3, 4].map(value => <option key={value} value={value}>{value}</option>)}
            </select>
            <button className="secondary" onClick={newGame} disabled={busy}>Nouvelle partie</button>
          </div>
          {game && <>
            <div className="turn" aria-live="polite" style={{ '--ink': current.ink, '--tint': current.tint }}>
              <span className="die" aria-label={game.lastRoll ? 'Dernier dé : ' + game.lastRoll : 'Dé non lancé'}>
                {game.lastRoll ?? '–'}
              </span>
              <div>
                <strong>{game.winner === null ? 'Tour de ' + current.name : current.name + ' a gagné !'}</strong>
                {game.winner === null && <div>{game.die === null ? 'Lance le dé.' : 'Choisis un pion à déplacer.'}</div>}
              </div>
            </div>
            <button className="primary" disabled={busy || game.die !== null || game.winner !== null}
              onClick={() => action('roll')}>Lancer le dé</button>
            {game.legalMoves.length > 0 && (
              <div className="moves" aria-label="Pions déplaçables">
                {game.legalMoves.map(token => (
                  <button key={token} className="secondary" disabled={busy}
                    onClick={() => action('move', { token })}>Pion {token + 1}</button>
                ))}
              </div>
            )}
          </>}
          {error && <p className="error" role="alert">{error}</p>}
        </section>
        <section className="panel rules">
          <h2>Règles</h2>
          <ul>
            <li>Un 6 fait sortir un pion ou avancer un pion déjà en jeu, puis donne un lancer supplémentaire.</li>
            <li>Sur une case sans étoile, un pion qui arrive renvoie les adversaires présents à leur base.</li>
            <li>Atteins l’arrivée avec le nombre exact. Le premier à rentrer ses quatre pions gagne.</li>
          </ul>
        </section>
      </aside>
    </div>
  );
}
