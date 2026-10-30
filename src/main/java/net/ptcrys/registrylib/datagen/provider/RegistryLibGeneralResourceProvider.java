package net.ptcrys.registrylib.datagen.provider;

import net.ptcrys.registrylib.RegistryCore;
import net.ptcrys.registrylib.datagen.ProviderType;
import net.ptcrys.registrylib.util.ImageUtil;
import net.ptcrys.registrylib.util.map.NestedMultiMap;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class RegistryLibGeneralResourceProvider implements RegistryLibProvider {

    private final RegistryCore parent;
    private final Path path;

    private final NestedMultiMap<String, String, BiFunction<Path, OutputStream, Path>> generates = NestedMultiMap.create(HashMap::new, ArrayList::new);

    public RegistryLibGeneralResourceProvider(RegistryCore parent, PackOutput output) {
        this.parent = parent;
        path = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(parent.getModid());
    }

    public void addItemTexture(BiFunction<Path, OutputStream, Path> generate) {
        addTexture("item", generate);
    }

    public void addBlockTexture(BiFunction<Path, OutputStream, Path> generate) {
        addTexture("block", generate);
    }

    public void addTexture(String name, BiFunction<Path, OutputStream, Path> generate) {
        add("textures", name, generate);
    }

    public void add(String a, String b, BiFunction<Path, OutputStream, Path> generate) {
        generates.put(a, b, generate);
    }

    public BiFunction<Path, OutputStream, Path> simpleTexture(
                                                              String path, Supplier<BufferedImage> image) {
        return (p, stream) -> {
            try {
                return ImageUtil.writeToStream(path, image.get(), p, stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.CLIENT;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        parent.genData(ProviderType.GENERAL_RESOURCE, this);
        return CompletableFuture.runAsync(
                () -> {
                    cleanGeneratedNamespaces();
                    List<RuntimeException> failures = new ArrayList<>();
                    generates.getMap().forEach(
                            (category, namedGenerators) -> {
                                Path categoryPath = this.path.resolve(category);
                                namedGenerators.forEach(
                                        (name, generators) -> {
                                            Path target = categoryPath.resolve(name);
                                            try {
                                                Files.createDirectories(target);
                                            } catch (IOException | RuntimeException e) {
                                                recordFailure(failures, target, e);
                                                return;
                                            }
                                            generators.forEach(
                                                    generator -> {
                                                        Path output = target;
                                                        try {
                                                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                                            HashingOutputStream hashedBytes = new HashingOutputStream(Hashing.sha256(), bytes);
                                                            output = generator.apply(target, hashedBytes);
                                                            cache.writeIfNeeded(output, bytes.toByteArray(), hashedBytes.hash());
                                                        } catch (IOException | RuntimeException e) {
                                                            recordFailure(failures, output, e);
                                                        }
                                                    });
                                        });
                            });
                    if (!failures.isEmpty()) {
                        RuntimeException exception = new RuntimeException(
                                "Failed to generate " + failures.size() + " resource output(s)");
                        failures.forEach(exception::addSuppressed);
                        throw exception;
                    }
                });
    }

    private static void recordFailure(List<RuntimeException> failures, Path path, Exception cause) {
        RuntimeException failure = new RuntimeException("Failed to generate resource at " + path, cause);
        LOGGER.error("{}", failure.getMessage(), cause);
        failures.add(failure);
    }

    private void cleanGeneratedNamespaces() {
        parent.getGeneratedNamespaceCleanups().forEach(this::cleanGeneratedNamespace);
    }

    private void cleanGeneratedNamespace(String relativePath) {
        String normalizedPath = relativePath.replace('\\', '/');
        String[] parts = normalizedPath.split("/", -1);
        if (normalizedPath.matches("^[A-Za-z]:.*") || normalizedPath.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Generated resource cleanup path must be relative: " + relativePath);
        }
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException(
                        "Generated resource cleanup path cannot contain empty, '.' or '..' path segments: " + relativePath);
            }
        }
        if (parts.length < 3 || !"textures".equals(parts[0])) {
            throw new IllegalArgumentException(
                    "Generated resource cleanup path must target a specific textures subdirectory, " + "for example textures/item/generated_materials: " + relativePath);
        }

        Path normalizedNamespaceRoot = path.normalize();
        Path texturesRoot = normalizedNamespaceRoot.resolve("textures");
        Path target = normalizedNamespaceRoot.resolve(Path.of(normalizedPath)).normalize();
        if (!target.startsWith(texturesRoot) || target.equals(texturesRoot)) {
            throw new IllegalArgumentException(
                    "Generated resource cleanup path must remain within a textures subdirectory: " + relativePath);
        }
        if (!Files.exists(target)) return;
        try (var stream = Files.walk(target)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(
                            p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException e) {
                                    throw new RuntimeException("Failed to clean generated resource path: " + p, e);
                                }
                            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to clean generated resource path: " + target, e);
        }
    }

    @Override
    public String getName() {
        return parent.getModid();
    }
}
