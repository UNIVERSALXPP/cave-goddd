# Mocap Addon - Knockback & Inventory Drop (1.0.0)

Fabric addon for Mocap (Motion Capture) on Minecraft 1.21.11. Drop both jars into `mods/`.
Mocap is a required dependency and is never bundled or modified.

Build: `gradle wrapper` (once, Gradle 8.14+/9), then `./gradlew build` -> `build/libs/mocap_addon-1.0.0.jar`.

Commands (op level 2): `/mocap playback recording setting <inventorydrop|knockback> [true|false]`
Config: `config/mocap_addon.json` (both default true).
