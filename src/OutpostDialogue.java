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
            w("EVACUATION ORDER: HOLD THE EAST ROAD."),
            r("Then let me through. There are people beyond that gate."),
            w("ICHOR CONTAMINATION. ALL DEPARTURES SUSPENDED."),
            r("Mara is still waiting for them. You were meant to keep their road open."),
            w("ROAD SECURE. NO SURVIVORS PERMITTED THROUGH."),
            r("That isn't what they asked you to do.")),
    GATE("THE ROAD OPENS",
            r("The chain is broken. Stretcher tracks run through the gate and into the trees."),
            r("Only one set of footprints comes back. Mara's."),
            r("The others got this far. I can follow them.")),
    CAMP_AFTER("A PLACE TO RETURN TO",
            m("The gate's quiet."),
            r("It's open. I'm going after the stretcher tracks."),
            m("I tried. The roots have swallowed the road. There's a dry scarf by the fire; take it before you go."),
            r("I'm not sure it would stay on."),
            m("Then I'll keep it dry."));

    public enum Speaker { RAINORAY, MARA, WARDEN }
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
}
