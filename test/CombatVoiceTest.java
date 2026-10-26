import java.util.Arrays;
import java.util.Map;

public final class CombatVoiceTest {
    public static void main(String[] args) throws Exception {
        var cacheField=GameAudio.class.getDeclaredField("DIALOGUE");cacheField.setAccessible(true);
        @SuppressWarnings("unchecked") var cache=(Map<String,byte[]>)cacheField.get(null);
        byte[] pcm=new byte[22050];for(int i=0;i<pcm.length;i+=2){pcm[i]=(byte)0xe8;pcm[i+1]=3;}
        for(String family:new String[]{"attack","heavy","hurt"})for(int i=1;i<=8;i++)cache.put("effort_"+family+"_"+i,pcm);
        cache.put("test_sage",pcm);GameAudio.setMuted(false);GameAudio.setMasterVolume(1);GameAudio.setSfxVolume(1);GameAudio.setDialogueVolume(1);
        GameAudio.setDialogue(null,false);
        assert !GameAudio.playCombatEffort(false,false,false) : "slime attacks keep their existing bubble effects";
        assert GameAudio.playCombatEffort(false,true,false);
        assert !GameAudio.playCombatEffort(false,true,false) : "one effort per attack burst";
        int[] mix=new int[256];GameAudio.mixVoices(mix);assert Arrays.stream(mix).anyMatch(n->n>0);
        assert GameAudio.dialogueLevel()==0 : "efforts cannot drive Sage's speech pulse";
        GameAudio.setDialogue("test_sage",false);assert !GameAudio.playCombatEffort(true,true,false);
        GameAudio.setDialogue(null,false);mix=new int[256];GameAudio.mixVoices(mix);
        assert Arrays.stream(mix).allMatch(n->n==0) : "Sage interrupted and discarded the effort";
        GameAudio.mixVoices(new int[70000]);GameAudio.setDialogue(null,true);
        assert !GameAudio.playCombatEffort(false,true,true);
        GameAudio.setDialogue(null,false);assert GameAudio.playCombatEffort(false,true,true);
        GameAudio.setDialogue(null,true);GameAudio.setDialogue(null,false);GameAudio.mixVoices(new int[70000]);
        GameAudio.setDialogueVolume(0);assert !GameAudio.playCombatEffort(true,true,false);
        GameAudio.setDialogueVolume(1);GameAudio.setSfxVolume(0);assert !GameAudio.playCombatEffort(true,true,false);
        GameAudio.setSfxVolume(1);assert GameAudio.playCombatEffort(true,false,false) : "Rainoray can react to real damage in either form";
        GameAudio.setDialogue(null,true);GameAudio.setDialogue(null,false);
        for(String family:new String[]{"attack","heavy","hurt"}) {
            int count=family.equals("attack")?6:family.equals("heavy")?4:8;
            var heard=new java.util.HashSet<byte[]>();
            for(int i=1;i<=count;i++)cache.put("effort_"+family+"_"+i,new byte[]{(byte)i,3,0,0});
            var field=GameAudio.class.getDeclaredField("effortVoice");field.setAccessible(true);
            for(int i=0;i<count;i++) {
                GameAudio.mixVoices(new int[70000]);
                assert GameAudio.playCombatEffort(family.equals("hurt"),true,family.equals("heavy"));
                Object voice=field.get(null);var data=voice.getClass().getDeclaredField("pcm");data.setAccessible(true);
                assert heard.add((byte[])data.get(voice)) : "repeat before exhausting "+family+" takes";
            }
        }
        GameAudio.setDialogue("test_sage",false);double duration=GameAudio.dialogueDuration();
        GameAudio.mixVoices(new int[70000]);assert duration==.5&&GameAudio.dialogueDuration()==duration;
        GameAudio.setDialogue(null,false);
        System.out.println("CombatVoiceTest passed: accepted event channel, form, cadence, speech priority, pause and volume");
    }
}
