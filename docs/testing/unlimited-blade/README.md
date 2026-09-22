# Unlimited Blade form — 2026-09-16

Enable **Menu > Admin / Testing > Unlimited Blade form**, resume, then press Q.
Ichor stays full until disabled. Health damage and cooldowns still apply.
Continue or New Game clears the setting; normal saves remain protected during testing.

- `Player` owns the flag and stops timer, skill and hit Ichor costs.
- `RuinedOutpostGame` protects saves and keeps the flag across test travel/reset and death.
- `DebugPanel` exposes the toggle; `B2BJ` shows UNLIMITED on the HUD.
- All 67 assertion programs and packaged asset checks passed (`./build.sh`).
- `PlayerCheatsTest` covers drain, disable/re-enable, death, resets and save isolation.
- `CampaignMenuTest` covers the embedded checkbox and reopening the panel.
- `LiveCampaignSmoke` uses native F1 / Alt+U, Q, combo and charge inputs, waits beyond normal expiry, then verifies Continue clears the cheat. It uses an isolated save.

[Admin control](admin.png) · [Live unlimited HUD](hud.png)
