# Fun2Go

一款以旅遊規劃與景點探索為核心的 Android 原生應用程式。

---

## 專案資訊

- **應用程式名稱**：Fun2Go
- **Package Name**：`com.funTrip.fun2go`
- **版本**：1.0（Version Code: 1）
- **最低支援 API**：26（Android 8.0）
- **目標 API**：36（Android 15）
- **語言**：Kotlin 2.0.21
- **UI 語言**：繁體中文

---

## 主要功能

- **使用者管理**
  - 查看使用者個人資料（姓名、Email）
  - 更新使用者資訊
  - 使用者認證框架

- **景點搜尋與探索**
  - 依關鍵字搜尋景點
  - 顯示景點詳細資訊（名稱、類別、地址、評分、座標）
  - 瀏覽景點圖片與描述
  - 依地理位置半徑搜尋

- **行程管理**
  - 建立與管理旅遊行程
  - 依天數組織行程
  - 新增景點至特定天的行程
  - 調整景點順序
  - 設定抵達時間與備註
  - 複製公開行程
  - 發布行程

- **收藏系統**
  - 新增 / 移除收藏景點
  - 查看收藏景點列表

---

## 專案結構

```
Fun2Go/
├── app/
│   ├── src/main/
│   │   ├── java/com/funTrip/fun2go/
│   │   │   ├── MainActivity.kt              # 應用程式入口
│   │   │   ├── data/
│   │   │   │   ├── model/                   # 資料模型
│   │   │   │   │   ├── User.kt
│   │   │   │   │   ├── Spot.kt
│   │   │   │   │   ├── Itinerary.kt
│   │   │   │   │   ├── ItineraryDay.kt
│   │   │   │   │   ├── ItinerarySpot.kt
│   │   │   │   │   └── ApiRequests.kt
│   │   │   │   ├── remote/                  # API 通訊
│   │   │   │   │   ├── ApiService.kt
│   │   │   │   │   ├── RetrofitClient.kt
│   │   │   │   │   └── NetworkResult.kt
│   │   │   │   └── repository/              # Repository 層
│   │   │   │       ├── BaseRepository.kt
│   │   │   │       └── TripRepository.kt
│   │   │   └── ui/
│   │   │       ├── adapter/                 # RecyclerView Adapters
│   │   │       │   └── SpotAdapter.kt
│   │   │       ├── theme/                   # Compose 主題設定
│   │   │       │   ├── Theme.kt
│   │   │       │   ├── Color.kt
│   │   │       │   └── Type.kt
│   │   │       └── viewmodel/               # MVVM ViewModels
│   │   │           └── MainViewModel.kt
│   │   └── res/                             # 資源檔案
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml                   # Gradle 版本目錄
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradlew / gradlew.bat
```

---

## 架構模式

採用 **MVVM（Model-View-ViewModel）** 架構：

- **Model 層** (`data/`)
  - 定義資料模型（User、Spot、Itinerary 等）
  - 透過 Retrofit 與後端 API 溝通
  - Repository 封裝資料存取邏輯

- **View 層** (`ui/`)
  - `MainActivity.kt` 負責畫面顯示與使用者互動
  - XML 佈局搭配 ConstraintLayout 與 CardView
  - `SpotAdapter.kt` 驅動 RecyclerView 列表

- **ViewModel 層** (`ui/viewmodel/`)
  - `MainViewModel.kt` 持有 LiveData 管理 UI 狀態
  - 透過 ViewModelScope 執行協程非同步操作

**資料流向：**

```
UI (MainActivity)
  ↓ 觀察 LiveData
ViewModel (MainViewModel)
  ↓ 呼叫
Repository (TripRepository)
  ↓ 安全呼叫
Remote API (Retrofit / ApiService)
  ↓ 回傳
NetworkResult (sealed class)
  ↓ 傳回 LiveData
UI 更新畫面
```

---

## 技術棧與依賴套件

- **語言與建置**
  - Kotlin 2.0.21
  - Android Gradle Plugin 8.13.2
  - Java 11

- **核心 AndroidX**
  - `core-ktx:1.17.0`
  - `appcompat:1.7.1`
  - `lifecycle-runtime-ktx:2.6.1`
  - `activity-compose:1.8.0`
  - `constraintlayout:2.1.4`
  - `recyclerview:1.3.2`
  - `cardview:1.0.0`

- **Jetpack Compose（Material 3）**
  - Compose BOM: 2024.09.00
  - `compose.ui`、`compose.material3`、`compose.ui.tooling`

- **網路通訊**
  - Retrofit 3.0.0（REST API 客戶端）
  - OkHttp 4.11.0（含 Logging Interceptor）
  - Gson（JSON 序列化 / 反序列化）

- **非同步處理**
  - Kotlin Coroutines 1.7.3
  - LiveData 2.6.2

- **測試**
  - JUnit 4.13.2
  - Espresso 3.7.0
  - AndroidX Test (JUnit)

---

## API 串接

- **Base URL**：`https://v1api.samuelray.net/`
- **共約 96 個 API 端點**，涵蓋：
  - 使用者（建立、查詢、更新、取得行程與收藏）
  - 行程（CRUD、複製、發布、詳細資訊）
  - 行程天數（新增、更新、刪除）
  - 行程景點（新增、移除、更新、排序）
  - 景點（關鍵字/類別/地理位置搜尋、詳細資訊、建立）
  - 收藏（新增 / 移除）

**NetworkResult 錯誤處理：**

```kotlin
sealed class NetworkResult<T> {
    class Success<T>(data: T) : NetworkResult<T>()
    class Error<T>(message: String) : NetworkResult<T>()
    class Loading<T> : NetworkResult<T>()
}
```

---

## 設定檔說明

| 檔案 | 用途 |
|------|------|
| `app/build.gradle.kts` | App 模組建置設定 |
| `build.gradle.kts` | 根專案建置設定 |
| `settings.gradle.kts` | 專案設定 |
| `gradle/libs.versions.toml` | Gradle 版本目錄（統一管理依賴版本）|
| `AndroidManifest.xml` | 應用程式宣告（權限、Activity）|
| `res/values/themes.xml` | Material 主題設定 |
| `res/values/colors.xml` | 色彩調色盤（紫色 / 粉色 / 青色系）|
| `res/values/strings.xml` | 字串資源 |

**應用程式權限：**
- `android.permission.INTERNET`（API 呼叫所需）

---

## 主題設計

- 繼承 `android:Theme.Material.Light.NoActionBar`
- Material Design 3 色彩方案
- 主色調：紫色 / 粉色 / 青色
