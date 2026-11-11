export const colors = [
  { name: 'Rouge', ink: '#af3445', tint: '#f7c6c9' },
  { name: 'Vert', ink: '#236c52', tint: '#bde3cd' },
  { name: 'Jaune', ink: '#906006', tint: '#ffe0a0' },
  { name: 'Bleu', ink: '#245f9a', tint: '#bfdaf3' },
];

function rotate([row, column], turns) {
  for (let turn = 0; turn < turns; turn++) {
    [row, column] = [column, 14 - row];
  }
  return [row, column];
}

const quarter = [
  [6, 1], [6, 2], [6, 3], [6, 4], [6, 5],
  [5, 6], [4, 6], [3, 6], [2, 6], [1, 6], [0, 6], [0, 7], [0, 8],
];

export const track = colors.flatMap((_, player) => quarter.map(cell => rotate(cell, player)));
export const isSafe = index => index % 13 === 0 || index % 13 === 8;
export const cellKey = cell => cell.join(',');
export const lanes = colors.map((_, player) =>
  Array.from({ length: 6 }, (_, step) => rotate([7, step + 1], player)));
const yard = [[2, 2], [2, 3], [3, 2], [3, 3]];

export function tokenCell(player, token, step) {
  if (step < 0) return rotate(yard[token], player);
  if (step <= 50) return track[(13 * player + step) % track.length];
  return lanes[player][step - 51];
}

export function placement([row, column]) {
  return { gridRow: row + 1, gridColumn: column + 1 };
}

export function tokenPath(player, token, from, to) {
  const start = tokenCell(player, token, from);
  if (from < 0 || to <= from || to - from > 6) {
    return [start, tokenCell(player, token, to)];
  }
  return Array.from({ length: to - from + 1 }, (_, index) =>
    tokenCell(player, token, from + index));
}
