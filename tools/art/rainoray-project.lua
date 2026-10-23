-- Aseprite/Pixie Dust timeline import and lossless export. See the source project's README.
local root = app.params.root or "."
local project = app.params.project or (root .. "/assets/source/characters/rainoray/Rainoray-Human.pixie-dust")
local mode = app.params.mode or "create"
local directions = {"south", "east", "north", "west", "southeast", "northeast", "northwest", "southwest"}
-- sheet, tag, block, cell, frames, total milliseconds, contact milliseconds (if any).
-- Timings follow BladeAnimation, Player and RuinedOutpostGame; round boundaries, not every frame.
local actions = {
  {"idle", "idle", 0, 80, 8, 1333}, {"run", "run", 0, 80, 8, 500},
  {"start", "start", 0, 80, 4, 120}, {"stop", "stop", 0, 80, 4, 120},
  {"turn", "turn", 0, 80, 4, 120},
  {"slash", "draw_cut", 0, 160, 12, 300, 90},
  {"slash", "thrust", 1, 160, 12, 340, 100},
  {"slash", "diagonal_cut", 2, 160, 12, 400, 140},
  {"slash", "overhead_cut", 3, 160, 12, 540, 200},
  {"slash", "dash_slash", 4, 160, 12, 360, 80},
  {"cleave", "cleave", 0, 160, 12, 580, 180},
  {"cleave", "spin_cleave", 1, 160, 12, 700, 180},
  {"charge", "charge", 0, 80, 8, 350}, {"hold", "hold", 0, 80, 8, 1000},
  {"dash", "dash", 0, 160, 8, 120}, {"cast", "cast", 0, 160, 8, 350},
  {"guard", "guard", 0, 160, 8, 300}, {"hurt", "hurt", 0, 160, 8, 320},
  {"counter", "counter", 0, 160, 8, 320},
}
local trails = {slash=true, cleave=true, cast=true, counter=true}
local function duration(a, i)
  local count, total = a[5], a[6]
  if a[7] then
    if i <= 4 then count, total = 4, a[7]
    else i, count, total = i-4, count-4, total-a[7] end
  end
  return (math.floor(total*i/count+0.5)-math.floor(total*(i-1)/count+0.5))/1000
end
local function fresh(path)
  assert(not app.fs.isFile(path), "Refusing to overwrite: " .. path)
end
local function sheetPath(name, effects)
  local dir = app.params.sheets or (root .. (effects and "/assets/effects" or "/assets/characters/blade"))
  return dir .. "/rainoray_" .. name .. (effects and "_trail" or "") .. ".png"
end

if mode == "create" then
  fresh(project)
  local sprite = Sprite(160, 160, ColorMode.RGB)
  local before = sprite.layers[1]
  before.name = "Before polish"
  local body = sprite:newLayer(); body.name = "Character"
  local fx = sprite:newLayer(); fx.name = "Effects"
  local frame, clips = 0, {}
  for _, a in ipairs(actions) do
    local sheet = Image{fromFile=sheetPath(a[1], false)}
    local old = app.params.before and Image{fromFile=app.params.before .. "/rainoray_" .. a[1] .. ".png"} or sheet
    local trail = trails[a[1]] and Image{fromFile=sheetPath(a[1], true)} or nil
    assert(sheet.width == a[4]*a[5] and sheet.height >= (a[3]+1)*#directions*a[4], "Unexpected sheet layout")
    assert(old.width == sheet.width and old.height == sheet.height, "Before layer layout differs")
    for row, direction in ipairs(directions) do
      local first = frame+1
      for i = 1, a[5] do
        frame = frame+1
        if frame > 1 then sprite:newEmptyFrame(frame) end
        sprite.frames[frame].duration = duration(a, i)
        local crop = Rectangle((i-1)*a[4], (a[3]*#directions+row-1)*a[4], a[4], a[4])
        local offset = Point((160-a[4])/2, (160-a[4])/2)
        sprite:newCel(body, frame, Image(sheet, crop), offset)
        sprite:newCel(before, frame, Image(old, crop), offset)
        if trail then
          local cel = Image(trail, Rectangle((i-1)*160, (a[3]*#directions+row-1)*160, 160, 160))
          if not cel:isEmpty() then sprite:newCel(fx, frame, cel, Point(0, 0)) end
        end
      end
      clips[#clips+1] = {first, frame, a[2] .. "_" .. direction, row}
    end
  end
  -- Adding timeline frames can extend existing tags; set ranges after all cels exist.
  for _, clip in ipairs(clips) do
    local tag = sprite:newTag(clip[1], clip[2])
    tag.name = clip[3]
    tag.color = Color{r=42+clip[4]*24, g=150, b=190}
    assert(tag.toFrame.frameNumber-tag.fromFrame.frameNumber+1 == clip[2]-clip[1]+1)
  end
  before.isVisible = false; before.isEditable = false
  assert(frame == 1344 and #sprite.tags == 152 and #sprite.layers == 3)
  app.layer = body; app.frame = sprite.frames[1]
  app.fs.makeAllDirectories(app.fs.filePath(project))
  sprite:saveAs(project)
  assert(app.fs.isFile(project), "Editor could not save the native project")
  sprite:close()
  print("Created 1344 frames, 152 action/direction tags, Character + Effects + hidden Before polish")
elseif mode == "export" then
  local out = assert(app.params.out, "Specify a fresh export directory")
  local sprite = Sprite{fromFile=project}
  assert(sprite.width == 160 and sprite.height == 160 and #sprite.frames == 1344 and #sprite.tags == 152,
         "Keep the canvas, frame count and action tags unchanged for the game")
  local layers, tags, sheets, effects = {}, {}, {}, {}
  for _, layer in ipairs(sprite.layers) do layers[layer.name] = layer end
  assert(#sprite.layers == 3 and layers.Character and layers.Effects and layers["Before polish"],
         "Merge added paint layers into Character or Effects before exporting")
  for _, tag in ipairs(sprite.tags) do
    assert(not tags[tag.name], "Duplicate action tag: " .. tag.name)
    tags[tag.name] = tag
  end
  for _, a in ipairs(actions) do
    local height = (a[3]+1)*#directions*a[4]
    if not sheets[a[1]] or sheets[a[1]].height < height then
      sheets[a[1]] = Image(a[5]*a[4], height, ColorMode.RGB)
      if trails[a[1]] then effects[a[1]] = Image(a[5]*160, (a[3]+1)*#directions*160, ColorMode.RGB) end
    end
  end
  local function render(layer, frame)
    assert(layer.isImage and layer.blendMode == BlendMode.NORMAL, "Use normal image layers")
    local image = Image(160, 160, ColorMode.RGB)
    local cel = layer:cel(frame)
    if cel then image:drawImage(cel.image, cel.position, math.floor(cel.opacity*layer.opacity/255+0.5)) end
    return image
  end
  local used = {}
  for _, a in ipairs(actions) do
    for row, direction in ipairs(directions) do
      local tag = assert(tags[a[2] .. "_" .. direction], "Missing action/direction tag")
      local first = tag.fromFrame.frameNumber
      assert(tag.toFrame.frameNumber-first+1 == a[5], "Wrong number of frames in " .. tag.name)
      for i = 1, a[5] do
        local frame, offset = first+i-1, (160-a[4])/2
        assert(not used[frame], "Overlapping action tags")
        used[frame] = true
        local full = render(layers.Character, frame)
        local body = Image(full, Rectangle(offset, offset, a[4], a[4]))
        local restored = Image(160, 160, ColorMode.RGB)
        restored:drawImage(body, Point(offset, offset))
        assert(restored:isEqual(full), "Character paint outside the game cell in " .. tag.name)
        sheets[a[1]]:drawImage(body, Point((i-1)*a[4], (a[3]*#directions+row-1)*a[4]))
        local fx = render(layers.Effects, frame)
        if effects[a[1]] then effects[a[1]]:drawImage(fx, Point((i-1)*160, (a[3]*#directions+row-1)*160))
        else assert(fx:isEmpty(), "This game action has no separate effect sheet: " .. tag.name) end
      end
    end
  end
  for name in pairs(sheets) do fresh(out .. "/rainoray_" .. name .. ".png") end
  for name in pairs(effects) do fresh(out .. "/rainoray_" .. name .. "_trail.png") end
  app.fs.makeAllDirectories(out)
  for name, image in pairs(sheets) do image:saveAs(out .. "/rainoray_" .. name .. ".png") end
  for name, image in pairs(effects) do image:saveAs(out .. "/rainoray_" .. name .. "_trail.png") end
  sprite:close()
  print("Exported 14 body sheets + 4 effects sheets; native project kept intact")
else
  error("mode must be create or export")
end
