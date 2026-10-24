import java.util.List;
import java.util.Locale;

/** Area One: the last evacuation order survived the people who gave it. */
public enum OutpostDialogue {
    WAKE("THE LAST WATCH",
            r("Rain. Cold stone. I was in the supply room a moment ago."),
            r("One more count before dawn. That's what I kept telling myself."),
            r("No hands. No boots. I seem to have kept the exhaustion."),
            r("There's a lamp beyond the ditch. Please let someone be holding it.")),
    CAMP("THE LAMP AT CAMP",
            m("Easy. The last thing that crawled out of that ditch tried to eat my boots."),
            r("I'll try to be a better guest. I'm Rainoray. Quartermaster."),
            m("Rainoray? Your supplies reached us. You never did."),
            r("I'm having some trouble accounting for myself. Where is everyone?"),
            m("We sent the wounded east. Then the rift struck the gatehouse. The Warden turned on the road it was guarding."),
            r("And you stayed?"),
            m("Someone has to be here if they come back. I'm Mara."),
            m("Reach the east gate. See if the Warden can still hear us. I'll keep the lamp lit.")),
    CAMP_WAIT("MARA",
            m("The gatehouse is east, beyond the inner court. Don't let the old uniforms fool you."),
            r("I used to order those uniforms. I know how little they tell you.")),
    CAMP_LATE("THE LAMP AT CAMP",
            m("The gate's quiet. Was that your doing?"),
            r("Rainoray. Quartermaster. I was looking for a way out."),
            m("You're the name on our supply slips? I'm Mara. I kept a bed for you."),
            m("We sent the wounded east before the Warden turned. I tried to follow, but the roots had closed the forest road."),
            r("Then that's where I'll look next."),
            m("Come back if you find them. Come back if you don't, too.")),
    SIGNAL("THE MISSING NAMES",
            r("A dispatch, folded under the bell rope. 'Hold the east road until the last stretcher passes.'"),
            r("No death count. Just names marked missing. Mine is here, too."),
            r("Someone crossed it out, then wrote it back in. Mara should see this.")),
    INFIRMARY("ONE EMPTY BED",
            r("Every bed was stripped for bandages. Except this one."),
            r("A note on the blanket: 'For whoever comes back.'"),
            r("There's a dry dressing underneath. I'll leave it where she can find it.")),
    SUPPLIES("THE LAST DELIVERY",
            r("My seal. These are the crates I stayed to count."),
            r("'Blankets on top. Leave the weapons if the cart is too heavy.' My handwriting, too."),
            r("The blankets are gone. Good. At least that part worked.")),
    REPORT("STILL MISSING",
            r("I found the evacuation list. You crossed out my name."),
            m("I thought I'd seen you fall. Then I thought of how far away I was."),
            r("So you wrote it back in."),
            m("Yes. I'm glad I did.")),
    WARDEN("THE LAST ORDER",
            w("HALT. THE LAST STRETCHER HAS NOT PASSED."),
            r("They're gone. Mara's the only one left. Open the gate."),
            w("ICHOR DETECTED. YOU ARE NOT ON THE EVACUATION LIST."),
            r("Rainoray. Quartermaster. You watched me bring those supplies in."),
            w("QUARTERMASTER: DECEASED. HOLD THE ROAD."),
            r("Then you'll have to stop me twice.")),
    GATE("THE ROAD OPENS",
            r("The chain is broken. Stretcher tracks run through the gate and into the trees."),
            r("Only one set of footprints comes back. Mara's."),
            r("The others got this far. I can follow them.")),
    CAMP_AFTER("A PLACE TO RETURN TO",
            m("The gate's quiet."),
            r("It's open. I'm going after the stretcher tracks."),
            m("I tried. The roots have swallowed the road. There's a dry scarf by the fire; take it before you go."),
            r("I'm not sure it would stay on."),
            m("Then I'll keep it dry.")),
    SURVIVOR_TOBIN("BEHIND THE FENCE",
            t("Who's there? If you're a rift thing, I'm mostly bandages. Poor eating."),
            r("Rainoray. Quartermaster. Mostly water, these days."),
            t("The quartermaster? We carried your crates out and left your name on the list."),
            t("The fence came down behind me. I couldn't climb it, and nothing friendly fits under."),
            r("Mara's lamp is still lit at Captain Camp. Can you make it that far?"),
            t("For a lamp? I'll crawl. Thank you.")),
    SURVIVOR_ILSE("INSIDE THE THORNS",
            i("Stay back. I've got one good arm and I'm not afraid to use it."),
            r("Rainoray. Quartermaster. I cut the thorns, not you."),
            i("Ilse. Signals. I hid in here when the brambles came up. They grew faster than I could cut."),
            i("That chest is the quartermaster's cache. Locked tight, and the key went with the storehouse."),
            r("Then it's still my job. Go to Mara's lamp at Captain Camp."),
            i("If anyone can find one small key in this mess, it's the man who counted every nail.")),
    SURVIVOR_VENN("THE CULVERT",
            v("Took them long enough to send someone. I pictured someone taller."),
            r("Nobody sent me. I'm afraid I'm just late."),
            v("Venn. Sergeant, once. I told the stretcher crew to go on without me."),
            v("Then the culvert silted up and I spent a week talking to the rain."),
            r("Mara is keeping a fire at Captain Camp. She has room for you."),
            v("Mara. Good. Then I'll go and be shouted at properly.")),
    LEDGER("THE QUARTERMASTER'S CACHE",
            r("The cache opens. Bandages, a little coin, and my ledger."),
            r("Every crate, every blanket, every name that signed for them."),
            r("Tobin, Ilse, Venn. All signed the same morning. I'll see that the count comes out right.")),
    CAMP_RESCUED("SOMEONE CAME BACK",
            m("Someone walked into my camp tonight and asked for you by name."),
            r("I found them behind a fence I couldn't have climbed even with legs."),
            m("Then keep looking. The dispatch listed more people than the Warden let through.")),
    CAMP_HOME("THREE BEDS",
            m("Tobin, Ilse and Venn. Three beds that aren't empty anymore."),
            r("The count finally adds up. Better than I'd hoped."),
            m("Ilse found the last of the good tea. Take the coin we were saving with it. You've earned both."));

    /** The scene for the survivor in FieldObjects.SURVIVORS order. */
    static OutpostDialogue survivor(int index) {
        return switch (index) { case 0 -> SURVIVOR_TOBIN; case 1 -> SURVIVOR_ILSE; default -> SURVIVOR_VENN; };
    }

    public enum Speaker { RAINORAY, MARA, WARDEN, TOBIN, ILSE, VENN }
    public record Line(Speaker speaker, String text) { }
    private final String title;
    private final List<Line> lines;

    OutpostDialogue(String title, Line... lines) { this.title=title;this.lines=List.of(lines); }
    public String title() { return title; }
    public List<Line> lines() { return lines; }
    public String key() { return "story_outpost_"+name().toLowerCase(Locale.ROOT); }
    private static Line r(String text) { return new Line(Speaker.RAINORAY,text); }
    private static Line m(String text) { return new Line(Speaker.MARA,text); }
    private static Line w(String text) { return new Line(Speaker.WARDEN,text); }
    private static Line t(String text) { return new Line(Speaker.TOBIN,text); }
    private static Line i(String text) { return new Line(Speaker.ILSE,text); }
    private static Line v(String text) { return new Line(Speaker.VENN,text); }
}
