import java.util.Arrays;
import java.util.List;

public final class RainorayVoidAudioTest {
    private static void drain(){GameAudio.stopLoops();GameAudio.mixVoices(new int[22050*5]);}
    public static void main(String[] args)throws Exception {
        GameAudio.setMuted(false);GameAudio.setMasterVolume(1);GameAudio.setSfxVolume(1);GameAudio.setMusicVolume(0);GameAudio.setDialogueVolume(0);
        try {
            for(var cue:List.of(GameAudio.Cue.VOID_CHARGE,GameAudio.Cue.VOID_DASH,GameAudio.Cue.VOID_RUPTURE,
                    GameAudio.Cue.SEAM_FRACTURE,GameAudio.Cue.PARRY_CLASH,GameAudio.Cue.COUNTER_CUT,
                    GameAudio.Cue.EMPOWER_CHARGE,GameAudio.Cue.HEAVY_CLEAVE)) {
                assert GameAudio.samples(cue).length>0&&cue.layer==null&&!cue.pitched&&!cue.chirp;
            }
            assert GameAudio.samples(GameAudio.Cue.HEAVY_CLEAVE).length==2;
            assert GameAudio.samples(GameAudio.Cue.SEAM_FRACTURE).length==3;
            assert GameAudio.samples(GameAudio.Loop.DEVOUR.family).length==1;
            drain();GameAudio.playSynced(GameAudio.Cue.PARRY_CLASH,0,.20,.15);
            int[] silence=new int[1102];GameAudio.mixVoices(silence);assert Arrays.stream(silence).allMatch(v->v==0);
            int[] sound=new int[4096];GameAudio.mixVoices(sound);assert Arrays.stream(sound).anyMatch(v->v!=0);
            drain();GameAudio.playSynced(GameAudio.Cue.VOID_RUPTURE,0,0,.28);
            sound=new int[1102];GameAudio.mixVoices(sound);
            assert Arrays.stream(sound).map(Math::abs).max().orElse(0)>5000 : "a live impact skips the old 280ms pre-roll";
            for(double bad:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}) {
                boolean rejected=false;try{GameAudio.playSynced(GameAudio.Cue.PARRY_CLASH,bad,.15);}catch(IllegalArgumentException e){rejected=true;}
                assert rejected;
            }
            drain();var game=RainorayKitTest.fight();var panel=new B2BJ(false,game);
            game.primaryPress(1,0);panel.step(.25);
            assert GameAudio.loopRunning(GameAudio.Loop.BLADE_CHARGE)&&!GameAudio.loopRunning(GameAudio.Loop.CHARGE);
            game.pause();panel.step(.02);assert !GameAudio.loopRunning(GameAudio.Loop.BLADE_CHARGE);
            drain();game.togglePause();RainorayKitTest.ready(game);assert game.voidSever(1,0);
            RainorayKitTest.call(panel,"handleEvents",new Class<?>[]{List.class},game.drainEvents());
            sound=new int[11025];GameAudio.mixVoices(sound);assert Arrays.stream(sound).anyMatch(v->v!=0);
            GameAudio.setMuted(true);drain();GameAudio.playSynced(GameAudio.Cue.VOID_RUPTURE,0,.28);
            sound=new int[11025];GameAudio.mixVoices(sound);assert Arrays.stream(sound).allMatch(v->v==0);
            System.out.println("RainorayVoidAudioTest passed: packaged takes, exact delays, immediate impacts, mute and human charge cancellation");
        } finally {GameAudio.setMuted(false);drain();GameAudio.setMusicVolume(.25);GameAudio.setDialogueVolume(1);}
    }
}
