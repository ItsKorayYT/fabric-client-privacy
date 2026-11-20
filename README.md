<h1 align="center">Fabric Client Privacy</h1>
<p align="center">Share less about your installed mods when you join a Minecraft server.</p>
<p align="center">
  <a href="https://github.com/ItsKorayYT/fabric-client-privacy/releases/download/v1.1.0/fabric-client-privacy-1.1.0%2B26.3.jar"><img alt="Download the v1.1.0 client JAR" src="https://img.shields.io/badge/Download_client_JAR-v1.1.0-276749?style=for-the-badge"></a>
</p>

## Install

1. Set up **Minecraft Java 26.3** with **Fabric Loader 0.20.1+** and **Java 25**.
2. Drop the [client JAR](https://github.com/ItsKorayYT/fabric-client-privacy/releases/download/v1.1.0/fabric-client-privacy-1.1.0%2B26.3.jar) into your Minecraft profile’s `mods/` folder.
3. For channel filtering, also install a **Fabric API build for Minecraft 26.3** from **0.160.5 through 0.166.2**, including 0.165.0. The other protections work with Fabric Loader alone.

Launch the game and join as usual. The server needs no setup.

## What the server sees

| Information | With default settings |
| --- | --- |
| Client brand | The client reports `vanilla`. |
| Network channels | Fabric advertises Minecraft, negotiation and transport channels, plus any channels you allow. |
| Known packs | The client confirms only matching built-in 26.3 packs. Server resource-pack downloads work separately. |
| Sign text | For supported US-English sign probes, a mod translation key uses its fallback text or the raw key. |

Required mod handshakes and gameplay behavior can still identify a modified client.

## Configure

After the first launch, close Minecraft and edit `config/fabric-client-privacy.json`. Each protection has its own switch. If a server needs a channel, add its exact ID to `allowedChannels`, for example `["example:map_sync"]`, then restart. [View all settings and defaults](examples/fabric-client-privacy.json)

<details>
<summary><strong>Build, tests and credits</strong></summary>

With JDK 25, run `./gradlew clean build` (Windows: `gradlew.bat clean build`). The installable JAR appears in `build/libs/`.

The compatibility suite checks the loaded Fabric API version, registration codecs, configuration and all six Mixin targets. Run it for another supported build with `./gradlew test -PfabricApiVersion=0.165.0+26.3`. A 26.3 client completed loopback configuration exchanges with and without Fabric API. Full gameplay, required mod handshakes and in-world sign editing still need manual testing.

Original code uses the [MIT license](LICENSE). Projects consulted include [ClientSpoofer](https://github.com/FabiPunktExe/ClientSpoofer), [modcloak](https://github.com/Celec7/modcloak) and [OpSec](https://github.com/aurickk/OpSec). The [Fabric example project](https://github.com/FabricMC/fabric-example-mod/tree/26.3) supplied the Apache-2.0 Gradle wrapper.

</details>
