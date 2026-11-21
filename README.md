<h1 align="center">Fabric Client Privacy</h1>
<p align="center">Limit the optional client and mod data you share when you join a Minecraft server.</p>
<p align="center">
  <a href="https://github.com/ItsKorayYT/fabric-client-privacy/releases"><img alt="Minecraft 26.3 client releases" src="https://img.shields.io/badge/Download-client_JAR-276749?style=for-the-badge"></a>
</p>

## Install

1. Set up **Minecraft Java 26.3** with **Fabric Loader 0.20.1+** and **Java 25**.
2. Install **Fabric Client Privacy 1.2.0** in your Minecraft profile’s `mods/` folder. Use the build instructions below if the release is still pending.
3. If you use Fabric API, versions **0.160.5 through 0.166.2** for 26.3 support filtered Fabric channel negotiation. The rest of the protections work with Fabric Loader alone.

Launch the game and join as usual. The server needs no setup.

## What the server sees

| Information | With default settings |
| --- | --- |
| Client brand | The client reports `vanilla`. |
| Network channel advertisements | Fabric advertises protocol channels and any channels you allow. Advertisements from unrecognized Fabric Networking builds are dropped. |
| Custom login queries | The client sends an empty response to queries on channels you have not allowed. Mod-provided login answers can carry data only for an approved query. |
| Configuration and gameplay custom payloads | The client sends built-in brand and supported Fabric negotiation traffic, plus payloads on channels you allow. |
| Known packs | The client confirms only matching built-in 26.3 packs. Server resource-pack downloads work separately. |
| Sign text | For supported US-English sign probes, a mod translation key uses its fallback text or the raw key. |

A Minecraft server still receives your connection IP address, account identity, client settings and gameplay traffic. Servers can log what they receive, and permitted mod traffic and gameplay behavior can identify a modified client. This mod filters Minecraft protocol traffic. Launcher, Mojang and third-party web requests are outside its scope.

## Configure

After the first launch, close Minecraft and edit `config/fabric-client-privacy.json` in the profile. `blockUnapprovedCustomPayloads` defaults to `true`, including when the setting is missing from an older config. A server that requires a mod handshake can refuse the connection. To enable a trusted channel, add its exact ID to `allowedChannels`, for example `["example:map_sync"]`, then restart. This allows channel advertisements, login responses and custom payloads for that ID. Enable `debugLogging` to record the first blocked payload and declined login query without logging their contents. [View all settings and defaults](examples/fabric-client-privacy.json)

<details>
<summary><strong>Build, tests and credits</strong></summary>

With JDK 25, run `./gradlew clean build` (Windows: `gradlew.bat clean build`). The installable JAR appears in `build/libs/`.

The compatibility suite checks the loaded Fabric API version, registration codecs, configuration and the five Mixin targets. It also checks the bytes emitted by an instrumented connection in the login, configuration and play phases. Run it for another supported build with `./gradlew test -PfabricApiVersion=0.165.0+26.3`.

Original code uses the [MIT license](LICENSE). Projects consulted include [ClientSpoofer](https://github.com/FabiPunktExe/ClientSpoofer), [modcloak](https://github.com/Celec7/modcloak) and [OpSec](https://github.com/aurickk/OpSec). The [Fabric example project](https://github.com/FabricMC/fabric-example-mod/tree/26.3) supplied the Apache-2.0 Gradle wrapper.

</details>
