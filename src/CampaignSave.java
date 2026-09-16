import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** Versioned checkpoint snapshots. Combat encounters restart when a checkpoint is loaded. */
public final class CampaignSave {
    private static final int VERSION = 1;
    private static final int MAX_BYTES = 65_536;

    public record Progress(int biome, int checkpoint, Set<Integer> clearedBosses,
            Set<String> unlocks, Map<String, Integer> upgrades, Set<String> questFlags,
            Set<String> endings, int shards) {
        public Progress {
            if (biome < 0 || biome >= CampaignWorld.AREA_COUNT || checkpoint < 0 || checkpoint > 1)
                throw new IllegalArgumentException("Invalid biome or checkpoint");
            if (shards < 0 || shards > 100_000) throw new IllegalArgumentException("Invalid shard count");
            clearedBosses = Set.copyOf(clearedBosses);
            if (clearedBosses.stream().anyMatch(boss -> boss < 0 || boss >= CampaignWorld.AREA_COUNT))
                throw new IllegalArgumentException("Invalid cleared boss");
            for (int boss = 0; boss < clearedBosses.size(); boss++) {
                if (!clearedBosses.contains(boss))
                    throw new IllegalArgumentException("Cleared bosses must follow campaign order");
            }
            if (biome > clearedBosses.size())
                throw new IllegalArgumentException("Biome requires previous bosses cleared");
            unlocks = identifiers(unlocks);
            CampaignStory story = new CampaignStory();
            story.restoreQuestFlags(questFlags);
            questFlags = story.questFlags();
            endings = identifiers(endings);
            if (!Set.of("CLOSE_RIFT", "HUMAN_FORM").containsAll(endings))
                throw new IllegalArgumentException("Invalid ending");
            if (!endings.isEmpty() && clearedBosses.size() != 4)
                throw new IllegalArgumentException("Ending requires all bosses cleared");
            upgrades = Map.copyOf(upgrades);
            for (var upgrade : upgrades.entrySet()) {
                int maximum = switch (upgrade.getKey()) {
                    case "vitality", "capacity", "efficiency" -> 3;
                    case "edge" -> 1;
                    default -> throw new IllegalArgumentException("Invalid upgrade name");
                };
                if (upgrade.getValue() < 0 || upgrade.getValue() > maximum)
                    throw new IllegalArgumentException("Invalid upgrade level");
            }
        }

        public Progress(int biome, int checkpoint, Set<Integer> clearedBosses, Set<String> unlocks,
                Map<String, Integer> upgrades, Set<String> questFlags, Set<String> endings) {
            this(biome, checkpoint, clearedBosses, unlocks, upgrades, questFlags, endings, 0);
        }
    }

    private CampaignSave() {}

    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".b2bj", "campaign.properties");
    }

    /** Missing saves are empty; unreadable, corrupt and unsupported saves report IOException. */
    public static Optional<Progress> load(Path path) throws IOException {
        byte[] bytes;
        try (var input = Files.newInputStream(path)) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (NoSuchFileException missing) {
            return Optional.empty();
        }
        if (bytes.length > MAX_BYTES) throw new IOException("Campaign save exceeds size limit");
        try {
            Properties values = new Properties();
            values.load(new ByteArrayInputStream(bytes));
            if (Integer.parseInt(required(values, "version")) != VERSION)
                throw new IOException("Unsupported campaign save version");
            Set<Integer> bosses = new HashSet<>();
            for (String boss : tokens(required(values, "clearedBosses"))) {
                if (!bosses.add(Integer.parseInt(boss)))
                    throw new IllegalArgumentException("Duplicate cleared boss");
            }
            Map<String, Integer> upgrades = new HashMap<>();
            for (String upgrade : tokens(required(values, "upgrades"))) {
                String[] pair = upgrade.split(":", -1);
                if (pair.length != 2 || upgrades.put(pair[0], Integer.parseInt(pair[1])) != null)
                    throw new IllegalArgumentException("Invalid upgrade");
            }
            return Optional.of(new Progress(Integer.parseInt(required(values, "biome")),
                    Integer.parseInt(required(values, "checkpoint")), bosses,
                    tokens(required(values, "unlocks")), upgrades,
                    tokens(required(values, "questFlags")), tokens(required(values, "endings")),
                    Integer.parseInt(required(values, "shards"))));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid campaign save: " + invalid.getMessage(), invalid);
        }
    }

    /** Atomic replacement is required: unsupported filesystems leave the existing save intact. */
    public static void save(Path path, Progress progress) throws IOException {
        Properties values = new Properties();
        values.setProperty("version", Integer.toString(VERSION));
        values.setProperty("biome", Integer.toString(progress.biome()));
        values.setProperty("checkpoint", Integer.toString(progress.checkpoint()));
        values.setProperty("shards", Integer.toString(progress.shards()));
        values.setProperty("clearedBosses", progress.clearedBosses().stream().sorted()
                .map(String::valueOf).collect(Collectors.joining(",")));
        values.setProperty("unlocks", joined(progress.unlocks()));
        values.setProperty("upgrades", progress.upgrades().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(upgrade -> upgrade.getKey() + ":" + upgrade.getValue()).collect(Collectors.joining(",")));
        values.setProperty("questFlags", joined(progress.questFlags()));
        values.setProperty("endings", joined(progress.endings()));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        values.store(bytes, "Blob to Blade campaign");
        if (bytes.size() > MAX_BYTES) throw new IOException("Campaign save exceeds size limit");

        Path target = path.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".b2bj-", ".tmp");
        try {
            try (FileChannel output = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer data = ByteBuffer.wrap(bytes.toByteArray());
                while (data.hasRemaining()) output.write(data);
                output.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String required(Properties values, String key) {
        String value = values.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Missing " + key);
        return value;
    }

    private static Set<String> tokens(String value) {
        if (value.isEmpty()) return Set.of();
        Set<String> result = new HashSet<>();
        for (String token : value.split(",", -1)) {
            if (token.isEmpty() || !result.add(token))
                throw new IllegalArgumentException("Empty or duplicate value");
        }
        return result;
    }

    private static Set<String> identifiers(Set<String> values) {
        if (values.size() > 256) throw new IllegalArgumentException("Too many progress flags");
        for (String value : values) identifier(value);
        return Set.copyOf(values);
    }

    private static void identifier(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_.-]{1,64}"))
            throw new IllegalArgumentException("Invalid progress identifier");
    }

    private static String joined(Set<String> values) {
        return values.stream().sorted().collect(Collectors.joining(","));
    }
}
