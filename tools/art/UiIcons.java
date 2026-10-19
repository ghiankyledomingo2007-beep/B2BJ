import java.util.Map;

/** Hand-placed UI sprites as character maps: icons (12x12), boss emblems (16x16), cursor and trims. */
final class UiIcons {
    private UiIcons() { }

    static final Map<Character, Integer> KEY = Map.ofEntries(
            Map.entry('k', Pal.c(0x0b0a14)), Map.entry('w', Pal.c(0xece2c8)), Map.entry('x', Pal.c(0xffffff)),
            Map.entry('g', Pal.GOLD[1]), Map.entry('G', Pal.GOLD[2]), Map.entry('y', Pal.GOLD[3]), Map.entry('Y', Pal.GOLD[4]), Map.entry('z', Pal.GOLD[5]),
            Map.entry('c', Pal.CYAN[1]), Map.entry('C', Pal.CYAN[2]), Map.entry('t', Pal.CYAN[3]), Map.entry('T', Pal.CYAN[4]), Map.entry('u', Pal.CYAN[5]),
            Map.entry('b', Pal.BONE[0]), Map.entry('B', Pal.BONE[1]), Map.entry('o', Pal.BONE[2]), Map.entry('O', Pal.BONE[3]),
            Map.entry('r', Pal.c(0x503330)), Map.entry('R', Pal.c(0x996f5d)), Map.entry('n', Pal.c(0xb48a67)), Map.entry('N', Pal.c(0xcbaa6f)),
            Map.entry('f', Pal.FIRE[1]), Map.entry('F', Pal.FIRE[2]), Map.entry('h', Pal.FIRE[3]), Map.entry('H', Pal.FIRE[4]), Map.entry('e', Pal.FIRE[5]),
            Map.entry('m', Pal.c(0x18172a)), Map.entry('M', Pal.c(0x252037)), Map.entry('s', Pal.c(0x3c3a56)), Map.entry('S', Pal.c(0x635e7c)), Map.entry('l', Pal.c(0x8f8bb0)),
            Map.entry('i', Pal.IRON[3]), Map.entry('I', Pal.IRON[4]), Map.entry('j', Pal.IRON[5]), Map.entry('J', Pal.c(0xcfd6e2)),
            Map.entry('v', Pal.VIOLET[2]), Map.entry('V', Pal.VIOLET[3]), Map.entry('p', Pal.VIOLET[4]),
            Map.entry('d', Pal.CRIMSON[2]), Map.entry('D', Pal.CRIMSON[3]), Map.entry('q', Pal.CRIMSON[4]), Map.entry('Q', Pal.c(0xf16458)),
            Map.entry('1', Pal.STONE[2]), Map.entry('2', Pal.STONE[3]), Map.entry('3', Pal.STONE[4]), Map.entry('4', Pal.STONE[5]),
            Map.entry('5', Pal.STONE[6]), Map.entry('6', Pal.STONE[7]),
            Map.entry('W', Pal.WOOD[3]), Map.entry('X', Pal.WOOD[4]), Map.entry('Z', Pal.WOOD[5]));

    /** Paints a character map at (ox, oy); '.' is transparent. Rows must all be `width` long. */
    static void put(Canvas c, int ox, int oy, String name, String... rows) {
        int width = rows[0].length();
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != width) throw new IllegalStateException(name + " row " + y + " is " + rows[y].length() + " wide, not " + width);
            for (int x = 0; x < width; x++) {
                char ch = rows[y].charAt(x);
                if (ch == '.') continue;
                Integer colour = KEY.get(ch);
                if (colour == null) throw new IllegalStateException(name + ": unknown colour '" + ch + "'");
                c.set(ox + x, oy + y, colour);
            }
        }
    }

    /** The icon maps in {@code Ui.Icon} order. */
    static String[][] icons() {
        return new String[][]{
            { // SHARD
                ".....kk.....", "....kzYk....", "...kzYyGk...", "..kzYyyGGk..", "..kYyyyGgk..", "..kYyyGGgk..",
                "..kyyyGggk..", "..kyyGGggk..", "...kyGggk...", "....kGgk....", ".....kk.....", "............"},
            { // OBJECTIVE
                ".....kk.....", "....kuTk....", "...kuTTtk...", "..kuTTtttk..", ".kuTTttttCk.", "kTTtttttCCCk",
                "kTtttttCCCck", ".ktttttCCck.", "..ktttCCck..", "...ktCCck...", "....kCck....", ".....kk....."},
            { // SKULL
                "............", "...kkkkkk...", "..kOOOOOOk..", ".kOOOOOOOOk.", ".kOkkOOkkOk.", ".kOkkOOkkok.",
                ".kOOOkkOOok.", "..kOOOOOok..", "..kOkOkOok..", "...kkkkkk...", "............", "............"},
            { // CAMP
                ".....kk.....", ".....khk....", "....khHk....", "...khHHhk...", "...kHeHHk...", "..khHeeHhk..",
                "..kFhHeHFk..", "..kFFhhFFk..", ".kkk....kkk.", "kXZXkkkkXZXk", ".kkWXZZXWkk.", "...kkkkkk..."},
            { // GATE
                "...kkkkkk...", "..k455554k..", ".k45kkkk54k.", ".k4k1111k4k.", ".k4k1tt1k4k.", ".k4k1tt1k4k.",
                ".k4k1111k4k.", ".k4k1111k4k.", ".k3k1111k3k.", ".k3k1111k3k.", "kk3kkkkkk3kk", "kkkkkkkkkkkk"},
            { // MEMORY
                ".....kk.....", "....kYYk....", "....kYyk....", "...kYzzyk...", ".kkYzzzzykk.", "kYYzzzzzzyyk",
                "kyyyzzzzyGGk", ".kkyyzzyGkk.", "...kyyyGk...", "....kyGk....", "....kGGk....", ".....kk....."},
            { // CACHE
                "............", "..kkkkkkkk..", ".kZXXXXXXZk.", ".kXWWWWWWXk.", "kkkkkyykkkkk", "kZXXkYGkXXZk",
                "kXWWkGgkWWXk", "kXWWWkkWWWXk", "kXWWWWWWWWXk", "kWrrrrrrrrWk", "kkkkkkkkkkkk", "............"},
            { // PLAYER
                "............", "............", ".....kk.....", "....kTTk....", "...kuTTtk...", "..kuTTtttk..",
                ".kTTtktktCk.", ".kTttktktCk.", ".kTtttttCCk.", "..kCCCCCCk..", "...kkkkkk...", "............"},
            { // MENU
                "............", "............", ".kkkkkkkkkk.", ".kwwwwwwwwk.", ".kkkkkkkkkk.", ".kkkkkkkkkk.",
                ".kwwwwwwwwk.", ".kkkkkkkkkk.", ".kkkkkkkkkk.", ".kwwwwwwwwk.", ".kkkkkkkkkk.", "............"},
            { // GEAR
                "....kkkk....", ".kk.klSk.kk.", ".kSkkSSkkSk.", "..kSSlSSSk..", "kkkSSkkSSkkk", "klSSk..kSSSk",
                "kSSSk..kSSSk", "kkkSSkkSSkkk", "..kSSSSSSk..", ".kSkkSSkkSk.", ".kk.kSSk.kk.", "....kkkk...."},
            { // SOUND
                "............", "....kk......", "...kSk...w..", "kkklSk.w..w.", "kllllk..w.w.", "kllllk..w.w.",
                "kllllk..w.w.", "kkklSk.w..w.", "...kSk...w..", "....kk......", "............", "............"},
            { // EYE
                "............", "............", "...kkkkkk...", ".kkwwwwwwkk.", "kwwwkttkwwwk", "kwwktkkTkwwk",
                "kwwktkkTkwwk", "kwwwkttkwwwk", ".kkwwwwwwkk.", "...kkkkkk...", "............", "............"},
            { // FLASK
                "....kkkk....", "....knnk....", "....kjjk....", "....kjjk....", "...kjhhjk...", "..kjhHHhjk..",
                ".kjhHeHhhjk.", ".kjhHHhhFjk.", ".kjhhhhFFjk.", ".kjFFFFFFjk.", "..kjjjjjjk..", "...kkkkkk..."},
            { // MAP
                "............", "kkkkkkkkkkkk", "kOOOoooOOOOk", "kOOOoooOOdOk", "kOdOooOOOOOk", "kOOdooOOdOOk",
                "kOOOdoOdOOOk", "kOOOodOOOOOk", "kOOOoooOOOOk", "kkkkkkkkkkkk", "............", "............"},
            { // KEYS
                "............", ".kkkkkkkkkk.", ".kJJJJJJJJk.", ".kJjjjjjjIk.", ".kJjjkkjjIk.", ".kJjkjjkjIk.",
                ".kJjkkkkjIk.", ".kJjkjjkjIk.", ".kJIIIIIIIk.", ".kiiiiiiiik.", ".kkkkkkkkkk.", "............"},
            { // DOOR
                ".kkkkkkk....", ".kZXXXZk....", ".kXWWWXk....", ".kXWWWXk.k..", ".kXWWWXkkTk.", ".kXWyWTTTTTk",
                ".kXWWWXkkTk.", ".kXWWWXk.k..", ".kXWWWXk....", ".kXWWWXk....", "kkkkkkkkk...", "............"},
            { // BACK
                "............", "............", "....kk......", "...kTk......", "..kTTkkkkkk.", ".kuTTTTTTTk.",
                ".kTTTTTTTCk.", "..kTTkkkkkk.", "...kTk......", "....kk......", "............", "............"},
            { // PLAY
                "............", "..kk........", "..kukk......", "..kuTTkk....", "..kTTTTTkk..", "..kTTTTTTTk.",
                "..kTTTTTTCk.", "..kTTTTCkk..", "..kTTCkk....", "..kCkk......", "..kk........", "............"},
            { // NEW
                "............", "....kkkk....", "....kYyk....", "....kYyk....", ".kkkkYykkkk.", ".kYYYzzYYyk.",
                ".kyyyzzyyGk.", ".kkkkyGkkkk.", "....kyGk....", "....kyGk....", "....kkkk....", "............"},
            { // HEART
                "............", ".kkkk..kkkk.", "kQQqqkkQqqDk", "kQxQqqqqqqDk", "kQQqqqqqqDDk", ".kqqqqqqDDk.",
                "..kqqqqDDk..", "...kqqDDk...", "....kDDk....", ".....kk.....", "............", "............"},
            { // CROWN
                "............", "............", ".k...kk...k.", "kYk.kYYk.kYk", "kYYkYzzYkYYk", "kYyYyzzyYyYk",
                "kyyyyyyyyyGk", "kyTyyQqyyTGk", "kyyyyyyyyGGk", "kkkkkkkkkkkk", "............", "............"},
            { // STAIR
                "kkkkk.......", "k666k.......", "k555kkkk....", "k555666k....", "k444555kkkk.", "k444444666k.",
                "k333333555k.", "k333333333k.", "k222222222k.", "kkkkkkkkkkk.", "............", "............"},
            { // TIP
                ".....kk.....", "....kYYk....", "...kYkkyk...", "..kYykkyGk..", ".kYyykkyyGk.", "kYyyykkyyyGk",
                "kyyyyyyyyGGk", ".kyyykkyGGk.", "..kyykkGGk..", "...kyyyGk...", "....kGGk....", ".....kk....."},
            { // SWORD
                "..........kk", ".........kJk", "........kJjk", ".......kJjk.", "......kJjk..", ".....kJjk...",
                ".k..kJjk....", ".kykJjk.....", "..kyjk......", ".kRkyk......", "kRk..k......", "kk.........."},
        };
    }

    static final String[] WARDEN = {
        "................", "...k...kk...k...", "..kYk.kYYk.kYk..", "..kYykkYYkkyYk..", "...kVVVVVVVVk...",
        "..kVpVVVVVVVVk..", "..kVpVVVVVVVvk..", ".kVVkkkkkkkkVvk.", ".kVktTTTTTTtkvk.", ".kVVkkkkkkkkVvk.",
        "..kVVVvVVvVVvk..", "..kVVvkVVkvVvk..", "...kVvkVVkvvk...", "....kkkvvkkk....", "......kkkk......", "................"};

    static final String[] BRIAR = {
        "................", "...kkk....kkk...", "..kpVVk..kVVvk..", ".kpVVVVkkVVVvvk.", "kYkVVVVVVVVVvkYk",
        ".kyVVVVVVVVvvyk.", ".kVyVVVVVVVvyvk.", "..kVyyVVVvyyvk..", "..kVVVyyyyvvvk..", "...kVVVVVvvvk...",
        "..kYkVVVvvvkYk..", ".....kVVvvk.....", "......kvvk......", ".......kk.......", "................", "................"};

    static final String[] CURSOR = {
        "...k........", "..kyk.......", "..kykkkkkkk.", "kkkyJJJJJJk.", "kRRyjjjjjjjk",
        "kkkyiiiiiik.", "..kykkkkkkk.", "..kyk.......", "...k........"};

    static final String[] CURSOR_GLINT = {
        "...k........", "..kyk.......", "..kykkkkkkk.", "kkkyJJJJJxk.", "kRRyjjjjjxxk",
        "kkkyiiiiiik.", "..kykkkkkkk.", "..kyk.......", "...k........"};

    static final String[] CLASP = {
        ".kkkkkkkkkk.", "kNnnnkknnnRk", "knRRkTukRRrk", "knRRktTkRRrk", "knRRkkkkRRrk", "kRrrrrrrrrrk", ".kkkkkkkkkk."};

    static final String[] DIAMOND = {"...k...", "..kzk..", ".kzYyk.", "kzYyyGk", ".kyyGk.", "..kGk..", "...k..."};

    static final String[] MOUSE_L = {
        "..kkkkkk..", ".kTTkjjjk.", "kTTTkjjjjk", "kTTTkjjjjk", "kTTTkjjjjk", "kkkkkkkkkk", "kjjjjjjjjk",
        "kjjjjjjjjk", "kjjjjjjjjk", "kjjjjjjjik", "kjjjjjjiik", ".kjjjjiik.", "..kiiiik..", "...kkkk..."};

    /** A map turned on its side (columns become rows). */
    static String[] transpose(String[] rows) {
        String[] out = new String[rows[0].length()];
        for (int x = 0; x < out.length; x++) {
            StringBuilder line = new StringBuilder();
            for (String row : rows) line.append(row.charAt(x));
            out[x] = line.toString();
        }
        return out;
    }

    static String[] mirror(String[] rows) {
        String[] out = new String[rows.length];
        for (int i = 0; i < rows.length; i++) out[i] = new StringBuilder(rows[i]).reverse().toString();
        return out;
    }
}
