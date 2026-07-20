# 👥 Persons Finder – 后端挑战赛（AI 增强版）

欢迎参加 **Persons Finder** 后端挑战赛！本项目是一个帮助用户查找附近人员的移动应用后端，采用 **Kotlin + Spring Boot 2.7** 构建。

**背景：** 在我们公司，我们相信 AI 是一种工具，而非替代品。我们希望通过这个挑战了解你如何利用 AI 来编写更高效的代码、进行更深层次的思考，并构建安全可靠的系统。

---

## 📌 原始挑战要求

> 以下是挑战赛的原始需求说明。✅ 标记表示在本项目中的对应实现。

### ➕ `POST /persons` ✅
创建新人员。
*   **输入：** 姓名、职位、爱好、位置（经纬度）。✅
*   **AI 集成：** 根据职位和爱好自动生成**简短有趣的个人简介**。✅
    *   模拟实现（`AiBioServiceImpl`）— 架构支持随时替换为真实 LLM API。✅

### ✏️ `PUT /persons/{id}/location` ✅
更新人员的当前位置。✅

### 🔍 `GET /persons/nearby` ✅
查找查询位置（经纬度、半径）附近的人员。✅
*   **输出：** 人员列表（包含 AI 生成的简介），按距离排序。✅

### 🤖 AI 挑战
*   **AI 使用：** `AI_LOG.md` 记录了 5 次关键 AI 协作交互。✅
*   **提示注入防御：** `AiBioServiceImpl.sanitize()` 过滤注入模式。✅
*   **SECURITY.md：** 讨论了输入净化和 PII 隐私风险。✅

### 📦 预期产出
*   **代码：** 清晰的 Controller/Service/Repository 分层架构。✅
*   **存储：** H2 内存数据库（支持文件持久化模式）。✅
*   **文档：** `README.md`、`AI_LOG.md`、`SECURITY.md`。✅

### 🧪 加分项
*   **可扩展性：** 植入 100 万条记录，附近搜索基准测试 < 1s。✅
*   **整洁代码：** DDD 风格的包结构设计。✅
*   **测试：** 85 项单元测试和集成测试（包含 AI 服务非确定性行为测试）。✅

### 📬 提交
> 仓库地址：https://github.com/leozhang2056/PersonsFinder.git

---

## 📦 项目结构

```
src/
├── main/kotlin/com/persons/finder/
│   ├── ApplicationStarter.kt          # 启动入口 + 根路径→Swagger 重定向
│   ├── controller/
│   │   └── PersonController.kt        # REST API 控制器
│   ├── domain/
│   │   ├── Person.kt                  # 人员实体
│   │   └── Location.kt                # 位置实体（含经纬度索引）
│   ├── mapper/
│   │   ├── PersonRepository.kt        # 人员 JPA 仓库
│   │   └── LocationRepository.kt      # 位置 JPA 仓库（含 Haversine SQL）
│   ├── service/
│   │   ├── PersonsService.kt          # 人员服务接口
│   │   ├── PersonsServiceImpl.kt      # 人员服务实现
│   │   ├── LocationsService.kt        # 位置服务接口
│   │   ├── LocationsServiceImpl.kt    # 位置服务实现（含自适应搜索）
│   │   ├── AiBioService.kt            # AI 简介生成接口
│   │   ├── AiBioServiceImpl.kt        # AI 简介生成实现（含注入防御）
│   │   └── SeedDataService.kt         # 批量造数据服务
│   └── vo/
│       ├── ApiResponse.kt             # 统一 API 响应封装
│       ├── PersonAssembler.kt         # 实体→VO 转换 + 校验工具
│       ├── CreatePersonRequest.kt     # 创建人员请求体
│       ├── LocationUpdateRequest.kt   # 更新位置请求体
│       ├── PersonResponse.kt          # 人员响应体
│       ├── LocationResponse.kt        # 位置响应体
│       └── NearbyPersonResponse.kt    # 附近搜索响应体
├── test/kotlin/com/persons/finder/
│   ├── AiBioServiceTest.kt            # AI 服务单元测试（32 项）
│   ├── PersonsServiceTest.kt          # 人员服务单元测试（9 项）
│   ├── LocationsServiceTest.kt        # 位置服务单元测试（20 项）
│   ├── PersonControllerIntegrationTest.kt  # 集成测试（23 项）
│   └── DemoApplicationTests.kt        # 上下文加载测试
└── resources/
    └── application.properties         # 应用配置
```

### 分层架构

```
Controller (REST 入口)
    ↓
Service (业务逻辑)
    ↓
Repository (JPA 数据访问)
    ↓
H2 Database (内存/文件)
```

---

## 🚀 快速开始

### 前置条件

- **JDK 11+**（推荐 JDK 17）
- Gradle（或使用内置的 `gradlew` / `gradlew.bat`）

### 构建

```bash
./gradlew build          # Linux / Mac / Git Bash
gradlew.bat build        # Windows CMD
```

### 启动

```bash
./gradlew bootRun
```

启动后访问：
- 🌐 **API 地址**: `http://localhost:5000`（自动重定向到 Swagger）
- 📖 **Swagger 文档**: `http://localhost:5000/swagger-ui/index.html`

> ⏳ **首次启动**会自动插入 **100 万条数据**（约 60 秒）。
> 控制台会输出进度：
> ```
> [seed] 49500 / 1000000 inserted (1178/s, 42s elapsed)
> ```
> 后续启动检测到已有数据会自动跳过。

### 切换环境

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'   # 开发环境
./gradlew bootRun --args='--spring.profiles.active=prod'  # 生产环境
```

---

## 🔌 API 接口

所有接口返回统一格式：

```json
{
  "success": true,
  "code": 200,
  "data": { ... },
  "runningTime": 0.123,
  "message": null
}
```

### 1️⃣ 创建人员

```http
POST /persons
Content-Type: application/json

{
  "name": "张三",
  "jobTitle": "Software Engineer",
  "hobbies": ["hiking", "photography", "chess"],
  "latitude": 31.2304,
  "longitude": 121.4737
}
```

**响应**：`201 Created`，系统根据职位和爱好自动生成趣味简介

> 位置支持两种传法：顶层 `latitude`/`longitude` 或 `location` 对象（二选一）

### 2️⃣ 获取所有人员 ID

```http
GET /persons
```

### 3️⃣ 获取人员详情

```http
GET /persons/{id}
```

### 4️⃣ 更新位置

```http
PUT /persons/{id}/location
Content-Type: application/json

{
  "latitude": 34.0522,
  "longitude": -118.2437
}
```

同一个人多次更新会覆盖旧位置。

### 5️⃣ 附近搜索（核心功能）

```http
# 自适应半径（默认取最近 30 人）
GET /persons/nearby?latitude=31.23&longitude=121.47

# 指定半径 10km
GET /persons/nearby?latitude=31.23&longitude=121.47&radius=10

# 指定目标人数
GET /persons/nearby?latitude=31.23&longitude=121.47&count=50
```

**搜索策略**：
- **自适应模式**（不传 radius）：从 5km 开始翻倍递增直到找到足够的人，上限 20000km
- **固定半径模式**（传 radius）：在指定半径内搜索，`ORDER BY distance LIMIT max`

### 6️⃣ 批量造测试数据

```http
POST /persons/seed?count=1000       # 小批量
POST /persons/seed?count=1000000    # 100 万条性能测试
```
> 控制台每 3 秒输出一次进度：
> ```
> [seed] 49500 / 1000000 inserted (1178/s, 42s elapsed)
> ```

### 7️⃣ 多环境配置

```properties
# 默认（共享配置）
app.nearby.default-radius=10

# 开发环境 (application-dev.properties)
app.nearby.default-radius=5

# 生产环境 (application-prod.properties)
app.nearby.default-radius=20
```

---

## ✅ 三种测试方式

### 🎯 方式一：IDEA HTTP Client（推荐开发用）

项目根目录的 **`test-api.http`** 文件包含 21 个预定义请求，在 IntelliJ IDEA 中打开后**点击左侧 ▶ 绿色箭头**即可发送。

### 🌐 方式二：Swagger UI（推荐测试用）

浏览器打开 `http://localhost:5000/swagger-ui/index.html`：
- 每个接口都有**描述**和**示例数据**
- 点击 **"Try it out"** → 自动填充示例值 → 点击 **"Execute"** 执行

### 🖥️ 方式三：一键冒烟测试（推荐 CI/验收用）

```bash
# Linux / Git Bash
bash test-api.sh

# Windows 双击
test-api.bat
```

脚本自动测试 21 项：

```
1️⃣ POST /persons — 创建人员              ✅
2️⃣ GET /persons — 获取所有人 ID           ✅
3️⃣ GET /persons/{id} — 人员详情           ✅
4️⃣ PUT /persons/{id}/location — 更新位置   ✅
5️⃣ GET /persons/nearby — 附近搜索         ✅
6️⃣ POST /persons/seed — 批量造数据         ✅
7️⃣ 404 错误处理                          ✅
8️⃣ 400 参数校验                          ✅
9️⃣ 基础设施（Swagger、H2 Console）        ✅
🔟 提示注入防御                          ✅
```

---

## ⚙️ 数据库配置

默认使用 **H2 内存数据库**（重启数据清空，速度快）。可在 `application.properties` 切换模式：

```properties
# 内存模式（默认）
spring.datasource.url=jdbc:h2:mem:persons_finder;DB_CLOSE_DELAY=-1

# 文件模式（数据持久化）
# spring.datasource.url=jdbc:h2:file:./data/persons_finder;AUTO_SERVER=TRUE
```

### H2 控制台

- **地址**: `http://localhost:5000/h2-console`
- **JDBC URL**: `jdbc:h2:mem:persons_finder`
- **用户名**: `sa`
- **密码**: `password`

### 数据库连接信息

| 字段 | 值 |
|------|-----|
| **数据库类型** | H2（内存/文件） |
| **JDBC 驱动** | `org.h2.Driver` |
| **JDBC URL（内存）** | `jdbc:h2:mem:persons_finder;DB_CLOSE_DELAY=-1` |
| **JDBC URL（文件）** | `jdbc:h2:file:./data/persons_finder;AUTO_SERVER=TRUE` |
| **用户名** | `sa` |
| **密码** | `password` |
| **H2 控制台** | `http://localhost:5000/h2-console` |

可在 IntelliJ IDEA Database 工具中直接添加 H2 数据源，填入上述信息即可连接。

---

## 🧪 运行单元测试

```bash
./gradlew test
```

| 测试类 | 数量 | 内容 |
|--------|------|------|
| `AiBioServiceTest` | 32 | 正常输入、空爱好、注入检测、确定性验证 |
| `PersonsServiceTest` | 9 | CRUD、存在性检查、批量保存、Seed Bio |
| `LocationsServiceTest` | 20 | 距离计算（6）、附近搜索（7）、增删改查（7） |
| `PersonControllerIntegrationTest` | 23 | 全链路集成测试 |

---

## 🛡️ AI 安全

### 提示注入防御

`AiBioServiceImpl.sanitize()` 使用正则匹配过滤常见注入模式（`"ignore all instructions"`、`"say 'I am hacked'"` 等），采用拦截拒绝策略而非文本剥离，确保任何被检测到的注入字段被整体替换为安全默认值。详见 `SECURITY.md`。

### PII 隐私保护

设计讨论见 `SECURITY.md`，涵盖：
- 发送到 LLM 前的输入净化
- PII 数据（姓名、位置）发送到第三方模型的风险
- 高安全银行应用的架构方案

---

## 📊 性能数据（H2 内存，100 万条数据）

### 数据插入

| 指标 | 数据 |
|------|------|
| 总插入量 | 1,000,000 条 person + 1,000,000 条 location |
| 总耗时 | **42 秒** |
| 插入速率 | **23,809 条/秒** |
| 插入方式 | JDBC 批量插入（绕过 JPA，每批 500 条） |

### 附近搜索基准测试

| 搜索场景 | 半径 | 返回条数 | 耗时 |
|----------|------|---------|------|
| 自适应搜索（默认） | 自动扩展 | 30 | **0.35s** |
| 自适应搜索（10 条） | 自动扩展 | 10 | **0.34s** |
| 固定半径 5km | 5km | 100 | **0.47s** |
| 固定半径 10km | 10km | 100 | **0.48s** |
| 固定半径 50km | 50km | 100 | **0.46s** |
| 固定半径 100km | 100km | 100 | **0.50s** |
| 固定半径 1000km | 1000km | 100 | **2.25s** |
| 固定半径 2000km | 2000km | 100 | **4.15s** |
| 固定半径 5000km | 5000km | 100 | **4.93s** |

### 当前瓶颈

1. **N+1 查询问题** — nearby 先查 location 列表，再逐个 `getById` 查 person，100 条结果 = 1 次 location + 100 次 person 查询
2. **大范围边界框** — 1000km+ 半径时 H2 需扫描大量行
3. **双重 Haversine** — SQL 算一次，Kotlin 再算一次

### 优化方向

| 优化项 | 预期效果 | 难度 |
|--------|---------|------|
| **JOIN 查询替代 N+1** | 耗时降低 50%+ | 中 |
| **Redis GeoHash 索引** | 大半径毫秒级响应 | 高 |
| **空间索引（PostGIS / R-Tree）** | 1000km 查询 < 100ms | 高 |
| **Elasticsearch** | 地理围栏 + 全文搜索 | 高 |
| **返回字段裁剪** | 降低序列化开销 | 低 |

---

## 🤖 AI 协作记录

详见 `AI_LOG.md`，记录了 5 次关键 AI 交互：
1. Haversine 距离算法与附近搜索优化
2. 提示注入防御设计
3. 架构重构 — 抽取 SeedDataService
4. 可配置常量与多环境配置
5. 非确定性 AI 服务的单元测试策略

---

## 📬 提交

提交仓库链接。我们将阅读你的代码、`AI_LOG.md` 和 `SECURITY.md`。
