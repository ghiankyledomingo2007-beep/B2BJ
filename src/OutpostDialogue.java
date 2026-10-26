import java.util.List;
import java.util.Locale;

/** Area One: the last evacuation order survived the people who gave it. */
public enum OutpostDialogue {
    WAKE("ONE MORE CHANCE",
            r("Stay with me. I've almost got it open."),
            r("Did they get out?"),
            s("I cannot confirm. Your previous body has been lost."),
            r("Then... who am I talking to?"),
            s("Raphael. Stay with my voice."),
            s("Reconstructing a body that can survive here."),
            r("A slime? That was your best option?"),
            s("It was the one that survived."),
            r("I can still change back?"),
            s("For a while. Once we have enough Ichor."),
            r("Where are my hands?"),
            s("Try moving toward the light."),
            r("Someone's still out there."),
            s("One step at a time."),
            r("You'll have to talk me through the steps.")),
    CAMP("THE LAMP AT CAMP",
            m("Stop there. There are wounded behind me."),
            r("I heard someone calling. I wasn't coming for the food."),
            s("You are looking at the food."),
            r("I'm having a strange morning."),
            m("Then start with your name."),
            r("Rainoray. That's the part I'm sure about."),
            m("I'm Shuna. Our people were sent east. Then the rift struck, and the Warden sealed their road."),
            m("I have to keep this ward standing. If it fails, the wounded have nowhere safe to rest."),
            m("Someone called from the storehouse south of the road. You might fit under its fence."),
            m("Follow the torn banners east, then north at Inner Court. Be careful. The dead still know how to fight.")),
    CAMP_WAIT("SHUNA",
            m("East past the banners, then north at Inner Court. The signal tower lies north of camp if you need bearings."),
            r("East, then north. I'll remember."),
            m("Rest to steady your Ichor. Leave supplies here or at a lit brazier; we'll gather them.")),
    CAMP_LATE("THE LAMP AT CAMP",
            m("The gate's quiet. Was that your doing?"),
            r("Rainoray. I was looking for a way through."),
            m("I'm Shuna. You've opened a road we thought we'd lost."),
            m("We sent the wounded east before the Warden turned. I tried to follow, but the roots had closed the forest road."),
            r("Then that's where I'll look next."),
            m("Come back if you find them. Come back if you don't, too.")),
    SIGNAL("THE BROKEN ORDER",
            r("A dispatch. 'Seal the road against Ichor-bearing life.' Someone added it over the evacuation order."),
            s("That includes your new body. The Warden will see a threat, not a traveler."),
            r("There's a list of missing people underneath. Shuna should see it.")),
    INFIRMARY("ONE EMPTY BED",
            r("Every bed was stripped for bandages. Except this one."),
            r("A note on the blanket: 'For whoever comes back.'"),
            r("There's a dry dressing underneath. I'll leave it where she can find it.")),
    SUPPLIES("WHAT THEY LEFT BEHIND",
            r("Weapons left behind. Empty spaces where the blankets were."),
            r("A note: 'Leave the heavy crates. Make room for the wounded.'"),
            r("We packed our van like that. I hope those supplies reached someone.")),
    REPORT("STILL MISSING",
            r("I found the dispatch. The Warden was told to seal the road against Ichor."),
            m("After we sent the wounded through. No one could get back to them."),
            r("Kaijin, Eren, Rigurd. You left their names marked missing."),
            m("Until someone brings me an answer, that's where they'll stay.")),
    WARDEN("THE LAST ORDER",
            w("CONTAMINATED LIFEFORM. PASSAGE DENIED."),
            r("There are people on the other side."),
            w("THE ROAD IS SEALED FOR THEIR PROTECTION."),
            s("The order is damaged. It cannot distinguish a rescue from an attack."),
            r("Can you shut it down?"),
            s("Not from here. Keep moving. I need to see how it fights.")),
    GATE("THE ROAD OPENS",
            r("The chain is broken. Stretcher tracks run through the gate and into the trees."),
            r("Only one set of footprints comes back. Shuna's."),
            r("The others got this far. I can follow them.")),
    CAMP_AFTER("A PLACE TO RETURN TO",
            m("The gate's quiet."),
            r("It's open. I'm going after the stretcher tracks."),
            m("I tried. The roots have swallowed the road. There's a dry scarf by the fire; take it before you go."),
            r("I'm not sure it would stay on."),
            m("Then I'll keep it dry.")),
    SURVIVOR_TOBIN("BEHIND THE FENCE",
            t("Who's there? If you're a rift thing, I'm mostly bandages. Poor eating."),
            r("Rainoray. Mostly water, these days."),
            t("Kaijin. I worked on the gate relay. That sound means the Warden's still running a broken order."),
            t("The fence came down behind me. I couldn't climb it with this leg."),
            r("Shuna's lamp is still lit at Captain Camp. Can you make it that far?"),
            t("For a lamp? I'll crawl. Thank you.")),
    SURVIVOR_ILSE("INSIDE THE THORNS",
            i("Stay back. I've got one good arm and I'm not afraid to use it."),
            r("Rainoray. I cut the thorns, not you."),
            i("Eren. I was scouting the road. Those roots moved against the wind, then closed behind me."),
            i("That chest holds our spare supplies. Locked tight, and the key went with the storehouse."),
            r("I'll find it. Go to Shuna's lamp at Captain Camp."),
            i("Watch the roots when the wind stops. They don't stop with it.")),
    SURVIVOR_VENN("THE CULVERT",
            v("Took them long enough to send someone. I pictured someone taller."),
            r("Nobody sent me. I heard you were missing."),
            v("Rigurd. I stayed with the last evacuees, then told the stretcher crew to go on without me."),
            v("Then the culvert silted up and I spent a week talking to the rain."),
            r("Shuna is keeping a fire at Captain Camp. She has room for you."),
            v("Shuna. Good. Then I'll go and be shouted at properly.")),
    LEDGER("THE EVACUATION CACHE",
            r("The cache opens. Bandages, a little coin, and the evacuation ledger."),
            s("Three names match the people missing from camp."),
            r("Kaijin, Eren, Rigurd. Names, not numbers. Let's find out what happened to them.")),
    CAMP_RESCUED("SOMEONE CAME BACK",
            m("Someone walked into my camp tonight and asked for you by name."),
            r("They were still out there, waiting. I couldn't leave them."),
            m("Then keep looking. The dispatch listed more people than the Warden let through.")),
    CAMP_HOME("THREE BEDS",
            m("Kaijin, Eren and Rigurd. Three beds that aren't empty anymore."),
            r("The count finally adds up. Better than I'd hoped."),
            m("Eren found the last of the good tea. Take the coin we were saving with it. You've earned both.")),
    FOREST_ARRIVAL("THE WHITE CLOTH",
            r("The tracks are deeper here. They were carrying someone."),
            s("And stopping often. There is cloth tied ahead."),
            r("A route marker. Someone wanted them followed.")),
    FOREST_SEDGE("THE STRANDED CROSSING",
            d("Watch the bank. Looks solid until you put a foot on it."),
            r("I'm following the stretcher crew from the outpost."),
            d("I got them across before the roots took the river. The injured are sheltering beneath the old stair."),
            d("Souei. I marked a path back, but the roots keep closing it."),
            r("Rainoray. Shuna is waiting beyond the gate. The road is open now."),
            d("Then they have somewhere safe to go. Follow the white cloth north, then east over the ford."),
            s("The roots all pull toward that clearing. We should find what is holding them."),
            d("I'll guard their crossing. You reach the tree. Watch the ground; it reaches farther than its branches.")),
    FOREST_SEDGE_WAIT("SOUEI",
            d("White cloth through the grove, then east across the dry ford. The stair is beyond the thorn clearing."),
            r("Can you help them back if I open it?"),
            d("The rope is ready. Clear the roots and I'll bring them through.")),
    FOREST_SEDGE_LATE("AN OPEN WAY",
            d("The roots went slack. I thought the whole bank was falling."),
            r("The tree at the clearing is down. I'm looking for the people from the outpost."),
            d("Under the old stair. I'm Souei. I've been guarding their route back. We can move them now."),
            r("Then bring the rope. There's a refuge waiting beyond the gate.")),
    FOREST_WAYFINDER("A PATH BACK",
            r("White strips on every branch. He must have torn up half his sail."),
            s("The marks face both ways."),
            r("He wasn't just showing them where to go. He expected them to come back.")),
    FOREST_MOONWELL("UNDER THE SURFACE",
            r("Water, at last. Why hasn't this well dried up?"),
            s("Its roots are cut. The water here no longer feeds the heart."),
            r("Then that thing is drinking the river.")),
    FOREST_ORCHARD("THE LAST HARVEST",
            r("Someone left the good fruit within reach. Took the bruised ones."),
            s("For whoever followed them."),
            r("We packed our van like that. Best supplies where people could reach them.")),
    FOREST_BRIARHEART("WHAT HOLDS THE ROAD",
            r("Those are stretcher poles caught in its roots."),
            s("The entrance is behind it. I can hear movement below."),
            r("Then we make a path. Keep an eye on the ground for me."),
            s("I will. Stay moving while I learn its pattern.")),
    FOREST_SEDGE_AFTER("BRING THE ROPE",
            d("The white cloth's moving again. There hasn't been a breeze through here in days."),
            r("The roots have loosened. We can reach the stair."),
            d("I'll bring a stretcher. You see who's still down there.")),
    FOREST_END("THE LAST STRETCHER",
            r("There's a lamp below. Someone answered."),
            s("More than one voice. They are coming toward us."),
            r("Easy. The path is clear. There's a refuge waiting at the outpost."),
            s("The last stretcher has crossed."),
            r("All of them?"),
            d("Everyone we found. They're at the camp."),
            r("Good. I wanted to hear someone say it."));

    private static final double[] OPENING_SECONDS={6,3,4.5,3,4,5.5,3,3,3.5,4,2.5,3.5,3.5,3,4};
    static double openingSeconds(int index) { return OPENING_SECONDS[index]; }

    /** The scene for the survivor in FieldObjects.SURVIVORS order. */
    static OutpostDialogue survivor(int index) {
        return switch (index) { case 0 -> SURVIVOR_TOBIN; case 1 -> SURVIVOR_ILSE; default -> SURVIVOR_VENN; };
    }

    public enum Speaker { RAINORAY, MARA, WARDEN, TOBIN, ILSE, VENN, INNER_VOICE, SEDGE;
        public String label() { return switch(this) { case INNER_VOICE -> "RAPHAEL"; case MARA -> "SHUNA"; case TOBIN -> "KAIJIN"; case ILSE -> "EREN"; case VENN -> "RIGURD"; case SEDGE -> "SOUEI"; default -> name().replace('_',' '); }; }
    }
    public record Line(Speaker speaker, String text) { }
    private final String title;
    private final List<Line> lines;

    OutpostDialogue(String title, Line... lines) { this.title=title;this.lines=List.of(lines); }
    public String title() { return title; }
    public int biome() { return name().startsWith("FOREST_")?1:0; }
    public List<Line> lines() { return lines; }
    public Line line(int index, boolean metMara, int rescued) {
        if(!metMara) {
            if(this==GATE&&index==1)return r("Only one set of footprints comes back toward camp.");
            if(this==SIGNAL&&index==2)return r("A list of missing people. Someone at the camp might know these names.");
            if((this==SURVIVOR_TOBIN||this==SURVIVOR_ILSE||this==SURVIVOR_VENN)&&index==4)
                return r("There's a lamp by the captain's tent. Head there. Someone is keeping it lit.");
        }
        if(this==WARDEN&&index==1) {
            if(rescued>0)return r("People are still coming back. You're blocking their way. Open the gate.");
            if(!metMara)return r("There's no stretcher on this road. Let me through. Someone may still need help.");
        }
        return lines.get(index);
    }
    public String voiceKey(int index, boolean metMara, int rescued) {
        String suffix=this==WARDEN&&index==1&&rescued>0?"_rescued"
                :line(index,metMara,rescued)!=lines.get(index)?"_stranger":"";
        return name().toLowerCase(Locale.ROOT)+"_"+String.format(Locale.ROOT,"%02d",index+1)+suffix;
    }
    public String key() { return "story_outpost_"+name().toLowerCase(Locale.ROOT); }
    private static Line r(String text) { return new Line(Speaker.RAINORAY,text); }
    private static Line m(String text) { return new Line(Speaker.MARA,text); }
    private static Line w(String text) { return new Line(Speaker.WARDEN,text); }
    private static Line t(String text) { return new Line(Speaker.TOBIN,text); }
    private static Line i(String text) { return new Line(Speaker.ILSE,text); }
    private static Line v(String text) { return new Line(Speaker.VENN,text); }
    private static Line s(String text) { return new Line(Speaker.INNER_VOICE,text); }
    private static Line d(String text) { return new Line(Speaker.SEDGE,text); }
}
