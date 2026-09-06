# Pillar Collision TDD Evidence

## User journey

As a player, I want collision to match the visible pillar base so transparent sprite pixels do not act as an invisible wall.

## RED

`rtk java -ea -cp out RuinedOutpostMapTest`

Failed first with `transparent pixels beside pillar must not block player`, then with
`transparent pixels below pillar must not block player` after adding screenshot regression coverage.
Movement-level test also failed with `player feet should reach visible pillar base` while player collision still used the full sprite radius.

## GREEN

`rtk javac -Xlint:all -d out src/*.java test/*.java`

All tests passed with assertions enabled:

- `PlayerMovementTest`
- `SlimeAnimationTest`
- `RuinedOutpostMapTest`
- `TerrainRenderingTest`

## Guarantee

Pillar center remains solid while player collision can pass beside and below transparent pixels. Dash and normal movement use a 24-pixel foot circle with a 24-pixel downward offset, allowing natural top-down sprite overlap while keeping the visible base solid. Pillar dimensions use measured sprite alpha bounds: horizontal half-width `120`, lower edge offset `294`.

## Known gap

No automated screenshot comparison. Final feel still needs one manual pass around all four sides of pillar.

Git checkpoints were skipped because target files already contained uncommitted work from the active game-development session.
