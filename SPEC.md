# Roomik — spec & roadmap

Android app for drawing a floor plan (several rooms, walls, doors, windows) in 2D and
then viewing it in 3D — from outside (orbit / dollhouse) and from inside (walk-through).
On the plan you also place sockets, light switches and lamps; that electrical layer is
sent to **elektronik**, which builds the distribution board from it (§4), and the app
works out **what to buy**: cable by type and length, boxes, mechanisms, frames and
other accessories (§5).
Furniture is secondary and comes after the plan → 3D → electrics loop works well.

- **Package:** `com.dawoyan.roomik` (same prefix as `com.dawoyan.elektronik`)
- **Stack:** Kotlin, Jetpack Compose (Material 3), single-activity, same toolchain as
  `elektronik` (compileSdk/targetSdk 35, minSdk 26, Compose BOM 2024.12.01)
- **3D:** own small OpenGL ES 3.0 renderer in a `GLSurfaceView` (see §8)
- **UI languages:** ru, en, hy — follows system locale, with an in-app override
- **Units:** metric only (cm in the model, shown as cm or m); areas in m²
- **Offline:** no account, no network, no ads
- **APK naming:** `roomik-<version>.apk`

## 1. Core loop

**Draw walls → rooms appear automatically → add doors/windows → place sockets, switches
and lamps → tap "3D" → look at it from outside, then step inside → send the electrics
to elektronik to get the board.** Switching 2D ↔ 3D is instant and keeps the same
position (the 3D camera starts above the spot you were looking at in 2D; going back
centres 2D on where you stood).

## 2. 2D floor plan editor

### 2.1 Walls
- The plan is a graph of wall segments joined at corner nodes — not separate room
  rectangles. A wall between two rooms is one shared wall.
- Tools:
  - **Draw wall:** tap–tap or drag chains of segments; snaps to 0/45/90°, to existing
    corners and walls, and to the grid; live length label while drawing; type an exact
    length for the segment being drawn.
  - **Add room:** drop a rectangle by width × length; its walls merge with any walls
    they touch.
  - **Edit:** drag a corner; drag a wall parallel to itself (neighbours stretch);
    tap a wall → exact length; split a wall; delete a wall or corner.
- Wall properties: thickness (default outer 30 cm, inner 10 cm; editable per wall),
  height (project default 270 cm; per-wall override later).
- Dimensions shown are **clear inner** dimensions of rooms; wall thickness grows
  symmetrically around the drawn centre line.

### 2.2 Rooms
- Rooms are detected automatically as closed loops of walls (faces of the wall graph).
- Each room: name (Kitchen, Bedroom… presets + free text), floor colour, area (m²) and
  perimeter computed live, label drawn in the middle.
- An unclosed chain of walls is allowed (partition, half-drawn plan) — it just doesn't
  form a room yet.

### 2.3 Openings
- **Door:** width, height (default 80 × 205), hinge side, swing direction; arc drawn in 2D.
- **Window:** width, height, sill height (default 120 × 140, sill 90).
- **Passage:** opening without a leaf, optional height (full height = gap in wall).
- Attached to a wall by offset; slide along it, move with it, cannot overlap or leave
  the wall. Exact offset from either corner can be typed.

### 2.4 Canvas
- Two-finger pan, pinch zoom, double-tap to fit.
- Grid 1 / 5 / 10 / 50 cm; snapping can be switched off.
- Undo / redo (≥ 50 steps). Autosave on every change.
- Layer toggles: dimensions, grid, room labels, areas.
- Optional background image (photo of a paper plan) with scale calibration by marking
  a known length — trace over it. *(0.3)*

## 3. 3D view

### 3.1 Geometry generated from the plan
- Walls extruded to height with correct mitred joints at corners and T-junctions.
- Door and window holes cut through walls; window gets a simple frame and translucent
  glass; door gets a frame and a leaf drawn open ~30°.
- Floor slab per room in its colour; ceiling per room.
- Outside: ground plane, exterior wall colour, simple flat roof slab that can be hidden.
- Regenerated on entering 3D (plans are small — full rebuild takes milliseconds).

### 3.2 Outside view (orbit)
- One finger orbit, two-finger pan, pinch zoom around the house.
- **Dollhouse mode:** roof and ceilings hidden so rooms are visible from above; walls
  facing the camera can be auto-lowered ("cutaway") so the interior isn't blocked.
- Toggle roof on → proper exterior look with windows and doors.
- Tap a room → camera flies in and switches to inside view at that spot.

### 3.3 Inside view (walk-through)
- First-person camera at eye height 160 cm (adjustable).
- Left thumb pad moves, drag on the right looks around; double-tap a floor spot to
  walk there.
- Collision with walls; passes through doors and passages.
- Ceilings shown; daylight comes through windows.
- Mini-map in the corner showing position and view direction on the 2D plan; tap it to
  jump back to 2D.

### 3.4 Look
- Flat colours with directional sun light + ambient, simple shadow map, soft ambient
  occlusion baked per vertex at wall/floor edges so rooms don't look flat.
- Sun direction slider (time of day). Light/dark sky.
- Wall and floor colour per room; a small set of floor patterns (wood, tile) later.

## 4. Electrical layer and link to elektronik

### 4.1 Points on the plan
A separate **Electrics** mode in the 2D editor (layer can be hidden).

| Point | Placed on | Properties |
|---|---|---|
| **Socket** | wall | outlets in the frame (1–5), height from floor (default 30 cm; 110 over a worktop), earthed, splash-proof (IP44), optional note ("TV", "fridge") |
| **Appliance outlet** | wall or floor | name, power kW, own line yes/no, three-phase yes/no — presets taken from elektronik's room presets (oven 3.0, cooktop 7.0, washer 2.2, water heater 2.0, air cond. 2.5 …) |
| **Switch** | wall | gangs (1–3), height (default 90 cm), type: one-way / two-way (проходной) / intermediate / dimmer; warns if placed behind a door's swing |
| **Lamp** | ceiling or wall | type: ceiling lamp, spotlight, wall light, LED strip (drawn as a line), power W (default 10), mounting height for wall lights |
| **Junction box** | wall | optional, for drawing cable routes later |
| **Board (щиток)** | wall | one per plan; where all lines start |

- Wall points attach to a wall by offset and side (which room they face), like
  openings: they slide along the wall, move with it, and can't sit on a door or window.
  Exact distance from a corner can be typed.
- Ceiling points snap to the room centre, to halves/thirds, and to each other;
  "grid of spots" tool: N × M spotlights spread evenly in a room.
- Standard Russian/GOST-style plan symbols in 2D; legend on export.

### 4.2 Switch → lamp links
- Tap a switch gang, then tap the lamps it controls → a **light group**. Shown as a thin
  dashed arc from switch to lamps (schematic, not a cable route).
- Two-way switches: two (or more, with intermediates) switches linked to the same group.
- Checks: lamp without a switch, switch gang without lamps, two-way switch without a pair.

### 4.3 In 3D
- Sockets, switches and frames appear on the walls at their real heights; lamps on the
  ceiling/walls. In the inside view this shows whether a socket ends up behind a door,
  too low under a window, or a switch on the wrong side of the doorway.
- Lamps are real light sources in the walk-through (0.3 lighting pass); tapping a switch
  in 3D turns its light group on/off.

### 4.4 Hand-off to elektronik
elektronik already plans a board from *groups of loads per place* (`Room` with
`RoomLoad`s of kind `LIGHTS` / `SOCKETS` / appliance, in `core/Rooms.kt`). The plan
maps onto that directly:

| Roomik | elektronik |
|---|---|
| room (name + type: kitchen, bathroom, bedroom …) | `Room` (group) with matching `RoomType` — sets wet/RCD 10 vs 30 mA, earthed lights |
| all lamps in the room | one `RoomLoad(kind = LIGHTS, count = lamps, kw = sum of W)` |
| all socket outlets in the room | one `RoomLoad(kind = SOCKETS, count = outlets)` |
| each appliance outlet | `RoomLoad(name, kw, ownLine, threePhase)` |
| outdoor points (outside the outer walls) | group of type `YARD` |
| storey / wing (later) | `Room.zone` |

- **Send:** button "Щиток → elektronik" exports a small JSON file
  (`*.elplan`, `schemaVersion`, plan id, groups as above, per-group stable ids) and
  opens it in elektronik via a share intent (`ACTION_VIEW` + FileProvider URI).
  If elektronik isn't installed, the file can simply be saved/shared.
- **Receive (change in elektronik):** new intent filter for `*.elplan`; import creates a
  new project, or — if a project is already linked to this plan id — **updates its
  groups**, matched by the stable group/load ids. Devices are regenerated by
  `RoomPlanner` as today; hand-edited devices (`Device.edited`) survive, as they
  already do.
- One-way for now: the plan is the source of truth for *what loads exist where*;
  elektronik stays the source of truth for breakers, RCDs, cables and board layout.
  Electrical sizing rules are **not** duplicated in Roomik — it calls
  elektronik's `core` as a library (§5.1).
- **Back-link (later):** elektronik returns line numbers per load (e.g. "L7 — Kitchen
  sockets"); Roomik then labels each socket/lamp with its line, colours points
  by line, and can estimate cable length per line from board position along the walls.

## 5. What to buy (materials list)

### 5.1 Lines
To know cable types the app must know which line each point is on. Roomik uses
elektronik's `core` module as a library (Gradle composite build, `includeBuild("../elektronik")`)
and runs the same `RoomPlanner` + cable table (`core/Electrics.kt`) — so both apps give
the same lines and the rules live in one place:
- lights of a room → one 10 A line, 1.5 mm²;
- sockets → 16 A lines, 2.5 mm² (split by outlet count as elektronik does);
- appliance with own line → cross-section from its power (e.g. cooktop 7 kW → 6 mm²);
- three-phase load → 5-core cable.
Each point on the plan shows its line; points can be moved to another line by hand.

### 5.2 Cable routes
- Routes are generated automatically and drawn on the plan (layer **Cables**), one
  colour per line:
  - trunk from the board along walls at a routing level — **under the ceiling**
    (default, 15 cm below) or **in the floor**, chosen per project;
  - vertical drops/rises to each socket, switch and wall light; ceiling runs to lamps;
  - sockets of one line are daisy-chained in the shortest order; lights go
    board → switch → lamps (or via a junction box, if "junction boxes" style is chosen);
  - wall crossings through door heads or drilled holes, counted.
- Only horizontal and vertical runs in walls (no diagonals) — the usual rule, so nobody
  drills into a cable later.
- A route can be corrected by hand: drag a segment to another wall, or pin a waypoint.
- Length per run = route + slack at every box (default 15 cm) + tail in the board
  (default 1 m) ; then a reserve (default 10 %) per cable type.

### 5.3 Cable list
| Use | Cable (default ВВГнг(А)-LS; NYM selectable) |
|---|---|
| lights, no earth wire (living rooms, per elektronik `lightEarth`) | 2×1.5 |
| lights with earth (bathroom, garage, yard…) | 3×1.5 |
| switch drop: one-gang / two-gang / three-gang | 2×1.5 / 3×1.5 / 4×1.5 |
| between two-way switches | 3×1.5 |
| socket lines | 3×2.5 |
| big appliances | 3×4, 3×6 … from power |
| three-phase loads | 5×2.5, 5×4 … |
| board feed | from elektronik's project (supply current) |

Output: metres per type, per line and total, rounded up to what is sold (by the metre,
or 50 / 100 m coils — setting), with the remainder shown.

### 5.4 Accessories
Counted from the plan:
- **Back boxes** (подрозетники): one per mechanism; type by wall (solid / drywall —
  wall property); deep ones where a line is looped through.
- **Mechanisms:** sockets (earthed, IP44, with shutters), switches by type (one-way,
  two-way, intermediate, dimmer; 1/2/3-gang).
- **Frames** by size: 1, 2, 3, 4, 5-post — from how points are grouped side by side.
- **Junction boxes** and **connectors** (Wago 2/3/5-way) — counted from the joints in
  the routes.
- **Conduit** (гофра 16 / 20 mm) — metres, for runs in ceiling/floor/drywall; **chase
  length** (штроба) in solid walls, in metres, for estimating work.
- **Fixing:** cable clips / dowel-clamps, one per 30–40 cm of run.
- **Lamps:** fittings by type, LED strip metres + power supply sized from W/m.
- **Wall holes** to drill (count).
- **Board gear** — breakers, RCDs/RCBOs, box, comb busbars, N/PE bars, labels: taken
  from elektronik's own inventory for the linked project and shown as a separate
  section (or "open in elektronik" if not linked yet).

### 5.5 The list screen
- Grouped: Cable / Boxes & mechanisms / Connections / Conduit & fixing / Lighting /
  Board. Switch between **total** and **per room**.
- Every number can be tapped to see where it comes from (highlighted on the plan).
- Manual additions and overrides (e.g. "+1 spare socket"); overrides are kept when the
  plan changes and marked as manual.
- Optional price per item → total cost (AMD); prices remembered across projects.
- "Bought" checkboxes; share as PDF or plain text (for a shop or electrician).

## 6. Other features
- **Projects:** list with thumbnail, create / rename / duplicate / delete.
- **Summary:** per-room area, total area, wall lengths, wall surface area (for paint),
  list of openings; per-room count of sockets, switches, lamps (shopping list: frames,
  mechanisms, back boxes).
- **Export:** PNG / PDF of the 2D plan to scale; screenshot of the 3D view; project
  file `.roomplan` (JSON) for backup and transfer.
- **Furniture (later):** catalog of simple box-shaped items placed in 2D and shown as
  3D blocks.

## 7. Screens
1. **Projects** — grid of cards, FAB "New plan".
2. **Editor 2D** — full-screen canvas; top bar (undo, redo, layers, ⋮); bottom tool
   strip **Select / Wall / Room / Door / Window / Electrics**; bottom sheet for the selection;
   prominent **3D** button.
3. **View 3D** — full-screen; toggle **Outside / Inside**; roof, cutaway, sun controls;
   **2D** button.
4. **Materials** (what to buy), **Summary**, **Settings** (language, grid step, default wall height/thickness,
   eye height, theme).

## 8. Architecture

- `core/` — pure Kotlin, JVM unit-tested:
  - model + JSON serialization (kotlinx.serialization, `schemaVersion`);
  - wall graph operations (insert with merge/split at intersections, move, delete);
  - room detection (planar face traversal), area/perimeter;
  - wall outline builder (thickness, mitres, T-junctions);
  - mesh builder: walls with holes, floors/ceilings (polygon triangulation by
    ear-clipping), frames; outputs plain vertex/index arrays;
  - snapping engine; edit commands for undo/redo; walk collision.
- `app/` — Compose UI, `EditorViewModel` with immutable state, 2D `Canvas` composable,
  3D `GLSurfaceView` wrapped in `AndroidView`.
- **3D engine choice:** own OpenGL ES 3.0 renderer (two shaders: lit + shadow depth).
  The scene is a few thousand triangles of flat-coloured geometry, so an engine such
  as Filament/SceneView adds ~10–20 MB and API churn for little gain. Mesh generation
  lives in `core`, so the renderer can be swapped later if textures/PBR are wanted.

### Data model (lengths Int mm internally to avoid drift, shown as cm)
```kotlin
data class Plan(id, name, updatedAt, defaults: Defaults,
                nodes: List<Node>, walls: List<Wall>, rooms: List<RoomInfo>, items: List<Item>)
data class Node(id, x, y)
data class Wall(id, a: NodeId, b: NodeId, thickness, height: Int?, openings: List<Opening>)
data class Opening(id, type: DOOR|WINDOW|PASSAGE, offset, width, height, sill,
                   hinge: LEFT|RIGHT?, swing: A_SIDE|B_SIDE?)
data class RoomInfo(key /* derived from its wall loop */, name, type: RoomType, floorColor, wallColor)
data class WallPoint(id, kind: SOCKET|APPLIANCE|SWITCH|WALL_LAMP|JUNCTION|BOARD,
                     wall: WallId, side: A|B, offset, height, props…)
data class CeilingPoint(id, kind: LAMP|SPOT|STRIP, x, y, watts, strip: List<Point>?)
data class LightGroup(id, lamps: List<PointId>, switches: List<SwitchGangRef>)
```
`RoomType` uses the same names as elektronik's enum so the export needs no mapping table.
Room polygons are derived, never stored; `RoomInfo` only keeps user-set attributes and
is re-matched to detected loops after each edit.

## 9. Key risks
| Risk | Mitigation |
|---|---|
| Wall graph editing (merge, split, T-junctions) is the hardest logic | Pure-Kotlin `core` with heavy unit tests before any UI; restrict 0.1 to snapped angles |
| Room attributes lost when the loop changes | Match rooms by overlap of old/new polygon, not by id |
| Wall joints look broken in 3D | Build 2D wall outlines first (visible in the 2D editor too), extrude those |
| One-finger gesture conflicts in 2D (draw vs pan) | Explicit tool modes; two fingers always pan/zoom |
| Walk controls awkward on a phone | Tap-to-walk as the primary way, thumb pad secondary |
| Plan and elektronik project drift apart after re-export | Stable ids per group/load; import is an update-by-id, never a blind replace; elektronik shows what changed before applying |
| Electrical rules duplicated in two apps | Roomik depends on elektronik `core` (composite build); no sizing code of its own |
| Auto cable routes differ from how the electrician really runs them | Routes visible and editable; routing level and style are settings; reserve %; list says "estimate" |
| Composite build ties the two projects' Kotlin/serialization versions together | Keep both on the same toolchain; `core` is plain Kotlin with one dependency |
| GL context loss / rotation | Meshes rebuilt from the plan on surface creation; no GL-only state |

## 10. Roadmap
| Version | Content |
|---|---|
| **0.1** | Projects; draw walls + add rectangular rooms; shared walls; auto room detection with areas; edit corners/walls with exact lengths; grid/angle/wall snapping; undo/redo; JSON autosave; ru/en/hy. **3D outside orbit view** with extruded walls, floors, dollhouse mode |
| **0.2** | Doors, windows, passages in 2D and cut into 3D walls; **inside walk-through** with collision and mini-map; roof on/off; tap room to fly in |
| **0.3** | **Electrics:** sockets, switches, lamps, appliance outlets, board on the plan; switch→lamp groups; shown in 3D; export `.elplan` + **elektronik import** (new elektronik release); **materials list v1** — boxes, mechanisms, frames, lamps counted |
| **0.4** | **Cables:** lines via elektronik `core`, automatic routes on the plan, cable list by type and length, conduit, connectors, fixing; prices and total; share list as PDF/text |
| **0.5** | Lighting pass (shadows, AO, sun slider); room/wall colours; cutaway walls; background image tracing; lamps as light sources, switches work in 3D |
| **0.6** | Export PNG/PDF/3D screenshot, `.roomplan` import/export; per-wall heights; floor patterns |
| **0.7** | elektronik back-link: board gear in the materials list, line numbers synced with the hand-edited board |
| **0.8** | Furniture: box catalog in 2D, blocks in 3D |
| later | Multiple floors and stairs, pitched roofs, AR room measuring, link to `kafelito` (tiling) |

## 11. Acceptance for 0.1
- Draw a 3-room flat (two rooms sharing a wall, plus a corridor) in under two minutes;
  each room shows its name and correct area.
- Drag the shared wall → both rooms' areas update; type an exact wall length.
- Tap **3D** → the flat appears with correct wall joints, orbit/zoom smooth at 60 fps
  on a mid-range phone; tap **2D** → back at the same place.
- Kill the app mid-edit → reopen → nothing lost.
- `core` tests pass: graph merge/split, room detection, area, wall outlines,
  triangulation.
