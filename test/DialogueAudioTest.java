import java.util.Arrays;
import java.util.Map;

public final class DialogueAudioTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        var field=GameAudio.class.getDeclaredField("DIALOGUE");field.setAccessible(true);
        var cache=(Map<String,byte[]>)field.get(null);
        byte[] pcm=new byte[2000];for(int i=0;i<pcm.length;i+=2){pcm[i]=(byte)0xe8;pcm[i+1]=3;}
        cache.put("test_voice",pcm);cache.put("test_next",pcm);
        GameAudio.setMasterVolume(1);GameAudio.setSfxVolume(0);GameAudio.setDialogueVolume(.5);GameAudio.setMuted(false);
        GameAudio.setDialogue("test_voice",false);
        int[] mix=new int[10];GameAudio.mixVoices(mix);assert Arrays.stream(mix).allMatch(n->n==500);
        assert GameAudio.dialogueLevel()>0&&GameAudio.dialogueLevel()<=1 : "seal responds to recorded speech";
        GameAudio.setDialogue("test_voice",true);
        assert GameAudio.dialogueLevel()==0 : "pause stops the speech pulse";
        Arrays.fill(mix,0);GameAudio.mixVoices(mix);assert Arrays.stream(mix).allMatch(n->n==0);
        GameAudio.setDialogue("test_voice",false);
        Arrays.fill(mix,0);GameAudio.mixVoices(mix);assert Arrays.stream(mix).allMatch(n->n==500);
        GameAudio.setDialogue("test_next",false);
        Arrays.fill(mix,0);GameAudio.mixVoices(mix);assert Arrays.stream(mix).allMatch(n->n==500) : "advancing replaces rather than overlaps";
        GameAudio.setMuted(true);Arrays.fill(mix,0);GameAudio.mixVoices(mix);assert Arrays.stream(mix).allMatch(n->n==0);
        assert GameAudio.dialogueLevel()==0;
        GameAudio.setMuted(false);GameAudio.setDialogue(null,false);
        assert GameAudio.dialogueLevel()==0;
        Arrays.fill(mix,0);GameAudio.mixVoices(mix);assert Arrays.stream(mix).allMatch(n->n==0) : "skip stops voice";
        GameAudio.setDialogue("missing_voice",false);GameAudio.mixVoices(mix);
        assert Arrays.stream(mix).allMatch(n->n==0) : "missing clip remains silent, no synthesized substitute";
        GameAudio.setDialogue("test_voice",false);GameAudio.mixVoices(new int[2000]);
        GameAudio.setDialogue("test_voice",false);Arrays.fill(mix,0);GameAudio.mixVoices(mix);
        assert Arrays.stream(mix).allMatch(n->n==0) : "finished line must not repeat each frame";
        try {GameAudio.setDialogue("../escape",false);throw new AssertionError();}catch(IllegalArgumentException expected) { }
        GameAudio.setDialogue(null,false);GameAudio.setSfxVolume(1);GameAudio.setDialogueVolume(1);
        var samplesField=GameAudio.class.getDeclaredField("SAMPLES");samplesField.setAccessible(true);
        var samples=(Map<String,byte[][]>)samplesField.get(null);
        var previous=samples.put("charge_loop",new byte[][]{pcm});
        try {
            GameAudio.startLoop(GameAudio.Loop.CHARGE,1);
            Arrays.fill(mix,0);GameAudio.mixVoices(mix);
            int effectsLevel=mix[0];assert effectsLevel>0;
            assert Arrays.stream(mix).allMatch(n->n==effectsLevel);
            GameAudio.setDialogue("test_voice",false);
            Arrays.fill(mix,0);GameAudio.mixVoices(mix);
            assert Arrays.stream(mix).allMatch(n->n==1000+effectsLevel/2) : "speech stays full level while effects drop by half";
            GameAudio.setDialogue("test_voice",true);
            Arrays.fill(mix,0);GameAudio.mixVoices(mix);
            assert Arrays.stream(mix).allMatch(n->n==effectsLevel) : "paused speech must not duck effects";
            GameAudio.setDialogue("test_voice",false);GameAudio.setDialogueVolume(0);
            Arrays.fill(mix,0);GameAudio.mixVoices(mix);
            assert Arrays.stream(mix).allMatch(n->n==effectsLevel) : "muted speech must not duck effects";
        } finally {
            GameAudio.stopLoops();GameAudio.setDialogue(null,false);GameAudio.setDialogueVolume(1);
            if(previous==null)samples.remove("charge_loop");else samples.put("charge_loop",previous);
        }
        System.out.println("DialogueAudioTest passed: independent gain, speech ducking, pause, advance, skip, mute and missing files");
    }
}
