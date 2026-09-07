from PIL import Image
src="docs/art-review/outpost-fable/landmarks-retry/watch-platform-rejected-1.png"
im=Image.open(src).convert("RGBA"); px=im.load(); w,h=im.size
removed=0
for y in range(h):
    for x in range(w):
        r,g,b,a=px[x,y]
        if a==0: continue
        lum=r*.299+g*.587+b*.114
        # pale lavender-grey ground slab: low saturation, bluish, lum 88-125
        if lum>=86 and lum<=128 and b>=r-2 and b>=g and max(r,g,b)-min(r,g,b)<=22:
            px[x,y]=(0,0,0,0); removed+=1
opaque=sum(1 for y in range(h) for x in range(w) if px[x,y][3]>0)
print("removed",removed,"opaque",opaque,"%.1f%%"%(100*opaque/(w*h)))
im.save("/home/ghiankylledomingo/.claude/jobs/718d9a29/tmp/ref/watch-platform-masked.png")
big=Image.new("RGBA",(w*4*2+16,h*4),(52,58,50,255))
big.alpha_composite(Image.open(src).convert("RGBA").resize((w*4,h*4),Image.NEAREST),(0,0))
big.alpha_composite(im.resize((w*4,h*4),Image.NEAREST),(w*4+16,0))
big.save("/home/ghiankylledomingo/.claude/jobs/718d9a29/tmp/ref/watch-platform-mask-contact.png")
