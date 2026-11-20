# Fabric Client Privacy

Client-side privacy controls for Minecraft Java Edition **26.3**. The mod limits the brand, channel names, known packs and certain translations a multiplayer server can learn from a client. Required handshakes, mod traffic and gameplay behavior can still identify a modified client.

## Installation

Install Fabric Loader 0.20.1 or later for Minecraft 26.3 and use Java 25. Put the release JAR in the client's `mods/` directory. The server needs no installation. For Fabric channel filtering, use Fabric API **0.166.2+26.3**. The other protections work independently of Fabric API.

## Configuration

First launch creates `config/fabric-client-privacy.json`. Edit it while the client is closed and restart. The [example configuration](examples/fabric-client-privacy.json) contains the defaults.

| Setting | Default | Effect |
| --- | --- | --- |
| `enabled` | `true` | Master switch for all protections and diagnostics. |
| `spoofClientBrand` | `true` | Sends `vanilla` in the login-finished brand payload. Fabric and local brand lookups continue to work normally. |
| `filterModChannels` | `true` | Filters the channel lists in Fabric registration and unregistration payloads for remote connections. |
| `allowedChannels` | `[]` | Additional exact channel identifiers to advertise, for example `["example:map_sync"]`. |
| `filterKnownPacks` | `true` | Limits known-pack negotiation to built-in 26.3 pack tuples. |
| `protectTranslationProbes` | `true` | Applies the English vanilla missing-key behavior to supported sign-editor inputs. |
| `debugLogging` | `false` | Records the first incoming registration request per process. No addresses, identifiers or payload contents are logged. |

Missing fields use defaults. Unknown fields are ignored. Invalid boolean values use the individual default and invalid allowlist entries are skipped. A malformed or oversized file is retained for repair and the current session uses defaults.

## Protocol behavior and compatibility

**Channels.** Fabric processes incoming registration, version negotiation and play/configuration callbacks normally. The mod recognizes Fabric Networking API v1's typed `minecraft:register`, `minecraft:unregister` and `c:register` advertisements. It keeps the packet type, common protocol/version fields, Minecraft namespace entries, `c:register`, `c:version` and `fabric:split`. An empty result is encoded by the original codec. A peer can still observe registration and transport support. Unknown payload implementations and mod data messages pass through, including mandatory responses.

Channel advertisements tell a server which features it can use. If a server needs a mod channel, add its exact ID to the allowlist or disable channel filtering before joining. The typed adapter activates only for the inspected Networking API module version `6.4.1+c58129785d`. Other versions pass traffic through. This mod registers no channels of its own.

**Known packs.** The client receives a server offer before selecting packs from its local trusted repository. The mod narrows that offer before selection, so the local registry resource manager and the reply agree. The permitted namespace/version pair is `minecraft` and `26.3`, with IDs `core`, `trade_rebalance`, `minecart_improvements` and `redstone_experiments`. Only locally available matches can be returned. Servers may need to send additional registry data. Downloadable resource packs use separate packets.

**Sign translations.** In 26.3 the sign editor flattens components to editable strings and can return those strings to the server. In US English, a standalone, argument-free translation such as `item.example.widget` can reveal a mod when `example` is a loaded mod ID. For a key absent from vanilla's built-in English table the mod uses the component's explicit fallback or the raw key. With a downloaded server pack or a user file pack selected, the editor uses normal rendering. Formatted fallbacks, composite lines, other locales and unrelated keys retain normal behavior. Anvil renames, chat rendering, arbitrary keybind probes and third-party protocols are outside this protection.

## Build and verification

Use JDK 25 and run `./gradlew clean build` (Windows users can run `gradlew.bat clean build`). The installable artifact is `build/libs/fabric-client-privacy-1.0.0+26.3.jar`. The sources JAR is for development. Run `./gradlew runClient` for a development client.

The build uses Gradle 9.8.2 and the unobfuscated `net.fabricmc.fabric-loom` plugin 1.19.4. Minecraft 26.3 ships with Mojang names. The test suite loads all five target classes through Fabric Loader's JUnit launcher, checks transformed methods, exercises configuration recovery, registration codecs, allowlists, pack selection and simple sign fallbacks. GitHub Actions runs the build on Java 25 and attaches the installable JAR to releases for pushed version tags.

A 26.3 development client started under Xvfb with Mesa Vulkan software rendering. Using the packaged JAR, a loopback configuration-phase test peer completed common version negotiation, decoded the play-channel list, observed the outgoing brand `vanilla`, decoded the Fabric channel advertisement and received only `minecraft:core:26.3` in response to a mixed known-pack offer. A second run with the official Mojang client JAR and Fabric Loader alone also returned the vanilla brand and the expected pack reply. The fixture closed after configuration. Manual coverage still needed includes full gameplay, mod-required server handshakes, resource-pack delivery and editing signs in-world.

## Sources and license

The implementation was written independently against the official 26.3 client JAR (SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87`) and Fabric's published networking module sources. Dependency and tooling references include the [Mojang version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json), [Fabric development page](https://fabricmc.net/develop/), [Fabric docs](https://docs.fabricmc.net/), [Fabric Maven](https://maven.fabricmc.net/) and [26.3 example project](https://github.com/FabricMC/fabric-example-mod/tree/26.3).

Prior work consulted for feature scope includes [ClientSpoofer](https://github.com/FabiPunktExe/ClientSpoofer), [modcloak](https://github.com/Celec7/modcloak) and [OpSec](https://github.com/aurickk/OpSec). Original project code is covered by the [MIT License](LICENSE). The Gradle wrapper comes from the Fabric example project and retains Gradle's Apache-2.0 licensing.
