/** One-time conversations and repeatable, paced combat warnings. */
public enum OutpostBark {
    LEAVING_CAMP(OutpostDialogue.Speaker.MARA,"Keep to the road. I'll keep the lamp lit."),
    HEARD_SURVIVOR(
            new OutpostDialogue.Line(OutpostDialogue.Speaker.RAINORAY,"Was that a voice? South of the road."),
            new OutpostDialogue.Line(OutpostDialogue.Speaker.INNER_VOICE,"Confirmed. Someone is still alive."),
            new OutpostDialogue.Line(OutpostDialogue.Speaker.RAINORAY,"Then we're not leaving them here.")),
    AFTER_RESCUE(OutpostDialogue.Speaker.INNER_VOICE,"One name off the missing list. Keep looking."),

    FIGHT_LEDGER(Trigger.FIGHT,r("That was not on the supply list."),s("I have started a separate list.")),
    FIGHT_LISTEN(Trigger.FIGHT,r("Let's listen before we move."),s("Agreed. The road carries sound.")),
    FIGHT_ROAD(Trigger.FIGHT,r("This road used to mean safe passage."),s("It can again. That is why we continue.")),
    FIGHT_COUNTING(Trigger.FIGHT,r("I preferred counting things."),s("You still count who gets home.")),
    FIGHT_JOB(Trigger.FIGHT,r("Do I get a quieter job after this?"),s("You have not finished this one.")),
    FIGHT_ORDER(Trigger.FIGHT,r("Someone taught them to hold this road."),s("The order survived its purpose.")),
    FIGHT_WATCH(Trigger.FIGHT,r("One moment. I'll check behind us."),s("A sensible habit. Keep it.")),
    FIGHT_SHAKING(Trigger.FIGHT,r("Funny. I didn't stop shaking."),s("You moved anyway.")),
    FIGHT_REASON(Trigger.FIGHT,r("I don't want to get used to that."),s("Then remember why you are here.")),
    FIGHT_FORWARD(Trigger.FIGHT,r("Forward, then. One problem at a time."),s("One step is enough for now.")),
    FIGHT_NUMBERS(Trigger.FIGHT,r("Are you keeping count too?"),s("Only of the decisions that matter.")),
    FIGHT_MOMENT(Trigger.FIGHT,r("Give me a moment before we go."),s("Take it. Rushing is not recovery.")),

    HURT_SHELTER(Trigger.CLOSE_CALL,r("That was closer than I'd planned."),s("Find shelter before another fight.")),
    HURT_DISTANCE(Trigger.CLOSE_CALL,r("All right. A little less bravery."),s("A little more distance would help.")),
    HURT_CAMP(Trigger.CLOSE_CALL,r("I could use that campfire now."),s("A retreat can still be progress.")),
    HURT_DELIBERATE(Trigger.CLOSE_CALL,r("Tell me that looked deliberate."),s("I would prefer to be accurate.")),

    WALK_ROUTES(Trigger.EXPLORING,r("I don't know which way is safe."),s("We will learn. Keep the camp behind us.")),
    WALK_BAG(Trigger.EXPLORING,r("I keep reaching for a bag I lost."),s("Old habits are slow to catch up.")),
    WALK_QUESTIONS(Trigger.EXPLORING,r("Do my questions ever tire you?"),s("Not yet. You may continue.")),
    WALK_THOUGHTS(Trigger.EXPLORING,r("You can talk without being asked."),s("I was leaving room for your thoughts.")),
    WALK_QUIET(Trigger.EXPLORING,r("Was this a busy road once?"),s("The cart tracks suggest it was.")),
    WALK_WAITING(Trigger.EXPLORING,r("Who is still waiting out there?"),s("Someone may be asking that about you.")),
    WALK_NAME(Trigger.EXPLORING,r("Raphael. Are you still there?"),s("Yes. You do not have to keep checking.")),
    WALK_MAP(Trigger.EXPLORING,r("A map never shows how a place feels."),s("Someone must carry that part.")),
    WALK_CALM(Trigger.EXPLORING,r("If I sound calm, don't be fooled."),s("I hear more than your tone.")),
    WALK_NAILS(Trigger.EXPLORING,r("I used to worry about late deliveries."),s("You noticed what others overlooked.")),

    CAMP_LAMP(Trigger.CAMP,r("She really is keeping that lamp lit."),s("She is making a place to return to.")),
    CAMP_PERSON(Trigger.CAMP,r("She spoke to me like I was a person."),s("You still are.")),
    CAMP_TALK(Trigger.CAMP,r("I didn't expect to miss ordinary talk."),s("You do not have to stop having it.")),
    CAMP_PROMISE(Trigger.CAMP,r("I wish I could promise her more."),s("Return when you can. Start with that.")),

    WALK_BIRDS(Trigger.EXPLORING,s("You've gone quiet."),r("I'm listening for the birds.")),
    WALK_FEET(Trigger.EXPLORING,r("How do I trip without feet?"),s("You're finding new ways.")),
    WALK_BODY(Trigger.EXPLORING,r("Is this body holding together?"),s("Yes. I'd tell you if it wasn't.")),
    WALK_WIND(Trigger.EXPLORING,s("There. A little less wind."),r("You noticed I was cold?")),
    FIGHT_SAW(Trigger.FIGHT,r("Did you see that?"),s("Even the part you meant to do.")),
    FIGHT_RIGHT(Trigger.FIGHT,s("You're leaving your right side open."),r("Remind me before the next one.")),
    FIGHT_HARMLESS(Trigger.FIGHT,r("Aren't slimes meant to be harmless?"),s("You've met the local slimes.")),
    CAMP_WARM(Trigger.CAMP,r("That fire almost feels warm."),s("Then stay a moment.")),
    FOREST_WALK_SOUNDS(Trigger.EXPLORING,s("You're following every sound again."),r("One of them might be someone.")),
    FOREST_WALK_LOST(Trigger.EXPLORING,r("Do you ever get lost?"),s("I would call it gathering information.")),
    FOREST_WALK_CALM(Trigger.EXPLORING,r("How are you always so calm?"),s("One of us has to be.")),
    FOREST_WALK_GROUND(Trigger.EXPLORING,s("The ground is firmer on the left."),r("Thanks. I'm tired of sinking.")),
    FOREST_FIGHT_CREDIT(Trigger.FIGHT,r("I almost had that one."),s("You did have it. Give yourself that much.")),
    FOREST_FIGHT_LISTEN(Trigger.FIGHT,s("Wait. Listen before we move."),r("Right. Quiet doesn't always mean safe.")),
    FOREST_FIGHT_SIT(Trigger.FIGHT,r("Remind me never to sit down here."),s("I was about to.")),
    FOREST_CAMP_RETURN(Trigger.CAMP,s("The boat is still there."),r("Good. I like knowing we can go back.")),
    SAGE_HIT_ROOM(Trigger.WARNING,s("Easy. Give yourself a little room.")),
    SAGE_HIT_HURT(Trigger.WARNING,s("That one hurt. Step back.")),
    SAGE_HIT_STAY(Trigger.WARNING,s("I'm here. Keep moving.")),
    SAGE_HIT_FOOTING(Trigger.WARNING,s("Watch your footing.")),
    SAGE_HIT_PATIENT(Trigger.WARNING,s("Don't rush the next move.")),
    SAGE_HIT_STEADY(Trigger.WARNING,s("You're still in this.")),
    WARDEN_VOICE_STAND(Trigger.BOSS,new OutpostDialogue.Line(OutpostDialogue.Speaker.WARDEN,"Stand down.")),
    WARDEN_VOICE_LINE(Trigger.BOSS,new OutpostDialogue.Line(OutpostDialogue.Speaker.WARDEN,"Hold the line.")),
    WARDEN_VOICE_GATE(Trigger.BOSS,new OutpostDialogue.Line(OutpostDialogue.Speaker.WARDEN,"The gate stays shut.")),
    WARDEN_VOICE_ORDER(Trigger.BOSS,new OutpostDialogue.Line(OutpostDialogue.Speaker.WARDEN,"I was ordered to protect them.")),
    // Its dying words as it kneels (owner 2026-10-29): 「……通行を……許可、する……」
    WARDEN_LAST(Trigger.BOSS,new OutpostDialogue.Line(OutpostDialogue.Speaker.WARDEN,"...Passage... granted...")),

    WARDEN_START(Trigger.BOSS,s("Keep moving. I'll analyze its attacks."),r("I only need one opening.")),
    WARDEN_READ(Trigger.BOSS,s("I've got its rhythm. I'll warn you."),r("Good. Tell me when to move.")),
    WARDEN_PHASE(Trigger.BOSS,s("Careful. It's getting desperate now."),r("Good. I'll use that.")),
    WARDEN_WEAK(Trigger.BOSS,s("It's nearly down. Don't get careless.")),
    WARDEN_CLOSE_CALL(Trigger.BOSS,s("Fall back. You need room.")),
    WARDEN_VICTORY(Trigger.BOSS,r("The road's open."),s("And you are still here.")),
    SAGE_TARGET(Trigger.WARNING,s("Move! It's coming down.")),
    SAGE_TARGET_AGAIN(Trigger.WARNING,s("Get clear of that spot.")),
    SAGE_SWEEP(Trigger.WARNING,s("Get back. It's swinging wide.")),
    SAGE_SWEEP_AGAIN(Trigger.WARNING,s("Too close. Back away.")),
    SAGE_CHARGE(Trigger.WARNING,s("Sidestep. It's coming straight at you.")),
    SAGE_CHARGE_AGAIN(Trigger.WARNING,s("Move sideways. Let it pass.")),
    SAGE_SHOCKWAVE(Trigger.WARNING,s("Back up. Here comes the wave.")),
    SAGE_SHOCKWAVE_AGAIN(Trigger.WARNING,s("Get some distance. Another wave.")),
    SAGE_FISSURE(Trigger.WARNING,s("Move! The ground's breaking.")),
    SAGE_FISSURE_AGAIN(Trigger.WARNING,s("Off that line. It's splitting.")),
    SAGE_HIT(Trigger.WARNING,s("That hit landed. Keep your distance.")),
    SAGE_HIT_AGAIN(Trigger.WARNING,s("Careful. Don't let it catch you again.")),

    IDENTIFY_REMNANT(Trigger.IDENTIFY,s("A remnant. It still knows how to fight."),r("Then I'll give it some space.")),
    IDENTIFY_GUARD(Trigger.IDENTIFY,s("Outpost guard. Let the shield swing past."),r("Right. I'll go around the shield.")),
    IDENTIFY_SPITTER(Trigger.IDENTIFY,s("Ichor spitter. Watch where it aims."),r("Not getting that stuff on me.")),
    IDENTIFY_KNIGHT(Trigger.IDENTIFY,s("Fallen Knight. Strike after its swing."),r("I'll wait for my turn, then.")),
    FOREST_IDENTIFY_IMP(Trigger.IDENTIFY,s("Rift Imp. It strikes twice. Wait for both."),r("Two swings. Got it.")),
    FOREST_IDENTIFY_WOLF(Trigger.IDENTIFY,s("Thorn Wolf. Give those jaws some room."),r("Yeah, I saw those teeth.")),
    FOREST_IDENTIFY_SHAMAN(Trigger.IDENTIFY,s("Mire Shaman. It'll curse the ground."),r("Standing still was a bad idea, huh.")),

    BEELZEBUB_CALL(Trigger.SKILL,r("Devour them. Beelzebub!")),
    BEELZEBUB_CALL_AGAIN(Trigger.SKILL,r("Beelzebub!")),
    BEELZEBUB_CALL_TAKE(Trigger.SKILL,r("I'll take it all!")),

    FOREST_CROSSING(r("Those roots weren't there a moment ago."),s("They move when the heart tightens.")),
    FOREST_GROVE(r("The cloth is caught up there."),s("Still pointing east. He chose well.")),
    FOREST_RIDGE(r("I can see the old river from here."),s("Follow the stones. They still mark a crossing.")),
    FOREST_FORD(r("A boat without a river. That's a new one."),s("The roots have taken more than the road.")),

    FOREST_FIGHT_IMPS(Trigger.FIGHT,r("That little thing tried to get behind me."),s("It watched your first move. Then committed.")),
    FOREST_FIGHT_WOLVES(Trigger.FIGHT,r("I heard the claws before I saw it."),s("The landing gives it away.")),
    FOREST_FIGHT_SHAMAN(Trigger.FIGHT,r("I thought it was aiming at me."),s("At the ground beneath you. Keep moving.")),
    FOREST_FIGHT_ROOTS(Trigger.FIGHT,r("Even the roots don't want us here."),s("Someone beyond them does.")),
    FOREST_FIGHT_MUD(Trigger.FIGHT,r("That was a very expensive patch of mud."),s("You can go around the next one.")),
    FOREST_FIGHT_BREATH(Trigger.FIGHT,r("I still want to catch my breath."),s("The habit is useful. Take a moment.")),
    FOREST_FIGHT_NOISE(Trigger.FIGHT,r("So much for passing through quietly."),s("Quiet was unlikely. Passing through is enough.")),
    FOREST_FIGHT_BETTER(Trigger.FIGHT,r("That felt a little less accidental."),s("You saw the opening before I did.")),
    FOREST_HURT_DISTANCE(Trigger.CLOSE_CALL,r("I went in too soon."),s("Let the roots settle before you return.")),
    FOREST_HURT_WAIT(Trigger.CLOSE_CALL,r("I need a moment."),s("Take the clear ground. I'll keep watch.")),
    FOREST_WALK_CLOTH(Trigger.EXPLORING,r("White cloth. I keep checking for it."),s("It is easier to follow a promise you can see.")),
    FOREST_WALK_COMPANY(Trigger.EXPLORING,r("You don't have to wait for a question."),s("I was listening to the trees.")),
    FOREST_WALK_BOOTS(Trigger.EXPLORING,r("At least I can't get mud in my boots."),s("You found an advantage.")),
    FOREST_WALK_NAMES(Trigger.EXPLORING,r("I wish I knew who was on those stretchers."),s("You can ask them when we get there.")),
    FOREST_WALK_HOME(Trigger.EXPLORING,r("The outpost almost feels like home now."),s("Someone is waiting for you there.")),
    FOREST_WALK_MEMORY(Trigger.EXPLORING,r("The roots are narrowing the road."),s("They are still growing. Keep to the middle.")),
    FOREST_CAMP_ROPE(Trigger.CAMP,r("He kept the rope ready all this time."),s("Waiting can be work, too.")),
    FOREST_CAMP_BOAT(Trigger.CAMP,r("He could have left when the river went."),s("The people he guards could not.")),

    FOREST_BOSS_START(Trigger.BOSS,s("I'll watch how the roots move. Stay clear."),r("Tell me when that heart opens.")),
    FOREST_BOSS_READ(Trigger.BOSS,s("The roots follow a pattern. I can read it now."),r("All right. I'm listening.")),
    FOREST_BOSS_OPEN(Trigger.BOSS,s("It's open. Strike the heart.")),
    FOREST_BOSS_PHASE(Trigger.BOSS,s("It's drawing more roots in. Watch the gaps."),r("I see them. Keep talking.")),
    FOREST_BOSS_WEAK(Trigger.BOSS,s("The heart is fading. One more opening.")),
    FOREST_BOSS_CLOSE_CALL(Trigger.BOSS,s("Get clear. Let yourself recover.")),
    FOREST_BOSS_VICTORY(Trigger.BOSS,r("The roots are letting go."),s("Listen. Someone is calling from below.")),
    FOREST_ROOT_WARNING(Trigger.WARNING,s("Between the roots. Leave that strip.")),
    FOREST_ROOT_AGAIN(Trigger.WARNING,s("Roots coming up. Take the gap.")),
    FOREST_SEED_WARNING(Trigger.WARNING,s("Seeds beneath you. Find clear ground.")),
    FOREST_SEED_AGAIN(Trigger.WARNING,s("Move away. Those seeds will burst.")),
    FOREST_CAGE_WARNING(Trigger.WARNING,s("One side is open. Go through it.")),
    FOREST_CAGE_AGAIN(Trigger.WARNING,s("Out through the gap. Don't stay inside."));

    public enum Trigger { STORY, FIGHT, CLOSE_CALL, EXPLORING, CAMP, BOSS, WARNING, IDENTIFY, SKILL }
    private final Trigger trigger;
    private final java.util.List<OutpostDialogue.Line> lines;
    OutpostBark(OutpostDialogue.Speaker speaker,String text) { this(new OutpostDialogue.Line(speaker,text)); }
    OutpostBark(OutpostDialogue.Line... lines) { this(Trigger.STORY,lines); }
    OutpostBark(Trigger trigger,OutpostDialogue.Line... lines) { this.trigger=trigger;this.lines=java.util.List.of(lines); }
    public Trigger trigger() { return trigger; }
    public int biome() { return name().startsWith("FOREST_")?1:0; }
    public boolean combat() { return trigger==Trigger.BOSS||trigger==Trigger.WARNING||trigger==Trigger.IDENTIFY||trigger==Trigger.SKILL; }
    public boolean banter() { return !combat()&&trigger!=Trigger.STORY; }
    public java.util.List<OutpostDialogue.Line> lines() { return lines; }
    public OutpostDialogue.Line line(int index) { return lines.get(index); }
    public double lineSeconds() { return trigger==Trigger.IDENTIFY?4.4:line(0).speaker()==OutpostDialogue.Speaker.WARDEN||biome()==1&&!combat()?4.2:lines.size()>1||combat()?3.6:6; }
    static OutpostBark identification(CampaignEnemy.Kind kind) { return switch(kind) {
        case OUTPOST_SCOUT -> IDENTIFY_REMNANT;
        case OUTPOST_GUARD -> IDENTIFY_GUARD;
        case OUTPOST_SPITTER -> IDENTIFY_SPITTER;
        case FALLEN_KNIGHT -> IDENTIFY_KNIGHT;
        case RIFT_IMP -> FOREST_IDENTIFY_IMP;
        case THORN_WOLF -> FOREST_IDENTIFY_WOLF;
        case MIRE_SHAMAN -> FOREST_IDENTIFY_SHAMAN;
        case CINDER_HEXER, BRIARLING -> null;
    }; }
    // Keep the first clip and campaign unlock IDs compatible with existing saves.
    public String voiceKey(int index) { return key()+(index==0?"":"_"+(index+1)); }
    public String key() { return "bark_"+name().toLowerCase(java.util.Locale.ROOT); }
    static OutpostBark unheard(Trigger trigger,java.util.Set<String> heard) {
        return unheard(trigger,heard,0);
    }
    static OutpostBark unheard(Trigger trigger,java.util.Set<String> heard,int biome) {
        return unheard(trigger,heard,biome,null);
    }
    static OutpostBark unheard(Trigger trigger,java.util.Set<String> heard,int biome,CampaignEnemy.Kind kind) {
        var choices=java.util.Arrays.stream(values()).filter(b->b.biome()==biome&&b.trigger==trigger&&!heard.contains(b.key())&&b.fits(kind)).toList();
        return choices.isEmpty()?null:choices.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(choices.size()));
    }
    boolean fits(CampaignEnemy.Kind kind) { return switch(this) {
        case FOREST_FIGHT_IMPS -> kind==CampaignEnemy.Kind.RIFT_IMP;
        case FOREST_FIGHT_WOLVES -> kind==CampaignEnemy.Kind.THORN_WOLF;
        case FOREST_FIGHT_SHAMAN -> kind==CampaignEnemy.Kind.MIRE_SHAMAN;
        default -> true;
    }; }
    static OutpostBark hitReaction(int index) {
        return switch(Math.floorMod(index,8)) {
            case 0 -> SAGE_HIT;
            case 1 -> SAGE_HIT_AGAIN;
            case 2 -> SAGE_HIT_ROOM;
            case 3 -> SAGE_HIT_HURT;
            case 4 -> SAGE_HIT_STAY;
            case 5 -> SAGE_HIT_FOOTING;
            case 6 -> SAGE_HIT_PATIENT;
            default -> SAGE_HIT_STEADY;
        };
    }
    private static OutpostDialogue.Line r(String text) { return new OutpostDialogue.Line(OutpostDialogue.Speaker.RAINORAY,text); }
    private static OutpostDialogue.Line s(String text) { return new OutpostDialogue.Line(OutpostDialogue.Speaker.INNER_VOICE,text); }
}
