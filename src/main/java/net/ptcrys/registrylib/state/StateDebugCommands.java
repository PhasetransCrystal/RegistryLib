package net.ptcrys.registrylib.state;

import net.ptcrys.registrylib.RegistryCore;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.JsonOps;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

public final class StateDebugCommands {

    private static final DynamicCommandExceptionType INVALID_VALUE = new DynamicCommandExceptionType(value -> Component.literal(String.valueOf(value)));

    private static final Method HAS_PERMISSION = findHasPermission();

    private static Method findHasPermission() {
        try {
            return CommandSourceStack.class.getMethod("hasPermission", int.class);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private StateDebugCommands() {}

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("registrylib")
                        .then(
                                Commands.literal("state")
                                        .then(
                                                Commands.literal("list")
                                                        .executes(StateDebugCommands::listStates))
                                        .then(
                                                Commands.literal("get")
                                                        .then(
                                                                Commands.argument(
                                                                        "id", ResourceKeyArgument.key(Registries.CUSTOM_STAT))
                                                                        .suggests(StateDebugCommands::suggestReadableStateIds)
                                                                        .executes(StateDebugCommands::getWorldState)
                                                                        .then(
                                                                                Commands.argument("chunkX", IntegerArgumentType.integer())
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                        "chunkZ", IntegerArgumentType.integer())
                                                                                                        .executes(StateDebugCommands::getChunkState)))))
                                        .then(
                                                Commands.literal("set")
                                                        .then(
                                                                Commands.argument(
                                                                        "id", ResourceKeyArgument.key(Registries.CUSTOM_STAT))
                                                                        .suggests(StateDebugCommands::suggestWritableStateIds)
                                                                        .then(
                                                                                Commands.argument("tail", StringArgumentType.greedyString())
                                                                                        .executes(StateDebugCommands::setState))))
                                        .then(
                                                Commands.literal("debug")
                                                        .then(
                                                                Commands.argument(
                                                                        "id", ResourceKeyArgument.key(Registries.CUSTOM_STAT))
                                                                        .suggests(StateDebugCommands::suggestReadableStateIds)
                                                                        .executes(StateDebugCommands::debugWorldState)
                                                                        .then(
                                                                                Commands.argument("chunkX", IntegerArgumentType.integer())
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                        "chunkZ", IntegerArgumentType.integer())
                                                                                                        .executes(
                                                                                                                StateDebugCommands::debugChunkState)))))));
    }

    private static int listStates(CommandContext<CommandSourceStack> context) {
        StringBuilder builder = new StringBuilder("Registered RegistryLib states:");
        readableStates(context.getSource())
                .forEach(
                        entry -> builder
                                .append('\n')
                                .append(entry.identifier())
                                .append(" [")
                                .append(entry.scope().id())
                                .append(entry.debugConfig().enabled() ? ", debug" : "")
                                .append(entry.debugConfig().writable() ? ", writable" : "")
                                .append(']'));
        context.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
        return 1;
    }

    private static int getWorldState(CommandContext<CommandSourceStack> context)
                                                                                 throws CommandSyntaxException {
        StateEntry<?> entry = requireState(context, StateScope.WORLD, false);
        requireRead(context, entry);
        ServerLevel level = context.getSource().getLevel();
        WorldStateEntry<?> world = (WorldStateEntry<?>) entry;
        Object value = world.getIfPresent(level).orElse(null);
        context.getSource().sendSuccess(() -> Component.literal(formatValue(entry, value)), false);
        return 1;
    }

    private static int getChunkState(CommandContext<CommandSourceStack> context)
                                                                                 throws CommandSyntaxException {
        StateEntry<?> entry = requireState(context, StateScope.CHUNK, false);
        requireRead(context, entry);
        ChunkStateEntry<?> chunk = (ChunkStateEntry<?>) entry;
        ChunkPos pos = chunkPos(context);
        Object value = chunk.getIfLoaded(context.getSource().getLevel(), pos).orElse(null);
        context.getSource().sendSuccess(() -> Component.literal(formatValue(entry, value)), false);
        return 1;
    }

    private static int setState(CommandContext<CommandSourceStack> context)
                                                                            throws CommandSyntaxException {
        StateEntry<?> entry = requireState(context, null, true);
        requireWrite(context, entry);
        String tail = StringArgumentType.getString(context, "tail");
        if (entry.scope() == StateScope.CHUNK) {
            setChunkValue(context, entry, parseChunkSetInput(tail));
        } else {
            setWorldValue(context, entry, tail);
        }
        context.getSource().sendSuccess(() -> Component.literal("Set " + entry.identifier()), true);
        return 1;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void setWorldValue(
                                      CommandContext<CommandSourceStack> context,
                                      StateEntry<?> entry,
                                      String raw) throws CommandSyntaxException {
        Object value = parseValue(raw, (StateEntry) entry);
        ((WorldStateEntry) entry).set(context.getSource().getLevel(), value);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void setChunkValue(
                                      CommandContext<CommandSourceStack> context,
                                      StateEntry<?> entry,
                                      ChunkSetInput input) throws CommandSyntaxException {
        Object value = parseValue(input.value(), (StateEntry) entry);
        ((ChunkStateEntry) entry).set(context.getSource().getLevel(), input.pos(), value);
    }

    private static int debugWorldState(CommandContext<CommandSourceStack> context)
                                                                                   throws CommandSyntaxException {
        StateEntry<?> entry = requireState(context, StateScope.WORLD, false);
        requireRead(context, entry);
        ServerLevel level = context.getSource().getLevel();
        boolean present = ((WorldStateEntry<?>) entry).getIfPresent(level).isPresent();
        context.getSource().sendSuccess(() -> Component.literal(debug(entry, present, null)), false);
        return 1;
    }

    private static int debugChunkState(CommandContext<CommandSourceStack> context)
                                                                                   throws CommandSyntaxException {
        StateEntry<?> entry = requireState(context, StateScope.CHUNK, false);
        requireRead(context, entry);
        ChunkPos pos = chunkPos(context);
        boolean present = ((ChunkStateEntry<?>) entry).getIfLoaded(context.getSource().getLevel(), pos).isPresent();
        context.getSource().sendSuccess(() -> Component.literal(debug(entry, present, pos)), false);
        return 1;
    }

    private static String debug(StateEntry<?> entry, boolean present, ChunkPos pos) {
        return "State " + entry.identifier() + "\nscope=" + entry.scope().id() + "\npresent=" + present + (pos == null ? "" : "\nchunk=" + pos.x() + "," + pos.z()) + "\ndebugWritable=" + entry.debugConfig().writable() + "\ncodec=" + entry.codec();
    }

    private static String formatValue(StateEntry<?> entry, Object value) {
        if (value == null) return entry.identifier() + " is not present";
        String encoded = encodeValue(entry, value);
        if (encoded.length() > 2048) {
            encoded = encoded.substring(0, 2048) + "...";
        }
        return entry.identifier() + " = " + encoded;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String encodeValue(StateEntry<?> entry, Object value) {
        Object encoded = ((StateEntry) entry).codec().encodeStart(JsonOps.INSTANCE, value).result().orElse(value);
        return String.valueOf(encoded);
    }

    private static <T> T parseValue(String value, StateEntry<T> entry) throws CommandSyntaxException {
        try {
            return entry
                    .codec()
                    .parse(JsonOps.INSTANCE, JsonParser.parseString(value))
                    .getOrThrow(message -> new IllegalArgumentException("Invalid state value: " + message));
        } catch (RuntimeException exception) {
            throw INVALID_VALUE.create(exception.getMessage());
        }
    }

    private static void requireRead(CommandContext<CommandSourceStack> context, StateEntry<?> entry)
                                                                                                     throws CommandSyntaxException {
        if (!entry.debugConfig().enabled() || !hasPermission(context.getSource(), entry.debugConfig().readPermission())) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                    .dispatcherParseException()
                    .create("State debug read is disabled");
        }
    }

    private static void requireWrite(CommandContext<CommandSourceStack> context, StateEntry<?> entry)
                                                                                                      throws CommandSyntaxException {
        if (!entry.debugConfig().enabled() || !entry.debugConfig().writable() || !hasPermission(context.getSource(), entry.debugConfig().writePermission())) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                    .dispatcherParseException()
                    .create("State debug write is disabled");
        }
    }

    private static boolean hasPermission(CommandSourceStack source, int permission) {
        if (HAS_PERMISSION != null) {
            try {
                return (Boolean) HAS_PERMISSION.invoke(source, permission);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to check command permission", exception);
            }
        }
        return switch (permission) {
            case 0 -> true;
            case 1 -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
            case 2 -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
            case 3 -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
            case 4 -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER);
            default -> throw new IllegalArgumentException("Permission must be between 0 and 4: " + permission);
        };
    }

    @SuppressWarnings("unchecked")
    private static StateEntry<?> requireState(
                                              CommandContext<CommandSourceStack> context,
                                              @Nullable StateScope scope,
                                              boolean writable)
                                                                throws CommandSyntaxException {
        Identifier id = getId(context, "id");
        String raw = getRawId(context, "id");
        Optional<StateEntry<?>> entry = findUniqueState(context, scope, writable);
        if (entry.isEmpty() || (scope != null && entry.get().scope() != scope) || (writable && !entry.get().debugConfig().writable())) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                    .dispatcherParseException()
                    .create("Unknown state: " + raw);
        }
        return entry.get();
    }

    private static ChunkPos chunkPos(CommandContext<CommandSourceStack> context) {
        return new ChunkPos(
                IntegerArgumentType.getInteger(context, "chunkX"),
                IntegerArgumentType.getInteger(context, "chunkZ"));
    }

    private static Optional<StateEntry<?>> findUniqueState(
                                                           CommandContext<CommandSourceStack> context,
                                                           @Nullable StateScope scope,
                                                           boolean writable)
                                                                             throws CommandSyntaxException {
        Identifier id = getId(context, "id");
        String raw = getRawId(context, "id");
        List<StateEntry<?>> matches = matchingStates(context.getSource(), id, raw, scope, writable);
        if (matches.size() > 1) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                    .dispatcherParseException()
                    .create("Ambiguous state id: " + raw);
        }
        return matches.stream().findFirst();
    }

    private static List<StateEntry<?>> matchingStates(
                                                      CommandSourceStack source,
                                                      Identifier id,
                                                      String raw,
                                                      @Nullable StateScope scope,
                                                      boolean writable) {
        return allStates().stream()
                .filter(e -> scope == null || e.scope() == scope)
                .filter(e -> writable ? canWrite(source, e) : canRead(source, e))
                .filter(
                        e -> e.identifier().equals(id) || (!raw.contains(":") && e.identifier().getPath().equals(raw)))
                .toList();
    }

    private static ChunkSetInput parseChunkSetInput(String tail) throws CommandSyntaxException {
        StringReader reader = new StringReader(tail);
        int x = reader.readInt();
        reader.skipWhitespace();
        int z = reader.readInt();
        reader.skipWhitespace();
        if (!reader.canRead()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                    .dispatcherParseException()
                    .create("Missing state value");
        }
        return new ChunkSetInput(new ChunkPos(x, z), reader.getString().substring(reader.getCursor()));
    }

    private static Collection<StateEntry<?>> allStates() {
        return RegistryCore.getRegistryCores().stream()
                .flatMap(core -> core.getStateEntries().stream())
                .toList();
    }

    private static Identifier getId(CommandContext<CommandSourceStack> context, String name)
                                                                                             throws CommandSyntaxException {
        ResourceKey<?> key = context.getArgument(name, ResourceKey.class);
        return key.identifier();
    }

    private static String getRawId(CommandContext<CommandSourceStack> context, String name)
                                                                                            throws CommandSyntaxException {
        for (var node : context.getNodes()) {
            if (node.getNode().getName().equals(name)) {
                return context.getInput().substring(node.getRange().getStart(), node.getRange().getEnd());
            }
        }
        return "";
    }

    private static CompletableFuture<Suggestions> suggestReadableStateIds(
                                                                          CommandContext<CommandSourceStack> context,
                                                                          SuggestionsBuilder builder) {
        return suggestStateIds(context, builder, false);
    }

    private static CompletableFuture<Suggestions> suggestWritableStateIds(
                                                                          CommandContext<CommandSourceStack> context,
                                                                          SuggestionsBuilder builder) {
        return suggestStateIds(context, builder, true);
    }

    private static CompletableFuture<Suggestions> suggestStateIds(
                                                                  CommandContext<CommandSourceStack> context,
                                                                  SuggestionsBuilder builder,
                                                                  boolean writableOnly) {
        SuggestionsBuilder argumentBuilder = builder.createOffset(currentArgumentStart(builder));
        String remaining = argumentBuilder.getRemainingLowerCase();
        allStates().stream()
                .filter(e -> writableOnly ? canWrite(context.getSource(), e) : canRead(context.getSource(), e))
                .map(StateEntry::identifier)
                .filter(id -> matchesIdSuggestion(id, remaining))
                .map(Identifier::toString)
                .forEach(argumentBuilder::suggest);
        return argumentBuilder.buildFuture();
    }

    private static Collection<StateEntry<?>> readableStates(CommandSourceStack source) {
        return allStates().stream().filter(entry -> canRead(source, entry)).toList();
    }

    private static boolean canRead(CommandSourceStack source, StateEntry<?> entry) {
        return entry.debugConfig().enabled() && hasPermission(source, entry.debugConfig().readPermission());
    }

    private static boolean canWrite(CommandSourceStack source, StateEntry<?> entry) {
        return entry.debugConfig().enabled() && entry.debugConfig().writable() && hasPermission(source, entry.debugConfig().writePermission());
    }

    private static int currentArgumentStart(SuggestionsBuilder builder) {
        String input = builder.getInput();
        int start = Math.min(builder.getStart(), input.length());
        while (start > 0 && !Character.isWhitespace(input.charAt(start - 1))) {
            start--;
        }
        return start;
    }

    private static boolean matchesIdSuggestion(Identifier id, String remaining) {
        return id.toString().toLowerCase(Locale.ROOT).startsWith(remaining) || id.getPath().toLowerCase(Locale.ROOT).startsWith(remaining);
    }

    private record ChunkSetInput(ChunkPos pos, String value) {}
}
