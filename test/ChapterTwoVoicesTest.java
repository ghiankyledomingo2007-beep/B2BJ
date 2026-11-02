import java.util.HashSet;

public final class ChapterTwoVoicesTest {
    public static void main(String[] args) throws Exception {
        var clips=new HashSet<String>();
        for(var scene:OutpostDialogue.values())if(scene.biome()==1)for(int i=0;i<scene.lines().size();i++) {
            String key=scene.voiceKey(i,true,0);assert clips.add(key);
            assert ChapterOneVoicesTest.duration(key)>.5 : key;
        }
        for(var bark:OutpostBark.values())if(bark.biome()==1)for(int i=0;i<bark.lines().size();i++) {
            String key=bark.voiceKey(i);assert clips.add(key);
            double duration=ChapterOneVoicesTest.duration(key);
            assert duration>.5&&(bark.banter()?duration<5:duration+.15<bark.lineSeconds()) : "forest speech cut off: "+key;
        }
        assert clips.size()==124 : "complete forest script: "+clips.size();
        for(String family:new String[]{"attack","heavy","hurt"})for(int i=1;i<=(family.equals("attack")?6:family.equals("heavy")?4:8);i++) {
            double duration=ChapterOneVoicesTest.duration("effort_"+family+"_"+i);
            assert duration>.1&&duration<2.0 : "combat feedback must remain a brief effort";
        }
        System.out.println("ChapterTwoVoicesTest passed: 124 forest lines, timing, PCM and eighteen combat efforts");
    }
}
