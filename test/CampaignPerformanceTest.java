import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Device-specific headless observations, never a portable frame-rate pass/fail gate. */
public final class CampaignPerformanceTest {
    public static void main(String[] args) throws Exception {
        int frames=args.length==0?30:Integer.parseInt(args[0]);
        if(frames<1||frames>1200)throw new IllegalArgumentException("frames must be 1..1200");
        boolean muted=GameAudio.muted();GameAudio.setMuted(true);
        Path directory=Files.createTempDirectory("b2bj-performance-");
        int enemies=0;
        try {
            System.out.printf(Locale.ROOT,"Campaign performance: Java %s, %s %s, %d logical CPUs; %d measured frames/scenario%n",
                    System.getProperty("java.version"),System.getProperty("os.name"),
                    System.getProperty("os.arch"),Runtime.getRuntime().availableProcessors(),frames);
            System.out.println("Full 1280x720 logical scene; 640x360 output is scaled 0.5x. Muted audio; fixture healing; no timing thresholds.");
            for(int biome=0;biome<CampaignWorld.AREA_COUNT;biome++) {
                Path save=directory.resolve("biome-"+biome+".properties");
                var priorBosses=new HashSet<Integer>();
                for(int prior=0;prior<biome;prior++)priorBosses.add(prior);
                CampaignSave.save(save,new CampaignSave.Progress(biome,0,priorBosses,Set.of(),
                        Map.of(),Set.of(),Set.of(),0));
                var game=RuinedOutpostGame.campaign(save);
                assert game.continueCampaign()&&game.biome()==biome;
                int count=game.scouts().size();enemies+=count;
                assert count==CampaignWorld.area(biome).enemies().size();
                var panel=new B2BJ(false,game);panel.setSize(1280,720);
                for(int width:new int[]{1280,640})for(boolean expanded:new boolean[]{false,true}) {
                    panel.getActionMap().get(expanded?"mapPressed":"mapReleased")
                            .actionPerformed(new ActionEvent(panel,ActionEvent.ACTION_PERFORMED,"performance"));
                    measure(panel,game,width,expanded,frames,count);
                }
                measureTerrainScan(game.map(),frames);
                Files.delete(save);
            }
            assert enemies==105 : "measurement must include the authored campaign roster";
            System.out.println("CampaignPerformanceTest passed: 105 authored enemies across four biomes; maximum 33 before boss summons.");
        } finally {
            GameAudio.setMuted(muted);
            try(var paths=Files.list(directory)) {
                for(Path path:paths.toList())Files.delete(path);
            }
            Files.delete(directory);
        }
    }

    private static void measure(B2BJ panel,RuinedOutpostGame game,int width,boolean expanded,int frames,int enemies) {
        var image=new BufferedImage(width,width*9/16,BufferedImage.TYPE_INT_ARGB);
        var graphics=image.createGraphics();graphics.scale(width/1280.0,width/1280.0);
        int warmup=Math.min(60,Math.max(20,frames/2));
        long[] updates=new long[frames],draws=new long[frames],totals=new long[frames];
        for(int i=-warmup;i<frames;i++) {
            // Sample authored encounter clearings, preserving all enemies and normal AI activation.
            var location=game.campaignArea().landmarks().get(2+Math.floorMod(i,6));
            game.player().relocate(location.x(),location.y());game.player().heal();
            long start=System.nanoTime();
            panel.step(1/60.0);
            long updated=System.nanoTime();
            panel.paint(graphics);
            long rendered=System.nanoTime();
            if(i>=0) {updates[i]=updated-start;draws[i]=rendered-updated;totals[i]=rendered-start;}
            assert !game.story().blocksGameplay() : "benchmark must stay in active gameplay";
        }
        graphics.dispose();
        assert (image.getRGB(width/2,image.getHeight()/2)>>>24)==255 : "complete frame must render";
        System.out.printf(Locale.ROOT,"%s | enemies=%d | %dx%d | map=%s | update mean %.3fms | draw mean %.3fms | frame p50 %.3fms p95 %.3fms max %.3fms%n",
                game.campaignArea().name(),enemies,width,image.getHeight(),expanded,
                mean(updates),mean(draws),percentile(totals,.5),percentile(totals,.95),percentile(totals,1));
    }

    private static void measureTerrainScan(RuinedOutpostMap map,int frames) {
        long elapsed=0,checksum=0;
        for(int sample=-8;sample<frames;sample++) {
            long start=System.nanoTime();
            for(int y=0;y<map.heightInTiles();y+=2)for(int x=0;x<map.widthInTiles();x+=2)
                checksum+=map.tileMask(x,y);
            if(sample>=0)elapsed+=System.nanoTime()-start;
        }
        System.out.printf(Locale.ROOT,"%s | 1536 terrain queries for one minimap: mean %.3fms | checksum=%d%n",
                CampaignWorld.area(map.biome()).name(),elapsed/(frames*1_000_000.0),checksum);
    }

    private static double mean(long[] values) { return Arrays.stream(values).average().orElseThrow()/1_000_000; }
    private static double percentile(long[] values,double quantile) {
        long[] sorted=values.clone();Arrays.sort(sorted);
        return sorted[Math.min(sorted.length-1,(int)Math.ceil(sorted.length*quantile)-1)]/1_000_000.0;
    }
}
