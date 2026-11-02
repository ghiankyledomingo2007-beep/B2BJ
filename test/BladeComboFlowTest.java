/** Early intent connects cuts without skipping contact, storing extra hits or shortening the finisher. */
public final class BladeComboFlowTest {
    public static void main(String[] args)throws Exception {
        GameAudio.setMuted(true);
        bufferedChain();singleClickRecovery();holdAndInterruptions();contactPoses();
        System.out.println("BladeComboFlowTest passed: early buffer, connected cuts, recovery, hold, interruption and contact poses");
    }
    private static void bufferedChain() {
        var game=BladeCombatTest.fixture();BladeCombatTest.click(game,1,0);
        for(int move=0;move<3;move++) {
            BladeCombatTest.advance(game,.15);
            assert game.primaryPress(0,-1) : "a click during follow-through must register for move "+move;
            BladeCombatTest.advance(game,.02);game.primaryRelease(0,-1);
            assert !game.primaryPress(-1,0) : "only one next attack can be queued";
            while(game.bladeElapsed()<RuinedOutpostGame.strikeLinkTime(move)-.011)game.update(.005,0,0);
            assert game.bladeMove()==move : "queued cut cannot cancel active contact";
            BladeCombatTest.advance(game,.025);
            assert game.bladeMove()==move+1&&game.bladeLinked() : "next cut starts before returning to guard";
            assert game.bladeAimY()==-1&&game.bladeAimX()==0 : "queued direction is retained";
            assert game.bladeElapsed()<.04 : "next cut retains its own anticipation";
        }
        BladeCombatTest.advance(game,.32);
        assert !game.primaryPress(1,0) : "finisher cannot chain out of its heavy recovery";
        assert game.bladeMove()==3&&game.bladeAttacking();
        BladeCombatTest.advance(game,.5);
        assert !game.bladeAttacking() : "three buffered clicks cannot repeat indefinitely";
    }
    private static void singleClickRecovery() {
        var game=BladeCombatTest.fixture();game.primaryPress(1,0);BladeCombatTest.advance(game,.05);game.primaryRelease(1,0);
        assert Math.abs(game.player().attackCooldown()-(RuinedOutpostGame.strikeDuration(0)-.05))<1e-6
                : "wind-up credit must also shorten the input lock";
        BladeCombatTest.advance(game,.3);assert game.bladeAttacking()&&!game.bladeLinked();
        BladeCombatTest.advance(game,.06);assert !game.bladeAttacking()&&game.player().attackCooldown()==0;
        assert game.primaryPress(1,0) : "no invisible cooldown after the pose ends";
    }
    private static void holdAndInterruptions()throws Exception {
        var game=BladeCombatTest.fixture();BladeCombatTest.click(game,1,0);BladeCombatTest.advance(game,.15);
        assert game.primaryPress(1,0);BladeCombatTest.advance(game,.85);
        assert game.charging()&&!game.bladeAttacking() : "holding during recovery must not queue repeated light cuts";
        game.primaryRelease(1,0);assert game.bladeMove()==RuinedOutpostGame.SPIN_CLEAVE&&!game.bladeLinked();
        for(String action:new String[]{"pause","dash","hurt","revert"}) {
            game=BladeCombatTest.fixture();BladeCombatTest.click(game,1,0);BladeCombatTest.advance(game,.15);
            assert game.primaryPress(1,0);game.primaryRelease(1,0);
            switch(action) {
                case "pause" -> {game.pause();game.togglePause();}
                case "dash" -> {assert game.dash(0,1);}
                case "hurt" -> {
                    game.player().setGodMode(false);
                    var hit=RuinedOutpostGame.class.getDeclaredMethod("hurtPlayer",int.class,double.class,double.class);
                    hit.setAccessible(true);hit.invoke(game,1,game.player().x()+50,game.player().y());
                }
                case "revert" -> game.player().spendBladeIchor(game.player().ichor());
            }
            BladeCombatTest.advance(game,.7);assert !game.bladeAttacking()&&!game.bladeLinked() : action+" clears the buffered follow-up";
        }
    }
    private static void contactPoses() {
        for(int move=1;move<4;move++) {
            double contact=RuinedOutpostGame.strikeWindup(move);
            assert B2BJ.bladeAttackFrame(0,move,true)==1 : "connected cut starts ready, not at neutral guard";
            assert B2BJ.bladeAttackFrame(contact-.001,move,true)==3;
            assert B2BJ.bladeAttackFrame(contact,move,true)==4 : "damage and contact pose stay synchronized";
            assert B2BJ.bladeAttackFrame(contact+.04,move,true)==B2BJ.bladeAttackFrame(contact+.04,move,false);
        }
    }
}
