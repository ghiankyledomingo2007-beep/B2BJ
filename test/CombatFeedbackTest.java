public final class CombatFeedbackTest {
    public static void main(String[] args) {
        combatEventsProduceAndThenClearScreenFeedback();
        System.out.println("CombatFeedbackTest passed");
    }

    private static void combatEventsProduceAndThenClearScreenFeedback() {
        CombatFeedback feedback = new CombatFeedback();

        feedback.enemyHit(false);
        assert feedback.hitAlpha() > 0;
        assert feedback.shakeStrength() > 0;

        feedback.playerHit();
        assert feedback.damageAlpha() > 0;

        feedback.transformed();
        assert feedback.ichorAlpha() > 0;

        feedback.guardianSlam();
        assert feedback.shakeStrength() >= 8;

        feedback.victory();
        assert feedback.victoryAlpha() > 0;

        feedback.update(2);

        assert feedback.hitAlpha() == 0;
        assert feedback.damageAlpha() == 0;
        assert feedback.ichorAlpha() == 0;
        assert feedback.victoryAlpha() == 0;
        assert feedback.shakeStrength() == 0;
    }
}
