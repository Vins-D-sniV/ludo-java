import assert from 'node:assert/strict';
import test from 'node:test';
import { cellKey, isSafe, lanes, tokenCell, track } from './board.js';

test('the 52 shared cells are distinct and separate from the finish lanes', () => {
  assert.equal(new Set(track.map(cellKey)).size, 52);
  const finishCells = lanes.flat();
  assert.equal(new Set([...track, ...finishCells].map(cellKey)).size, 76);
});

test('each color has a safe start, a continuous path, and its own finish', () => {
  for (let player = 0; player < 4; player++) {
    assert.deepEqual(tokenCell(player, 0, 0), track[13 * player]);
    assert.ok(isSafe(13 * player));
    assert.deepEqual(tokenCell(player, 0, 56), lanes[player][5]);
    assert.equal(new Set(Array.from({ length: 4 }, (_, token) =>
      cellKey(tokenCell(player, token, -1)))).size, 4);
    for (let step = 1; step <= 56; step++) {
      const [row, column] = tokenCell(player, 0, step);
      const [previousRow, previousColumn] = tokenCell(player, 0, step - 1);
      const distance = Math.abs(row - previousRow) + Math.abs(column - previousColumn);
      assert.ok(distance === 1 || (distance === 2 && row !== previousRow && column !== previousColumn));
    }
  }
});
