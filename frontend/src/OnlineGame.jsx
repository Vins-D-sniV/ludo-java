import { useEffect, useRef, useState } from 'react';
import { colors } from './board.js';
import { Board } from './Board.jsx';

const api = '/api/online-games';
const roomParameter = 'partie';
const storageKey = id => 'ludo-online:' + id;
const joinKey = id => 'ludo-online-join:' + id;

export function invitedRoom() {
  return new URL(window.location.href).searchParams.get(roomParameter);
}

function invitation(id) {
  const url = new URL(window.location.href);
  url.searchParams.delete(roomParameter);
  if (id) url.searchParams.set(roomParameter, id);
  return url;
}

function randomToken() {
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 15) | 64;
  bytes[8] = (bytes[8] & 63) | 128;
  const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('');
  return [hex.slice(0, 8), hex.slice(8, 12), hex.slice(12, 16), hex.slice(16, 20), hex.slice(20)].join('-');
}

async function request(path, { token, body, method = 'GET', signal } = {}) {
  const headers = {};
  if (token) headers['X-Player-Token'] = token;
  if (body) headers['Content-Type'] = 'application/json';
  const response = await fetch(api + path, {
    method, headers, signal, cache: 'no-store',
    body: body && JSON.stringify(body),
  });
  if (response.ok) return response.json();
  const details = await response.json().catch(() => ({}));
  const error = new Error(details.message || 'Impossible de continuer cette partie.');
  error.status = response.status;
  throw error;
}

function newest(previous, next) {
  if (!previous || previous.id !== next.id) return next;
  if ((previous.ready && !next.ready) || previous.state.revision > next.state.revision) return previous;
  return next;
}

export function OnlineGame() {
  const [room, setRoom] = useState(invitedRoom);
  const [token, setToken] = useState(() => room && sessionStorage.getItem(storageKey(room)));
  const [view, setView] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [connection, setConnection] = useState('');
  const [expired, setExpired] = useState(false);
  const [copied, setCopied] = useState(false);
  const pending = useRef(false);

  useEffect(() => {
    if (!room || !token) return;
    let stopped = false;
    let timer;
    const controller = new AbortController();

    async function refresh() {
      try {
        const current = await request('/' + encodeURIComponent(room), { token, signal: controller.signal });
        if (stopped) return;
        setView(previous => newest(previous, current));
        setConnection('');
      } catch (failure) {
        if (stopped) return;
        if ([400, 403, 404].includes(failure.status)) {
          setConnection(failure.message);
          setExpired(true);
          return;
        }
        setConnection('Connexion interrompue. Reconnexion en cours…');
      }
      if (!stopped) timer = setTimeout(refresh, 1000);
    }

    refresh();
    return () => {
      stopped = true;
      clearTimeout(timer);
      controller.abort();
    };
  }, [room, token]);

  async function perform(action) {
    if (pending.current) return;
    pending.current = true;
    setBusy(true);
    setError('');
    try {
      await action();
    } catch (failure) {
      setError(failure instanceof TypeError ? 'Connexion au serveur impossible. Réessaie.' : failure.message);
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }

  function connect(ticket) {
    const id = ticket.game.id;
    sessionStorage.setItem(storageKey(id), ticket.token);
    sessionStorage.removeItem(joinKey(id));
    history.replaceState(null, '', invitation(id));
    setView(ticket.game);
    setRoom(id);
    setToken(ticket.token);
  }

  function create() {
    perform(async () => connect(await request('', { method: 'POST' })));
  }

  function join() {
    perform(async () => {
      const candidate = sessionStorage.getItem(joinKey(room)) ?? randomToken();
      sessionStorage.setItem(joinKey(room), candidate);
      connect(await request('/' + encodeURIComponent(room) + '/join', {
        method: 'POST', token: candidate,
      }));
    });
  }

  function play(action, extra = {}) {
    perform(async () => {
      const current = await request('/' + room + '/' + action, {
        method: 'POST', token, body: { revision: view.state.revision, ...extra },
      });
      setView(previous => newest(previous, current));
    });
  }

  function leave() {
    if (room && expired) sessionStorage.removeItem(storageKey(room));
    history.replaceState(null, '', invitation(null));
    setRoom(null);
    setToken(null);
    setView(null);
    setError('');
    setConnection('');
    setExpired(false);
    setCopied(false);
  }

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(invitation(room).href);
      setCopied(true);
    } catch {
      document.getElementById('invite-link')?.select();
    }
  }

  const state = view?.state;
  const myTurn = view?.ready && state.turn === view.player && state.winner === null;
  const canPlay = myTurn && !busy && !connection;
  const current = state && colors[state.winner ?? state.turn];

  return (
    <div className="layout">
      <Board game={state} busy={!canPlay} onMove={piece => play('move', { token: piece })} />
      <aside>
        <section className="panel">
          <h2>Partie en ligne</h2>
          {!room && <button className="primary" disabled={busy} onClick={create}>Créer une partie</button>}
          {room && !token && <>
            <p>Tu as reçu une invitation à jouer.</p>
            <button className="primary" disabled={busy} onClick={join}>Rejoindre la partie</button>
          </>}
          {token && !view && !connection && <p role="status">Connexion à la partie…</p>}
          {view && !expired && <>
            <p>Tu joues les pions <strong>{colors[view.player].name.toLowerCase()}</strong>.</p>
            {!view.ready ? (
              <div className="invite">
                <p role="status">En attente du deuxième joueur.</p>
                <label htmlFor="invite-link">Lien d’invitation</label>
                <input id="invite-link" readOnly value={invitation(room).href} onFocus={event => event.target.select()} />
                <button className="secondary" onClick={copyLink}>{copied ? 'Lien copié' : 'Copier le lien'}</button>
              </div>
            ) : <>
              <div className="turn" aria-live="polite" style={{ '--tint': current.tint, '--ink': current.ink }}>
                <span className="die" aria-label={state.lastRoll ? 'Dernier dé : ' + state.lastRoll : 'Dé non lancé'}>
                  {state.lastRoll ?? '–'}
                </span>
                <div>
                  <strong>{state.winner === null
                    ? (myTurn ? 'À toi de jouer' : 'Tour de ' + current.name)
                    : (state.winner === view.player ? 'Tu as gagné !' : current.name + ' a gagné !')}</strong>
                  {myTurn && <div>{state.die === null ? 'Lance le dé.' : 'Choisis un pion à déplacer.'}</div>}
                </div>
              </div>
              <button className="primary" disabled={!canPlay || state.die !== null}
                onClick={() => play('roll')}>Lancer le dé</button>
              {myTurn && state.legalMoves.length > 0 && (
                <div className="moves" aria-label="Pions déplaçables">
                  {state.legalMoves.map(piece => (
                    <button key={piece} className="secondary" disabled={!canPlay}
                      onClick={() => play('move', { token: piece })}>Pion {piece + 1}</button>
                  ))}
                </div>
              )}
            </>}
          </>}
          {connection && <p className="error" role="status">{connection}</p>}
          {error && <p className="error" role="alert">{error}</p>}
          {room && <button className="secondary leave" disabled={busy} onClick={leave}>
            {view && !expired ? 'Quitter cette partie sur cet écran' : 'Retour'}
          </button>}
        </section>
      </aside>
    </div>
  );
}
