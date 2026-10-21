import bpy, bmesh, sys, json, math, hashlib
from pathlib import Path
from mathutils import Quaternion
import numpy as np
ROOT=Path(__file__).resolve().parents[5]
OUT=ROOT/'docs/art-review/sprite3d-run/source/rainoray-editable'
sys.path.insert(0,str(ROOT/'tools/sprite3d'))
import puppet, animate, render_frames
source=ROOT/'docs/art-review/sprite3d-run/sheets/puppet.blend'
selection=json.loads((OUT/'sword-faces.json').read_text())
assert hashlib.sha256(source.read_bytes()).hexdigest()==selection['source_sha256'], 'Source changed; inspect the face selection again'
bpy.ops.wm.open_mainfile(filepath=str(source))
body=bpy.data.objects['Mesh_0'];arm=bpy.data.objects['RainorayRig']
original_faces=len(body.data.polygons)
for domain,name,count in [('POINT','source_vertex',len(body.data.vertices)),('FACE','source_face',original_faces)]:
 attr=body.data.attributes.new(name,'INT',domain)
 attr.data.foreach_set('value',range(count))
baseline=body.copy();baseline.data=body.data.copy();bpy.context.scene.collection.objects.link(baseline);baseline.hide_render=True
bpy.ops.object.select_all(action='DESELECT');body.select_set(True);bpy.context.view_layer.objects.active=body
bpy.context.tool_settings.mesh_select_mode=(False,False,True)
bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.select_all(action='DESELECT');bpy.ops.object.mode_set(mode='OBJECT')
for i in selection['polygons']:body.data.polygons[i].select=True
bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.separate(type='SELECTED');bpy.ops.object.mode_set(mode='OBJECT')
sword=next(o for o in bpy.context.selected_objects if o!=body)
body.name='RainorayBody';sword.name='RainoraySword'
assert len(body.data.polygons)+len(sword.data.polygons)==original_faces
assert set(p.value for p in sword.data.attributes['source_face'].data)==set(selection['polygons'])
assert body.parent==sword.parent==arm
# Close the cut and adjacent source boundary edges; preserve the original vertices and UVs.
cap_counts={}
for ob,colour in [(body,(1,.84,.59,1)),(sword,(.018,.014,.012,1))]:
 material=bpy.data.materials.new(ob.name+'Seam');material.diffuse_color=colour;ob.data.materials.append(material)
 bm=bmesh.new();bm.from_mesh(ob.data)
 seam=[e for e in bm.edges if e.is_boundary and all(v.co.x<-.24 and v.co.z<-.12 for v in e.verts)]
 print('SEAM',ob.name,len(seam))
 caps=[g for g in bmesh.ops.triangle_fill(bm,edges=seam,use_beauty=True)['geom'] if isinstance(g,bmesh.types.BMFace)]
 assert caps, 'Hand seam could not be closed'
 for face in caps:face.material_index=len(ob.data.materials)-1
 bmesh.ops.triangulate(bm,faces=caps)
 bm.to_mesh(ob.data);bm.free();ob.data.update()
 cap_counts[ob.name]=len(ob.data.polygons)-(len(selection['polygons']) if ob==sword else original_faces-len(selection['polygons']))
# The original spatial weighting included fingers in 'blade'; keep the hand independent.
reweighted={body.name:set(),sword.name:set()}
for ob in [body,sword]:
 blade=ob.vertex_groups['blade']; hand=ob.vertex_groups['hand.R']
 for v in ob.data.vertices:
  weight=next((g.weight for g in v.groups if g.group==blade.index),0.)
  if ob==body and weight>0:
   hand.add([v.index],weight,'ADD');blade.remove([v.index]);reweighted[ob.name].add(v.index)
  elif ob==sword and weight<.999999:
   puppet.set_only(ob,v.index,{'blade':1.});reweighted[ob.name].add(v.index)
print('REWEIGHTED',{name:len(ids) for name,ids in reweighted.items()})
# A removable front surface covers the old markings; bangs remain in front.
verts=[(puppet.CX,.018,.535)];uvs=[(.5,.5)];faces=[]
rings,segments=10,48
for ring in range(1,rings+1):
 r=ring/rings
 for j in range(segments):
  angle=2*math.pi*j/segments;x=r*math.cos(angle);z=r*math.sin(angle)
  verts.append((puppet.CX+.205*x,.018+.10*x*x+.026*z*z,.535+.205*z));uvs.append((.5+.5*x,.5+.5*z))
for j in range(segments):faces.append((0,1+j,1+(j+1)%segments))
for ring in range(1,rings):
 start=1+(ring-1)*segments;next_start=start+segments
 for j in range(segments):
  k=(j+1)%segments;faces.append((start+j,next_start+j,next_start+k,start+k))
mesh=bpy.data.meshes.new('SimpleMaskSurface');mesh.from_pydata(verts,[],faces);mesh.update()
mask=bpy.data.objects.new('RainorayMask',mesh);bpy.context.scene.collection.objects.link(mask)
uv=mesh.uv_layers.new(name='MaskUV')
for loop in mesh.loops:uv.data[loop.index].uv=uvs[loop.vertex_index]
mat=bpy.data.materials.new('SimpleMask');mat.use_nodes=True
img=bpy.data.images.load(str(OUT/'mask-simple.png'));img.pack()
tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=img;tex.interpolation='Closest'
mat.node_tree.nodes.active=tex;mat.node_tree.links.new(tex.outputs['Color'],mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'])
mesh.materials.append(mat)
mask.parent=arm;group=mask.vertex_groups.new(name='head');group.add(list(range(len(verts))),1,'REPLACE')
mod=mask.modifiers.new('Armature','ARMATURE');mod.object=arm
# Compare deformation against the intact puppet at start/middle/end of each action and facing.
poser=puppet.Poser(arm);maximum=0.;repair_delta=0.;samples=0
for action in {**animate.locomotion.FRAMES,**animate.attacks.FRAMES,**animate.reactions.FRAMES}:
 for facing,view in animate.VIEWS.items():
  poses,_,_=animate.frame_posers(action,None,view)
  for index in sorted({0,len(poses)//2,len(poses)-1}):
   poses[index](poser);deps=bpy.context.evaluated_depsgraph_get();deps.update()
   reference=baseline.evaluated_get(deps).data
   coords=np.empty(len(reference.vertices)*3);reference.vertices.foreach_get('co',coords);coords=coords.reshape(-1,3)
   for ob in [body,sword]:
    ev=ob.evaluated_get(deps).data;actual=np.empty(len(ev.vertices)*3);ev.vertices.foreach_get('co',actual);actual=actual.reshape(-1,3)
    ids=np.array([item.value for item in ob.data.attributes['source_vertex'].data])
    delta=np.abs(actual-coords[ids]);unchanged=[i for i in range(len(ids)) if i not in reweighted[ob.name]]
    error=float(np.max(delta[unchanged]));maximum=max(maximum,error);repair_delta=max(repair_delta,float(np.max(delta)))
    assert error<1e-6,(action,facing,index,ob.name,error)
   samples+=1
bpy.data.objects.remove(baseline,do_unlink=True)
for pb in arm.pose.bones:pb.matrix_basis.identity()
bpy.context.view_layer.update()
scene=bpy.context.scene;render_frames.setup_render(scene,900)
for facing in ['south','east','north','west']:
 render_frames.place_camera(scene,facing,2.,-.96,(.15,.19),900,680,780,elevation=4)
 sword.hide_render=True;scene.render.filepath=str(OUT/('body-'+facing+'.png'));bpy.ops.render.render(write_still=True)
 sword.hide_render=False;scene.render.filepath=str(OUT/('armed-'+facing+'.png'));bpy.ops.render.render(write_still=True)
for facing in ['south','east']:
 render_frames.place_camera(scene,facing,.48,-.46,(-.34,.10),900,720,810,elevation=0)
 sword.hide_render=True;scene.render.filepath=str(OUT/('hand-'+facing+'.png'));bpy.ops.render.render(write_still=True)
# Save a body-first editing scene. Toggle sword's eye and camera icons to restore it.
sword.hide_render=True;sword.hide_set(True);arm.show_in_front=True;arm.data.display_type='STICK';arm.data.bones['blade'].hide=True
for ob in scene.objects:ob.select_set(False)
arm.select_set(True);bpy.context.view_layer.objects.active=arm
for screen in bpy.data.screens:
 for area in screen.areas:
  if area.type=='VIEW_3D':
   space=area.spaces.active;space.shading.type='SOLID';space.shading.light='FLAT';space.shading.color_type='TEXTURE'
   space.region_3d.view_rotation=Quaternion((1,0,0),math.pi/2);space.region_3d.view_location=(puppet.CX,.19,0)
   space.region_3d.view_distance=3.4;space.region_3d.view_perspective='ORTHO'
for img in bpy.data.images:
 if img.source=='FILE' and not img.packed_file:img.pack()
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'Rainoray-Body-and-Separate-Sword.blend'))
report={'source_sha256':selection['source_sha256'],'original_faces':original_faces,'sword_original_faces':len(selection['polygons']),'cap_faces':cap_counts,'bones':len(arm.data.bones),'pose_samples':samples,'maximum_unaffected_vertex_error':maximum,'maximum_reweighted_vertex_delta':repair_delta,'reweighted_vertices':{name:len(ids) for name,ids in reweighted.items()},'objects':{o.name:len(o.data.vertices) for o in [body,sword,mask]}}
(OUT/'verification.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
