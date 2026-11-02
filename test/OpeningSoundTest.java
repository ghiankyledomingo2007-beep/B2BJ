import java.util.Arrays;

public final class OpeningSoundTest {
    public static void main(String[] args) throws Exception {
        GameAudio.setMasterVolume(1);GameAudio.setSfxVolume(1);GameAudio.setMusicVolume(0);GameAudio.setMuted(false);
        GameAudio.setDialogue(null,false);GameAudio.setOpeningSound(null,false);
        for(var clip:GameAudio.OpeningClip.values()) {
            GameAudio.setOpeningSound(clip,false);
            var voice=voice();assert voice!=null : "missing opening sound: "+clip;
            var at=voice.getClass().getDeclaredField("at");at.setAccessible(true);
            int[] audio=new int[22050];GameAudio.mixVoices(audio);
            assert Arrays.stream(audio).anyMatch(s->s!=0) : "sound must be audible: "+clip;
            int position=at.getInt(voice);
            GameAudio.setOpeningSound(clip,true);audio=new int[1000];GameAudio.mixVoices(audio);
            assert Arrays.stream(audio).allMatch(s->s==0)&&at.getInt(voice)==position : "pause freezes the sound bed";
            GameAudio.setOpeningSound(clip,false);
            assert voice()==voice&&at.getInt(voice)==position : "resume must not restart the shot";
            GameAudio.setOpeningSound(null,false);audio=new int[1000];GameAudio.mixVoices(audio);
            assert voice()==null&&Arrays.stream(audio).allMatch(s->s==0) : "skip clears the shot immediately";
        }
        for(boolean muted:new boolean[]{false,true}) {
            GameAudio.setMuted(muted);GameAudio.setSfxVolume(muted?1:0);
            GameAudio.setOpeningSound(GameAudio.OpeningClip.MEMORY,false);
            int[] audio=new int[22050];GameAudio.mixVoices(audio);
            assert Arrays.stream(audio).allMatch(s->s==0) : "opening obeys mute and effects volume";
            GameAudio.setOpeningSound(null,false);
        }
        GameAudio.setMuted(false);GameAudio.setSfxVolume(1);
        var game=RuinedOutpostGame.campaign();game.begin();var panel=new B2BJ(false,game);panel.step(.01);
        var field=GameAudio.class.getDeclaredField("openingClip");field.setAccessible(true);
        assert field.get(null)==GameAudio.OpeningClip.MEMORY;
        game.advanceDialogue();game.advanceDialogue();panel.step(.01);
        assert field.get(null)==GameAudio.OpeningClip.AWAKEN;
        game.skipDialogue();panel.step(.01);assert field.get(null)==null&&voice()==null;
        System.out.println("OpeningSoundTest passed: packaged cues, pause/resume, skip, mute, effects volume and scene routing");
    }
    private static Object voice() throws Exception {
        var field=GameAudio.class.getDeclaredField("openingVoice");field.setAccessible(true);return field.get(null);
    }
}
