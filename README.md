# RegistryLib

A Minecraft mod built with NeoForge.

RegistryLib is a NeoForge registry library that reduces boilerplate for items, blocks, fluids,
BlockEntities, tooltips, grouped content setup, and related data generation. It keeps registration,
models, loot, recipes, language entries, renderer bindings, and other common mod content wiring in
one fluent builder workflow so projects can scale from simple prototypes to larger content mods
without scattering setup code across many classes.

> **⚠️ Important – Modification Chain**  
> This project is a **second‑generation modification** of the original [Registrate](https://github.com/tterrag1098/Registrate).
> - **Original work** – Registrate by tterrag (MPL‑2.0).
> - **First modification** – [RegistryLib](https://registrylib.gtodyssey.com/) by GregTech Odyssey (GTO), which changed the package to `com.gtodyssey.registrylib` and updated NeoForge.
> - **This modification** – by PhasetransCrystal, which uses the package `net.ptcrys.registrylib` and extends the library with shared registration pipelines, Group configuration, attachment-backed state, crops, worldgen helpers, and related API and data generation improvements.
>
> All MPL‑2.0 requirements (retention of copyright notices, source availability, and disclosure of modifications) are complied with. The complete source code of this fork is publicly available in [this repository](https://github.com/PhasetransCrystal/RegistryLib).

## Documentation

For setup instructions, usage guides, and examples for this fork, choose the documentation matching your branch or release from the version directory:

[RegistryLib documentation](https://registrylib.ptcrys.net/)

This is the primary place to learn how to use RegistryLib.

- **Minecraft**: 26.2
- **NeoForge**: 26.2.0.93
- **Gradle**: 9.6.1 (included wrapper)
- **Java**: 25

---

## License

This project is based on [Registrate](https://github.com/tterrag1098/Registrate) by **tterrag** – an absolutely brilliant piece of work. Its elegant design makes registry management in Minecraft mods a genuine pleasure to use. The thoughtful API, the fluent builder patterns, and the sheer amount of boilerplate it eliminates are a testament to tterrag's craftsmanship. We are deeply grateful for this incredible open-source contribution to the modding community.

We also thank **GregTech Odyssey (GTO)** for their first modification (RegistryLib), which updated the library to NeoForge 26.1 and repackaged it under `com.gtodyssey.registrylib`. Their work made this further modification possible.

**Modifications in this fork** (in compliance with MPL 2.0 §1.10):
- Package name changed from `com.gtodyssey.registrylib` to `net.ptcrys.registrylib`.
- Registration pipelines shared by Core instances with the same modid.
- Extended Group defaults and customization, tooltip behavior, and language generation.
- Attachment-backed world and chunk state, crop registration, and worldgen helpers.
- Additional examples, tests, documentation, and build and data generation improvements.

Licensed under [MPL-2.0](LICENSE). Portions of the source code are derived from or constitute Modifications of Registrate and of the GTO RegistryLib, and are therefore subject to the Mozilla Public License, v. 2.0.

In accordance with MPL 2.0 §3.2, if you receive this library in executable form (e.g. as a JAR file), the corresponding source code (this repository) is made available to you at no more than the cost of distribution.
