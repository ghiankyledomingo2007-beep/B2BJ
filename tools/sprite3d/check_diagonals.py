"""Read-only rig/camera inspection for the four new diagonal views. Run with Blender -b --python."""
import hashlib
import json
import math
import sys
from pathlib import Path
import bpy
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/"tools/sprite3d"))
import animate,puppet,render_frames
source=ROOT/"docs/art-review/sprite3d-run/source/rainoray-editable/Rainoray-Body-and-Separate-Sword.blend"
original=hashlib.sha256(source.read_bytes()).hexdigest()
bpy.ops.wm.open_mainfile(filepath=str(source))
scene=bpy.context.scene
arm=scene.objects["RainorayRig"]
assert len(arm.pose.bones)==20
assert scene.objects["RainoraySword"].parent==arm
report={"source_sha256":original,"bones":[{"name":b.name,"parent":b.parent.name if b.parent else None} for b in arm.data.bones],
        "objects":[{"name":o.name,"type":o.type,"parent":o.parent.name if o.parent else None,"scale":list(o.scale),"hidden":o.hide_render} for o in scene.objects],"views":{}}
p=puppet.Poser(arm)
for facing,signs in {"southeast":(1,1),"northeast":(1,-1),"northwest":(-1,-1),"southwest":(-1,1)}.items():
    p.begin();floor=p.lowest_foot()
    render_frames.place_camera(scene,facing,puppet.REST_HEIGHT,floor-puppet.SOLE,(puppet.CX,puppet.CY),80,68,73,15,3)
    bpy.context.view_layer.update()
    origin=Vector((puppet.CX,puppet.CY,floor))
    a=world_to_camera_view(scene,scene.camera,origin)
    b=world_to_camera_view(scene,scene.camera,origin+Vector((0,-1,0)))
    dx,dy=b.x-a.x,a.y-b.y
    assert dx*signs[0]>0 and dy*signs[1]>0,(facing,dx,dy)
    samples={}
    for action in ["idle","run","start","stop","turn","dash","guard","hurt","slash0","slash1","slash2","slash3","dashslash","cleave","spincleave","charge","hold","cast","counter"]:
        poses,_,_=animate.frame_posers(action,None,animate.VIEWS[facing]);data=[]
        for index in sorted({0,len(poses)//2,len(poses)-1}):
            poses[index](p)
            assert all(math.isfinite(v) for bone in arm.pose.bones for row in bone.matrix for v in row)
            data.append({"frame":index,"lowest_foot":p.lowest_foot(),"blade_hilt":list(p.head("blade")),"blade_tip":list(p.tail("blade"))})
        samples[action]=data
    report["views"][facing]={"screen_forward":[dx,dy],"samples":samples}
assert hashlib.sha256(source.read_bytes()).hexdigest()==original
out=ROOT/"docs/art-review/rainoray-eight-way/motion-inspection.json"
out.write_text(json.dumps(report,indent=2)+"\n")
print("PASS: 20-bone source intact; 228 diagonal pose samples; all four camera headings match movement")
