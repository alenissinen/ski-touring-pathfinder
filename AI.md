# Kysymys

Can you analyze the current stage and features of my project, and based on that give me 10 new features I should implement?

Malli: Claude Code (Opus 4.7 High)

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
