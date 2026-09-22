-- Run from the project root with: aseprite --batch --script tools/export-human-review.lua
-- Native pixels, four direction tags, original art preserved on a hidden layer.
local root = app.params.root or "."
local base = root .. "/docs/art-review/"
local sprite = Sprite(80, 80, ColorMode.RGB)
local original = sprite.layers[1]
original.name = "Original source"
local corrected = sprite:newLayer()
corrected.name = "Manual mask correction"
for index, direction in ipairs({"south", "east", "north", "west"}) do
  if index > 1 then sprite:newEmptyFrame(index) end
  sprite.frames[index].duration = 0.25
  sprite:newCel(original, index, Image{fromFile=base .. "human-motion-fix/armed-idle-" .. direction .. "/frame-0.png"}, Point(0, 0))
  sprite:newCel(corrected, index, Image{fromFile=base .. "human-motion-manual/canonical-" .. direction .. ".png"}, Point(0, 0))
end
for index, direction in ipairs({"south", "east", "north", "west"}) do
  sprite:newTag(index, index).name = direction
end
original.isVisible = false
assert(#sprite.frames == 4 and #sprite.layers == 2 and #sprite.tags == 4)
for index, tag in ipairs(sprite.tags) do
  assert(tag.fromFrame.frameNumber == index and tag.toFrame.frameNumber == index)
end
sprite:saveAs(base .. "human-motion-manual/canonical.aseprite")
sprite:close()

local run = Sprite(80, 80, ColorMode.RGB)
local source = run.layers[1]
source.name = "Installed run - original"
local edits = run:newLayer()
edits.name = "Manual correction draft - review required"
local before = Image{fromFile=root .. "/assets/characters/blade/rainoray_run.png"}
local after = Image{fromFile=base .. "human-motion-manual/run-prototype.png"}
for frame = 1, 48 do
  if frame > 1 then run:newEmptyFrame(frame) end
  local rectangle = Rectangle(((frame-1)%12)*80, math.floor((frame-1)/12)*80, 80, 80)
  run:newCel(source, frame, Image(before, rectangle), Point(0, 0))
  run:newCel(edits, frame, Image(after, rectangle), Point(0, 0))
  run.frames[frame].duration = 0.05
end
for index, direction in ipairs({"south", "east", "north", "west"}) do
  run:newTag((index-1)*12+1, index*12).name = direction
end
source.isVisible = false
assert(#run.frames == 48 and #run.layers == 2 and #run.tags == 4)
run:saveAs(base .. "human-motion-manual/run-draft.aseprite")
run:close()
