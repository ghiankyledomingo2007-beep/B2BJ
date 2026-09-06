public final class OutpostStory {
    public enum Phase {
        PROLOGUE,
        HUNT,
        GUARDIAN,
        ESCAPE,
        COMPLETE,
        DEAD
    }

    public static final int SCOUT_TOTAL = 21;

    private Phase phase = Phase.PROLOGUE;
    private int defeatedScouts;

    public void begin() {
        if (phase == Phase.PROLOGUE) {
            phase = Phase.HUNT;
        }
    }

    public void scoutDefeated() {
        if (phase != Phase.HUNT || defeatedScouts == SCOUT_TOTAL) {
            return;
        }
        defeatedScouts++;
    }

    public void enterGatehouse() { if (phase == Phase.HUNT) phase = Phase.GUARDIAN; }
    public void resume() { phase = Phase.HUNT; }

    public void guardianDefeated() {
        if (phase == Phase.GUARDIAN) {
            phase = Phase.ESCAPE;
        }
    }

    public void escaped() {
        if (phase == Phase.ESCAPE) {
            phase = Phase.COMPLETE;
        }
    }

    public void playerDied() {
        if (phase != Phase.COMPLETE) {
            phase = Phase.DEAD;
        }
    }

    public void restart() {
        phase = Phase.PROLOGUE;
        defeatedScouts = 0;
    }

    public Phase phase() {
        return phase;
    }

    public int defeatedScouts() {
        return defeatedScouts;
    }

    public boolean blocksGameplay() {
        return phase == Phase.PROLOGUE
                || phase == Phase.COMPLETE
                || phase == Phase.DEAD;
    }

    public String objective() {
        return switch (phase) {
            case PROLOGUE -> "PRESS ENTER TO WAKE";
            case HUNT -> "REACH THE EAST GATEHOUSE";
            case GUARDIAN -> "BREAK THE OUTPOST WARDEN";
            case ESCAPE -> "CROSS THE EASTERN GATE";
            case COMPLETE -> "OUTPOST CLEARED";
            case DEAD -> "PRESS R TO REFORM";
        };
    }

    public String overlayTitle() {
        return switch (phase) {
            case PROLOGUE -> "RUINED OUTPOST";
            case COMPLETE -> "OUTPOST CLEARED";
            case DEAD -> "FORM LOST";
            default -> "";
        };
    }

    public String[] overlayLines() {
        return switch (phase) {
            case PROLOGUE -> new String[] {
                    "RAINORAY. ANOTHER SHIFT. THEN NOTHING.",
                    "YOU WAKE AS A SLIME AMONG THE FALLEN KNIGHTS.",
                    "THEIR ICHOR OFFERS A BLADE. ITS POWER WILL NOT LAST.",
                    "PRESS ENTER"
            };
            case COMPLETE -> new String[] {
                    "THE KINGDOM OPENS BEFORE YOU.",
                    "THE CORRUPTED FOREST WAITS. THIS IS ONLY THE BEGINNING.",
                    "PRESS R TO RUN AGAIN"
            };
            case DEAD -> new String[] {
                    "YOUR ICHOR RETURNS TO THE STONES.",
                    "PRESS R TO REFORM AT YOUR LAST CAMP"
            };
            default -> new String[0];
        };
    }
}
