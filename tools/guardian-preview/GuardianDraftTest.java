/** Focused real-engine checks. Run with assertions enabled against Guardian-preview.jar. */
public final class GuardianDraftTest {
    private static void advance(GuardianDraft b,double seconds,double px,double py){for(double t=0;t<seconds-1e-8;t+=.01)b.update(Math.min(.01,seconds-t),px,py);}
    private static GuardianDraft boss(){var b=new GuardianDraft(1000,550);b.activate(1250,550);return b;}
    private static void active(GuardianDraft b,GuardianDraft.Move move,double px,double py){b.forceMove(move,px,py);advance(b,b.telegraphDuration(),px,py);assert b.state()==Guardian.State.SLAM;}
    public static void main(String[] args)throws Exception{
        var b=boss();double before=b.x();advance(b,.5,1400,550);assert b.x()>before+70:"No pursuit";
        b=boss();b.forceMove(GuardianDraft.Move.RUSH,1400,550);advance(b,.8,1400,550);double angle=b.angle();advance(b,.4,800,250);assert b.committed()&&b.angle()==angle:"Late homing";
        b=boss();active(b,GuardianDraft.Move.BACKHAND,1140,550);advance(b,.15,1140,550);assert b.hits(1140,550):"Front sweep missed";assert !b.hits(860,550):"Sweep hit behind";b.parried();assert b.coreExposed():"Parry did not open armor";
        b=boss();active(b,GuardianDraft.Move.CORE_BEAM,1200,550);advance(b,.45,1200,550);assert !b.hits(1070,550):"Beam inner space unsafe";assert b.hits(1200,550):"Beam lane missed";assert !b.hits(800,550):"Beam hit behind";b.parried();assert b.state()==Guardian.State.SLAM:"Beam became parryable";
        b=boss();active(b,GuardianDraft.Move.DETACHED_FISTS,1250,550);int first=b.impactNumber();double second=b.secondX();advance(b,.5,850,250);assert b.impactNumber()==first+1&&b.secondX()==second:"Second fist timing/commitment";
        GameAudio.setMuted(true);b=boss();b.forceMove(GuardianDraft.Move.DETACHED_FISTS,1250,550);var fx=new GuardianDraftFx(null,null,null);int pulses=0;
        for(int i=0;i<225;i++){b.update(.01,1250,550);if(fx.update(.01,b)){pulses++;assert b.state()==Guardian.State.SLAM;assert Math.abs(b.stateSeconds()-(pulses==1?.2:.68))<.011:"Shake missed fist contact";}}
        assert pulses==2:"Expected exactly two contact pulses";
        b=boss();b.hurt(601);active(b,GuardianDraft.Move.FIST,1250,550);advance(b,b.activeDuration()+b.recoverDuration()+.02,1250,550);assert b.state()==Guardian.State.ENRAGE;assert !b.hits(1000,550);
        fx=new GuardianDraftFx(null,null,null);pulses=0;
        for(int i=0;i<140;i++){b.update(.01,1250,550);if(fx.update(.01,b)){pulses++;assert Math.abs(b.stateSeconds()-1.2)<.011:"Phase bloom missed its audio";}}
        assert b.enraged()&&pulses==1;
        var map=RuinedOutpostMap.campaign(0);b=new GuardianDraft(map.guardianX(),map.guardianY());b.activate(b.x()-300,b.y()+100);double homeX=b.x(),homeY=b.y();
        for(int i=0;i<3600;i++){double a=i/60.,px=homeX+Math.cos(a)*450,py=homeY+Math.sin(a)*300;b.update(1./60,px,py,map);assert Double.isFinite(b.x())&&Double.isFinite(b.y());assert Math.hypot(b.x()-homeX,b.y()-homeY)<=900.01;assert !map.guardianDraftBlocked(b.x(),b.y()+32,60):"Warden crossed terrain";}
        for(boolean human:new boolean[]{false,true}){
            var game=PreviewGuardianCombat.scene(human,false);assert game.player().bladeForm()==human;assert game.guardian() instanceof GuardianDraft;
            var field=RuinedOutpostGame.class.getDeclaredField("savePath");field.setAccessible(true);assert field.get(game)==null:"Preview has a save target";
            assert game.primaryPress(1,0):"Original primary input failed";game.update(.05,0,0);game.primaryRelease(1,0);for(int i=0;i<120;i++)game.update(.01,0,0);
            assert game.dash(1,0):"Original dash input failed";
        }
        System.out.println("GuardianDraftTest passed: pursuit, commitment, shapes, parry, phase, terrain, original inputs and no-save isolation");
    }
}
