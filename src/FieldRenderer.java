import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

/**
 * Draws FieldObjects at 2x: the yards' fences and gates, things lying about, the people still missing,
 * and what the camp gains as they come home. Each piece is handed to the caller with the ground line it
 * should be depth sorted on.
 */
final class FieldRenderer {
    private FieldRenderer() { }

    private static final BufferedImage FENCE = B2BJ.loadImage("assets/props/outpost/stake_wall.png"),
            SIDE = B2BJ.loadImage("assets/props/outpost/field_palisade_side.png"),
            CRAWL = B2BJ.loadImage("assets/props/outpost/field_crawl.png"),
            // The near (south) wall is knee high so the yard stays visible from the 3/4 camera.
            LOW = B2BJ.loadImage("assets/props/outpost/field_fence_low.png"),
            CRAWL_LOW = B2BJ.loadImage("assets/props/outpost/field_crawl_low.png"),
            THORNS = B2BJ.loadImage("assets/props/forest/set_bramble.png"),
            KEY = B2BJ.loadImage("assets/props/outpost/field_key.png"),
            TIN = B2BJ.loadImage("assets/props/outpost/field_tin.png"),
            CHEST = B2BJ.loadImage("assets/props/outpost/field_chest.png"),
            PEOPLE = B2BJ.loadImage("assets/props/outpost/field_survivor.png"),
            BEDROLL = B2BJ.loadImage("assets/props/outpost/field_bedroll.png");
    static final int FENCE_W = 96, FENCE_H = 112, LOW_H = 56, SIDE_W = 16, SIDE_H = 64, PERSON_W = 48, PERSON_H = 40;

    /** Hands every visible field piece to `queue` as (ground y, painter). */
    static void queue(Graphics2D g, RuinedOutpostGame game, int cx, int cy, long frame, BiConsumer<Double, Runnable> queue) {
        if (!game.campaignMode()) return;
        int area = game.biome();
        for (var yard : FieldObjects.yards(area)) {
            for (int i = -1; i <= 1; i++) {
                double x = yard.x() + i * FieldObjects.Yard.SECTION;
                int variant = Math.floorMod((int) yard.x() / 64 + i, 3);
                queue.accept(yard.northY(), () -> fence(g, x, yard.northY(), variant, cx, cy));
                if (i != 0) queue.accept(yard.southY(), () -> lowFence(g, x, yard.southY(), (variant + 1) % 3, cx, cy));
            }
            for (int side = -1; side <= 1; side += 2) {
                double x = yard.x() + side * (FieldObjects.Yard.HALF_WIDTH + FieldObjects.Yard.SIDE / 2);
                for (int k = 0; k < 2; k++) {
                    double bottom = yard.northY() + (k + 1) * FieldObjects.Yard.SIDE_SECTION;
                    int variant = Math.floorMod(k + side + (int) yard.y() / 64, 3);
                    queue.accept(bottom, () -> side(g, x, bottom, variant, cx, cy));
                }
            }
        }
        for (var spot : FieldObjects.all(area)) switch (spot.kind()) {
            case CRAWL -> queue.accept(spot.y(), () -> {
                if (CRAWL_LOW != null) sprite(g, CRAWL_LOW, 0, 0, FENCE_W, LOW_H, spot.x(), spot.y() + 4, cx, cy);
                else sprite(g, CRAWL, 0, 0, FENCE_W, FENCE_H, spot.x(), spot.y() + 4, cx, cy);
            });
            case BRAMBLE -> { if (!game.fieldDone(spot)) queue.accept(spot.y(), () -> thorns(g, spot, cx, cy)); }
            case KEY, TIN -> {
                var at = game.fieldPosition(spot);
                if (at != null) queue.accept(at.y(), () -> item(g, spot.kind(), at.x(), at.y(), frame, cx, cy));
            }
            case CACHE -> queue.accept(spot.y(), () -> sprite(g, CHEST, game.fieldDone(spot) ? 32 : 0, 0, 32, 28, spot.x(), spot.y(), cx, cy));
            case SURVIVOR -> {
                if (!game.fieldDone(spot)) queue.accept(spot.y(), () -> person(g, spot.variant(), 0, spot.x(), spot.y(), cx, cy));
                else if (area == 0) {
                    var seat = FieldObjects.campSeat(game.campaignArea().hub(), spot.variant());
                    queue.accept(seat.y() - 1, () -> sprite(g, BEDROLL, spot.variant() % 2 * 40, 0, 40, 16, seat.x(), seat.y() + 6, cx, cy));
                    queue.accept(seat.y(), () -> person(g, spot.variant(), 1, seat.x(), seat.y(), cx, cy));
                }
            }
        }
        if (area == 0) {
            int tins = game.tinsDelivered();
            var stack = FieldObjects.campTins(game.campaignArea().hub());
            for (int i = 0; i < tins; i++) {
                double x = stack.x() + (i % 2) * 30 - 15, y = stack.y() - (i / 2) * 18;
                queue.accept(stack.y() + i * .01, () -> sprite(g, TIN, 0, 0, 14, 12, x, y, cx, cy));
            }
        }
    }

    /** The carried thing seen through the slime's body, low in the jelly beside its star. */
    static void carried(Graphics2D g, RuinedOutpostGame game, int cx, int cy, long frame) {
        var item = game.carried();
        if (item == null || game.player().bladeForm()) return;
        var p = game.player();
        double bob = Math.sin(frame * .08) * 2;
        var inside = (Graphics2D) g.create();
        inside.setComposite(AlphaComposite.SrcOver.derive(.78f));
        item(inside, item.kind(), p.x() + 14, p.y() + 30 + Math.round(bob / 2) * 2, 0, cx, cy);
        inside.dispose();
    }

    private static void fence(Graphics2D g, double x, double ground, int variant, int cx, int cy) {
        sprite(g, FENCE, variant * FENCE_W, 0, FENCE_W, FENCE_H, x, ground + 4, cx, cy);
    }

    private static void lowFence(Graphics2D g, double x, double ground, int variant, int cx, int cy) {
        if (LOW == null) fence(g, x, ground, variant, cx, cy);
        else sprite(g, LOW, variant * FENCE_W, 0, FENCE_W, LOW_H, x, ground + 4, cx, cy);
    }

    private static void side(Graphics2D g, double x, double bottom, int variant, int cx, int cy) {
        if (SIDE == null) { fallback(g, x, bottom, SIDE_W, SIDE_H, cx, cy); return; }
        int left = snap(x - cx) - SIDE_W, top = snap(bottom - cy) - SIDE_H * 2;
        CampaignRenderer.cell2x(g, SIDE, left, top, variant * SIDE_W, 0, SIDE_W, SIDE_H);
    }

    private static void thorns(Graphics2D g, FieldObjects.Spot gate, int cx, int cy) {
        if (THORNS == null) { fallback(g, gate.x(), gate.y(), 96, 30, cx, cy); return; }
        // Two clumps of the forest's bramble overlap the gap and spill over the posts either side.
        for (int k = -1; k <= 1; k += 2) sprite(g, THORNS, (k < 0 ? 0 : 80), 0, 80, 38, gate.x() + k * 48, gate.y() + 6, cx, cy);
    }

    private static void item(Graphics2D g, FieldObjects.Kind kind, double x, double y, long frame, int cx, int cy) {
        if (kind == FieldObjects.Kind.KEY) sprite(g, KEY, (int) (frame / 10 % 4) * 12, 0, 12, 12, x, y, cx, cy);
        else sprite(g, TIN, 0, 0, 14, 12, x, y, cx, cy);
    }

    private static void person(Graphics2D g, int who, int pose, double x, double y, int cx, int cy) {
        sprite(g, PEOPLE, FieldObjects.SPRITE_COLUMN[who] * PERSON_W, pose * PERSON_H, PERSON_W, PERSON_H, x, y, cx, cy);
    }

    /** A sheet cell whose bottom centre stands on (x, y), at 2x. */
    private static void sprite(Graphics2D g, BufferedImage sheet, int sx, int sy, int w, int h, double x, double y, int cx, int cy) {
        if (sheet == null || sx + w > sheet.getWidth() || sy + h > sheet.getHeight()) { fallback(g, x, y, w, h, cx, cy); return; }
        CampaignRenderer.cell2x(g, sheet, snap(x - cx) - w, snap(y - cy) - h * 2, sx, sy, w, h);
    }

    /** Missing art never hides a gameplay object. */
    private static void fallback(Graphics2D g, double x, double y, int w, int h, int cx, int cy) {
        g.setColor(new java.awt.Color(0x3a2a20));
        g.fillRect(snap(x - cx) - w, snap(y - cy) - h * 2, w * 2, h * 2);
    }

    private static int snap(double v) { return (int) Math.round(v / 2) * 2; }
}
