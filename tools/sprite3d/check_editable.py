"""Run inside Blender: blender -b --python-exit-code 1 --python tools/sprite3d/check_editable.py.
Requires the original local puppet cache and the editable .blend. Writes renders only in a temporary folder.
"""
import bpy,sys,tempfile,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
work=tempfile.TemporaryDirectory(prefix='rainoray-check-');TMP=Path(work.name)
sys.path.insert(0,str(ROOT/'tools/sprite3d'))
import animate
editable=ROOT/'docs/art-review/sprite3d-run/source/rainoray-editable/Rainoray-Body-and-Separate-Sword.blend'
bpy.ops.wm.open_mainfile(filepath=str(editable))
body=bpy.data.objects['RainorayBody'];sword=bpy.data.objects['RainoraySword'];arm=bpy.data.objects['RainorayRig']
assert len(arm.data.bones)==20
blade=body.vertex_groups['blade'].index
assert not any(g.group==blade and g.weight>0 for v in body.data.vertices for g in v.groups)
blade=sword.vertex_groups['blade'].index
assert all(any(g.group==blade and abs(g.weight-1)<1e-6 for g in v.groups) for v in sword.data.vertices)
assert all(o.parent==arm for o in [body,sword,bpy.data.objects['RainorayMask']])
import locomotion,puppet
poser=puppet.Poser(arm)
for facing in ('east','west'):
 view=animate.VIEWS[facing]
 for action in ('run','start','stop','turn'):
  directions=[];relative_tips=[];tips=[];feet=[]
  for frame in locomotion.frames(action,locomotion.FRAMES[action]):
   locomotion.pose(poser,*frame,view)
   directions.append(poser.direction('blade'))
   relative_tips.append(poser.tail('blade')-poser.head('blade'))
   tips.append(poser.tail('blade').z);feet.append(poser.lowest_foot())
  if action=='run':
   # Measure from the hilt so body bob is not mistaken for wrist sway.
   angle=max(math.degrees(a.angle(b)) for a in directions for b in directions)
   sweep=max((a-b).length for a in relative_tips for b in relative_tips)
   assert angle<5,(facing,'run blade swings too far',angle)
   assert sweep<.13,(facing,'run tip sweeps too far from hilt',sweep)
  assert min(tips)>=min(feet)-puppet.SOLE,(facing,action,'sword clips ground')
for facing,view in animate.VIEWS.items():
 old_view={k:v for k,v in view.items() if k!='carry_direction'}
 for action in ('idle',*animate.attacks.FRAMES,*animate.reactions.FRAMES):
  poses,_,_=animate.frame_posers(action,None,view)
  old_poses,_,_=animate.frame_posers(action,None,old_view)
  for pose,old_pose in zip(poses,old_poses):
   old_pose(poser);before=poser.snapshot()
   pose(poser);after=poser.snapshot()
   assert all(max(abs(after[name][r][c]-matrix[r][c]) for r in range(4) for c in range(4))<1e-5 for name,matrix in before.items()),(facing,action,'carry changes idle/combat')
for facing in ('south','north'):
 view=animate.VIEWS[facing];tips=[];feet=[]
 for frame in locomotion.frames('run',8):
  locomotion.pose(poser,*frame,view)
  direction=poser.direction('blade')
  assert direction.z<-.60 and direction.x<-.1,(facing,tuple(direction))
  tips.append(poser.tail('blade').z);feet.append(poser.lowest_foot())
 assert min(tips)>min(feet)-puppet.SOLE-.02,(facing,'sword clips ground')
 for frame in locomotion.frames('idle',8):
  locomotion.pose(poser,*frame,view);before=poser.snapshot()
  locomotion.pose(poser,*frame,{k:v for k,v in view.items() if k!='carry_direction'})
  assert all(max(abs(poser.snapshot()[name][r][c]-matrix[r][c]) for r in range(4) for c in range(4))<1e-5 for name,matrix in before.items())
poser.begin()
sword.hide_render=False;sword.hide_set(False)
bpy.ops.wm.save_as_mainfile(filepath=str(TMP/'armed.blend'))
for name,source in [('separate',TMP/'armed.blend'),('hidden',editable),('original',ROOT/'docs/art-review/sprite3d-run/sheets/puppet.blend')]:
 sys.argv=['animate.py','--',str(source),str(TMP/name),'--action','slash0','--frames','12','--views','south','--cell','160','--trail']
 animate.main()
 meshes=[o for o in bpy.context.scene.objects if o.type=='MESH' and not o.hide_render]
 if name!='hidden':assert all(tuple(o.color)==(0.,0.,0.,1.) for o in meshes),name
 assert bool(list((TMP/name).glob('*/trail_*.png')))==(name!='hidden'),name
work.cleanup()
print('PASS: stable side carry, clear transitions, unchanged idle/combat; original/split trails and hidden sword')
