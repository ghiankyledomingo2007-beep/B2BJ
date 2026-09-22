"""Offline native-pixel corrections, explicitly requested by the owner.

No generation service, smoothing or runtime dependency. Source art stays in
the previous selection manifest; corrected frames are staged for visual review.
"""
from pathlib import Path
import math
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "docs/art-review/human-motion-fix"
OUT = ROOT / "docs/art-review/human-motion-manual"
DIRECTIONS = ("south", "east", "north", "west")
IVORY = "#f4f3e9"
SHADE = "#ced8d4"
MARK = "#535461"
EDGE = "#596773"
INK = "#151722"
COAT = "#353e88"
COAT_DARK = "#272e62"
TRIM = "#a7c3dc"


def limb(draw, points, width=5, color=COAT):
    points=[(round(x),round(y)) for x,y in points]
    draw.line(points, fill=INK, width=width+2, joint="curve")
    draw.line(points, fill=color, width=width, joint="curve")
    for x,y in points[1:-1]:
        draw.ellipse((x-width//2,y-width//2,x+width//2,y+width//2),fill=color)


def sword(draw, hand, tip):
    hx,hy=hand;tx,ty=tip
    length=math.hypot(tx-hx,ty-hy)
    ux,uy=(tx-hx)/length,(ty-hy)/length
    nx,ny=-uy,ux
    def point(x,y):return round(x),round(y)
    guard=(hx+ux*4,hy+uy*4)
    draw.line([point(hx-ux*2,hy-uy*2),point(*guard)],fill=INK,width=3)
    draw.line([point(hx-ux,hy-uy),point(*guard)],fill="#6f736a",width=1)
    gx,gy=guard
    draw.line([point(gx-nx*3,gy-ny*3),point(gx+nx*3,gy+ny*3)],fill="#c6b87d",width=2)
    draw.polygon([point(gx+ux+nx,gy+uy+ny),point(tx,ty),point(gx+ux-nx*2,gy+uy-ny*2)],fill=INK)
    draw.line([point(gx+ux*2,gy+uy*2),point(tx-ux,ty-uy)],fill="#dce8e5",width=2)
    draw.line([point(gx+ux*2-nx,gy+uy*2-ny),point(tx-ux*3,ty-uy*3)],fill="#91a7b3",width=1)
    draw.rectangle((round(hx)-1,round(hy)-1,round(hx)+1,round(hy)+1),fill="#dbc8a1")



def canonical(direction):
    image = Image.open(SOURCE / f"armed-idle-{direction}/frame-0.png").convert("RGBA")
    original = image.copy()
    draw = ImageDraw.Draw(image)
    if direction == "south":
        draw.polygon([(34,14),(42,14),(44,17),(44,24),(41,27),(36,27),(33,24),(33,17)], fill=EDGE)
        draw.polygon([(34,16),(36,15),(41,15),(43,17),(43,24),(41,26),(36,26),(34,24)], fill=IVORY)
        draw.line([(34,21),(34,24),(36,26),(41,26),(43,24)], fill=SHADE)
        draw.line([(36,16),(35,18),(37,19),(36,20)], fill=MARK)
        draw.line([(41,16),(42,18),(40,19),(41,20)], fill=MARK)
        draw.line([(35,22),(37,23),(40,23),(42,22)], fill=MARK)
        draw.point((35,23), fill=MARK); draw.point((42,23), fill=MARK)
        draw.point((39,20), fill="#be443d")
        # Reuse the source fringe, stopping above the fixed facial markings.
        for y in range(12,18):
            for x in range(33,45):
                r,g,b,a=original.getpixel((x,y))
                if a and g>90 and b-r>=30:
                    image.putpixel((x,y),(r,g,b,a))
    elif direction in ("east", "west"):
        # A profile has one visible flame marking; the red jewel lies at the nose edge.
        patch=Image.new("RGBA",(8,14));p=ImageDraw.Draw(patch)
        p.polygon([(0,0),(4,0),(7,3),(7,10),(4,13),(1,12),(0,9)],fill=EDGE)
        p.polygon([(1,1),(4,1),(6,3),(6,10),(4,12),(2,11),(1,9)],fill=IVORY)
        p.line([(1,9),(2,11),(4,12)],fill=SHADE)
        p.line([(3,2),(2,4),(4,5),(3,6)],fill=MARK)
        p.line([(2,9),(4,10),(6,9)],fill=MARK);p.point((2,10),fill=MARK)
        p.point((6,6),fill="#be443d")
        left=40 if direction=="east" else 31
        if direction=="west":patch=patch.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
        draw.rectangle((left,14,left+7,27),fill=(0,0,0,0))
        image.alpha_composite(patch,(left,14))
        for y in range(12,28):
            for x in range(left,left+8):
                r,g,b,a=original.getpixel((x,y))
                if a and g>90 and b-r>=30:
                    image.putpixel((x,y),(r,g,b,a))
    return image


def blue_cloth(c):
    r,g,b,a=c
    return a and b>r*1.3 and b>g*1.15


def hair(c):
    r,g,b,a=c
    return a and g>85 and b-r>=25


def clear_weapon(image, polygon, repair=None):
    region=Image.new("L",image.size);ImageDraw.Draw(region).polygon(polygon,fill=255)
    original=image.copy()
    for y in range(80):
        for x in range(80):
            if region.getpixel((x,y)) and not blue_cloth(original.getpixel((x,y))):
                image.putpixel((x,y),(0,0,0,0))
                if repair and repair[0]<=x<=repair[1] and repair[2]<=y<=repair[3]:
                    image.putpixel((x,y),original.getpixel((repair[4],y)))


def erase_old_blade(image,direction):
    """Remove only long neutral-metal components in the reviewed weapon region."""
    bounds={"south":(40,40,65,59),"east":(10,40,45,70),"north":(24,37,44,70),"west":(35,39,67,70)}
    x0,y0,x1,y1=bounds[direction];original=image.copy()
    points=set()
    for y in range(y0,y1):
        for x in range(x0,x1):
            r,g,b,a=original.getpixel((x,y))
            if a and min(r,g,b)>110 and max(r,g,b)-min(r,g,b)<(80 if direction=="north" and y>=44 else 34):points.add((x,y))
    while points:
        seed=points.pop();component={seed};todo=[seed]
        while todo:
            x,y=todo.pop()
            for dx,dy in ((-1,-1),(0,-1),(1,-1),(-1,0),(1,0),(-1,1),(0,1),(1,1)):
                q=(x+dx,y+dy)
                if q in points:points.remove(q);component.add(q);todo.append(q)
        if len(component)<7:continue
        for x,y in component:
            for dy in (-1,0,1):
                for dx in (-1,0,1):
                    xx,yy=x+dx,y+dy
                    c=original.getpixel((xx,yy))
                    if not blue_cloth(c) and (not hair(c) or direction=="north" and yy>=44):
                        image.putpixel((xx,yy),(0,0,0,0))
                        if direction=="north" and 31<=xx<=41 and 46<=yy<=59:
                            image.putpixel((xx,yy),(53,62,136,255))


def run_prototype(direction,frame):
    manifest=json.loads((SOURCE/"selection.json").read_text())
    names=manifest["sheets"]["assets/characters/blade/rainoray_run.png"]["frames"]
    im=Image.open(SOURCE/names[DIRECTIONS.index(direction)*12+frame]).convert("RGBA")
    original=im.copy();d=ImageDraw.Draw(im)
    erase_old_blade(im,direction)
    swing=math.sin(frame*math.tau/12)
    if direction=="south":
        dx=[0,1,1,1,0,0,0,1,1,1,0,0][frame]
        clear_weapon(im,[(42,42),(47,41),(61,54),(56,57),(43,47)])
        d.rectangle((20,33,49,44),fill=(0,0,0,0))
        d.polygon([(28+dx,31),(40+dx,31),(43+dx,43),(29+dx,45),(27+dx,39)],fill=INK)
        d.polygon([(29+dx,32),(39+dx,32),(41+dx,42),(29+dx,42)],fill=COAT)
        d.line([(35+dx,33),(35+dx,42)],fill=TRIM)
        grip=(25+dx,44+round(2*swing));free=(47+dx,40-round(5*swing))
        limb(d,[(27+dx,32),(23+dx,38),grip],4)
        limb(d,[(42+dx,32),(47+dx,36),free],4)
        sword(d,grip,(grip[0]-22,grip[1]+16))
        d.rectangle((free[0]-1,free[1]-1,free[0]+1,free[1]+1),fill="#ddc9a3")
        im.alpha_composite(original.crop((20,27,49,33)),(20,27))
        d.rectangle((26+dx,7,43+dx,27),fill=(0,0,0,0))
        im.alpha_composite(canonical(direction).crop((26,6,51,28)),(21+dx,4))
    elif direction=="east":
        top=[18,18,17,16,17,17,19,19,18,17,16,17][frame]
        clear_weapon(im,[(18,65),(22,69),(43,48),(40,44)])
        grip=(41,49+round(swing))
        limb(d,[(46,40),(41,44),grip],3,COAT_DARK)
        sword(d,grip,(grip[0]-26,grip[1]+15))
        # Keep the generated hair and legs, replace only the rigid face plate.
        d.rectangle((48,top,57,top+17),fill=(0,0,0,0))
        patch=canonical(direction).crop((40,14,48,28)).resize((8,16),Image.Resampling.NEAREST)
        im.alpha_composite(patch,(49,top+1))
        for yy in range(top-2,top+17):
            for x in range(47,58):
                c=original.getpixel((x,yy))
                if hair(c) or (yy>=top+14 and c[3] and c[0]>c[2]+40):im.putpixel((x,yy),c)
    elif direction=="north":
        clear_weapon(im,[(29,40),(41,40),(41,69),(29,69)],(33,39,42,60,42))
        for y in range(35,51):
            for x in list(range(22,34))+list(range(48,62)):
                if not hair(original.getpixel((x,y))):im.putpixel((x,y),(0,0,0,0))
        grip=(53,46+round(2*swing));free=(27,42-round(5*swing))
        limb(d,[(48,36),(52,40),grip],4)
        limb(d,[(32,36),(27,39),free],4)
        sword(d,grip,(grip[0]+23,grip[1]+15))
        d.rectangle((free[0]-1,free[1]-1,free[0]+1,free[1]+1),fill="#ddc9a3")
        for y in range(29,44):
            for x in range(22,62):
                if hair(original.getpixel((x,y))):im.putpixel((x,y),original.getpixel((x,y)))
    else:
        dy=[0,0,1,-1,0,-1,0,-1,0,0,-1,-1][frame]
        clear_weapon(im,[(36,44),(42,41),(61,63),(57,67),(37,50)])
        for y in range(37,48):
            for x in range(16,43):
                if not hair(original.getpixel((x,y))):im.putpixel((x,y),(0,0,0,0))
        d.rectangle((16,33,23,37),fill=(0,0,0,0))
        grip=(41,47+round(swing));free=(23+round(5*swing),42-round(3*swing))
        limb(d,[(35,35),(40,41),grip],3,COAT_DARK)
        sword(d,grip,(grip[0]+26,grip[1]+16))
        d.polygon([(28,36),(37,35),(41,46),(29,49),(26,43)],fill=INK)
        d.polygon([(29,37),(36,37),(39,46),(29,47),(28,42)],fill=COAT)
        limb(d,[(29,35),(24,40),free],4)
        d.rectangle((free[0]-1,free[1]-1,free[0]+1,free[1]+1),fill="#ddc9a3")
        im.alpha_composite(original.crop((23,30,35,37)),(23,30))
        d.polygon([(25,11+dy),(22,13+dy),(21,16+dy),(21,24+dy),(24,28+dy),(27,28+dy),(27,12+dy)],fill=EDGE)
        d.polygon([(25,12+dy),(23,14+dy),(22,16+dy),(22,24+dy),(24,27+dy),(26,27+dy),(26,13+dy)],fill=IVORY)
        d.line([(24,15+dy),(25,17+dy),(23,18+dy),(24,20+dy)],fill=MARK)
        d.point((22,21+dy),fill="#be443d")
        d.line([(22,24+dy),(24,25+dy),(26,24+dy)],fill=MARK)
    return im


def main():
    OUT.mkdir(exist_ok=True)
    sheet=Image.new("RGBA",(320,80))
    for row,direction in enumerate(DIRECTIONS):
        im=canonical(direction)
        assert im.size==(80,80) and im.getextrema()[3][0]==0
        im.save(OUT/f"canonical-{direction}.png")
        sheet.alpha_composite(im,(row*80,0))
    sheet.save(OUT/"canonical.png")
    run=Image.new("RGBA",(960,320))
    for row,direction in enumerate(DIRECTIONS):
        seen=set()
        for frame in range(12):
            im=run_prototype(direction,frame)
            assert im.size==(80,80) and im.getbbox() and im.getextrema()[3][0]==0
            seen.add(im.tobytes())
            run.alpha_composite(im,(80*frame,80*row))
        assert len(seen)==12, f"Repeated run drawing: {direction}"
    run.save(OUT/"run-prototype.png")


if __name__=="__main__":
    main()
