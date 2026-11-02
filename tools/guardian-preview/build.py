#!/usr/bin/env python3
"""Build a separate real-engine Warden preview; never replace the installed JAR."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
OUT = Path('/home/ghiankylledomingo/tmp/b2bj-guardian-native-2026-11-02/runtime')
SRC = OUT / 'src'
CLASSES = OUT / 'classes'
BASE = ROOT / 'build/B2BJ.jar'
BANK = HERE / 'sound/output'


def change(text, old, new, count=1):
    if text.count(old) != count:
        raise RuntimeError(f'Preview patch no longer matches ({text.count(old)} vs {count}): {old[:85]}')
    return text.replace(old, new)


def build():
    sound = json.loads((BANK / 'manifest.json').read_text())
    gains = {}
    for cue in sound['cues'].values():
        path = BANK / cue['file']
        if hashlib.sha256(path.read_bytes()).hexdigest() != cue['sha256']:
            raise RuntimeError('Sound bank differs from its manifest: ' + path.name)
        if cue['family'] in gains and gains[cue['family']] != cue['game_gain']:
            raise RuntimeError('Variants must share gain: ' + cue['family'])
        gains[cue['family']] = cue['game_gain']
    SRC.mkdir(parents=True, exist_ok=True)
    CLASSES.mkdir(exist_ok=True)
    before = hashlib.sha256(BASE.read_bytes()).hexdigest()
    guardian = (ROOT / 'src/Guardian.java').read_text()
    (SRC / 'Guardian.java').write_text(change(guardian, 'public final class Guardian', 'public class Guardian'))
    game = (ROOT / 'src/RuinedOutpostGame.java').read_text()
    game = change(game, 'guardian=new Guardian(map.guardianX(),map.guardianY(),Guardian.Profile.values()[biome]);', 'guardian=biome==0?new GuardianDraft(map.guardianX(),map.guardianY()):new Guardian(map.guardianX(),map.guardianY(),Guardian.Profile.values()[biome]);')
    game = change(game, 'hurtPlayer(guardian.profile()==Guardian.Profile.BRIARHEART?22:Guardian.SLAM_DAMAGE,', 'hurtPlayer(guardian instanceof GuardianDraft draft?draft.damage():guardian.profile()==Guardian.Profile.BRIARHEART?22:Guardian.SLAM_DAMAGE,')
    game = change(game, 'if(campaignMode&&near(guardian.x(),guardian.y(),1000)) {', 'if(!(guardian instanceof GuardianDraft)&&campaignMode&&near(guardian.x(),guardian.y(),1000)) {')
    (SRC / 'RuinedOutpostGame.java').write_text(game)
    terrain = (ROOT / 'src/RuinedOutpostMap.java').read_text()
    terrain = change(terrain, '    public boolean waterBlocked(double x,double y,double radius) {', '''    boolean guardianDraftBlocked(double x,double y,double radius) {
        if(waterBlocked(x,y,radius))return true;
        for(Obstacle o:fieldWalls)if(intersects(x,y,radius,o))return true;
        for(Obstacle o:fieldGates)if(intersects(x,y,radius,o))return true;
        for(Obstacle o:slimeGates)if(intersects(x,y,radius,o))return true;
        return false;
    }
    public boolean waterBlocked(double x,double y,double radius) {''')
    (SRC / 'RuinedOutpostMap.java').write_text(terrain)
    panel = (ROOT / 'src/B2BJ.java').read_text()
    panel = change(panel, 'private final BossFx bossAttackFx=new BossFx();', 'private final BossFx bossAttackFx=new BossFx();\n    private final GuardianDraftFx guardianDraftFx;')
    panel = change(panel, 'this.game=java.util.Objects.requireNonNull(game);', 'this.game=java.util.Objects.requireNonNull(game);\n        guardianDraftFx=new GuardianDraftFx(wardenMotionSheet,guardianSweepSheet,guardianChargeSheet);')
    panel = change(panel, 'CampaignRenderer.drawAreaCard(canvas,cardBiome,cardTime,viewWidth,reducedEffects);', 'if(!(game.guardian() instanceof GuardianDraft))CampaignRenderer.drawAreaCard(canvas,cardBiome,cardTime,viewWidth,reducedEffects);')
    panel = change(panel, 'var windup=bossAttackFx.update(seconds,game.guardian(),reducedEffects);', 'if(game.guardian() instanceof GuardianDraft draft&&guardianDraftFx.update(seconds,draft))feedback.guardianSlam();\n        var windup=game.guardian() instanceof GuardianDraft?null:bossAttackFx.update(seconds,game.guardian(),reducedEffects);')
    panel = change(panel, 'if(wardenArea())wardenFx.drawGround(canvas,game.guardian(),cameraX,cameraY);', 'if(game.guardian() instanceof GuardianDraft draft&&draft.alive())guardianDraftFx.ground(canvas,draft,cameraX,cameraY,reducedEffects);\n        else if(wardenArea())wardenFx.drawGround(canvas,game.guardian(),cameraX,cameraY);')
    panel = change(panel, 'if(wardenArea())wardenFx.queue(', 'if(wardenArea()&&(!(game.guardian() instanceof GuardianDraft)||!game.guardian().alive()))wardenFx.queue(')
    panel = change(panel, 'if(wardenArea())wardenFx.drawAbove(canvas,game.guardian(),cameraX,cameraY,frameCounter);', 'if(game.guardian() instanceof GuardianDraft draft&&draft.alive())guardianDraftFx.above(canvas,draft,cameraX,cameraY,reducedEffects);\n        else if(wardenArea())wardenFx.drawAbove(canvas,game.guardian(),cameraX,cameraY,frameCounter);')
    panel = change(panel, 'if (guardian.alive() && guardian.state() != Guardian.State.DORMANT) {', 'if(guardian instanceof GuardianDraft draft)guardianDraftFx.body(canvas,draft,cameraX,cameraY,reducedEffects);\n        if (guardian.alive() && guardian.state() != Guardian.State.DORMANT) {')
    panel = change(panel, 'var boss=game.guardian();\n        if(boss.profile()==Guardian.Profile.BRIARHEART)', 'var boss=game.guardian();\n        if(boss instanceof GuardianDraft)return;\n        if(boss.profile()==Guardian.Profile.BRIARHEART)')
    panel = change(panel, 'case GUARDIAN_SLAM -> {\n                    feedback.guardianSlam();', 'case GUARDIAN_SLAM -> {\n                    if(game.guardian() instanceof GuardianDraft)break;\n                    feedback.guardianSlam();')
    panel = change(panel, 'case GUARDIAN_ENRAGE -> {\n                    GameAudio.play', 'case GUARDIAN_ENRAGE -> {\n                    if(game.guardian() instanceof GuardianDraft)break;\n                    GameAudio.play')
    panel = change(panel, 'case GUARDIAN_PHASE_TWO -> {\n                    feedback.guardianSlam();', 'case GUARDIAN_PHASE_TWO -> {\n                    if(game.guardian() instanceof GuardianDraft)break;\n                    feedback.guardianSlam();')
    (SRC / 'B2BJ.java').write_text(panel)
    audio = (ROOT / 'src/GameAudio.java').read_text()
    audio = change(audio, 'HEAVY_CLEAVE(1.45, 146, 55, .24, true, null, false);', '''HEAVY_CLEAVE(1.45, 146, 55, .24, true, null, false),
        WARDEN_STEP(.35, 90, 60, .12, true, null, false),
        WARDEN_FISTS_LAUNCH(.45, 220, 110, .22, false, null, false),
        WARDEN_FISTS_IMPACT(.85, 110, 55, .25, true, null, false),
        WARDEN_BEAM_CHARGE(1.25, 110, 220, .2, false, null, false),
        WARDEN_BEAM(1.15, 110, 55, .25, false, null, false),
        WARDEN_CORE_OPEN(.65, 220, 330, .14, false, null, false);''')
    # Authored Guardian contacts and charge durations must not drift with random pitch.
    for name in ['WARDEN_WINDUP', 'WARDEN_SWEEP', 'WARDEN_RUSH', 'WARDEN_STRAIN', 'WARDEN_SLAM']:
        audio, n = re.subn(r'(' + name + r'\([^\n;]+), true\),', r'\1, true, null, false),', audio, count=1)
        if n != 1:
            raise RuntimeError('Missing Guardian cue ' + name)
    gain_cases = ''.join(f'case {name.upper()} -> {gain};' for name, gain in sorted(gains.items()))
    audio = change(audio, 'return scaled(resampled(pcm, pitch), SAMPLE_GAIN);',
                   'return scaled(resampled(pcm, pitch), SAMPLE_GAIN*(switch(cue){' + gain_cases + 'default -> 1.;}));')
    (SRC / 'GameAudio.java').write_text(audio)
    for path in HERE.glob('*.java'):
        shutil.copy2(path, SRC / path.name)
    subprocess.run(['rtk', 'proxy', 'javac', '--release', '17', '-Xlint:all', '-cp', str(BASE), '-sourcepath', str(SRC), '-d', str(CLASSES), *map(str, SRC.glob('*.java'))], check=True)
    jar = OUT / 'Guardian-preview.jar'
    replacements = {str(path.relative_to(CLASSES)): path.read_bytes() for path in CLASSES.rglob('*.class')}
    replacements.update({'assets/audio/' + cue['file']: (BANK / cue['file']).read_bytes() for cue in sound['cues'].values()})
    manifest = b'Manifest-Version: 1.0\nMain-Class: PreviewGuardianCombat\n\n'
    replacements['META-INF/MANIFEST.MF'] = manifest
    with zipfile.ZipFile(BASE) as old, zipfile.ZipFile(jar, 'w', zipfile.ZIP_DEFLATED) as new:
        for item in old.infolist():
            family = re.sub(r'_\d+$', '', Path(item.filename).stem)
            replaced_sound = item.filename.startswith('assets/audio/') and '/' not in item.filename[len('assets/audio/'):] and family in gains
            if item.filename not in replacements and not replaced_sound:
                new.writestr(item, old.read(item.filename))
        for name, data in replacements.items():
            new.writestr(name, data)
    assert hashlib.sha256(BASE.read_bytes()).hexdigest() == before
    (OUT / 'build.json').write_text(json.dumps({'installed_jar_sha256': before, 'preview_jar': str(jar), 'replaced_entries': sorted(replacements)}, indent=2))
    print(jar)


if __name__ == '__main__':
    build()
