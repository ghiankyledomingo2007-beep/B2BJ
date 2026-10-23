import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class AssetArchiveTest {
    public static void main(String[] args) throws Exception {
        var rows=Files.readAllLines(Path.of("archive/assets/2026-10-23/manifest.tsv"));
        assert rows.size()==23 : "the cleanup preserves all 22 retired files";
        for(String row:rows.subList(1,rows.size())) {
            var cells=row.split("\t");
            assert cells.length==4&&cells[0].startsWith("assets/")&&cells[1].startsWith("archive/assets/2026-10-23/");
            assert !Files.exists(Path.of(cells[0])) : "retired asset restored into runtime: "+cells[0];
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(cells[1])));
            assert HexFormat.of().formatHex(digest).equals(cells[2]) : "archive changed: "+cells[1];
            assert B2BJ.class.getResource("/"+cells[0])==null : "retired asset packaged: "+cells[0];
        }
        runtimeOnly();
        System.out.println("AssetArchiveTest passed: 22 preserved files, no retired runtime paths");
    }

    static void runtimeOnly() {
        for(String path:new String[]{"assets/characters/blade/blade_run.png","assets/characters/slime/slime_absorb.png",
                "assets/effects/blob_hit.png","assets/props/outpost/supplies.png",
                "assets/source/characters/mara_idle.generated.png","assets/source/outpost/tent.generated.png",
                "assets/source/outpost/dummy.generated.png",
                "archive/assets/2026-10-23/manifest.tsv"})
            assert B2BJ.class.getResource("/"+path)==null : "source/archive asset packaged: "+path;
    }
}
