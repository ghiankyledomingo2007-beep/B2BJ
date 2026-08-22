# B2BJ

A Java 2D game prototype with animated slime movement, camera tracking, tiled terrain, and collision-aware map props.

## Run

Requires Java 17 or newer.

```bash
mkdir -p out
javac -d out src/*.java
java -cp out B2BJ
```

Move with **WASD** or the **arrow keys**.

## Tests

```bash
javac -Xlint:all -d out src/*.java test/*.java
java -cp out PlayerMovementTest
java -cp out SlimeAnimationTest
java -cp out RuinedOutpostMapTest
```
