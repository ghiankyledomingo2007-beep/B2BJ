public final class WangTileset {
    private static final int CELL_SIZE = 32;
    private static final int[] COLUMNS = {2, 1, 2, 3, 2, 1, 0, 1, 3, 2, 3, 0, 1, 0, 3, 0};
    private static final int[] ROWS = {1, 1, 0, 0, 2, 0, 1, 3, 1, 3, 2, 0, 2, 2, 3, 3};

    private WangTileset() {
    }

    public static int sourceX(int cornerMask) {
        return COLUMNS[cornerMask] * CELL_SIZE;
    }

    public static int sourceY(int cornerMask) {
        return ROWS[cornerMask] * CELL_SIZE;
    }
}
