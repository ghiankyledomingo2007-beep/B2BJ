import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** GCD campaign narrative; the game owns travel, combat, rewards and ending selection. */
public final class CampaignStory {
    public enum Ending { CLOSE_RIFT, HUMAN_FORM }

    private static final String[] NPCS = {"MARA", "SEDGE", "ILYRA", "ORIN"};
    private static final String[] QUESTS = {
            "THE MISSING NAMES", "A ROAD FOR OTHERS", "AN ORDER RELEASED", "A WATCH SHARED"
    };
    private static final String[] MEMORIES = {
            "EXPEDITION DISPATCH", "WAYFINDER'S NOTE", "RELIEF ORDER", "MAINTENANCE RECORD"
    };
    private static final String[] BOSSES = {
            "OUTPOST WARDEN", "BRIARHEART", "OATHKEEPER", "ICHOR GOLEM"
    };
    private static final String[][] INTRO = {
            {"MARA: I THOUGHT THAT GLOW WAS A SIGNAL LAMP.",
                    "RAINORAY: WOULDN'T BE THE STRANGEST JOB I'VE HAD.",
                    "MARA: THE WARDEN BARS THE EAST ROAD. BREAK ITS WATCH.",
                    "IF YOU FIND THE LOST DISPATCH, BRING ME THEIR NAMES."},
            {"SEDGE: YOU CROSSED THE OUTPOST? SOMEONE SHOULD USE THOSE ROADS.",
                    "RAINORAY: WHERE DOES THIS ONE GO?",
                    "SEDGE: BELOW. BRIARHEART HAS ROOTS AROUND THE STAIR.",
                    "FIND THE WAYFINDER'S NOTE. OTHERS WILL NEED A SAFE ROAD."},
            {"ILYRA: THAT LIGHT KNOWS THESE HALLS. YOU DO NOT.",
                    "RAINORAY: I'M NOT ONE OF YOUR KNIGHTS.",
                    "ILYRA: THEY HELD UNTIL RELIEVED. THE OATHKEEPER STILL RINGS.",
                    "LOOK FOR THEIR RELIEF ORDER. SOMEONE MUST KNOW THEIR FATE."},
            {"ORIN: THE GOLEM WAS MEANT TO HOLD OUR ICHOR. IT FEEDS THE TEAR.",
                    "BREAK IT. SPEND THE CORE TO CLOSE THE RIFT AND REMAIN A SLIME.",
                    "KEEP THE CORE FOR A HUMAN BODY; THE OLD SEAL NEEDS A WATCH.",
                    "OUR MAINTENANCE RECORD MAY SHOW HOW TO SHARE THAT WATCH."}
    };
    private static final String[][] NOTES = {
            {"THE EAST ROAD STAYS OPEN UNTIL THE LAST STRETCHER PASSES.",
                    "IF COMMAND ASKS WHY THE BELL IS STILL RINGING,",
                    "TELL THEM TO CARRY ONE.",
                    "THE ATTACHED ROSTER HAS NAMES, BUT NO FINAL COUNT."},
            {"THE CAPTAIN TOOK THE STONE ROAD. WE TOOK THE WATERCOURSE.",
                    "IF THE FLAGS GO DARK, FOLLOW THE WHITE CLOTH.",
                    "IT WAS NEVER A SURRENDER.",
                    "AN EVACUATION ROUTE IS MARKED ALONG THE OLD CHANNEL."},
            {"RELIEF APPROVED. COURIER UNAVAILABLE.",
                    "HOLD THIS ORDER UNTIL THE ROADS ARE SECURED.",
                    "BY THEN, THE BELL HAD ALREADY STOPPED ANSWERING.",
                    "THE ORDER WAS WRITTEN. IT WAS NEVER CARRIED."},
            {"FOUR KEEPERS. SIX HOURS EACH.",
                    "NEVER BIND ONE HEART TO THE WHOLE DAY.",
                    "THE WARD NEEDS A WATCH, NOT A MARTYR.",
                    "THE SEAL WAS DESIGNED TO BE MAINTAINED IN SHIFTS."}
    };
    private static final String[][] TURN_IN = {
            {"MARA: NO FINAL COUNT. ONLY NAMES CROSSED OUT AND WRITTEN BACK.",
                    "RAINORAY: KEEP THE NAMES. LEAVE THE DOOR OPEN.",
                    "MARA: THEN I CAN DO BOTH. I'LL MAKE THIS CAMP A SHELTER.",
                    "THE MISSING NAMES / COMPLETE"},
            {"SEDGE: AN OLD WATERCOURSE. I KNOW WHERE THAT COMES OUT.",
                    "RAINORAY: CAN PEOPLE STILL USE IT?",
                    "SEDGE: ONCE I CLEAR IT. YOU AREN'T THE ONLY ONE OPENING ROADS.",
                    "A ROAD FOR OTHERS / COMPLETE"},
            {"ILYRA: THEY WERE MEANT TO COME HOME.",
                    "RAINORAY: WILL YOU WRITE THAT DOWN?",
                    "ILYRA: EVERY NAME. SOMEONE SHOULD HAVE TOLD THEM IN TIME.",
                    "AN ORDER RELEASED / COMPLETE"},
            {"ORIN: WE DESIGNED SHIFTS. I HAD FORGOTTEN THAT PART.",
                    "RAINORAY: START WITH THE PART WHERE YOU STOP.",
                    "ORIN: WHEN THE NEXT KEEPER ARRIVES. MARA CAN HELP ME FIND ONE.",
                    "A WATCH SHARED / COMPLETE"}
    };
    private static final String[][] AFTER_QUEST = {
            {"MARA: THERE'S ROOM BESIDE THE FIRE. COME BACK WHEN YOU CAN.",
                    "THE DISPATCH IS SAFE. I'M KEEPING THE DOOR OPEN."},
            {"SEDGE: THE WHITE CLOTH MARKS OUR ROAD NOW.",
                    "I'LL CLEAR THIS SIDE. YOU KEEP A WAY BACK."},
            {"ILYRA: THE RECORD SAYS THEY WERE ABANDONED, NOT DESERTERS.",
                    "THEIR NAMES WILL OUTLAST THE ORDER THAT LEFT THEM HERE."},
            {"ORIN: THE SEAL CAN HOLD, IF PEOPLE SHARE THE WATCH.",
                    "CLOSING THE RIFT SPENDS THE CORE. KEEPING IT RESTORES YOU.",
                    "BOTH CHOICES ARE YOURS, RAINORAY."}
    };

    private final Set<String> flags = new HashSet<>();
    private String title = "";
    private String[] lines = new String[0];

    /** Opens a conversation; true means the caller should award this quest once. */
    public boolean talk(int biome) {
        checkBiome(biome);
        boolean completed = flags.contains("memory_" + biome) && flags.add("quest_" + biome);
        boolean firstMeeting = flags.add("talked_" + biome);
        title = NPCS[biome];
        if (completed) lines = TURN_IN[biome];
        else if (firstMeeting) lines = INTRO[biome];
        else if (flags.contains("quest_" + biome)) lines = AFTER_QUEST[biome];
        else lines = new String[] {"THE ROAD IS OPEN TO YOU. TAKE YOUR TIME.",
                    "LOOK FOR THE " + MEMORIES[biome] + " ON A SIDE PATH.",
                    "RETURN TO " + NPCS[biome] + " WHEN YOU FIND IT."};
        return completed;
    }

    /** First optional landmark supplies the local memory; finding it again changes nothing. */
    public boolean discover(int biome) {
        checkBiome(biome);
        if (!flags.add("memory_" + biome)) return false;
        title = MEMORIES[biome];
        lines = NOTES[biome];
        return true;
    }

    public void advanceDialogue() { title = ""; lines = new String[0]; }
    public boolean dialogueOpen() { return lines.length > 0; }
    public String dialogueTitle() { return title; }
    public String[] dialogueLines() { return lines.clone(); }
    public Set<String> questFlags() { return Set.copyOf(flags); }

    /** Validates a complete snapshot before replacing live state. */
    public void restoreQuestFlags(Set<String> saved) {
        Objects.requireNonNull(saved, "quest flags");
        Set<String> restored = new HashSet<>();
        for (String flag : saved) {
            if (flag == null || !flag.matches("(talked|memory|quest)_[0-3]"))
                throw new IllegalArgumentException("Invalid quest flag: " + flag);
            restored.add(flag);
        }
        for (int biome = 0; biome < 4; biome++) if (restored.contains("quest_" + biome)) {
            restored.add("memory_" + biome);
            restored.add("talked_" + biome);
        }
        flags.clear();
        flags.addAll(restored);
        advanceDialogue();
    }

    public static String npcName(int biome) { checkBiome(biome); return NPCS[biome]; }
    public static String questName(int biome) { checkBiome(biome); return QUESTS[biome]; }
    public static String memoryName(int biome) { checkBiome(biome); return MEMORIES[biome]; }

    public String questObjective(int biome) {
        checkBiome(biome);
        if (flags.contains("quest_" + biome)) return QUESTS[biome] + " / COMPLETE";
        if (flags.contains("memory_" + biome)) return "RETURN TO " + NPCS[biome];
        if (!flags.contains("talked_" + biome)) return "TALK TO " + NPCS[biome] + " AT CAMP";
        return "FIND THE " + MEMORIES[biome];
    }

    public String objective(int biome, boolean bossDefeated) {
        checkBiome(biome);
        if (bossDefeated) return switch (biome) {
            case 0 -> "TAKE THE ROAD INTO THE CORRUPTED FOREST";
            case 1 -> "DESCEND INTO THE DEMON CATACOMBS";
            case 2 -> "CLIMB TO THE RIFT CITADEL";
            default -> "REACH THE RIFT AND CHOOSE ITS FATE";
        };
        if (!flags.contains("talked_" + biome)) return "TALK TO " + NPCS[biome] + " AT CAMP";
        if (flags.contains("memory_" + biome) && !flags.contains("quest_" + biome))
            return "RETURN TO " + NPCS[biome] + " WITH THE MEMORY";
        return "FIND AND DEFEAT THE " + BOSSES[biome];
    }

    public static String[] openingLines() {
        return new String[] {
                "RAINORAY. ANOTHER SHIFT. THEN NOTHING.",
                "YOU WAKE AS A SLIME AMONG THE FALLEN KNIGHTS.",
                "THEIR ICHOR OFFERS A BLADE. ITS POWER WILL NOT LAST.",
                "FIND THE RIFT BENEATH THE KINGDOM. DECIDE WHAT TO KEEP."
        };
    }

    /** Text for first arrival in a biome, not for returning through its portal. */
    public static String[] transitionLines(int biome) {
        checkBiome(biome);
        return switch (biome) {
            case 0 -> openingLines();
            case 1 -> new String[] {
                    "THE WARDEN FALLS. MARA LIGHTS A LAMP FOR THE EAST ROAD.",
                    "THE FOREST ANSWERS WITH A HUNDRED SMALLER LIGHTS.",
                    "WHITE CLOTH MARKS A PATH BETWEEN THE ROOTS."
            };
            case 2 -> new String[] {
                    "BRIARHEART'S ROOTS LOOSEN. THE STAIR OPENS BELOW.",
                    "SEDGE TIES WHITE CLOTH ABOVE THE DESCENT.",
                    "UNDER THE EARTH, AN OLD BELL CONTINUES TO RING."
            };
            default -> new String[] {
                    "THE OATHKEEPER'S BELL GOES QUIET. ILYRA OPENS A FRESH PAGE.",
                    "ABOVE THE VAULTS, THE CITADEL SHINES.",
                    "ITS LIGHT IS EVERYTHING THE KINGDOM LOST."
            };
        };
    }

    public static String endingTitle(Ending ending) {
        return switch (Objects.requireNonNull(ending, "ending")) {
            case CLOSE_RIFT -> "A PLACE BESIDE THE FIRE";
            case HUMAN_FORM -> "THE NEXT WATCH";
        };
    }

    public static String[] endingLines(Ending ending) {
        return switch (Objects.requireNonNull(ending, "ending")) {
            case CLOSE_RIFT -> new String[] {
                    "THE RIFT CLOSES FOR GOOD. THE BORROWED ARMOR FALLS AWAY.",
                    "RAINORAY REMAINS A SLIME. HE IS STILL HERE.",
                    "AT THE OUTPOST, MARA LEAVES A PLACE BESIDE THE FIRE.",
                    "THIS TIME, HE STAYS LONG ENOUGH TO FEEL ITS WARMTH."
            };
            case HUMAN_FORM -> new String[] {
                    "THE SEAL HOLDS. RAINORAY STANDS ON HUMAN FEET.",
                    "THE RIFT REMAINS. THE SURVIVORS WILL SHARE ITS WATCH.",
                    "WHEN THE NEXT KEEPER ARRIVES, HE HANDS OVER THE WATCH KEY.",
                    "THEN HE WALKS INTO THE MORNING."
            };
        };
    }

    private static void checkBiome(int biome) {
        if (biome < 0 || biome >= NPCS.length) throw new IllegalArgumentException("biome");
    }
}
