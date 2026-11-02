# Slightly wider gameplay camera — 2026-11-01

Owner requested a little zoom out. Gameplay worldZoom now reduces the existing display scale by0.25, clamped to native1x. Fullscreen1920×1080 and maximized1906×1071 change1.5x to1.25x:20% more world width/height.1440p changes2x to1.75x.720p/native windows remain1x. HUD/menu scaling is independent. Nearest-neighbour rendering retained; at1.25x some art pixels occupy alternating2/3 screen pixels instead of uniform3px blocks.

Java17/-Xlint compilation passed. CameraZoomTest, FullscreenViewportTest and DialoguePresentationTest passed; packaged CameraZoomTest reran from/tmp. Actual Java1920×1080 capture reviewed, with multi-viewport/state renders in `/home/ghiankylledomingo/tmp/b2bj-camera-out-2026-11-01/render`. Only B2BJ.class changed in the JAR; all other entries byte-identical. Installed SHA256 `608480929f091a375482b2f8f498ffd583f1eada66d70e7d6caeab2b526eb946`. Save hash unchanged across install. Scoped diff whitespace clean. No full suite/manual combat playthrough, native restart, commit or push.

Backup, scoped diff, logs, package audit and install record: `/home/ghiankylledomingo/tmp/b2bj-camera-out-2026-11-01`. Saved preview: `docs/art-review/camera-out-2026-11-01/game-1920.png`.
