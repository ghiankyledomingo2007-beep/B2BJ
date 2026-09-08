/** Fifty named deterministic gameplay scenarios; not fifty unassisted playthroughs. */
public final class DebugScenarioTest {
    private static int passed;
    @FunctionalInterface private interface Scenario { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        run("Legacy game rejects admin mutations", DebugSafetyTest::legacyRejectsCheats);
        run("Title rejects admin mutations", DebugSafetyTest::titleRejectsCheats);
        run("NPC dialogue rejects admin mutations", DebugSafetyTest::dialogueRejectsCheats);
        run("Dead player rejects admin mutations", DebugSafetyTest::deathRejectsCheats);
        run("Final choice rejects admin mutations", () -> DebugSafetyTest.endingBoundary(false));
        run("Completed campaign rejects admin mutations", () -> DebugSafetyTest.endingBoundary(true));
        run("Active campaign exposes read-only availability", DebugSafetyTest::activeAvailability);
        run("Paused campaign accepts admin toggles", DebugSafetyTest::pausedAvailability);
        run("Invalid warp IDs leave state untouched", DebugSafetyTest::invalidWarps);
        run("God mode rejects lethal health damage", DebugSafetyTest::godHealth);
        run("God mode rejects Blade hit Ichor penalty", DebugSafetyTest::godIchorPenalty);
        run("God mode preserves natural transformation drain", DebugSafetyTest::godNaturalDrain);
        run("Disabling god mode restores incoming damage", DebugSafetyTest::godCanBeDisabled);
        run("Disabling one-shot restores knight armor", DebugSafetyTest::oneShotCanBeDisabled);
        run("Refill restores health and Ichor", DebugSafetyTest::refillHealthAndIchor);
        run("Refill resets all combat cooldowns", DebugSafetyTest::refillCooldowns);
        run("Shard button grants exactly twenty-five", DebugSafetyTest::addsTwentyFiveShards);
        run("Turning toggles off keeps sandbox sticky", DebugSafetyTest::stickyAfterTogglesOff);
        run("Debug rewards boss clear and ending preserve disk save", DebugSafetyTest::diskRemainsPristine);
        run("Title Continue restores pristine cached save", DebugSafetyTest::cachedContinueRemainsPristine);
        run("Outpost camp warp is collision-free", () -> DebugSafetyTest.warp(0, false));
        run("Forest camp warp is collision-free", () -> DebugSafetyTest.warp(1, false));
        run("Catacombs camp warp is collision-free", () -> DebugSafetyTest.warp(2, false));
        run("Citadel camp warp is collision-free", () -> DebugSafetyTest.warp(3, false));
        run("Outpost boss warp preserves pause and wakes Warden", () -> DebugSafetyTest.warp(0, true));
        run("Forest boss warp preserves pause and wakes Briarheart", () -> DebugSafetyTest.warp(1, true));
        run("Catacombs boss warp preserves pause and wakes Oathkeeper", () -> DebugSafetyTest.warp(2, true));
        run("Citadel boss warp preserves pause and wakes Golem", () -> DebugSafetyTest.warp(3, true));
        run("Encounter reset restores enemies and defeated boss", DebugSafetyTest::resetEncounter);
        run("Normal portal travel retains sandbox flags", DebugSafetyTest::travelInSandbox);
        run("One-shot Water Slash defeats raised knight shield", () -> DebugSafetyTest.knightWeapon(DebugSafetyTest.Weapon.WATER));
        run("One-shot Tide Wave defeats raised knight shield", () -> DebugSafetyTest.knightWeapon(DebugSafetyTest.Weapon.TIDE));
        run("One-shot Blob dash defeats raised knight shield", () -> DebugSafetyTest.knightWeapon(DebugSafetyTest.Weapon.DASH));
        run("One-shot Blade cut defeats raised knight shield", () -> DebugSafetyTest.knightWeapon(DebugSafetyTest.Weapon.BLADE));
        run("One-shot Ichor Crescent defeats raised knight shield", () -> DebugSafetyTest.knightWeapon(DebugSafetyTest.Weapon.CRESCENT));
        run("One-shot Riposte defeats knight through real impact", () -> DebugSafetyTest.knightWeapon(DebugSafetyTest.Weapon.RIPOSTE));
        run("Connected water one-shot defeats armored awake boss", () -> DebugSafetyTest.awakeBoss(false));
        run("Connected dash one-shot defeats armored awake boss", () -> DebugSafetyTest.awakeBoss(true));
        run("Remote cast cannot kill dormant boss", DebugSafetyTest::dormantBossUntouched);
        run("Real terrain stops one-shot projectile", DebugSafetyTest::wallBlocksShot);
        run("Wrong-facing one-shot Blade cut cannot connect", DebugSafetyTest::wrongFacingCannotHit);
        run("Expired water range cannot hit distant target", DebugSafetyTest::beyondRangeCannotHit);
        run("Dash cancels pending one-shot water windup", DebugSafetyTest::dashCancelsPendingStrike);
        run("Pause freezes pending one-shot until resume", DebugSafetyTest::pauseFreezesPendingStrike);
        run("Reversion cancels pending Blade strike", DebugSafetyTest::reversionCancelsPendingBlade);
        run("Death cancels pending one-shot release", DebugSafetyTest::deathCancelsPendingStrike);
        run("Warp clears transient projectiles and pending combat", DebugSafetyTest::warpClearsTransientCombat);
        run("Focus loss clears held movement before resume", DebugSafetyTest::focusClearsMovement);
        run("Actual admin checkbox callbacks apply while paused", DebugSafetyTest::panelCallbacks);
        run("Death retry remains sandboxed until normal Continue", DebugSafetyTest::deathRetryStaysSandboxed);
        assert passed == 50 : "expected exactly fifty named scenarios, got " + passed;
        System.out.println("DebugScenarioTest passed: 50/50 named deterministic gameplay scenarios");
    }

    private static void run(String name, Scenario scenario) throws Exception {
        scenario.run();
        System.out.printf(java.util.Locale.ROOT, "SCENARIO %02d PASS %s%n", ++passed, name);
    }
}
