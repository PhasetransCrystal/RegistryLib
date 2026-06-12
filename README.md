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
> - **This modification** – by PhasetransCrystal, which **only changes the package name** from `com.gtodyssey.registrylib` to `net.phasetranscrystal.registrylib`. All other source code is identical to the GTO version.  
>
> All MPL‑2.0 requirements (retention of copyright notices, source availability, and disclosure of modifications) are complied with. The complete source code of this fork is publicly available in [this repository](https://github.com/PhasetransCrystal/RegistryLib).

## Documentation

For setup instructions, usage guides, and complete examples, read the documentation site of the upstream GTO modification (this fork changes only the package name, so the API usage remains the same):

### https://phasetranscrystal.github.io/RegistryLib/

This is the primary place to learn how to use RegistryLib.

- **Minecraft**: 26.1-snapshot.11
- **NeoForge**: 26.1
- **Gradle**: 9.0
- **Java**: 25

---

## License

This project is based on [Registrate](https://github.com/tterrag1098/Registrate) by **tterrag** – an absolutely brilliant piece of work. Its elegant design makes registry management in Minecraft mods a genuine pleasure to use. The thoughtful API, the fluent builder patterns, and the sheer amount of boilerplate it eliminates are a testament to tterrag's craftsmanship. We are deeply grateful for this incredible open-source contribution to the modding community.

We also thank **GregTech Odyssey (GTO)** for their first modification (RegistryLib), which updated the library to NeoForge 26.1 and repackaged it under `com.gtodyssey.registrylib`. Their work made this further modification possible.

**Modifications in this fork** (in compliance with MPL 2.0 §1.10):  
- Package name changed from `com.gtodyssey.registrylib` to `net.phasetranscrystal.registrylib`.  
- No other changes to the source code.

Licensed under [MPL-2.0](LICENSE). Portions of the source code are derived from or constitute Modifications of Registrate and of the GTO RegistryLib, and are therefore subject to the Mozilla Public License, v. 2.0.

In accordance with MPL 2.0 §3.2, if you receive this library in executable form (e.g. as a JAR file), the corresponding source code (this repository) is made available to you at no more than the cost of distribution.
