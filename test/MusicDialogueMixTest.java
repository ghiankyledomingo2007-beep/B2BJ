import java.util.Arrays;
import java.util.Map;

public final class MusicDialogueMixTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        var field=GameAudio.class.getDeclaredField("SAMPLES");field.setAccessible(true);
        var samples=(Map<String,byte[][]>)field.get(null);
        field=GameAudio.class.getDeclaredField("DIALOGUE");field.setAccessible(true);
        var dialogue=(Map<String,byte[]>)field.get(null);
        byte[] bed=constant(22050,4000),speech=constant(22050,1000);
        var original=samples.put("boss_music",new byte[][]{bed});
        dialogue.put("test_music_speech",speech);dialogue.put("test_music_reply",speech);
        try {
            GameAudio.setMuted(false);GameAudio.setMasterVolume(1);GameAudio.setMusicVolume(.25);
            GameAudio.setSfxVolume(0);GameAudio.setDialogueVolume(1);GameAudio.setDialogue(null,false);
            GameAudio.stopLoops();GameAudio.mixVoices(new int[22050]);
            GameAudio.startLoop(GameAudio.Loop.BOSS_MUSIC,1);
            int full=render(256)[255];assert full>0;
            GameAudio.setDialogue("test_music_speech",false);
            int[] attack=render(2205);for(int i=0;i<attack.length;i++)attack[i]-=1000;
            assert Math.abs(attack[0]-full)<=2 : "speech onset must not cut music volume instantly";
            assert Math.abs(attack[attack.length-1]-full*.35)<=2 : "music makes room for speech promptly";
            smooth(attack,full,false);
            GameAudio.setDialogue("test_music_reply",false);
            int[] reply=render(256);
            assert Arrays.stream(reply).allMatch(v->Math.abs(v-1000-full*.35)<=2) : "a speaker change cannot bounce music back up";
            // Let the recording end naturally, leaving its subtitle/key selected.
            render(22050);int[] release=render(22050);
            assert Math.abs(release[0]-full*.35)<=2 : "music must not jump at the end of a recording";
            smooth(release,release[0],true);
            assert release[2205]<full*.65 : "restore music gradually between spoken lines";
            assert Math.abs(release[release.length-1]-full)<=1 : "music recovers after speech ends";
            GameAudio.setDialogue("test_music_speech",false);render(2205);
            GameAudio.setDialogueVolume(0);assert render(22050)[22049]==full : "silent dialogue cannot leave music ducked";
            GameAudio.setMuted(true);assert Arrays.stream(render(256)).allMatch(v->v==0);
            GameAudio.setMuted(false);GameAudio.setMusicVolume(0);assert Arrays.stream(render(256)).allMatch(v->v==0);
            ambientUsesSameEnvelope(dialogue);
        } finally {
            GameAudio.stopLoops();GameAudio.setDialogue(null,false);GameAudio.setMusicState(0,false,false);
            GameAudio.setMuted(false);GameAudio.setMasterVolume(1);GameAudio.setMusicVolume(.25);
            GameAudio.setDialogueVolume(1);GameAudio.setSfxVolume(1);
            if(original==null)samples.remove("boss_music");else samples.put("boss_music",original);
            dialogue.remove("test_music_speech");dialogue.remove("test_music_reply");dialogue.remove("test_music_silence");
        }
        System.out.println("MusicDialogueMixTest passed: smooth speech transitions, replies, natural ending, mute and ambient music");
    }
    private static void ambientUsesSameEnvelope(Map<String,byte[]> dialogue) throws Exception {
        GameAudio.stopLoops();GameAudio.setDialogue(null,false);render(22050);
        GameAudio.setMusicVolume(.25);GameAudio.setDialogueVolume(1);GameAudio.setMusicState(0,false,true);
        dialogue.put("test_music_silence",constant(22050,0));
        var clock=GameAudio.class.getDeclaredField("musicSample");clock.setAccessible(true);
        var mix=GameAudio.class.getDeclaredMethod("mixMusic",int[].class);mix.setAccessible(true);
        clock.setLong(null,11025);int[] full=render(2205);mix.invoke(null,(Object)full);
        GameAudio.setDialogue("test_music_silence",false);clock.setLong(null,11025);
        int[] ducked=render(2205);mix.invoke(null,(Object)ducked);
        int checked=0;
        for(int i=0;i<full.length;i++)if(Math.abs(full[i])>30) {
            double ratio=ducked[i]/(double)full[i];
            if(i<100)assert ratio>.9 : "ambient music also enters the duck smoothly";
            if(i>1800){assert Math.abs(ratio-.35)<.04 : "ambient and recorded music share the same speech level";checked++;}
        }
        assert checked>30;
    }
    private static int[] render(int count) {int[] mix=new int[count];GameAudio.mixVoices(mix);return mix;}
    private static byte[] constant(int count,int value) {
        byte[] pcm=new byte[count*2];for(int i=0;i<pcm.length;i+=2){pcm[i]=(byte)value;pcm[i+1]=(byte)(value>>8);}return pcm;
    }
    private static void smooth(int[] samples,int previous,boolean rising) {
        for(int sample:samples) {
            assert Math.abs(sample-previous)<=2 : "music must stay continuous across mixer blocks";
            assert rising?sample>=previous:sample<=previous;
            previous=sample;
        }
    }
}
