/** Run with: java -cp build/B2BJ.jar tools/audio/ExportDialogue.java */
public class ExportDialogue {
    private static final java.util.Set<String> exported=new java.util.HashSet<>();
    public static void main(String[] args) {
        System.out.println("file\tspeaker\tscene\ttext");
        for(var scene:OutpostDialogue.values())for(int i=0;i<scene.lines().size();i++) {
            line(scene,i,true,0);line(scene,i,false,0);line(scene,i,true,1);
        }
        for(var bark:OutpostBark.values())for(int i=0;i<bark.lines().size();i++)
            System.out.println(bark.voiceKey(i)+".wav\t"+bark.line(i).speaker()+"\tEXPLORATION\t"+bark.line(i).text());
    }
    private static void line(OutpostDialogue scene,int index,boolean met,int rescued) {
        var line=scene.line(index,met,rescued);
        String key=scene.voiceKey(index,met,rescued);
        if(exported.add(key))System.out.println(key+".wav\t"+line.speaker()+"\t"+scene.title()+"\t"+line.text());
    }
}
