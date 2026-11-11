import { cellKey, colors, isSafe, lanes, placement, tokenCell, track } from './board.js';

const quadrants = ['1 / 1 / 7 / 7', '1 / 10 / 7 / 16', '10 / 10 / 16 / 16', '10 / 1 / 16 / 7'];

export function Board({ game, onMove, busy }) {
  const stacks = new Map();
  game?.players.forEach((tokens, player) => tokens.forEach((step, token) => {
    const cell = tokenCell(player, token, step);
    const key = cellKey(cell);
    if (!stacks.has(key)) stacks.set(key, { cell, pieces: [] });
    stacks.get(key).pieces.push({ player, token });
  }));

  return (
    <div className="board" role="group" aria-label="Plateau de Ludo">
      {quadrants.map((gridArea, player) => (
        <div className="yard" key={gridArea} style={{ gridArea, background: colors[player].tint }}>
          <span>{colors[player].name}</span>
        </div>
      ))}
      <div className="center" aria-hidden="true" />
      {track.map((cell, index) => (
        <div aria-hidden="true" className="cell" key={cellKey(cell)}
          style={{ ...placement(cell), background: index % 13 === 0 ? colors[index / 13].tint : undefined }}>
          {isSafe(index) ? '✦' : ''}
        </div>
      ))}
      {lanes.map((lane, player) => lane.map(cell => (
        <div aria-hidden="true" className="cell lane" key={cellKey(cell)}
          style={{ ...placement(cell), background: colors[player].tint }} />
      )))}
      {[...stacks.entries()].map(([key, { cell, pieces }]) => (
        <div className="stack" key={key} style={{ ...placement(cell), "--stack-size": Math.ceil(Math.sqrt(pieces.length)) }}>
          {pieces.map(({ player, token }) => {
            const playable = game.turn === player && game.legalMoves.includes(token);
            const label = colors[player].name + ', pion ' + (token + 1);
            const style = { background: colors[player].ink };
            return playable ? (
              <button key={label} className="piece playable" aria-label={'Déplacer ' + label}
                disabled={busy} style={style} onClick={() => onMove(token)}>{token + 1}</button>
            ) : <span key={label} className="piece" title={label} style={style}>{token + 1}</span>;
          })}
        </div>
      ))}
    </div>
  );
}
