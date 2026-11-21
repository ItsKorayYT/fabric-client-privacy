package io.github.itskorayyt.privacy;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PrivacyLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("fabric-client-privacy");
    private static final Map<Connection, EnumSet<Event>> OBSERVED = new WeakHashMap<>();
    private static final Set<Object> VIA_BLOCKS = Collections.newSetFromMap(new WeakHashMap<>());
    // Fixed 26.3 names keep packet IDs added by mods out of diagnostic logs.
    private static final Set<String> SERVERBOUND_TYPES = Set.of(
            "accept_code_of_conduct", "accept_teleportation", "attack", "block_entity_tag_query",
            "bundle_item_selected", "change_difficulty", "change_game_mode", "chat", "chat_ack",
            "chat_command", "chat_command_signed", "chat_session_update", "chunk_batch_received",
            "client_command", "client_information", "client_tick_end", "command_suggestion",
            "configuration_acknowledged", "container_button_click", "container_click", "container_close",
            "container_slot_state_changed", "cookie_response", "custom_click_action", "custom_payload",
            "custom_query_answer", "debug_subscription_request", "edit_book", "entity_tag_query",
            "finish_configuration", "hello", "intention", "interact", "jigsaw_generate", "keep_alive",
            "key", "lock_difficulty", "login_acknowledged", "move_player_pos", "move_player_pos_rot",
            "move_player_rot", "move_player_status_only", "move_vehicle", "paddle_boat",
            "pick_item_from_block", "pick_item_from_entity", "ping_request", "place_recipe",
            "player_abilities", "player_action", "player_command", "player_input", "player_loaded",
            "pong", "punch", "recipe_book_change_settings", "recipe_book_seen_recipe", "rename_item",
            "resource_pack", "seen_advancements", "select_known_packs", "select_trade", "set_beacon",
            "set_carried_item", "set_command_block", "set_command_minecart", "set_creative_mode_slot",
            "set_game_rule", "set_jigsaw_block", "set_structure_block", "set_test_block", "sign_update",
            "spectator_action", "status_request", "teleport_to_entity", "test_instance_block_action",
            "use_item", "use_item_on");

    public enum Event {
        LOGIN_FINISHED("Minecraft processed login completion."),
        REGISTRATION_RECEIVED("Minecraft handled an incoming channel registration."),
        LOGIN_QUERY_DECLINED("The privacy check declined a custom login query."),
        PAYLOAD_BLOCKED("The privacy check blocked an outgoing custom payload or login answer."),
        BRAND_REWRITTEN("The privacy check rewrote the outgoing client brand."),
        REGISTRATION_FILTERED("The privacy check filtered an outgoing channel advertisement."),
        SIGN_REQUEST_RECEIVED("Minecraft received a sign editor request."),
        SIGN_EDITOR_PREFILLED("Minecraft built sign editor prefill."),
        SIGN_PREFILL_SANITIZED("The privacy check sanitized structured sign prefill."),
        ANVIL_PREFILL_SANITIZED("The privacy check used built-in text for an anvil item name."),
        SIGN_UPDATE_QUEUED("A sign update reached Minecraft's outgoing packet hook.");

        private final String message;

        Event(String message) {
            this.message = message;
        }
    }

    private PrivacyLog() {}

    public static void traceEncoded(ConnectionProtocol phase, PacketType<?> type, Packet<?> packet, Set<String> observed) {
        String label = switch (packet) {
            case ServerboundCustomQueryAnswerPacket answer -> answer.payload() == null
                    ? "custom_query_answer (empty)" : "custom_query_answer (with data)";
            case ServerboundCustomPayloadPacket custom -> {
                var payload = custom.payload();
                if (payload instanceof BrandPayload) {
                    yield "custom_payload (brand)";
                }
                yield switch (payload.type().id().toString()) {
                    case "minecraft:register", "minecraft:unregister", "c:register", "c:version" ->
                            "custom_payload (channel negotiation)";
                    default -> "custom_payload (other)";
                };
            }
            default -> builtInTypeLabel(type);
        };
        if (observed.add(label)) {
            LOGGER.info("[Privacy trace] Minecraft encoded serverbound packet {} in {}.", label, phase.id());
        }
    }

    static String builtInTypeLabel(PacketType<?> type) {
        var id = type.id();
        return id.getNamespace().equals("minecraft") && SERVERBOUND_TYPES.contains(id.getPath())
                ? id.getPath() : "other";
    }

    public static void traceViaBlock(Object viaConnection) {
        if (!ClientPrivacy.settings().debugLogging() || viaConnection == null) {
            return;
        }
        synchronized (VIA_BLOCKS) {
            if (VIA_BLOCKS.add(viaConnection)) {
                LOGGER.info("[Privacy trace] The privacy check blocked a ViaFabricPlus custom payload.");
            }
        }
    }

    public static void trace(Connection connection, Event event) {
        var settings = ClientPrivacy.settings();
        if (!settings.debugLogging() || connection.getSending() != PacketFlow.SERVERBOUND || connection.isMemoryConnection()) {
            return;
        }
        synchronized (OBSERVED) {
            var events = OBSERVED.computeIfAbsent(connection, ignored -> {
                LOGGER.info("[Privacy trace] Connection settings enabled={} brand={} channels={} knownPacks={} signs={} payloads={} allowedChannelCount={} fabricAdapter={}",
                        settings.enabled(), settings.spoofClientBrand(), settings.filterModChannels(), settings.filterKnownPacks(),
                        settings.protectTranslationProbes(), settings.blockUnapprovedCustomPayloads(),
                        settings.allowedChannels().size(), ClientPrivacy.hasChannelAdapter());
                return EnumSet.noneOf(Event.class);
            });
            if (events.add(event)) {
                LOGGER.info("[Privacy trace] {}", event.message);
            }
        }
    }

    public static void unsupportedNetworking() {
        LOGGER.warn("Unrecognized Fabric Networking version. Channel advertisements will be blocked while channel filtering is enabled.");
    }

    public static void configSaveFailed(IOException cause) {
        LOGGER.warn("Could not save privacy configuration.", cause);
    }

    public static void configUnavailable() {
        LOGGER.warn("Could not load privacy configuration. Using defaults for this session.");
    }
}
