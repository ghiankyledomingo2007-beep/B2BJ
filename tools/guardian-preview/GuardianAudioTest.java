import java.util.Arrays;
import java.util.Map;

/** Packaged audio and real mixer checks; this does not assess listening preference. */
public final class GuardianAudioTest {
    private static void drain(){GameAudio.stopLoops();GameAudio.mixVoices(new int[22050*6]);}
    private static int sample(byte[] pcm,int index){return(short)((pcm[index]&255)|(pcm[index+1]<<8));}
    public static void main(String[] args){
        GameAudio.setMasterVolume(1);GameAudio.setSfxVolume(1);GameAudio.setMusicVolume(0);GameAudio.setDialogueVolume(0);GameAudio.setMuted(false);
        var counts=Map.ofEntries(Map.entry(GameAudio.Cue.WARDEN_WINDUP,1),Map.entry(GameAudio.Cue.WARDEN_SLAM,2),Map.entry(GameAudio.Cue.WARDEN_SWEEP,2),Map.entry(GameAudio.Cue.WARDEN_RUSH,2),Map.entry(GameAudio.Cue.WARDEN_FISTS_LAUNCH,1),Map.entry(GameAudio.Cue.WARDEN_FISTS_IMPACT,2),Map.entry(GameAudio.Cue.WARDEN_BEAM_CHARGE,1),Map.entry(GameAudio.Cue.WARDEN_BEAM,1),Map.entry(GameAudio.Cue.WARDEN_CORE_OPEN,1),Map.entry(GameAudio.Cue.WARDEN_STRAIN,1),Map.entry(GameAudio.Cue.WARDEN_STEP,2));
        for(var entry:counts.entrySet()){
            var cue=entry.getKey();var takes=GameAudio.samples(cue);assert takes.length==entry.getValue():"Wrong take count: "+cue;assert !cue.pitched&&cue.layer==null;
            for(byte[] pcm:takes){int peak=0;double sum=0;for(int i=0;i<pcm.length;i+=2){int value=sample(pcm,i);peak=Math.max(peak,Math.abs(value));sum+=value;}
                assert peak>500&&peak<30000:"Silent or over-hot recording: "+cue;assert Math.abs(sum/(pcm.length/2))<5:"DC offset: "+cue;
                assert Math.abs(sample(pcm,0))<4&&Math.abs(sample(pcm,pcm.length-2))<4:"Hard sample edge: "+cue;
            }
        }
        for(var entry:Map.of(GameAudio.Cue.WARDEN_BEAM_CHARGE,1.25,GameAudio.Cue.WARDEN_BEAM,1.15,GameAudio.Cue.WARDEN_STRAIN,1.4).entrySet())assert Math.abs(GameAudio.samples(entry.getKey())[0].length/2-entry.getValue()*22050)<=.500001:"Authored timing drift: "+entry.getKey();
        for(var cue:new GameAudio.Cue[]{GameAudio.Cue.WARDEN_SLAM,GameAudio.Cue.WARDEN_SWEEP,GameAudio.Cue.WARDEN_RUSH,GameAudio.Cue.WARDEN_FISTS_IMPACT,GameAudio.Cue.WARDEN_BEAM}){
            drain();GameAudio.play(cue);int[] sound=new int[220];GameAudio.mixVoices(sound);assert Arrays.stream(sound).map(Math::abs).max().orElse(0)>100:"Late attack onset: "+cue;
        }
        drain();GameAudio.setMuted(true);GameAudio.play(GameAudio.Cue.WARDEN_BEAM);int[] muted=new int[4096];GameAudio.mixVoices(muted);assert Arrays.stream(muted).allMatch(v->v==0);GameAudio.setMuted(false);
        System.out.println("GuardianAudioTest passed: 16 takes, fixed timing/pitch, levels, DC, edges, immediate attacks and mute");
    }
}
