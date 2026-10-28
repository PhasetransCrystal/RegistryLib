# Core 注册管线

## 已确认的目标和边界

同一 `modid` 可以存在多个 `RegistryCore`（包括派生类）。它们对同一注册管线的贡献必须汇合，避免各自执行或输出时覆盖、丢失内容。限制 Core 数量或复用首次 Core 对象不是目标。

文档面向使用 RegistryLib 管理自己模组注册的开发者，应优先解释用法、共享范围和配置边界。实现保持简单，复用原有队列和索引。来源：本次需求中的明确约束。

## 实现

- `RegistryCore.RegistrationPipeline` 按 `modid` 共享原有条目索引、注册队列、回调、完成状态、datagen 配置与 provider、datapack initializer，以及其他需要按 namespace 汇总的注册数据。
- `create` 仍创建新 Core；构造器取共享管线。首次 Core 负责该 namespace 的事件分发，后续 Core 不重复安装监听器、消费队列或生成文件。
- `getCoreByModId` 返回首次 Core；`getRegistryCores` 提供每个 namespace 的事件代表。
- builder 默认配置、默认创造标签页和 `skipErrors` 仍属于 Core 实例；事件执行的错误处理和优先级沿用首次 Core。
- `setDataGenerator` 对同一条目同一类型的配置替换语义未改变；本次解决的是多个 Core 的管线归属，不定义重复注册同一个资源 ID 的合并规则。
- 保留本任务开始前已有的多语言修改。

实现来源：`src/main/java/net/ptcrys/registrylib/RegistryCore.java`。使用文档：安装教程、多语言教程及 Design Decisions 中的多 Core 使用说明。

## 验证

回归位于 `src/main/java/net/ptcrys/registrylibtest/gametest/RegistryLibFeatureGameTests.java`：同 modid 多 Core 的条目实际注册、查询共享、回调只执行一次、完成状态共享；datagen 回调汇总、initializer 共享及不同 modid 隔离。

实际 datagen 验证两个 Core 的语言键同时存在于同一个 `assets/registrylibtest/lang/en_us.json`。测试元数据通过 `/tmp` 内 Gradle init 脚本补入忽略的构建目录；datagen 输出到 `/tmp`，未修改项目运行配置。

最终验证：`spotlessApply test check`、7 个 GameTest 和 `runData` 均通过。单元测试报告为 125 项、0 失败、25 跳过；跳过项不计为已运行验证。

## Group 扩展与配置边界

- Group 提供 `configureBlock`、`configureItem`、`configureFluid`、`configureBlockEntity`，重复调用按声明顺序追加。回调在内置 Group 默认配置之后、条目链配置之前执行。
- 回调仅作用于直接通过相应 Group 入口创建的 Builder；BlockItem 和流体桶通过对应的 BlockBuilder／FluidBuilder 配置方法处理。
- `applyBlockDefaults`、`applyItemDefaults`、`applyFluidDefaults`、`applyBlockEntityDefaults` 是 protected 扩展点，保留传入 Builder 的具体类型及父类型。已有 Group 入口已应用默认配置，自定义入口不要再次应用。
- `build(factory)` 可返回自定义 Group 类型；无参 `build()` 保持兼容。构建时复制标签内外层 Map 和配置回调列表，复用 Builder 不会改变已构建 Group 的这些配置。
- 来源：`Group.java`；使用文档：`docs-content/tutorials/05-group-system.md`。GameTest 在 `RegistryLibGroupGameTests.java`，覆盖实际注册属性、配置顺序、构建快照及自定义 Builder。
- 本次验证：`spotlessApply test check`、9 个 GameTest、`runData` 通过；生成文件确认语言前缀、自定义中文翻译和方块掉落正确。单元测试报告仍为 125 项、0 失败、25 跳过。
