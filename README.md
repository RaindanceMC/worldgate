# RaindanceMC/worldgate

- **`worldgate-module`** — Rain's Flexible Modules — World Gate: Lock, drain or close the Nether and the End to entry, and evacuate whoever is still inside. The portal-cancelling pattern is RainsCore's own — the same shape FarmWorldPortalListener uses — but the two locked dimensions, their three states and the evacuation itself belong wholly to this module; nothing here lives in Core.
- **`worldgate-standalone`** — Rain's World Gate: Rain's World Gate, as a plugin of its own. Nothing here but a build file and a descriptor: the worldgate module and the standard wrapper are shaded together, and that is the whole plugin.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework). Split out of `rains-flexible-modules` on 16.10.2026, history kept.

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `worldgate-standalone/target/RainsWorldGate-1.4.0.jar` — also published as `de.raindancer.modules:worldgate-standalone:1.4.0`

## Versions

A module's version is a property in this repository's root `pom.xml`, a plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.
