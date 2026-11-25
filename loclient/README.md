# Lo Client 1.5.0 (Fabric, Minecraft 1.21.11)

Cosmetic only: reads your own game state and draws on your own screen. Server-safe.

## Features
- HUD: FPS, XYZ, facing, ping, clock, biome, light, CPS, sprint/sneak, potion timers,
  armor + held-item durability, compass strip, WASD/space keystrokes
- HUD layout editor (press U, or Settings > HUD > Edit layout): drag anything anywhere
- Theme colors with a color wheel, brightness slider, hex color codes and presets:
  theme, HUD text, panel, crosshair, crosshair on target, sky, clouds, block outline, item glint
- Animated main menu, dark theme for Minecraft's own buttons/lists/scrollbars
- Settings screen with tabs, switches, sliders, color swatches and a smooth scrollbar
- Crosshair styles, zoom, freelook, third-person distance
- Sky tint, cloud tint, star field boost, block outline color, particle thinning
- Item glint color (generates the "Lo Client Glint" resource pack, see below)
- Fonts: Outfit, Jura, Tektur, Pixelify Sans, Instrument Sans (SIL OFL) or any .ttf/.otf you put in
  .minecraft/loclient/fonts/ - applied to all text and to the "Lo Client" logo (Settings > Style)
- Fullbright (key G, Settings > World): shows your own player as having Night Vision, client-side only
- Round pill main-menu buttons that grow and glow on hover
- Waypoints (B, /wp add|remove|list), 3 profile slots, Mod Menu integration

## Keys (rebind in Controls > Lo Client)
Right Shift settings | U edit HUD layout | H HUD | K keystrokes | J next corner + reset |
C zoom (hold) | Left Alt freelook (hold) | B add waypoint | G fullbright

## Item glint color
Pick the color in Settings > World. The mod writes the "Lo Client Glint" resource pack and
enables it for you (see Resource packs below).

## Resource packs
Lo Client generates "Lo Client Pack" (dark button/list textures + your font) and the glint pack in
your resourcepacks folder and keeps them enabled above your other packs. Packs with "font" in their
name stay above Lo Client's, so your own font packs win for fonts. Turn this off with
Settings > Style > "Lo Client packs on top".

## Notes
- The button/list theme is a fixed purple texture set inside the mod; the generated Lo Client Pack
  keeps it on top of other packs (unless you turn that off). The theme color setting recolors Lo Client's own screens and HUD.
- Star boost, block outline color and sky/cloud tint hook into Minecraft internals. If a future
  version changes them, that one feature quietly does nothing.

## Build
JDK 21. IntelliJ IDEA import, or `gradle build`. Jar: build/libs/Lo Client-1.5.0.jar
(not the -sources jar). Needs Fabric Loader + Fabric API for 1.21.11.
