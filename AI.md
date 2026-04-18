# Tiedoston sisältö

Tiedostosta löytyy kaikki Cursor cli-agentilta kysymäni kysymykset ja niihin saamani vastaukset sekä käytetyn mallin, sen ajattelun tason sekä kontekstin suurimman koon tokeneissa.

Tiedostoa ylläpitää subagentti, joka kirjoittaa tähän automaattisesti kaikki tiedot. Subagentti myös muuttaa vastauksen markdown yhteensopivaksi.

## Kysymykset

1. [10 features](#kysymys-1)
2. [Unit tests](#kysymys-2)

---

# Kysymys 1 <a id="kysymys-1"></a>

Can you analyze the current stage and features of my project, and based on that give me 10 new features I should implement?

Malli: Claude Code (Opus 4.7 High 1M)

# Vastaus

Here are features that naturally extend what you already have, ordered roughly by impact and feasibility:

1. **Elevation Profile View**

Display an elevation cross-section chart of the found path in the ImGui panel -- X axis = distance along path, Y
axis = elevation. This is very standard in ski touring apps and would be straightforward since you already have all
the node elevation data.

2. **Slope Heatmap Overlay (Toggle)**

You already compute slope angles for A\*. Add a togglable rendering mode in the fragment shader that colors terrain
by slope steepness (e.g., green < 25 degrees, yellow 25-35, red 35-45, black > 45). Ski tourers use this to assess
avalanche risk zones.

3. **Path Statistics Expansion**

Right now you show path length and elevation gain. You could add: total elevation loss, max slope along path,
estimated ascent time (using typical skinning speed ~300-400m/h vertical), and distance in km. All computable from
existing path data.

4. **Waypoint Support (Multi-Point Routes)**

Allow the user to click more than 2 points to create a multi-leg route (e.g., start -> summit -> descent endpoint),
running A\* between consecutive waypoints and stitching the paths together.

5. **Descent Mode / Downhill Pathfinding**

Your current A\* cost model is geared toward uphill skinning. Add a separate mode for ski descent that prefers
moderate slopes (20-35 degrees), avoids flat terrain and very steep terrain, and penalizes traversing rather than
descending.

6. **Aspect/Wind Direction Awareness**

Since you already have the slope gradient (dzdx, dzdy), you can compute aspect (compass direction a slope faces).
This could be used to penalize north-facing slopes (more ice) or to let users mark a wind direction and penalize
wind-loaded slopes (avalanche risk).

7. **Minimap**

Render a small 2D top-down minimap in the ImGui overlay showing the camera position, the full height map extent, and
the current path. This helps with orientation on large maps.

8. **LOD (Level of Detail) for Distant Chunks**

Your ChunkManager already has a TODO note about this. Generate lower-resolution meshes (e.g., every 2nd or 4th
vertex) for chunks far from the camera. This would significantly improve performance at high render distances.

9. **Normal-Based Shading (Hillshade)**

Add per-vertex or per-fragment normals computed from the height map and apply basic diffuse lighting (sun
direction). This would make the terrain much more readable compared to the current flat elevation gradient coloring.
Your README mentions you already did hillshading in the 2D POC.

10. **Export Path to GPX**

Allow saving the computed path as a GPX file that can be loaded into a GPS device or mapping app. GPX is a simple
XML format -- you have the grid coordinates and can convert them back to ETRS-TM35FIN real-world coordinates since
you store xLL, yLL, and cellSize.

# Kysymys 2 <a id="kysymys-2"></a>

Analyze the changes I've made to the codebase after the last question and give me classes and methods to add tests for. Don't write the tests, just give me a detailed plan.

Malli: Claude Code (Opus 4.7 High 1M)

# Vastaus

## Tier 1 — High Priority, Easy to Test (no GPU, no mocking)

### 1. pathfinding.AStar

This is the highest-value test target — a full A\* pathfinding algorithm with zero OpenGL dependencies.

**Methods to test:**

- `init(float, float, float, float)` — Verify start/goal are set, open set is populated, state is running
- `step()` — Verify single iteration advances the algorithm; verify goal detection returns false
- `getPath()` — Verify correct path reconstruction via parent chain
- `isRunning()` / `getUnModifiableOpenSet()` — State queries

**Test scenarios:**

- Start == goal (immediate termination)
- Simple known path on a flat grid (verify optimal route)
- Path on sloped terrain triggering slope penalty (verify penalty applied when angle > SLOPE_THRESHOLD)
- No valid path (completely blocked) — verify it terminates after max iterations
- Diagonal vs cardinal movement cost differences
- Negative grid coordinates (tests the packKey bit-packing)
- Single-cell heightmap
- Verify heuristic never overestimates (admissibility check)

---

### 2. pathfinding.Node

Pure data class, quick to test.

**Methods to test:**

- `setCosts(float g, float h)` — Verify f = g + h
- `compareTo(Node other)` — Lower f first; equal f values; negative/zero/positive
- `setElevation(float)` / `getY()` — Round-trip
- `setParent(Node)` / `getParent()` — Round-trip
- `setSlopeAngle(float)` / `getSlopeAngle()` — Round-trip, default is -1.0f
- `toString()` — Format verification

**Edge cases:**

- NaN / Infinity cost values in compareTo
- Negative coordinates in constructor

---

### 3. terrain.HeightMap (expand existing tests)

Already has 6 tests. Significant logic remains uncovered.

**Methods to add tests for:**

- `interpolateElevation(float, float)` — Bilinear interpolation between four grid corners; at exact grid point; at
  midpoint between cells; at heightmap boundaries
- `isLogicalOnGrid(int, int)` — Inside, at edges, just outside, negative coords
- `getElevation(int, int)` — Clamping behavior at out-of-bounds indices
- `getDataMinElevation()` / `getDataMaxElevation()` — Lazy computation, correctness on known data
- `getSlopeAngle(int, int)` — Additional cases: steep slope, varied 3x3 kernels, verify Horn's method formula
- `merge(List<HeightMap>)` — Two tiles merging correctly; mismatched cellSize throws exception; gap detection;
  overlapping tiles; single-map merge; empty/null list

**Edge cases:**

- Single-cell heightmap (1x1)
- Flat terrain (all same elevation)
- merge with tiles at different world offsets

---

### 4. terrain.HeightMapParser

File-based parsing with threading. Use temp files in tests.

**Methods to test:**

- `parse()` — Valid ASC file with known header + data; verify dimensions, cellSize, elevation values match
- `parse()` — Missing header fields throw HeightMapParseException
- `parse()` — Non-numeric data in grid rows throws exception
- `parse()` — Fewer rows than nrows header claims
- `parse()` — File not found throws FileNotFoundException

**Edge cases:**

- Extra whitespace in data lines
- NODATA values in the grid
- Very small file (1x1)
- Header with unexpected key names

---

### 5. application.Config + Config.Builder

**Methods to test:**

- Builder defaults — Verify all default values (width=800, height=600, targetFps=60, etc.)
- Builder fluent setters — Each setter correctly stores its value
- `build()` — Produces Config with matching values
- `toString()` — Format string correctness
- `toString()` with null heightMap — Currently causes NPE (bug to document or fix)

---

### 6. exceptions.HeightMapParseException / exceptions.ShaderException

Quick sanity tests.

**Methods to test:**

- Constructor with message + filePath — verify `getMessage()` contains both
- Constructor with message + cause + filePath — verify cause chain
- ShaderException with log — verify log is included in message

---

## Tier 2 — Medium Priority, Testable After Minor Refactoring

These classes contain valuable logic that is currently private or coupled to OpenGL. Tests here require making
methods package-private or extracting pure-logic helpers.

### 7. rendering.Camera

Most methods are pure JOML math — only `setMouseMode()` calls GLFW.

**Methods to test (no refactoring needed):**

- `update(Set<Integer>, float)` — Movement along front/right vectors based on WASD keys; empty key set returns early;
  zero deltaTime
- `rotate(double, double)` — Yaw wraps 0–360; pitch clamps to ±89; front vector updates correctly
- `onResize(int, int)` — Aspect ratio and projection matrix update
- `getViewMatrix()` / `getProjectionMatrix()` — Matrix correctness for known inputs

**Edge cases:**

- Pitch at exactly ±89 degrees
- Yaw going negative (should wrap to 360)
- Very large rotation deltas
- Zero-width or zero-height resize

---

### 8. terrain.Chunk (requires making mesh methods package-private)

**Methods to extract/expose:**

- `createVertexData()` — Verify vertex positions map correctly from heightmap grid to world coordinates
- `createIndexData()` — Verify correct triangle winding order, index count = CHUNK_SIZE² × 6
- `calculateNormals(float[], int[])` — Verify normal accumulation and normalization on known geometry

**Edge cases:**

- Chunk at negative grid coordinates
- Chunk at heightmap boundary (partial data)
- Flat terrain (normals all point up)

---

### 9. terrain.ChunkManager (requires making utility methods package-private)

**Methods to extract/expose:**

- `toChunkX(float)` / `toChunkZ(float)` — World-to-chunk coordinate conversion
- `chunkOverlapsHeightMapData(int, int)` — AABB overlap with heightmap bounds
- `isInsideLoadRadius` / `isPastUnloadRadius` — Circular boundary checks
- `packKey(int, int)` — Bit-packing with negative coordinates

**Edge cases:**

- Negative world positions
- Render distance of 0 or 1
- Camera exactly on chunk boundary

---

### 10. ui.ImGuiLayer (extract pure logic)

**Methods to extract and test:**

- `elevationGain()` — Sum of positive elevation deltas in path; null path; empty path; single node; descending-only
  path returns 0
- FPS ring buffer in `setFPS` — Verify buffer wraps correctly
- Munter method calculation (distance / Munter constant) — extract from `drawUI`

---

### 11. rendering.Renderer (extract pure logic)

**Methods to extract and test:**

- `buildMVP(Matrix4f)` — projection × view × model multiplication
- Sun direction — spherical (azimuth, elevation) to Cartesian vector
- Heatmap slope-to-color bucketing in `buildHeatmapTexture()` — slope angle → color byte

---

## Tier 3 — Low Priority / Not Unit-Testable

These are either trivial or fully GPU-dependent:

| Class                              | Reason                               |
| ---------------------------------- | ------------------------------------ |
| Main                               | Entry point, calls `System.exit()`   |
| application.Application            | Full GLFW+GL lifecycle orchestration |
| application.Window                 | All GLFW calls                       |
| rendering.Shader                   | All OpenGL calls                     |
| rendering.Texture                  | Constructor requires GL context      |
| ui.Launcher                        | Full GLFW+ImGui+TinyFD UI            |
| application.MouseMode / RenderMode | Trivial enums                        |
| application.Constants              | Only static final fields             |

---

## Summary — Recommended Test File Structure

| Test class                             | Target            | Tests (approx) |
| -------------------------------------- | ----------------- | -------------- |
| pathfinding.AStarTest                  | AStar             | 10–12          |
| pathfinding.NodeTest                   | Node              | 6–8            |
| terrain.HeightMapTest (expand)         | HeightMap         | +8–10 new      |
| terrain.HeightMapParserTest            | HeightMapParser   | 5–7            |
| application.ConfigTest                 | Config + Builder  | 5–6            |
| rendering.CameraTest                   | Camera            | 8–10           |
| exceptions.HeightMapParseExceptionTest | Exception classes | 3–4            |
| exceptions.ShaderExceptionTest         | Exception classes | 2–3            |

Total: ~50–60 new tests, with AStar and HeightMap being the highest-value targets.

> **Note:** Your pom.xml has JUnit 5 but no mocking library. If you proceed with Tier 2 tests (Camera, ChunkManager, etc.),
> consider adding Mockito to isolate GPU-dependent collaborators.
