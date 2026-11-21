<h1 align="center">Fabric Client Privacy</h1>
<p align="center">Limit the optional client and mod data you share when you join a Minecraft server.</p>
<p align="center">
  <a href="https://github.com/ItsKorayYT/fabric-client-privacy/releases"><img alt="Minecraft 26.3 client releases" src="https://img.shields.io/badge/Download-client_JAR-276749?style=for-the-badge"></a>
</p>

## Install

1. Set up **Minecraft Java 26.3** with **Fabric Loader 0.20.1+** and **Java 25**.
2. Install **Fabric Client Privacy 1.3.3** in your Minecraft profile’s `mods/` folder. Use the build instructions below if the release is still pending.
3. If you use Fabric API, versions **0.160.5 through 0.166.2** for 26.3 support filtered Fabric channel negotiation. The rest of the protections work with Fabric Loader alone.

Launch the game and join as usual. The server needs no setup.

## What the server sees

| Information | With default settings |
| --- | --- |
| Client brand | The client reports `vanilla`. |
| Network channel advertisements | Fabric advertises protocol channels and any channels you allow. Advertisements from unrecognized Fabric Networking builds are dropped. |
| Custom login queries | The client sends an empty response to queries on channels you have not allowed. Mod-provided login answers can carry data only for an approved query. |
| Configuration and gameplay custom payloads | The client sends built-in brand and supported Fabric negotiation traffic, plus payloads on channels you allow. The allowlist also applies to custom payloads sent directly by ViaFabricPlus 5.1.3. |
| Known packs | The client confirms only matching built-in 26.3 packs. Server resource-pack downloads work separately. |
| Sign text | Multiplayer sign editors use Minecraft's built-in translations. Mod and resource-pack keys use server-supplied fallbacks or raw keys. Keybind components use their raw IDs. |

A Minecraft server still receives your connection IP address, account identity, client settings and gameplay traffic. Servers can log what they receive, and permitted mod traffic and gameplay behavior can identify a modified client. This mod filters Minecraft protocol traffic. Launcher, Mojang and third-party web requests are outside its scope.

## Configure

Open **Mods → Fabric Client Privacy → Configure** with Mod Menu installed. With Fabric Loader alone, open **Options → Fabric Client Privacy** from the title or pause menu. Edit the toggles or the **Allowed Channels** list, then select **Save**. The settings file updates immediately. Rejoin the server to apply changes to login, brand, pack selection and channel announcements.

Leave the list empty to block optional mod payloads by default. To enable a trusted channel, enter its exact ID on its own line, for example `example:map_sync`. This allows channel advertisements, login responses and custom payloads for that ID. A server that requires a mod handshake can refuse the connection until its channels are allowed.

For a connection test, turn on **Debug logging** and reconnect. The `[Privacy trace]` lines in `logs/latest.log` record the active settings and the first occurrence of each observed event per connection. This trace excludes server addresses, channel IDs and packet contents.

You can also edit `config/fabric-client-privacy.json` while Minecraft is closed. [View the settings and defaults](examples/fabric-client-privacy.json)

<details>
<summary><strong>Build, tests and credits</strong></summary>

With JDK 25, run `./gradlew clean build` (Windows: `gradlew.bat clean build`). The installable JAR appears in `build/libs/`.

The compatibility suite checks the loaded Fabric API version, registration codecs, configuration and the seven Minecraft Mixin targets. It also checks the bytes emitted by an instrumented connection in the login, configuration and play phases. Run it for another supported build with `./gradlew test -PfabricApiVersion=0.165.0+26.3`.

Original code uses the [MIT license](LICENSE). Projects consulted include [ClientSpoofer](https://github.com/FabiPunktExe/ClientSpoofer), [modcloak](https://github.com/Celec7/modcloak) and [OpSec](https://github.com/aurickk/OpSec). The [Fabric example project](https://github.com/FabricMC/fabric-example-mod/tree/26.3) supplied the Apache-2.0 Gradle wrapper.

</details>
