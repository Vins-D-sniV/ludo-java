import { useLayoutEffect, useRef } from 'react';
import { cellKey, colors, isSafe, lanes, placement, tokenCell, tokenPath, track } from './board.js';

const quadrants = ['1 / 1 / 7 / 7', '1 / 10 / 7 / 16', '10 / 10 / 16 / 16', '10 / 1 / 16 / 7'];

function arrangePieces(players) {
  const pieces = [];
  const stacks = new Map();
  players?.forEach((tokens, player) => tokens.forEach((step, token) => {
    const cell = tokenCell(player, token, step);
    const key = cellKey(cell);
    const piece = { player, token, step, cell, id: player + ':' + token };
    if (!stacks.has(key)) stacks.set(key, []);
    stacks.get(key).push(piece);
    pieces.push(piece);
  }));

  for (const stack of stacks.values()) {
    const size = Math.ceil(Math.sqrt(stack.length));
    stack.forEach((piece, index) => {
      piece.size = size;
      piece.row = Math.floor(index / size);
      piece.column = index % size;
    });
  }
  return pieces;
}

function centerOffset(piece, axis) {
  return (piece[axis] + 0.5) / piece.size - 0.5;
}

function Piece({ piece, playable, busy, onMove }) {
  const element = useRef(null);
  const previous = useRef(piece);
  const animation = useRef(null);
  const { player, token, step, cell, size, row, column } = piece;
  const label = colors[player].name + ', pion ' + (token + 1);

  useLayoutEffect(() => {
    const before = previous.current;
    previous.current = piece;
    if (before.step === step && before.row === row && before.column === column && before.size === size) return;

    const slot = element.current;
    const { width, height } = slot.getBoundingClientRect();
    if (!width || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      animation.current?.cancel();
      return;
    }

    const { rowGap, columnGap } = getComputedStyle(slot.parentElement);
    const strideX = width + parseFloat(columnGap);
    const strideY = height + parseFloat(rowGap);
    const path = tokenPath(player, token, before.step, step);
    const frames = path.map(([y, x], index) => {
      const first = index === 0;
      const last = index === path.length - 1;
      const offsetX = first ? centerOffset(before, 'column') : last ? centerOffset(piece, 'column') : 0;
      const offsetY = first ? centerOffset(before, 'row') : last ? centerOffset(piece, 'row') : 0;
      return {
        x: (x - cell[1]) * strideX + (offsetX - centerOffset(piece, 'column')) * width,
        y: (y - cell[0]) * strideY + (offsetY - centerOffset(piece, 'row')) * height,
      };
    });
    if (animation.current?.playState === 'running') {
      const transform = new DOMMatrixReadOnly(getComputedStyle(slot).transform);
      frames[0].x += transform.m41;
      frames[0].y += transform.m42;
    }
    animation.current?.cancel();
    animation.current = slot.animate(
      frames.map(({ x, y }) => ({ transform: 'translate(' + x + 'px, ' + y + 'px)' })),
      { duration: Math.min(500, 120 + 60 * (frames.length - 1)), easing: 'ease-in-out' },
    );
  }, [piece, player, token, step, cell, size, row, column]);

  return (
    <div ref={element} className="piece-slot" style={{ ...placement(cell), '--stack-size': size }}>
      <button type="button" className={'piece' + (playable ? ' playable' : '')}
        title={label} aria-label={playable ? 'Déplacer ' + label : label}
        disabled={!playable || busy} onClick={() => onMove(token)}
        style={{ background: colors[player].ink, gridRow: row + 1, gridColumn: column + 1 }}>
        {token + 1}
      </button>
    </div>
  );
}

export function Board({ game, onMove, busy }) {
  return (
    <div className="board" role="group" aria-label="Plateau de Ludo">
      {quadrants.map((gridArea, player) => (
        <div className="yard" key={gridArea} style={{ gridArea, background: colors[player].tint }}>
          <div className="yard-slots" aria-hidden="true" />
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
      {arrangePieces(game?.players).map(piece => (
        <Piece key={piece.id} piece={piece} busy={busy} onMove={onMove}
          playable={game.turn === piece.player && game.legalMoves.includes(piece.token)} />
      ))}
    </div>
  );
}
