# fungo iOS App 規格文件

## 目錄
- [概述](#概述)
- [環境設定](#環境設定)
- [專案結構](#專案結構)
- [架構設計](#架構設計)
- [資料模型](#資料模型)
- [服務層](#服務層)
- [ViewModel 層](#viewmodel-層)
- [View 層](#view-層)
- [認證流程](#認證流程)
- [網路層](#網路層)
- [工具與元件](#工具與元件)
- [重要實作細節](#重要實作細節)
- [版本紀錄](#版本紀錄)

---

## 概述

**fungo** 是 iOS SwiftUI 旅遊行程規劃 App，對接 fun2Go 後端（`travel-itinerary-api-spec.md`）。

### 核心功能
- Google OAuth 2.0 登入
- 瀏覽公開行程（地圖 + 橫向卡片輪播）
- 地圖顯示附近景點 pins（依地理位置 Haversine 搜尋）
- 查看行程詳情，可複製他人行程
- 建立 / 編輯 / 刪除自己的行程（含天數、景點、排序、封面圖片）
- 編輯景點屬性（到達/離開時間、停留分鐘、備註）
- 查看其他使用者個人頁及其公開行程
- 收藏 / 取消收藏景點
- 編輯個人資料
- 包車預訂（瀏覽車輛、依車型篩選、預約下單）
- 行程連結包車（從行程詳情/編輯頁直接預約包車，自動預填天數與地址）
- 訂單管理（訂單列表、詳情、付款、取消、退款，訂單可連結行程）

### 技術規格

| 項目 | 值 |
|------|-----|
| Bundle ID | `com.raywoo.fungo` |
| iOS Deployment Target | 26.2 |
| Swift Version | 5.0 |
| Xcode | 26.2+ |
| 主要框架 | SwiftUI, MapKit, Security |
| SPM 依賴 | `GoogleSignIn-iOS` v9.0+ |
| 預設 Actor Isolation | `MainActor`（`SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor`） |
| Info.plist 位置 | 專案根目錄 `Info.plist`（非 `fungo/` 內） |

---

## 環境設定

### 開啟與建置

```bash
# 開啟 Xcode 專案
open fungo.xcodeproj

# CLI 建置（iphonesimulator）
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  xcodebuild -scheme fungo \
             -project fungo.xcodeproj \
             -sdk iphonesimulator \
             -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
             build
```

### 執行環境需求
- Xcode 26.2+（`xcode-select` 若指向 CommandLineTools，需設 `DEVELOPER_DIR`）
- iOS Simulator：iPhone 17 / 17 Pro / 17 Pro Max / Air、iPad 系列皆可
- 後端服務：`https://v1api.samuelray.net`（見 `travel-itinerary-api-spec.md`）

### Info.plist 必填項目
| Key | 說明 |
|-----|------|
| `GIDClientID` | Google Sign-In OAuth Client ID（iOS Client ID） |
| `CFBundleURLSchemes` | Google Sign-In 回調 URL scheme（`com.googleusercontent.apps.*`） |

---

## 專案結構

```
fungo/
├── fungoApp.swift                  # @main 入口
├── Info.plist                      # 專案根目錄（非 fungo/ 內）
├── Assets.xcassets/
├── Auth/
│   ├── AuthManager.swift           # Token 生命週期管理
│   ├── GoogleSignInBridge.swift    # Google Sign-In SDK async 封裝
│   └── KeychainHelper.swift        # Keychain 存取
├── Config/
│   ├── APIConfig.swift             # baseURL、timeout、defaultLimit
│   └── ServiceContainer.swift      # 依賴注入容器（含 myItineraries 共用快取）
├── Extensions/
│   ├── Date+Formatting.swift       # Date formatter 靜態實例 + ISODateParser（ISO 8601 解析 + locale-aware 格式化）
│   └── Decimal+Formatting.swift    # Decimal.formattedPrice（NT$ 千分位格式化）
├── Utilities/
│   ├── DayColorPalette.swift       # 行程天數色盤（8 色循環，地圖標記/路線用）
│   ├── HapticManager.swift         # 觸覺回饋工具（impact / notification / selection）
│   └── NetworkMonitor.swift        # 網路狀態偵測（Wi-Fi / Cellular / 斷線）
├── ImageCache/
│   └── ImageCache.swift            # NSCache 圖片快取（含 in-flight 去重）
├── Models/
│   ├── Auth/
│   │   ├── APIError.swift          # 錯誤型別
│   │   └── AuthResponse.swift      # Auth API 回應模型
│   ├── Itinerary.swift             # 行程列表項目（自訂解碼，缺欄位補預設值）
│   ├── ItineraryDetail.swift       # 行程詳情（含巢狀天數）
│   ├── ItineraryDay.swift          # 行程單日（Dual-key CodingKey）
│   ├── ItinerarySpot.swift         # 日內景點關聯表（Dual-key CodingKey）
│   ├── Spot.swift                  # 景點 / POI
│   ├── SpotTransition.swift        # 景點間交通
│   ├── User.swift                  # 用戶資料
│   ├── Vehicle.swift               # 車輛 + VehicleType enum
│   ├── Order.swift                 # 訂單 + OrderStatus enum
│   ├── CharterBooking.swift        # 包車預訂明細
│   ├── Payment.swift               # 付款記錄
│   ├── CharterItineraryContext.swift # 行程→包車預填資料結構體
│   ├── ItineraryDetail+MapHelpers.swift # MappableSpot + 地圖輔助方法（allMappableSpots / spotsForDay / region）
│   ├── UploadFolder.swift          # 上傳目標資料夾 enum（spots/vehicles/itineraries）
│   └── UploadResponse.swift        # 上傳回應模型（url: String）
├── Services/
│   ├── Networking/
│   │   ├── APIEndpoint.swift       # 所有端點定義與 URLRequest 組裝
│   │   ├── HTTPClient.swift        # actor，執行 HTTP 請求與解碼
│   │   └── UploadProgressDelegate.swift  # URLSessionTaskDelegate 上傳進度回報
│   ├── AuthService.swift
│   ├── DayService.swift
│   ├── FavoriteService.swift
│   ├── ItineraryService.swift
│   ├── ItinerarySpotService.swift
│   ├── SpotService.swift
│   ├── UserService.swift
│   ├── VehicleService.swift        # 車輛列表與詳情
│   ├── OrderService.swift          # 訂單 CRUD + 付款/取消/退款
│   └── UploadService.swift         # 圖片壓縮 + S3 上傳（含進度回報 + 自動重試）
├── ViewModels/
│   ├── AddToItineraryViewModel.swift   # 景點加入行程 state machine（含建立行程步驟）
│   ├── EditItineraryViewModel.swift
│   ├── ExploreViewModel.swift
│   ├── ItineraryDetailViewModel.swift
│   ├── MyItinerariesViewModel.swift
│   ├── ProfileViewModel.swift
│   ├── SpotSearchViewModel.swift
│   ├── UserProfileViewModel.swift      # 其他使用者個人頁 VM
│   ├── VehicleListViewModel.swift      # 車輛列表（篩選、可用性）
│   ├── OrderListViewModel.swift        # 訂單列表（分頁、狀態篩選）
│   ├── OrderDetailViewModel.swift      # 訂單詳情 + 付款/取消/退款操作
│   └── CharterBookingViewModel.swift   # 包車預約多步驟表單
└── Views/
    ├── Auth/
    │   ├── AuthGateView.swift
    │   └── LoginView.swift
    ├── Components/
    │   ├── BottomBarBackground.swift   # 底部固定操作列背景 ViewModifier
    │   ├── CachedAsyncImage.swift      # Phase-based 圖片載入（shimmer + fade-in）
    │   ├── CardStyle.swift             # 卡片樣式 ViewModifier（背景/圓角/陰影）
    │   ├── CategoryBadge.swift
    │   ├── DraggableBottomPanel.swift   # 自訂拖曳底部面板（PanelDetent + 橡皮筋 + 速度吸附）
    │   ├── FilterChip.swift            # 共用篩選標籤元件（含 .isSelected accessibility trait）
    │   ├── NumberedSpotMarker.swift   # 地圖數字標記圓圈（天數色 + 序號 + 選中態 + accessibility）
    │   ├── HeroImageView.swift         # Hero 圖片（含漸層遮罩）
    │   ├── ImagePickerUploadView.swift # 圖片選擇 + 壓縮上傳 + 預覽元件
    │   ├── LoadingStateView.swift
    │   ├── OrderStatusBadge.swift      # 訂單狀態 badge
    │   ├── PaginationFooterView.swift
    │   ├── ShimmerView.swift           # Shimmer 載入佔位動畫
    │   └── VehicleTypeBadge.swift      # 車型 badge
    ├── Explore/
    │   ├── AddToItinerarySheet.swift   # 景點加入行程（含內嵌建立行程表單）
    │   ├── CompactItineraryCard.swift
    │   ├── ExploreView.swift
    │   ├── ItineraryCardView.swift
    │   ├── ItineraryDetailView.swift
    │   ├── ItineraryMapView.swift      # 全螢幕行程地圖（日期篩選 + 路線 + 編號標記 + 底部面板）
    │   ├── ItinerarySpotPanelView.swift # 地圖底部景點面板（半高 sheet）
    │   ├── SpotAnnotationView.swift
    │   ├── SpotCardView.swift
    │   ├── SpotDetailView.swift
    │   ├── SpotDetailLoadingView.swift # 用 spot ID 載入景點詳情
    │   └── SpotSearchView.swift
    ├── Itinerary/
    │   ├── AddSpotSheet.swift
    │   ├── EditDaySheet.swift
    │   ├── EditDaySpotsView.swift
    │   ├── EditItineraryMetaSheet.swift
    │   ├── EditItineraryView.swift
    │   └── EditSpotSheet.swift         # 編輯景點時間/備註
    ├── MyItineraries/
    │   ├── CreateItinerarySheet.swift
    │   └── MyItinerariesView.swift
    ├── Charter/
    │   ├── CharterTabView.swift        # 包車 Tab root（車輛瀏覽 + 篩選）
    │   ├── VehicleCardView.swift       # 車輛卡片
    │   ├── VehicleDetailView.swift     # 車輛詳情 + 預約入口
    │   ├── CharterBookingSheet.swift   # 包車預約多步驟 sheet
    │   └── ItineraryCharterSheet.swift # 行程連結包車預約（車輛選擇→預約→確認）
    ├── Orders/
    │   ├── OrderListView.swift         # 訂單列表（狀態篩選 + 分頁）
    │   └── OrderDetailView.swift       # 訂單詳情 + 操作按鈕
    ├── Profile/
    │   ├── EditProfileSheet.swift
    │   ├── ProfileView.swift           # 含「我的訂單」入口
    │   └── UserProfileView.swift       # 其他使用者個人頁
    └── MainTabView.swift               # 4 個 Tab
```

> **Xcode 專案注意**：使用 `PBXFileSystemSynchronizedRootGroup`，`fungo/` 目錄下的新檔案自動加入，**不需**手動編輯 `.pbxproj`。

---

## 架構設計

### 整體分層

```
View  ──► ViewModel (@Observable)  ──► Service (nonisolated struct)  ──► HTTPClient (actor)
  │                                                                            │
  └── Components (reusable views)          Models (nonisolated struct) ◄───────┘
```

### Swift Concurrency 規則

| 類型 | 規則 | 原因 |
|------|------|------|
| `HTTPClient` | `actor` | 序列化網路請求與 decoder 存取 |
| `AuthManager` | `@Observable final class` + `@unchecked Sendable` | 跨 actor 傳遞；狀態由 MainActor 保護 |
| `ServiceContainer` | `@Observable final class` + `@unchecked Sendable` | 跨 ViewModel 共用狀態（`myItineraries`） |
| ViewModels | `@Observable final class`（隱式 `@MainActor`） | 驅動 SwiftUI 更新 |
| Models（Codable）| `nonisolated struct` | 可從 HTTPClient actor 跨界使用 |
| Services | `nonisolated struct` | 可從任何 actor 呼叫 |
| Enums（APIEndpoint、APIConfig 等）| `nonisolated enum` | 同上 |
| `KeychainHelper` | `nonisolated enum` | Keychain 呼叫在任何 context |

**重要**：`MainActor.run {}` 只接受同步閉包；無法在其中呼叫 `async` 函式。從 actor 存取 `@MainActor` 屬性（如 `AuthManager.accessToken`）只需 `await`，不需 `MainActor.run`。

### 依賴注入

`fungoApp` 建立 `HTTPClient`、`AuthManager`、`ServiceContainer` 三個根物件，透過 `.environment()` 注入整個 View 樹：

```swift
ContentView()
    .environment(authManager)
    .environment(services)
```

`ServiceContainer` 持有所有 Service 實例及共用狀態，Views 透過 `@Environment(ServiceContainer.self)` 取用。

### 導航結構

```
AuthGateView
├── LoginView（未登入）
└── MainTabView（已登入）
    ├── Tab 1：探索       → ExploreView
    │   └── NavigationStack → ItineraryDetailView → UserProfileView
    │                         └── sheet: ItineraryCharterSheet（行程連結包車）
    ├── Tab 2：我的行程   → MyItinerariesView
    │   └── NavigationStack → EditItineraryView → EditDaySpotsView
    │                         └── sheet: ItineraryCharterSheet（行程連結包車）
    ├── Tab 3：包車       → CharterTabView
    │   └── NavigationStack → VehicleDetailView → CharterBookingSheet
    ├── Tab 4：個人       → ProfileView
    │   └── NavigationStack → OrderListView → OrderDetailView
    │                                          └── NavigationLink: ItineraryDetailView（連結行程）
```

Sheets（Modal）：`CreateItinerarySheet`、`EditItineraryMetaSheet`、`EditDaySheet`、`EditSpotSheet`、`AddSpotSheet`、`EditProfileSheet`、`SpotSearchView`、`SpotDetailView`、`AddToItinerarySheet`（含內嵌 `CreateItineraryForm`）、`CharterBookingSheet`（多步驟預約）、`ItineraryCharterSheet`（行程連結包車：車輛選擇→預約→確認）、`CreateSpotSheet`（建立新景點，可帶入經緯度）、`EditSpotInfoSheet`（編輯景點屬性）

---

## 資料模型

所有模型皆為 `nonisolated struct`，實作 `Codable, Identifiable, Hashable, Sendable`。CodingKeys 統一做 snake_case → camelCase 對應。

### User

```swift
nonisolated struct User: Codable, Identifiable, Hashable, Sendable {
    let id: Int
    var name: String
    var email: String?
    var avatarURL: String?
    var createdAt: String?
}
```

### Itinerary（列表用）

```swift
nonisolated struct Itinerary: Codable, Identifiable, Hashable, Sendable {
    let id: Int
    var userID: Int?
    var title: String
    var coverImageURL: String?
    var destination: String?
    var totalDays: Int
    var isOfficial: Bool    // 缺欄位時預設 false
    var isPublic: Bool
    var copyCount: Int      // 缺欄位時預設 0
    var publishedAt: String?
    var createdAt: String?
    var updatedAt: String?
    // 後端 JOIN 欄位
    var authorName: String?
    var authorAvatar: String?
}
```

> **Flexible Decoding**：`Itinerary` 實作自訂 `init(from:)`，`isOfficial` 與 `copyCount` 缺欄位時補預設值（`false` / `0`）。POST /itineraries 建立回應不含這兩個欄位，若使用合成解碼器則 decode 會失敗。

### ItineraryDetail（詳情，含巢狀）

繼承 `Itinerary` 所有欄位，額外包含：
```swift
var days: [ItineraryDay]  // 巢狀天數陣列（含景點）
```

**Computed property**：`sortedDays` — 依 `dayNumber` 排序的天數陣列。

**Extension — `ItineraryDetail+MapHelpers.swift`**：

| 型別/方法 | 說明 |
|-----------|------|
| `MappableSpot` (`nonisolated struct`) | 扁平化景點（`id`, `dayNumber`, `sequenceIndex`, `coordinate`, `name`），用於地圖標記與 accessibility |
| `allMappableSpots` | 所有天數中有效座標的景點，依 day + orderIndex 排序 |
| `daysForIndex(_ dayIndex: Int)` | 指定天數的 `[ItineraryDay]`（0 = 全部天數）— 集中「0 = 全部」邏輯，供 MapView / PanelView 共用 |
| `spotsForDay(_ dayIndex: Int)` | 篩選指定天數景點（0 = 全部） |
| `region(forDayIndex:)` | 計算指定天數景點的邊界區域（1.4x padding） |

### ItineraryDay

```swift
nonisolated struct ItineraryDay: Codable, Identifiable, Hashable, Sendable {
    let id: Int          // Dual-key：優先嘗試 "day_id"，fallback "id"
    var dayNumber: Int
    var date: String?
    var note: String?
    var spots: [ItinerarySpot]?

    var sortedSpots: [ItinerarySpot]  // computed：(spots ?? []).sorted(by: orderIndex)
}
```

> **Dual-key CodingKey 模式**：`GET /itineraries/:id` 巢狀回應用 `json_build_object('day_id', d.id, ...)` 組裝，欄位名為 `"day_id"`；`POST /itineraries/:id/days` 建立回應欄位名為 `"id"`。`CodingKeys` 同時宣告 `case id = "day_id"` 與 `case altId = "id"`，`init(from:)` 先嘗試 `"day_id"`，失敗再嘗試 `"id"`。並實作手動 `encode(to:)`。

### ItinerarySpot（行程景點關聯表）

```swift
nonisolated struct ItinerarySpot: Codable, Identifiable, Hashable, Sendable {
    let id: Int           // Dual-key：優先嘗試 "itinerary_spot_id"，fallback "id"
    var orderIndex: Int
    var arrivalTime: String?
    var departureTime: String?
    var isCustomTime: Bool?
    var durationMinutes: Int?
    var note: String?
    var spot: Spot?       // 巢狀景點資料（detail 有，create 可能無）
}
```

> **Dual-key CodingKey 模式**：`GET /itineraries/:id` 巢狀回應欄位名為 `"itinerary_spot_id"`；`POST /days/:id/spots` 建立回應欄位名為 `"id"`。`CodingKeys` 同時宣告 `case id = "itinerary_spot_id"` 與 `case altId = "id"`，`init(from:)` 先嘗試 `"itinerary_spot_id"`，失敗再嘗試 `"id"`。另提供明確的成員逐一 `init` 供程式碼手動建構，並實作手動 `encode(to:)`。

### Spot

```swift
nonisolated struct Spot: Codable, Identifiable, Hashable, Sendable {
    let id: Int
    var name: String
    var address: String?
    var latitude: Double?    // DECIMAL → 自訂解碼（見下方）
    var longitude: Double?   // DECIMAL → 自訂解碼
    var category: SpotCategory?
    var imageURL: String?
    var googlePlaceID: String?
    var distanceKm: Double?  // DECIMAL → 自訂解碼
    var createdAt: String?
    var creatorId: Int?      // 建立者 user ID
    var isPublic: Bool?      // 是否公開
    var source: String?      // "official" 或 "user"
}
```

> **Flexible Decoding**：Node.js `pg` 驅動將 `DECIMAL` 欄位序列化為字串（`"25.03363000"`），`Spot` 實作自訂 `init(from:)`，透過 `decodeFlexibleDouble` 依序嘗試 `Double` 解碼再嘗試 `String` 轉換，避免 decode 失敗導致景點不顯示。

### SpotCategory（列舉）

| Case | rawValue | 顯示名稱 | SF Symbol |
|------|----------|---------|-----------|
| `.restaurant` | `"restaurant"` | 餐廳 | `fork.knife` |
| `.attraction` | `"attraction"` | 景點 | `mappin.and.ellipse` |
| `.nightMarket` | `"night_market"` | 夜市 | `sparkles` |
| `.accommodation` | `"accommodation"` | 住宿 | `bed.double` |
| `.cafe` | `"cafe"` | 咖啡廳 | `cup.and.saucer` |
| `.shopping` | `"shopping"` | 購物 | `bag` |

`SpotCategory` 另有 `color: Color` 計算屬性（統一全 App 分類顏色）：restaurant → `.orange`、attraction → `.blue`、nightMarket → `.purple`、accommodation → `.brown`、cafe → `.green`、shopping → `.pink`。`CategoryBadge`、`SpotAnnotationView`、`SpotSearchView` 均使用此屬性，不再各自定義顏色。

### TransportMode（列舉）

| Case | rawValue | 顯示名稱 |
|------|----------|---------|
| `.driving` | `"driving"` | 開車 |
| `.walking` | `"walking"` | 步行 |
| `.transit` | `"transit"` | 大眾運輸 |
| `.cycling` | `"cycling"` | 騎車 |

### Vehicle

```swift
nonisolated struct Vehicle: Codable, Identifiable, Hashable, Sendable {
    let id: Int
    var name: String
    var type: VehicleType
    var capacity: Int
    var pricePerDay: Decimal    // Flexible Decoding（Decimal 或 String）
    var imageURL: String?
    var description: String?
    var isAvailable: Bool
    var createdAt: String?
}
```

> **Flexible Decoding**：`pricePerDay` 使用自訂 `init(from:)` 先嘗試 `Decimal` 再嘗試 `String → Decimal`，fallback 為 `0`。同時實作手動 `encode(to:)`。

### VehicleType（列舉）

| Case | rawValue | 顯示名稱 | SF Symbol | 顏色 |
|------|----------|---------|-----------|------|
| `.sedan4` | `"sedan_4"` | 轎車 4人 | `car` | `.blue` |
| `.van9` | `"van_9"` | 九人座 | `car.side` | `.orange` |
| `.bus20` | `"bus_20"` | 巴士 20人 | `bus` | `.purple` |

### Order

```swift
nonisolated struct Order: Codable, Identifiable, Hashable, Sendable {
    let id: Int
    var userID: Int?
    var itineraryID: Int?
    var orderType: String       // 預設 "charter"
    var status: OrderStatus     // 預設 .pending
    var totalAmount: Decimal    // Flexible Decoding
    var note: String?
    var charterBooking: CharterBooking?  // try? 防止巢狀解碼失敗
    var payments: [Payment]?             // try? 防止巢狀解碼失敗
    var createdAt: String?
    var updatedAt: String?
}
```

> **Resilient Decoding**：`orderType` 使用 `(try? c.decodeIfPresent(...)) ?? "charter""`，`status` 使用 `(try? c.decode(...)) ?? .pending`，`charterBooking` 與 `payments` 使用 `try?` 讓巢狀解碼失敗時回傳 `nil` 而非拋錯。`totalAmount` 同 Vehicle 的 flexible decoding 模式。

### OrderStatus（列舉）

| Case | rawValue | 顯示名稱 | SF Symbol | 顏色 |
|------|----------|---------|-----------|------|
| `.pending` | `"pending"` | 待付款 | `clock` | `.orange` |
| `.confirmed` | `"confirmed"` | 已確認 | `checkmark.circle` | `.blue` |
| `.completed` | `"completed"` | 已完成 | `flag.checkered` | `.green` |
| `.cancelled` | `"cancelled"` | 已取消 | `xmark.circle` | `.gray` |

### CharterBooking

```swift
nonisolated struct CharterBooking: Codable, Identifiable, Hashable, Sendable {
    let id: Int
    var orderID: Int?
    var vehicleID: Int?
    var pickupLocation: String?
    var dropoffLocation: String?
    var pickupTime: String?
    var dropoffTime: String?
    var days: Int?
    var passengerCount: Int?
    var contactName: String?
    var contactPhone: String?
    var specialRequests: String?
    // Detail-only 欄位（GET /orders/:id 回傳）
    var vehicleName: String?
    var vehicleType: VehicleType?
    var vehicleCapacity: Int?
}
```

> 除 `id` 外所有欄位皆 optional，使用 auto-synthesized `Codable`（不需自訂 `init(from:)`）。列表 API 與詳情 API 回傳的欄位集合不同，全 optional 確保兩者均可正確解碼。

### Payment

```swift
nonisolated struct Payment: Codable, Identifiable, Hashable, Sendable {
    let id: Int
    var orderID: Int?
    var amount: Decimal     // Flexible Decoding
    var method: String      // 預設 "unknown"
    var status: String      // 預設 "unknown"
    var transactionID: String?
    var paidAt: String?
    var createdAt: String?
}
```

> **Resilient Decoding**：`method` 與 `status` 使用 `(try? c.decodeIfPresent(...)) ?? "unknown"` 避免缺欄位時 decode 失敗。`amount` 同 Vehicle 的 flexible decoding 模式。

### CharterItineraryContext（行程→包車預填資料）

```swift
nonisolated struct CharterItineraryContext: Sendable {
    let itineraryID: Int
    let itineraryTitle: String
    let totalDays: Int
    let pickupAddress: String?   // 第 1 天第 1 個景點地址
    let dropoffAddress: String?  // 最後一天最後景點地址
}
```

> **工廠方法**：`static func from(detail: ItineraryDetail) -> CharterItineraryContext`，依 `dayNumber` 排序天數，取第一天首個景點 `spot.address` 為 `pickupAddress`，最後一天末景點為 `dropoffAddress`。供 `ItineraryCharterSheet` 預填包車預約表單。

### Decimal+Formatting

```swift
extension Decimal {
    var formattedPrice: String  // "NT$5,000"（貨幣格式，無小數）
}
```

靜態 `NumberFormatter` 快取（`.currency` 樣式、`currencyCode = "TWD"`、`currencySymbol = "NT$"`、`maximumFractionDigits = 0`）。View 層直接使用 `formattedPrice`，不再手動拼接 `"NT$ "` 前綴。

### APIError

```swift
nonisolated enum APIError: LocalizedError, Sendable {
    case invalidURL
    case networkError(String)
    case httpError(statusCode: Int, message: String)
    case decodingError(String)
    case unauthorized
    case unknown
}
```

---

## 服務層

所有 Service 皆為 `nonisolated struct`，持有 `HTTPClient` 依賴。需認證的方法另接收 `AuthManager` 參數。

### APIConfig

```swift
nonisolated enum APIConfig {
    static let baseURL = "https://v1api.samuelray.net/api"
    static let defaultLimit = 20
    static let defaultTimeout: TimeInterval = 10
}
```

### ServiceContainer

```swift
@Observable
final class ServiceContainer: @unchecked Sendable {
    let authService: AuthService
    let itineraryService: ItineraryService
    let spotService: SpotService
    let userService: UserService
    let favoriteService: FavoriteService
    let dayService: DayService
    let itinerarySpotService: ItinerarySpotService
    let vehicleService: VehicleService
    let orderService: OrderService
    let uploadService: UploadService

    /// 跨 ViewModel 共用的使用者行程快取（含私人行程）
    var myItineraries: [Itinerary] = []
}
```

`myItineraries` 作為橋接快取，解決 `AddToItineraryViewModel` 建立新行程後 `MyItinerariesViewModel` 感知不到的問題（見重要實作細節第 6 點）。

### ItineraryService

| 方法 | 對應端點 | Auth |
|------|---------|:----:|
| `listPublic(limit:offset:)` | GET /itineraries | ❌ |
| `getDetail(id:)` | GET /itineraries/{id} | ❌ |
| `create(title:destination:totalDays:isPublic:coverImageURL:authManager:)` | POST /itineraries | ✅ |
| `update(id:fields:authManager:)` | PUT /itineraries/{id} | ✅ |
| `delete(id:authManager:)` | DELETE /itineraries/{id} | ✅ |
| `copy(id:authManager:)` | POST /itineraries/{id}/copy | ✅ |
| `publish(id:authManager:)` | POST /itineraries/{id}/publish | ✅ |

> `copy` 方法不需傳 `user_id`，後端 v1.3 起從 JWT Token 自動取得使用者身份。Request body 為空 `{}`。

### SpotService

| 方法 | 對應端點 | Auth |
|------|---------|:----:|
| `search(keyword:category:lat:lng:radius:)` | GET /spots | ❌ |
| `getDetail(id:)` | GET /spots/{id} | ❌ |
| `create(name:...:authManager:)` | POST /spots | ✅ |

### DayService

| 方法 | 對應端點 |
|------|---------|
| `create(itineraryID:dayNumber:date:note:authManager:)` | POST /itineraries/{id}/days |
| `update(itineraryID:dayID:date:note:authManager:)` | PUT /itineraries/{id}/days/{dayID} |
| `delete(itineraryID:dayID:authManager:)` | DELETE /itineraries/{id}/days/{dayID} |

### ItinerarySpotService

| 方法 | 對應端點 |
|------|---------|
| `add(dayID:spotID:orderIndex:authManager:)` | POST /days/{id}/spots |
| `update(dayID:spotID:fields:authManager:)` | PUT /days/{id}/spots/{spotID} |
| `remove(dayID:spotID:authManager:)` | DELETE /days/{id}/spots/{spotID} |
| `reorder(dayID:spotOrders:authManager:)` | PUT /days/{id}/spots/reorder |

### UserService

| 方法 | 對應端點 | Auth |
|------|---------|:----:|
| `getUser(id:)` | GET /users/{id} | ❌ |
| `updateUser(id:name:avatarURL:authManager:)` | PUT /users/{id} | ✅ |
| `getUserItineraries(id:authManager:)` | GET /users/{id}/itineraries | 可選 |
| `getUserFavorites(id:authManager:)` | GET /users/{id}/favorites | 可選 |

> `getUserItineraries(id:authManager:)` 與 `getUserFavorites(id:authManager:)` 的 `authManager` 參數皆為 `AuthManager? = nil`（選填）。傳入 `authManager` 時，`HTTPClient` 會附加 Bearer token，讓後端回傳完整資料。不傳時僅能取得公開資料。

### FavoriteService

| 方法 | 對應端點 |
|------|---------|
| `add(userID:spotID:authManager:)` | POST /favorites |
| `remove(spotID:userID:authManager:)` | DELETE /favorites/{spotID} |

### VehicleService

| 方法 | 對應端點 | Auth |
|------|---------|:----:|
| `list(type:available:)` | GET /vehicles | ❌ |
| `getDetail(id:)` | GET /vehicles/{id} | ❌ |

> 車輛為公開資料，不需認證。`type` 對應 `VehicleType.rawValue`（如 `"sedan_4"`），`available` 篩選是否可用。

### OrderService

| 方法 | 對應端點 | Auth |
|------|---------|:----:|
| `create(orderType:itineraryID:charter:authManager:)` | POST /orders | ✅ |
| `list(status:limit:offset:authManager:)` | GET /orders | ✅ |
| `getDetail(id:authManager:)` | GET /orders/{id} | ✅ |
| `update(id:fields:authManager:)` | PUT /orders/{id} | ✅ |
| `cancel(id:authManager:)` | POST /orders/{id}/cancel | ✅ |
| `pay(id:authManager:)` | POST /orders/{id}/pay | ✅ |
| `getPayments(id:authManager:)` | GET /orders/{id}/payments | ✅ |
| `refund(id:authManager:)` | POST /orders/{id}/refund | ✅ |

> `create` 的 `charter` 參數為 `[String: Any]?`，包含 `vehicle_id`、`pickup_location`、`pickup_time` 等包車明細欄位，以巢狀 JSON 傳送。`cancel`、`pay`、`refund` 的 request body 為空 `{}`。

### UploadService

```swift
nonisolated struct UploadService: Sendable {
    let client: HTTPClient
    func upload(imageData: Data, folder: UploadFolder, authManager: AuthManager) async throws -> String
    func uploadWithProgress(imageData:folder:authManager:onProgress:) async throws -> String
    func uploadWithRetry(imageData:folder:authManager:maxRetries:onProgress:) async throws -> String
    static func compressImage(_ image: UIImage, maxBytes: Int = 5_000_000, aggressive: Bool = false) -> (data: Data, image: UIImage)?
    static func resizeImage(_ image: UIImage, maxDimension: CGFloat = 1920) -> UIImage
}
```

> `upload()` 呼叫 `HTTPClient.upload()` multipart POST，回傳 S3 圖片 URL。`uploadWithProgress()` 使用 `HTTPClient.uploadWithProgress()` 搭配 `UploadProgressDelegate` 回報即時上傳進度。`uploadWithRetry()` 包裝 `uploadWithProgress()`，最多重試 2 次（指數退避 2s/4s），僅 `APIError.networkError` 觸發重試。`compressImage()` 先 resize 再 JPEG 漸進壓縮確保 ≤ 5MB；`aggressive: true`（行動網路）使用 1024px / quality 0.5 起始，`false`（Wi-Fi）使用 1920px / quality 0.8 起始。`UploadFolder` enum：`.spots`、`.vehicles`、`.itineraries`。

---

## ViewModel 層

所有 ViewModel 皆為 `@Observable final class`（隱式 `@MainActor`），在 View 以 `@State` 持有。

### ExploreViewModel

```
Constructor：init(service:spotService:userService:authManager:)
State：itineraries, nearbySpots, favoriteSpotIDs(Set<Int>), selectedCategory(SpotCategory?), filterMySpots(Bool), isLoading, isLoadingMore, isLoadingSpots, hasMorePages, error
Computed：filteredSpots([Spot])
Actions：loadItineraries(), loadMore(), refresh(), searchNearbySpots(lat:lng:radius:), loadFavorites()
```

`filteredSpots` 為純 client-side 過濾，支援 category × mySpots 雙重疊加：先以 `selectedCategory` 過濾分類，再以 `filterMySpots` + `authManager.currentUser.id` 過濾 `creatorId`。未登入時 `userId` 為 nil，mySpots 過濾自動跳過。ExploreView 登出時重置 `filterMySpots = false`。

`searchNearbySpots` 為同步方法（fire-and-forget），內部使用 `Task` + 500ms debounce（`Task.sleep`）防止快速滑動地圖時大量 API 請求。每次呼叫先 cancel 前一個 `searchTask`，並在 sleep 後檢查 `Task.isCancelled`。

`loadFavorites()` 呼叫 `UserService.getUserFavorites(id:authManager:)` 取得當前使用者收藏的景點 ID 集合，供地圖 Annotation 愛心 badge 使用。未登入時清空 `favoriteSpotIDs`。ExploreView 在三個時機呼叫：(1) `.task` 初始化；(2) `authManager.isAuthenticated` 變更（登入載入、登出清空）；(3) SpotDetailView sheet dismiss（收藏切換後同步）。

### ItineraryDetailViewModel

```
State：detail, isLoading, error, isCopying, copiedItinerary?(Itinerary), copyError
Computed：didCopy(Bool, copiedItinerary != nil)
Actions：load(), copy(authManager:)  // v1.3：不再需要 userID
```

純業務邏輯 ViewModel，不含地圖相關方法（地圖計算移至 `ItineraryDetail+MapHelpers` extension）。`ItineraryDetailView` 將 `viewModel.isCopying` / `viewModel.didCopy` 作為值傳入 `ItineraryMapView`。

> **v0.9 變更**：`didCopy: Bool` 改為 `copiedItinerary: Itinerary?`（stored），`didCopy` 變為 computed property（`copiedItinerary != nil`）。`copy()` 方法儲存 `ItineraryService.copy()` 回傳的 `Itinerary`，以取得複製後的行程 ID 供包車預約使用（連結至使用者自己的副本而非原始行程）。

### MyItinerariesViewModel

```
Constructor：init(itineraryService:userService:services:)
State：itineraries（初始值從 services.myItineraries 讀取）, isLoading, isCreating, error, createError
Actions：load(userID:authManager:), create(title:destination:totalDays:isPublic:coverImageURL:authManager:), delete(itinerary:authManager:)
```

`load(userID:authManager:)` 採用 **MERGE 策略**而非完全替換：
1. 向 API 取得已認證的行程列表（`fetched`）
2. 取 `services.myItineraries` 中不在 `fetched` ID 集合內的項目（`localOnly`，含本地建立但 API 未回傳的私人行程）
3. 合併為 `localOnly + fetched`
4. 同步回寫 `services.myItineraries`

`create()` 與 `delete()` 操作後同步更新 `services.myItineraries`。

`MyItinerariesView` 在 `isAuthenticated` 變為 `false`（登出）時，清空 `services.myItineraries = []`。

### EditItineraryViewModel

```
State：detail, isLoading, isSaving, error, saveError
Actions：load(), updateMeta(title:destination:isPublic:coverImageURL:authManager:), publish(authManager:),
         addDay(), updateDay(), deleteDay(),
         addSpot(dayID:spot:authManager:), updateSpot(dayID:spotID:arrivalTime:departureTime:durationMinutes:note:authManager:),
         removeSpot(), reorderSpots()
```

`updateMeta()` 使用 batch mutation 模式（copy to local var → mutate → assign back）避免多次 `@Observable` 通知觸發 SwiftUI 重新計算。`updateSpot()` 呼叫 `ItinerarySpotService.update()`，用 `NSNull()` 清除空欄位。更新本地 `detail.days[].spots[]` 時保留嵌套 `Spot` 物件（API response 可能不含）。

### ProfileViewModel

```
State：favorites, isLoadingFavorites, isUpdatingProfile, favoritesError, updateProfileError
Actions：loadFavorites(userID:), updateProfile(userID:name:avatarURL:authManager:), removeFavorite(spot:userID:authManager:)
```

### SpotSearchViewModel

```
State：spots, keyword, selectedCategory, filterMySpots, useLocation, searchRadius, isLoading, hasSearched
Properties：authManager(AuthManager?), displayedSpots(computed)
Actions：search(), clearFilters(), selectCategory(_:), toggleMySpots()
```

`selectCategory(_:)` 邏輯：取消最後一個 filter 且 keyword 為空時，重置 `hasSearched = false` 回到分類 Grid 狀態。

`filterMySpots` 為正交維度篩選（owner filter × category filter 可同時啟用）。`displayedSpots` 在 `filterMySpots` 啟用時以 `spot.creatorId == currentUser.id` 過濾。`toggleMySpots()` 切換時自動觸發搜尋。

### UserProfileViewModel

```
Constructor：init(userID:userService:)
State：user, itineraries, isLoading, error
Actions：load()
```

`load()` 使用 `async let` 平行呼叫 `getUser(id:)` + `getUserItineraries(id:)` 取得使用者資料及公開行程列表。

### AddToItineraryViewModel

```
Constructor：init(spotID:userService:itineraryService:dayService:itinerarySpotService:)
State：step: Step, createError: String?
Callback：onItineraryCreated: ((Itinerary) -> Void)?
Actions：loadItineraries(userID:authManager:), selectItinerary(_:), addToDay(_:authManager:),
         startCreatingItinerary(), cancelCreateItinerary(),
         createItinerary(title:destination:totalDays:isPublic:authManager:)
```

`Step` state machine：
```
loadingItineraries → selectItinerary([Itinerary])
                   ↕ startCreatingItinerary() / cancelCreateItinerary()
                   → createItinerary
                   → loadingDays(Itinerary)
                   → selectDay(Itinerary, [ItineraryDay])
                   → adding → success(dayNumber) / error(String)
```

`createItinerary(...)` 流程：
1. 呼叫 `ItineraryService.create` 取得新行程
2. 依 `totalDays` 逐一呼叫 `DayService.create` 建立天數
3. 將新行程插入 `cachedItineraries` 前端
4. 切換至 `selectItinerary` 步驟
5. 觸發 `onItineraryCreated?(newItinerary)` callback

`AddToItinerarySheet` 設定 callback，將新行程寫入 `services.myItineraries`，供 `MyItinerariesViewModel` 在下次初始化時讀取。

### VehicleListViewModel

```
State：vehicles, isLoading, error, selectedType?(VehicleType), showAvailableOnly(true)
Actions：load(), refresh()
```

`load()` 與 `refresh()` 共用 `fetchVehicles()` 私有方法。`load()` 有 `guard !isLoading` 防止重複觸發。

### OrderListViewModel

```
State：orders, isLoading, isLoadingMore, hasMorePages, error, statusFilter?(OrderStatus)
Actions：load(authManager:), loadMore(authManager:)
```

分頁邏輯同 `ExploreViewModel` pattern：首次 `load()` offset=0 替換；`loadMore()` offset=orders.count 追加。`statusFilter` 變更時重新 `load()`。

### OrderDetailViewModel

```
State：order?, isLoading, error, isPaying, isCancelling, isRefunding, actionError
Actions：load(authManager:), pay(authManager:), cancel(authManager:), refund(authManager:)
```

每個 action（pay/cancel/refund）成功後自動 `reload` detail 取得最新狀態。`actionError` 獨立於 `error`，用 `.alert` 顯示。觸覺回饋依 `actionError == nil` 判斷 success 或 error。

### CharterBookingViewModel

```
Step enum：fillForm → review → submitting → success(Order) / error(String)
表單：vehicle, pickupLocation, dropoffLocation, pickupDate, pickupTime, days, passengerCount, contactName, contactPhone, specialRequests
行程連結：itineraryID?(Int), itineraryTitle?(String)
初始化器：
  - init(vehicle:service:) — 獨立包車流程（Charter Tab）
  - init(vehicle:service:context:CharterItineraryContext) — 行程連結包車（預填 days/pickupLocation/dropoffLocation）
驗證：showValidation flag（點擊確認後才顯示錯誤）
  - pickupLocationError：上車地點不可空白
  - contactNameError：聯絡人姓名不可空白
  - contactPhoneError：聯絡電話不可空白
  - pickupDateTimeError：合併日期+時間不可早於現在
  - isValid：以上四項皆通過
  - 所有文字欄位驗證時 trim whitespace
計算：estimatedTotal(pricePerDay × days), combinedPickupDate(private, 合併日期+時間為 Date)
Actions：proceedToReview()（設 showValidation=true，isValid 才進 review）, backToForm(), submit(authManager:)
```

`submit()` 將表單資料組裝為 `[String: Any]` charter dict（所有文字欄位送出前 trim），合併日期+時間為 ISO 8601 格式（`yyyy-MM-dd'T'HH:mm:ss`），呼叫 `OrderService.create(orderType:itineraryID:charter:authManager:)`。若 `itineraryID` 不為 nil，訂單將連結至對應行程。

---

## View 層

### 導航入口

| View | 說明 |
|------|------|
| `AuthGateView` | 啟動時 restore session；使用 `@State hasRestored`（非 `authManager.isLoading`）控制 splash → MainTabView 切換，避免登入過程中 `isLoading` 重置導致 MainTabView 被銷毀（連帶 sheet 消失）。Splash 風格畫面（app icon + "fungo" 品牌文字 + ProgressView），帶 `.transition(.opacity)` + `.animation(.easeInOut(duration: 0.3))` 平滑切換 |
| `LoginView` | Apple 風格登入頁：全螢幕置中 `VStack(spacing:40)`，icon（`map.fill`）+ 自訂 `GoogleButton`；無 Spacer |
| `MainTabView` | 四個 Tab（探索、我的行程、包車、個人） |

#### LoginView 設計細節
- 整體佈局：`VStack(spacing: 40) { icon; buttonArea }.frame(maxWidth: .infinity, maxHeight: .infinity).padding(.horizontal, 32)`
- `GoogleButton`：`internal` 存取層級（ExploreView 登入 sheet 共用），`.buttonStyle(.plain)`，內部 `HStack { GoogleGMark; Text }` 加 `.frame(maxWidth: .infinity, alignment: .center)`；高 50pt，`systemBackground` 背景，`cornerRadius: 12`，輕陰影
- `GoogleGMark`：`internal` 存取層級，SwiftUI `Canvas` 繪製 Google 四色環形標誌（藍、紅、黃、綠四段弧 + 白色橫槓 cutout + 白色內圓），尺寸 22×22pt
- 不再 `import GoogleSignInSwift`，不使用 `GoogleSignInButton`

### 探索 Tab

| View | 說明 |
|------|------|
| `ExploreView` | MapKit 地圖（初始中心 25°N 121.5°E，span 1°），safeAreaInset 底部行程區塊，TopOverlay 搜尋按鈕 + 重新整理按鈕（附 `.accessibilityLabel("重新整理行程列表")`）。`.task` 初始化使用 `viewModel?.`（optional chaining）避免 force-unwrap crash。**分類 chips 列**：水平 ScrollView，已登入時首位顯示「我的景點」indigo chip（`filterMySpots` toggle）+ 垂直 `Divider`（height: 20）+ 分類 chips（`CategoryChipButton`），與 SpotSearchView chip 樣式一致。**右側浮動按鈕**（trailing 16pt，bottom 16pt above safeAreaInset）：定位按鈕（44×44 `.regularMaterial` Circle，點擊透過 `LocationManager.requestLocation()` 取得座標後 animate 地圖至 0.05° span）+ 建立景點 FAB（56×56 accentColor Circle）。**用戶位置藍點**：定位成功後在地圖顯示自訂 Annotation（32pt 淺藍光暈 + 14pt 藍色圓心 + 2pt 白色描邊），z-order 在所有景點 annotation 之下，`.allowsHitTesting(false)` 不攔截點擊。**長按手勢**（0.5s）建立景點：已登入 → `CreateSpotSheet`（帶入經緯度）；未登入 → 登入 sheet（`GoogleButton`，登入成功後 sheet 內容自動切換為 `CreateSpotSheet`，無需重新操作）。地圖 pin 在 sheet dismiss 時清除。SpotDetailView sheet 帶入 `initialIsFavorited`，dismiss 時重新載入 favorites 以同步愛心 badge |
| `CompactItineraryCard` | 橫式緊湊卡片（寬 240px，高 ~88px）：72px 封面圖 + 標題 / 目的地 / 作者 / 天數 / 複製數 / 官方標章 |
| `ItineraryCardView` | 直式完整卡片（160px 封面圖 + 文字區），供 MyItineraries 等頁面使用 |
| `SpotAnnotationView` | 地圖 Annotation：36px 彩色圓圈 + SF Symbol，顏色使用 `SpotCategory.color`。支援三種差異化標記：**我的景點**（`isMySpot`）顯示 42px indigo 外環（3pt 環寬）；**收藏景點**（`isFavorite`）顯示右上角紅色愛心 badge（白色圓底）；兩者可疊加。參數皆有 default `false`，向下相容。附加 `.accessibilityLabel` + `.accessibilityHint("點兩下查看詳情")` |
| `ItineraryDetailView` | 行程詳情（唯讀）：全螢幕 `ItineraryMapView`（地圖 + 日期篩選 + 路線 + 編號標記 + 自訂底部面板 overlay）。`@State authorUserID: Int?` + `.navigationDestination(item: $authorUserID)` → `UserProfileView` 處理作者導航。`charterContext(for:)` helper 根據身份選擇行程 ID（擁有者用原始 ID，複製者用 `copiedItinerary.id`）。未登入點擊複製彈出 `LoginView` sheet（登入成功後自動 dismiss） |
| `ItineraryMapView` | 全螢幕行程地圖（共用元件），接受 `detail: ItineraryDetail` + `isOwner` + `isCopying/didCopy/showActions`（含預設值）+ `onCopy/onCharter` 閉包 + `onAuthorTap: ((Int) -> Void)?` 回呼。頂部 `daySelectorBar`（`FilterChip` 日期篩選 + 總覽，底部 `Divider()` 分隔），MapKit `Map` 含天數色路線（`MapPolyline`）+ `NumberedSpotMarker` 編號標記（附 `.accessibilityLabel`），底部 `.overlay(alignment: .bottom)` 內嵌 `DraggableBottomPanelContainer`（自訂可拖曳面板，取代系統 sheet），面板包含 `ItinerarySpotPanelView`。選取標記時 camera animate 至該景點（`MKCoordinateSpan 0.01`）。**效能**：`let allSpots = detail.allMappableSpots` 一次計算後本地篩選，避免 N+1 重算。**選取清除**：切換天數時 `selectedSpotID = nil` 防止 ghost selection |
| `ItinerarySpotPanelView` | 自訂面板內容：行程 meta header（標題/目的地/天數/複製數/作者 — 作者透過 `onAuthorTap: ((Int) -> Void)?` 回呼導航）+ 天數 Section（`PanelSpotRow` 附帶序號色圓 + 名稱 + 地址，點擊更新 `selectedSpotID`，選中列高亮 `Color.accentColor.opacity(0.1)`）。使用 `detail.daysForIndex()` 集中篩選。`.scrollContentBackground(.hidden)` + `.contentMargins(.vertical, 8)` 配合面板背景。`PanelSpotRow` 附 `.accessibilityElement(children: .combine)` + `.accessibilityAddTraits(.isSelected)` |
| `SpotDetailLoadingView` | 接受 `spotID: Int`，呼叫 `SpotService.getDetail(id:)` 載入景點，三態（loading / error+重試 / SpotDetailView） |
| `SpotCardView` | 景點橫式卡片：縮圖、名稱、地址、類別、距離 |
| `SpotDetailView` | 景點詳情：地圖、地址、收藏按鈕（toolbar，附 `.accessibilityLabel`）；收藏切換成功觸發 `HapticManager.impact(.light)`，失敗顯示 `.alert("操作失敗")`；「加入行程」按鈕永遠顯示，未登入點擊彈 Alert 引導登入（登入成功後自動 dismiss LoginView sheet）。支援 `initialIsFavorited` 參數（ExploreView 從 `favoriteSpotIDs` 帶入）。**景點擁有者**（`creatorId == currentUser.id`）toolbar 顯示「...」Menu（編輯 → `EditSpotInfoSheet`、刪除 → `confirmationDialog`） |
| `SpotSearchView` | Modal 搜尋：開啟時顯示 2欄分類 Grid（已登入時含「我的景點」indigo 格子）；選分類或輸入關鍵字後切換為 chips filter + 結果列表；已登入時 chip 區首位顯示「我的景點」indigo chip，與分類 chips 之間有垂直 `Divider`（height: 20）視覺分隔；取消最後 filter 時回到 Grid。`@FocusState` 鍵盤管理：搜尋送出時自動收起鍵盤 |
| `AddToItinerarySheet` | 景點加入行程 sheet，依 `Step` 切換畫面（見下方） |

#### AddToItinerarySheet 畫面狀態
- `.loadingItineraries`：全螢幕 ProgressView
- `.selectItinerary([])` 空列表：`ContentUnavailableView` + 「建立新行程」按鈕
- `.selectItinerary([...])` 有列表：行程 List，各列顯示標題 / 目的地 / 天數；右下方「+ 新增行程」選項
- `.createItinerary`：內嵌 `CreateItineraryForm`（不是新 sheet），標題 / 目的地 / 天數 Stepper / 公開 Toggle；頂部「返回」按鈕回到行程列表
- `.loadingDays`：全螢幕 ProgressView，navigationTitle 顯示行程名
- `.selectDay`：天數 List，各列顯示第 N 天 / 日期 / 景點數
- `.adding`：全螢幕 ProgressView
- `.success`：成功畫面（綠色 checkmark + 說明文字 + 完成按鈕）
- `.error`：錯誤畫面（橘色警告圖示 + 重試按鈕）

### 我的行程 Tab

| View | 說明 |
|------|------|
| `MyItinerariesView` | 行程列表（`.listStyle(.plain)`），swipe-to-delete 帶 `.confirmationDialog` 二次確認，刪除成功觸發 `HapticManager.notification(.warning)`；右上角 + 開啟建立 sheet；每列顯示標題 / 目的地 / 天數 / 公開狀態；空狀態 `ContentUnavailableView` + "建立行程" CTA 按鈕（→ `showCreate`）；錯誤狀態 `ContentUnavailableView` + 重試按鈕；登出時清空 `services.myItineraries` |
| `CreateItinerarySheet` | 建立行程 Modal：標題（必填）、目的地、封面圖片（`ImagePickerUploadView`）、天數 Stepper（1-30）、公開 Toggle（預設 **關閉**）；接收 `viewModel: MyItinerariesViewModel`（非 async closure），建立成功觸發 `HapticManager.notification(.success)`；`@State imageUploading` 控制建立按鈕 disabled |

### 行程編輯

| View | 說明 |
|------|------|
| `EditItineraryView` | 雙模式切換（`@State showMap`）：**列表模式** — meta 區塊（編輯按鈕）、發佈按鈕、**包車預約入口**（橘色「為此行程預約包車」Label + footer 提示→ `ItineraryCharterSheet`）、天數列表（swipe edit/delete 帶 `.confirmationDialog` 二次確認 + `HapticManager`、新增天按鈕），push 到 `EditDaySpotsView`。**地圖模式** — `ItineraryMapView(isOwner: true, onCharter:, onAuthorTap:)`，右側浮動 overlay 按鈕（`list.bullet` 切回列表 + `slider.horizontal.3` 設定，36×36 `.ultraThinMaterial` Circle，頂部 52pt 偏移避開 daySelectorBar）。`@State authorUserID: Int?` + `.navigationDestination(item: $authorUserID)` → `UserProfileView` 處理面板作者導航。**Toolbar 條件隱藏**：列表模式顯示 map/settings 按鈕；地圖模式隱藏（改用 overlay 替代） |
| `EditDaySpotsView` | 單日景點編輯：點擊景點列開啟 `EditSpotSheet`；左滑刪除帶 `.confirmationDialog` 二次確認，移除成功觸發 `HapticManager.notification(.warning)`；左上角「排序」按鈕進入拖曳模式（附 `.accessibilityHint` 描述目前/可切換模式）；右上角 + 新增景點；空狀態 `ContentUnavailableView` + "新增景點" CTA 按鈕（→ `showAddSpot`） |
| `EditSpotSheet` | 編輯景點時間/備註：到達/離開時間使用 `DatePicker(.hourAndMinute)` + `Toggle` 控制是否設定；停留時間 TextField＋分鐘後綴；備註多行 TextField；接收 `viewModel: EditItineraryViewModel` + `dayID` + `spotID`（非 async closure）；儲存成功觸發 `HapticManager.notification(.success)` |
| `EditDaySheet` | 編輯天：日期 TextField、備註 TextEditor；接收 `viewModel: EditItineraryViewModel` + `authManager`（非 async closure），按 mode（.add/.edit）呼叫 addDay/updateDay；儲存成功觸發 `HapticManager.notification(.success)` |
| `EditItineraryMetaSheet` | 編輯標題、目的地、封面圖片（`ImagePickerUploadView`）、公開 Toggle；接收 `viewModel: EditItineraryViewModel`（非 async closure）；`@State imageUploading` 控制儲存按鈕 disabled；儲存成功觸發 `HapticManager.notification(.success)` |
| `AddSpotSheet` | 搜尋 + 新增景點：關鍵字 / 類別篩選（已登入時含「我的景點」indigo chip + `Divider` 分隔），景點列表 + 新增按鈕；接收 `viewModel: EditItineraryViewModel`（非 async closure）。`@FocusState` 鍵盤管理：搜尋送出 / toolbar 搜尋按鈕時自動收起鍵盤。`addSpotToDay(_:)` 統一方法處理新增景點（create-then-add 與 inline-add 共用），成功觸發 `HapticManager.notification(.success)`。`CreateSpotSheet` 可從空搜尋結果或列表底部開啟 |

### 包車 Tab

| View | 說明 |
|------|------|
| `CharterTabView` | Tab root，NavigationStack + VehicleListViewModel。頂部水平捲動 `FilterChip` 車型篩選（全部 + 3 種車型）+ 右側可用車輛 Toggle。LazyVGrid 2 欄 `VehicleCardView`。pull-to-refresh。loading / error（`ContentUnavailableView` + 重試按鈕）/ empty（`ContentUnavailableView`）三態。`navigationDestination` push 至 `VehicleDetailView` |
| `VehicleCardView` | 車輛卡片：`HeroImageView`（150px）+ 名稱 + `VehicleTypeBadge` + 價格（NT$）+ 容量 + 可用狀態綠/灰圓點。`.cardStyle(cornerRadius: 16)` + accessibility |
| `VehicleDetailView` | ScrollView：`HeroImageView`（240px）、名稱、VehicleTypeBadge、capacity、pricePerDay、description。`safeAreaInset(.bottom)` 固定「立即預約」CTA 按鈕（`.bottomBarBackground()`），附 `.accessibilityHint`（可用："點擊開始預約包車" / 不可用："此車輛目前不可預約"）。未登入 → alert 提醒登入（登入成功後自動 dismiss LoginView sheet）。已登入 → sheet 開啟 `CharterBookingSheet` |
| `CharterBookingSheet` | 多步驟預約 sheet（見下方） |

#### CharterBookingSheet 畫面狀態
- `.fillForm`：Form — 車輛資訊、上車/下車地點 TextField、出發日期 DatePicker（`in: Date()...`）、出發時間 DatePicker、天數 Stepper(1-30)、乘客人數 Stepper(1-capacity)、聯絡人姓名+電話、備註 TextField、預估金額、「確認預約資訊」按鈕（永遠可按，點擊觸發驗證）
  - 表單驗證：點擊按鈕後 `showValidation = true`，未通過的必填欄位下方顯示紅色 `.caption` 錯誤提示（上車地點、聯絡人姓名、聯絡電話、出發時間），驗證失敗觸發 `HapticManager.notification(.warning)`；使用者輸入後錯誤即時消失
- `.review`：List 摘要確認 — 車款/車型/每日費用、上車地點/下車地點/出發日期/出發時間/天數/人數、聯絡人/電話/備註、總金額（橘色醒目）、「確認送出」+「返回修改」按鈕
- `.submitting`：ProgressView + "正在建立訂單..."，`.interactiveDismissDisabled` 防止下滑關閉
- `.success(Order)`：綠色 checkmark + 訂單編號 + 金額 + 完成按鈕，`HapticManager.notification(.success)`
- `.error(String)`：橘色警告 + 錯誤訊息 + 返回修改按鈕，`HapticManager.notification(.error)`
- Toolbar：fillForm/review 顯示「取消」，error 顯示「關閉」，submitting/success 隱藏

#### ItineraryCharterSheet（行程連結包車）

從行程詳情或編輯行程進入的包車預約流程，接受 `CharterItineraryContext` 參數。NavigationStack 內使用 `Phase` enum 管理多階段流程：

- **Phase 1 — vehicleSelection（車輛選擇）**：
  - 頂部行程 badge（橘色底，顯示行程名稱與天數）
  - 水平捲動車型篩選 chips（全部 + 3 種車型），使用 Capsule 外形
  - 2 欄 `LazyVGrid`，重用 `VehicleCardView`，僅顯示可用車輛（`isAvailable`）
  - 載入使用 `VehicleService.list(available: true)`，`guard vehicles.isEmpty` 防止回到此頁時重複 API 請求
  - Loading / Error（含重試）/ Empty 三態

- **Phase 2+ — booking（預約流程）**：
  - 使用 `CharterBookingViewModel(vehicle:service:context:)` 初始化，預填天數/上車/下車地點
  - 表單頂部顯示「連結行程」badge（行程名稱）
  - 「更換車輛」按鈕回到 Phase 1，**保留**使用者已填寫的表單資料（僅替換車輛，若 passengerCount 超過新車容量則 clamp）
  - 確認/送出/成功/失敗畫面與 `CharterBookingSheet` 相同結構
  - 成功畫面額外顯示「已連結行程「{title}」」提示文字

- **NavigationTitle**：Phase 1 為「選擇車輛」，Phase 2+ 為「預約包車」
- **Toolbar**：vehicleSelection 與 booking(fillForm/review/error) 顯示「取消」，submitting/success 隱藏
- **`.interactiveDismissDisabled`**：僅在 submitting 階段阻止下滑關閉

### 訂單管理

| View | 說明 |
|------|------|
| `OrderListView` | 水平 `FilterChip` 狀態篩選（全部 + 4 狀態），List + OrderRow（訂單編號、車輛名、金額、`OrderStatusBadge`、日期使用 `ISODateParser.formatDate` locale-aware 格式化）。`PaginationFooterView` 分頁、pull-to-refresh。loading / error（`ContentUnavailableView` + 重試按鈕）/ empty（`ContentUnavailableView` + 說明文字）三態。從 detail 返回時自動 refresh（`needsRefresh` + `onAppear`/`onDisappear` 模式）|
| `OrderDetailView` | Sections：訂單基本資訊（編號/類型/狀態/金額/日期，日期使用 `ISODateParser.formatDateTime` locale-aware 格式化）、**連結行程**（當 `order.itineraryID != nil` 時顯示 NavigationLink → `ItineraryDetailView`）、包車詳情（車輛/地點/時間/天數/人數/聯絡人）、付款紀錄（金額/方式/狀態/時間，中文化顯示）。錯誤狀態使用 `ContentUnavailableView` + 重試按鈕。底部 action buttons 依訂單狀態顯示：pending → 「付款」（borderedProminent）+「取消訂單」（red bordered）；confirmed → 「申請退款」（red bordered）；completed/cancelled → 無按鈕。破壞性操作使用 `.confirmationDialog` 二次確認 + HapticManager |

> `OrderDetailView` 內建 `localizedPaymentStatus` 與 `localizedPaymentMethod` 靜態方法，將 API 回傳的英文狀態/方式（"paid"/"mock" 等）轉為中文顯示。

### 個人 Tab

| View | 說明 |
|------|------|
| `ProfileView` | 頭像、名稱、email；**「我的訂單」Section**（NavigationLink → `OrderListView`，Label("我的訂單", systemImage: "doc.text")）；收藏景點列表（swipe-to-remove 帶 `.confirmationDialog` 二次確認，取消收藏成功觸發 `HapticManager.impact(.light)`），登出按鈕；未登入狀態直接回傳 `LoginView()` |
| `UserProfileView` | 其他使用者個人頁：頭像（64px 圓形）+ 名稱 + email；公開行程列表（NavigationLink → `ItineraryDetailView`）；pull-to-refresh；loading / error / empty 狀態 |
| `EditProfileSheet` | 編輯顯示名稱 + 大頭照（`ImagePickerUploadView`）；接收 `viewModel: ProfileViewModel`（非 async closure）；`@State imageUploading` 控制儲存按鈕 disabled；儲存成功觸發 `HapticManager.notification(.success)` |

### 共用元件

| 元件 | 說明 |
|------|------|
| `DraggableBottomPanelContainer` | 自訂可拖曳底部面板（取代系統 `.sheet()`）。`PanelDetent` 三段吸附（`.collapsed` 180pt / `.medium` 50% / `.large` 近全螢幕）。`DragGesture(coordinateSpace: .global)` 避免底部對齊 jitter。velocity-based 方向吸附（快速 flick → 相鄰 detent）+ 慢拖 → 最近 detent。iOS 風格橡皮筋阻力（rubber-band）防止超出邊界。`.spring(response: 0.4, dampingFraction: 0.86)` 彈性動畫。`.ultraThinMaterial` 背景 + 圓角頂部 + 上方陰影。detent 切換觸發 `HapticManager.impact(.light)`。用於 `ItineraryMapView` |
| `CachedAsyncImage` | Phase-based 圖片載入：`.empty`（灰底 photo icon）→ `.loading`（`ShimmerView`）→ `.loaded`（fade-in 動畫）/ `.failed`（警告 icon）。cache hit 不動畫直接顯示。使用 `ImageCache.shared.load()` 去重並行下載 |
| `ShimmerView` | 水平漸層掃過動畫佔位效果，用於 `CachedAsyncImage` loading 狀態 |
| `HeroImageView` | `CachedAsyncImage` + 可選 `LinearGradient` 遮罩；預設高度 240px，可自訂 `height`、`gradientHeight`、`gradientOpacity`、`gradientStart`。用於 `VehicleCardView`、`VehicleDetailView`、`SpotDetailView`、`ItineraryDetailView` 等 |
| `CardStyle` | `.cardStyle(cornerRadius:)` ViewModifier：`systemBackground` 背景 + `RoundedRectangle` 裁切 + 輕陰影。用於 `VehicleCardView`、`CompactItineraryCard`、`SpotCardView`、`ItineraryCardView` 等 |
| `BottomBarBackground` | `.bottomBarBackground()` ViewModifier：padding + `ultraThinMaterial` 背景。用於 `VehicleDetailView`、`SpotDetailView`、`ItineraryDetailView` 底部固定 CTA |
| `ImagePickerUploadView` | 整合 `PhotosPicker` + `UploadService`（壓縮 + 上傳 + 進度 + 重試）+ 預覽。接收 `folder`、`uploadService`、`authManager`、`imageURL: Binding<String>`、`isUploadInProgress: Binding<Bool>`。上傳中顯示半透明進度條覆蓋圖片（`ProgressView(value:)` + 百分比文字），表單其他欄位可繼續填寫。使用 `NetworkMonitor.shared.isCellular` 決定壓縮策略。`.onDisappear` 自動取消上傳 Task。用於 `EditProfileSheet`、`CreateItinerarySheet`/`EditItineraryMetaSheet`、`CreateSpotSheet`/`EditSpotInfoSheet` |
| `LoadingStateView` | Loading spinner / Error（含重試按鈕）/ Empty 三態 |
| `CategoryBadge` | Pill 形狀，圖示 + 類別名稱，使用 `SpotCategory.color` 統一配色；附 `.accessibilityLabel(category.displayName)` |
| `PaginationFooterView` | 不可見 trigger，出現在可視範圍時觸發 `loadMore()` |
| `FilterChip` | 共用篩選標籤元件：選中時填色 + 白字，未選中時灰底 + 原色字；`.capsule` 外形，`.padding(.vertical, 10)` 觸控目標，`HapticManager.selection()` 回饋，`.accessibilityAddTraits(isSelected ? .isSelected : [])` 選取狀態。用於 `CharterTabView` 車型篩選、`OrderListView` 狀態篩選、`ItineraryMapView` 天數篩選 |
| `NumberedSpotMarker` | 地圖用數字標記：天數色彩圓圈 + 白字序號，選中態放大至 36pt + 陰影。顏色取自 `DayColorPalette.color(for:)`。附 `.accessibilityLabel(String(localized: "景點 \(number)"))` |
| `DayColorPalette` | 8 色循環色盤（red/blue/green/orange/purple/teal/pink/indigo），`color(for dayNumber:)` 靜態方法。用於 `ItineraryMapView` 路線 + `NumberedSpotMarker` 標記 + `ItinerarySpotPanelView` 序號圓點 |
| `VehicleTypeBadge` | 同 `CategoryBadge` pattern：pill 形狀 + SF Symbol + 車型名稱，顏色取自 `VehicleType.color` |
| `OrderStatusBadge` | 同 `CategoryBadge` pattern：pill 形狀 + SF Symbol + 狀態名稱，顏色取自 `OrderStatus.color` |

---

## 認證流程

### Google OAuth 流程

```
1. User tap → LoginView → GoogleButton
2. GoogleSignInBridge.signIn(presenting:)
   → 設定 GIDConfiguration(serverClientID: webClientID)  // 確保 id_token.aud 符合後端 GOOGLE_CLIENT_ID
   → GIDSignIn.sharedInstance.signIn(with:presenting:)
   → 取得 idToken
3. AuthManager.signInWithGoogle(idToken:)
   → AuthService.googleLogin(idToken:)
   → POST /api/auth/google { id_token }
   → 回傳 { user, accessToken, refreshToken }
4. AuthManager 儲存至 Keychain：
   - "accessToken"
   - "refreshToken"
   - "currentUser" (JSON)
5. currentUser 設定 → AuthGateView 切換至 MainTabView
```

### Token 自動刷新（401 Retry）

```
HTTPClient.request() → 401 → authManager.refreshAccessToken()
  → POST /api/auth/refresh { refreshToken }
  → 新 accessToken + refreshToken（舊 refreshToken 立即失效）
  → 更新 Keychain
  → 重試原始請求（僅一次）
```

### 登出

```
AuthManager.logout()
  → POST /api/auth/logout { refreshToken }
  → 清除 Keychain（accessToken、refreshToken、currentUser）
  → currentUser = nil → AuthGateView 切換至 LoginView
```

### Session Restore

```
AuthGateView.task → AuthManager.restoreSession()
  → 從 Keychain 讀取 accessToken、refreshToken、currentUser
  → 若存在則恢復登入狀態
  → hasRestored = true → 切換至 MainTabView
```

> **注意**：AuthGateView 使用 `@State hasRestored`（僅在 `restoreSession()` 完成後設為 `true`）控制 splash → MainTabView 切換，而非 `authManager.isLoading`。這避免了從子頁面（如 ItineraryDetailView、SpotDetailView）彈出 LoginView sheet 登入時，`signInWithGoogle()` 設定 `isLoading = true` 導致 MainTabView 被銷毀、sheet 提前消失的問題。

---

## 網路層

### HTTPClient（actor）

```swift
actor HTTPClient {
    private let session: URLSession      // timeout 10s request / 20s resource
    private let decoder: JSONDecoder     // 預設設定
    private let acceptLanguage: String   // init 時快取，每次請求重用
}
```

**請求流程**：
1. `prepareRequest(endpoint:token:)` 組裝 `URLRequest`，附加 `Accept-Language`（快取值）與 `Authorization` header
2. **若傳入 `authManager` 且 token 存在，一律附加 `Authorization: Bearer <accessToken>`**（不論 `endpoint.requiresAuth`）
3. 執行 `session.data(for:)`
4. HTTP 401 + `endpoint.requiresAuth` + `authManager` 存在 → refresh token → 重試一次（重試時同樣附加新 token）
5. `decodeResponse<T>()` → `JSONDecoder.decode(T.self, from: data)`
6. 失敗拋出 `APIError`（typed throws）

**Multipart 上傳**：`upload(imageData:folder:authManager:)` 組裝 `multipart/form-data` body（folder 欄位 + file 欄位），per-request timeout 60s（session-level 維持 20s），內建 401 refresh retry。`Data(capacity:)` 預分配避免重複 buffer realloc。`buildMultipartBody()` 私有方法統一組裝 multipart body，被 `upload()` 與 `uploadWithProgress()` 共用。

**帶進度上傳**：`uploadWithProgress(imageData:folder:authManager:onProgress:)` 將 multipart body 寫入暫存檔，使用獨立 `URLSession` + `UploadProgressDelegate`（`URLSessionTaskDelegate`）透過 `urlSession(_:task:didSendBodyData:...)` 回報上傳位元組進度。上傳完成後 `defer` 自動清理暫存檔並 `finishTasksAndInvalidate()` 釋放 session。同樣內建 401 refresh retry。

> **注意**：token 附加策略與 401 retry 觸發條件不同。token 附加僅需 authManager 不為 nil；retry 還需 `endpoint.requiresAuth == true`。

> **Accept-Language**：`init` 時從 `Locale.preferredLanguages` 取前 3 個語言組成 header（含 quality factor），快取為 `acceptLanguage` 屬性。`prepareRequest()` 每次請求直接使用快取值，不重複計算。後端根據此 header 回傳對應語系的 API 內容（spots.name、itineraries.title 等 JSONB 欄位）。

### APIEndpoint（nonisolated enum）

核心 computed properties：

| Property | 說明 |
|----------|------|
| `path` | URL 路徑（不含 baseURL） |
| `method` | `HTTPMethod`（GET/POST/PUT/DELETE） |
| `requiresAuth` | 是否在 401 時觸發 token refresh retry |
| `queryItems` | GET 參數（listItineraries 的分頁、searchSpots 的篩選） |
| `body` | POST/PUT request body（`[String: Any]`，序列化為 JSON） |
| `urlRequest(baseURL:)` | 組裝完整 `URLRequest` |

> `copyItinerary(id:)` case 的 `body` 為空 `{}`（v1.3 起後端從 JWT Token 取得使用者身份）。
>
> v1.4 新增 10 個 case：`listVehicles(type:available:)`、`getVehicle(id:)`（公開）；`createOrder(orderType:itineraryID:charter:)`、`listOrders(status:limit:offset:)`、`getOrder(id:)`、`updateOrder(id:fields:)`、`cancelOrder(id:)`、`payOrder(id:)`、`getOrderPayments(id:)`、`refundOrder(id:)`（需認證）。`createOrder` body 包含巢狀 `"charter": {...}` dict。`cancelOrder`/`payOrder`/`refundOrder` body 為空 `{}`。
>
> v2.0 新增 `upload` case（POST /upload，需認證）。body 由 `HTTPClient.upload()` 組裝 multipart/form-data，不經 `APIEndpoint.body`。

### 錯誤處理策略

| 層級 | 處理方式 |
|------|---------|
| 網路錯誤 | `APIError.networkError(description)` |
| HTTP 4xx/5xx | `APIError.httpError(statusCode:message:)`，message 取自 `{ "error": "..." }` |
| Decode 失敗 | `APIError.decodingError(description)` |
| 401 | 自動 refresh + retry；無法 refresh 時拋 `.unauthorized` |
| ViewModel | 多數以 `isLoading` / `error` 狀態呈現；non-critical（nearbySpots）靜默失敗 |

---

## 工具與元件

### ImageCache

```swift
final class ImageCache: @unchecked Sendable {
    static let shared = ImageCache()
    // NSCache：countLimit 200、totalCostLimit 100 MB（cost = 像素面積 × 4 bytes）
    func image(for url: String) -> UIImage?
    func store(_ image: UIImage, for url: String)
    func load(url: String, from validURL: URL) async -> UIImage?  // 去重並行下載
}
```

> `load()` 使用 `NSLock` + `inFlight: [String: Task]` dictionary 去重同 URL 並行下載。多個 `CachedAsyncImage` 同時請求同一圖片時只發出一次網路請求。下載完成自動 `store()`。

### KeychainHelper

```swift
nonisolated enum KeychainHelper {
    static func save(_ value: String, forKey key: String)
    static func load(forKey key: String) -> String?
    static func delete(forKey key: String)
    static func saveData(_ data: Data, forKey key: String)
    static func loadData(forKey key: String) -> Data?
}
```

常用 Key：`"accessToken"`、`"refreshToken"`、`"currentUser"`

### LocationManager

```swift
@Observable
final class LocationManager: NSObject, CLLocationManagerDelegate {
    var lastLocation: CLLocationCoordinate2D?
    func requestLocation() async -> CLLocationCoordinate2D?
}
```

One-shot 定位工具。`requestLocation()` 先檢查授權（未決定時 `requestWhenInUseAuthorization()`），再呼叫 `CLLocationManager.requestLocation()`，透過 `CheckedContinuation` 回傳座標。`lastLocation` 在 `didUpdateLocations` 中更新（在 resume continuation 之前），供 ExploreView 顯示藍色圓點 Annotation。定位失敗不清除 `lastLocation`（舊位置保留）。同一時間僅允許一個 request in-flight（`locationContinuation != nil` 時直接回傳 nil）。

### HapticManager

```swift
enum HapticManager {
    static func impact(_ style: UIImpactFeedbackGenerator.FeedbackStyle = .medium)
    static func notification(_ type: UINotificationFeedbackGenerator.FeedbackType)
    static func selection()
}
```

全 App 統一觸覺回饋入口。使用時機：

| 操作 | 回饋類型 |
|------|---------|
| 收藏切換成功 | `.impact(.light)` |
| 複製行程成功 / 建立行程成功 / 儲存景點成功 / 預約包車成功 / 付款成功 / 編輯個人資料成功 / 編輯行程資訊成功 / 編輯天成功 / 新增景點到行程成功 | `.notification(.success)` |
| 刪除行程 / 刪除天數 / 移除景點 | `.notification(.warning)` |
| 收藏操作失敗 / 預約失敗 / 付款失敗 / 取消失敗 / 退款失敗 | `.notification(.error)` |
| 篩選標籤切換 | `.selection()` |
| 面板 detent 切換 | `.impact(.light)` |

### Date+Formatting

```swift
extension DateFormatter {
    static let apiDate: DateFormatter          // yyyy-MM-dd
    static let apiTime: DateFormatter          // HH:mm:ss
    static let displayDate: DateFormatter      // .medium dateStyle, locale: .current
    static let displayDateTime: DateFormatter  // .medium dateStyle + .short timeStyle, locale: .current
    static let displayTime: DateFormatter      // HH:mm, locale: .current
}

enum ISODateParser {
    static func parse(_ isoString: String) -> Date?         // 支援 with/without fractional seconds
    static func formatDate(_ isoString: String) -> String    // locale-aware 日期（e.g. "2025年3月9日"）
    static func formatDateTime(_ isoString: String) -> String // locale-aware 日期+時間（e.g. "2025年3月9日 下午2:30"）
}
```

`ISODateParser` 為共用 ISO 8601 字串解析 + locale-aware 格式化工具。使用兩個靜態 `ISO8601DateFormatter`（with/without fractional seconds）解析 API 回傳的時間字串，再用 `DateFormatter.displayDate` / `displayDateTime` 格式化為使用者 locale 的顯示格式。解析失敗時 fallback 截取原始字串前 10/19 字元。用於 `OrderListView`（日期）、`OrderDetailView`（建立時間、出發時間、付款時間）。注意：不加 `nonisolated`，保持 MainActor 隔離（`DateFormatter` / `ISO8601DateFormatter` 非 thread-safe）。

---

## 重要實作細節

### 1. DECIMAL 欄位 Flexible Decoding

**問題**：Node.js `pg` 驅動將 PostgreSQL `DECIMAL(10,8)` / `DECIMAL(11,8)` 序列化為字串（如 `"25.03363000"`），Swift `JSONDecoder` 預設無法從字串 decode `Double`，導致 decode 拋錯，`searchNearbySpots` 靜默失敗，地圖無 pins。

**解法**：`Spot.init(from:)` 使用 `decodeFlexibleDouble` helper，先嘗試 `Double`，失敗再嘗試 `String → Double`，涵蓋 `latitude`、`longitude`、`distanceKm`。

### 2. ExploreView 底部面板高度

`ScrollView(.horizontal)` 在 `safeAreaInset(.bottom)` 的 layout pass 會接受整個剩餘高度的 proposal，導致面板撐高至全螢幕。解法：對 `ScrollView` 加 `.frame(height: 96)` 明確鎖高。

### 3. 地圖初始 Pins

`onMapCameraChange(frequency: .onEnd)` 只在使用者移動地圖後才觸發。`.task` 初始化時呼叫 `searchNearbySpots(lat: 25.0, lng: 121.5, radius: 50.0)`（同步 fire-and-forget，內部 500ms debounce）與 `await loadItineraries()`，讓地圖一開啟就有景點 pins。

### 4. Typed Throws

`HTTPClient` 使用 `throws(APIError)` typed throws（Swift 5.10+），呼叫端不需 downcast error。

### 5. ItineraryDay / ItinerarySpot Dual-key CodingKey 模式

後端不同 API 端點對同一欄位使用不同 JSON key：

| 模型 | 建立 API key | 詳情 API key |
|------|------------|------------|
| `ItineraryDay.id` | `"id"` | `"day_id"` |
| `ItinerarySpot.id` | `"id"` | `"itinerary_spot_id"` |

**解法**：`CodingKeys` 宣告主 key（詳情用）與 `altId`（建立用）兩個 case，`init(from:)` 先用 `decodeIfPresent` 嘗試主 key，失敗（nil）再用 `decode` 強制讀 `altId`。必須同時實作手動 `encode(to:)`（合成 `Encodable` 無法處理有 `altId` 的 CodingKeys）。`ItinerarySpot` 另提供明確的成員逐一 `init`，供需要手動構建實例的場景使用。

### 6. ServiceContainer.myItineraries 跨 ViewModel 共用快取

**問題**：`AddToItinerarySheet` 建立新行程後，若 `MyItinerariesViewModel` 尚未存在（MyItineraries Tab 尚未被訪問），行程無法進入 `MyItinerariesViewModel.itineraries`，用戶切換到 Tab 2 後看不到新建行程。

**解法**：`ServiceContainer` 新增 `var myItineraries: [Itinerary] = []` 作為橋接快取：
- `AddToItineraryViewModel.onItineraryCreated` callback → `services.myItineraries.insert(newItinerary, at: 0)`
- `MyItinerariesViewModel.init` → `self.itineraries = services.myItineraries`（讀取快取初始化）
- `MyItinerariesViewModel.load()` → MERGE 策略，保留 `services.myItineraries` 中不在 API 回傳的項目
- `create()` / `delete()` → 同步回寫 `services.myItineraries`
- 登出時 → `services.myItineraries = []` 清空

### 7. 景點排序 API

後端使用負數暫存策略避免 `(itinerary_day_id, order_index)` 唯一約束衝突，iOS 端直接傳送 `{ "spots": [{"itinerary_spot_id": X, "order_index": Y}, ...] }` 陣列即可。

### 8. EditDaySpotsView 互動設計

景點列有三種操作模式：
- **點擊景點列** → 開啟 `EditSpotSheet`（編輯到達/離開時間、停留分鐘、備註）
- **左滑** → 紅色「移除」刪除景點（trailing swipe action）
- **點擊左上角「排序」** → 進入拖曳排序模式（自訂 Button 取代系統 `EditButton()`，避免與「編輯」語義混淆）

`EditSpotSheet` 到達/離開時間使用 `DatePicker(displayedComponents: .hourAndMinute)` + `Toggle` 控制是否設定（取代純文字 TextField，避免格式錯誤）。初始化時從 `"HH:mm:ss"` 字串解析為 `Date`，儲存時格式化回 `"HH:mm:ss"`。停留時間與備註仍使用 TextField。

### 9. NavigationLink in ScrollView 樣式

`ItineraryDetailView` 作者區塊的 `NavigationLink` 位於 `ScrollView`（非 `List`）中，SwiftUI 會將內部文字染為 accent color。需加 `.buttonStyle(.plain)` 保持原本 `.secondary` 灰色。

### 10. Itinerary 缺欄位 Flexible Decoding

`POST /itineraries` 建立回應不含 `is_official` 與 `copy_count` 欄位。`Itinerary` 實作自訂 `init(from:)`，使用 `try? c.decodeIfPresent(...)  ?? defaultValue` 模式，缺欄位時補 `false` / `0`，避免建立行程後 decode 失敗導致 App 狀態錯誤。

### 11. Decimal Flexible Decoding（金額欄位）

**問題**：後端 PostgreSQL `DECIMAL` / `NUMERIC` 欄位經 Node.js `pg` 驅動序列化可能為字串（如 `"5000.00"`）或數值（`5000`）。

**解法**：`Vehicle.pricePerDay`、`Order.totalAmount`、`Payment.amount` 三個金額欄位皆使用自訂 `init(from:)` 依序嘗試 `Decimal` → `String → Decimal`，失敗時 fallback 為 `0`。所有金額顯示統一使用 `Decimal.formattedPrice` 擴充（靜態 `NumberFormatter` 快取，`.currency` 樣式、`currencyCode = "TWD"`、`currencySymbol = "NT$"`、無小數格式）。View 層直接使用 `formattedPrice`，不再手動拼接 `"NT$ "` 前綴。

### 12. Order/Payment Resilient Decoding

**問題**：訂單列表 API 與詳情 API 回傳的欄位集合可能不同，部分欄位在列表中不存在。

**解法**：
- `Order.orderType`：`(try? c.decodeIfPresent(...)) ?? "charter"` — 缺欄位時預設 `"charter"`
- `Order.status`：`(try? c.decode(...)) ?? .pending` — 解碼失敗時預設 `.pending`
- `Order.charterBooking` / `Order.payments`：`try?` — 巢狀解碼失敗時回傳 `nil` 而非拋錯
- `Payment.method` / `Payment.status`：`(try? c.decodeIfPresent(...)) ?? "unknown"` — 缺欄位時預設 `"unknown"`
- `CharterBooking`：除 `id` 外所有欄位皆 optional，使用 auto-synthesized Codable

### 13. CharterBookingSheet ISO 8601 時間合併 & 表單驗證

`CharterBookingViewModel` 表單使用兩個獨立 `DatePicker`（日期 `.date` + 時間 `.hourAndMinute`），透過 `combinedPickupDate` private computed property 合併為 `Date?`，供驗證（`combined > Date()`）與 API 提交（`pickupDateTime` ISO 8601 字串）共用。提交使用 `ISO8601DateFormatter` 搭配 `.withFullDate` + `.withTime` + `.withDashSeparatorInDate` + `.withColonSeparatorInTime` 選項。

表單驗證採用 `showValidation` flag 模式：點擊「確認預約資訊」時設為 `true`，各 error computed property 依據 `showValidation` + 欄位值決定是否回傳錯誤訊息。`isValid` 獨立於 `showValidation`，純粹檢查資料合法性。所有文字欄位驗證與送出時均 `trimmingCharacters(in: .whitespaces)`。

### 14. Sheet async closure crash 防範

**問題**：將 async closure（如 `onSave: (String, String?) async -> Bool`）作為 View struct 的 stored property，在 `.sheet` builder 中從 `if let` 解包捕獲 `viewModel`。當 `@Observable` 屬性變更觸發 SwiftUI re-evaluation 時，closure 的 capture context 記憶體損壞，導致 `EXC_BAD_ACCESS` in `swift_retain`。

**解法**：所有 6 個編輯 Sheet（`EditProfileSheet`、`CreateItinerarySheet`、`EditItineraryMetaSheet`、`AddSpotSheet`、`EditDaySheet`、`EditSpotSheet`）改為直接接收 ViewModel 作為 property，不再儲存 async closure。在 button action 中，進入 `Task` 前先將 reference type 捕獲為 local constant：

```swift
let vm = viewModel
let auth = authManager
Task {
    isSaving = true
    let success = await vm.doWork(authManager: auth)
    isSaving = false
    if success { dismiss() }
}
```

**安全的 pattern**：`CreateSpotSheet`、`EditSpotInfoSheet` 使用同步 callback `(Spot) -> Void` 或直接呼叫 service，不經 async closure，故不受此問題影響。

### 15. OrderListView 返回自動刷新

`OrderListView` 使用 `needsRefresh` state + `onAppear`/`onDisappear` 模式：首次進入時 load，離開時設 `needsRefresh = true`，再次進入時自動 refresh。確保從 `OrderDetailView` 執行付款/取消/退款後返回列表時顯示最新狀態。

### 16. HTTPClient 詳細解碼錯誤訊息

`HTTPClient.decodeResponse` 的 catch 區塊對 `DecodingError` 做 switch 判斷，顯示具體缺少的欄位名稱與 coding path（如 `"Missing key 'order_type' (path: Index 0)"`），而非原生的 `localizedDescription`，便於除錯。這些 detail 字串為英文純文字（不走 `String(localized:)`），因為內容包含 Swift type name 和 JSON coding path 等開發者除錯資訊。使用者看到的錯誤訊息由 `APIError.errorDescription` 的 `"資料解析錯誤: \(detail)"` 包裝，該層有正確的 i18n 翻譯。

### 17. 行程 × 包車深度整合

**目標**：讓行程規劃自然引導到包車預約，降低預約摩擦。`Order` 模型已有 `itineraryID: Int?` 欄位，此版本在 UI 層完成串接。

**入口點**（3 處）：
1. **ItineraryDetailView**（行程擁有者 / 複製後）→ 底部「預約包車」CTA
2. **EditItineraryView** → Section「為此行程預約包車」
3. **OrderDetailView**（已連結行程）→ Section「連結行程」→ NavigationLink 回到行程詳情

**資料流**：
- `CharterItineraryContext.from(detail:)` 從行程詳情提取預填資料（天數、首尾景點地址）
- `ItineraryCharterSheet` 接收 context，Phase 1 選車輛，Phase 2 建立 `CharterBookingViewModel(vehicle:service:context:)` 預填表單
- `submit()` 將 `itineraryID` 傳入 `OrderService.create()`，後端建立連結
- 複製行程時，`ItineraryDetailViewModel.copiedItinerary` 保存複製結果，`charterContext(for:)` 使用複製後的 ID 而非原始行程 ID

**獨立包車不受影響**：Charter Tab → `CharterBookingSheet` 使用原有 `init(vehicle:service:)` 初始化器，`itineraryID` 為 nil。

### 18. 上傳進度追蹤（UploadProgressDelegate）

`UploadProgressDelegate`（`nonisolated final class`，`URLSessionTaskDelegate + Sendable`）透過 `urlSession(_:task:didSendBodyData:totalBytesSent:totalBytesExpectedToSend:)` 回報上傳位元組比例。`HTTPClient.uploadWithProgress()` 建立獨立 `URLSession(configuration:delegate:delegateQueue:)` 使用此 delegate，搭配暫存檔 `upload(for:fromFile:)` 觸發 delegate 回呼。`defer` 清理暫存檔 + `finishTasksAndInvalidate()`。

### 19. NetworkMonitor 網路類型偵測

`@Observable final class NetworkMonitor`（`nonisolated(unsafe) static let shared` 單例，同 `ImageCache` 模式），使用 `NWPathMonitor` 偵測 `isCellular` / `isConnected`。`pathUpdateHandler` 透過 `Task { @MainActor in }` 更新屬性。`ImagePickerUploadView` 壓縮前讀取 `NetworkMonitor.shared.isCellular` 決定 aggressive 壓縮策略。

### 20. 上傳自動重試（指數退避）

`UploadService.uploadWithRetry()` 包裝 `uploadWithProgress()`，最多重試 2 次（間隔 2s / 4s 指數退避），僅 `APIError.networkError` 觸發重試（HTTP 4xx/5xx 不重試），使用 `Task.sleep(for:)` 支援取消。重試時立即 `onProgress(0)` 重置進度條，讓使用者看到即時回饋。

### 21. 非阻塞上傳 UI（`@Binding isUploadInProgress`）

`ImagePickerUploadView` 新增 `@Binding var isUploadInProgress: Bool`，作為上傳狀態的唯一真相來源（Single Source of Truth）。上傳中：半透明進度條覆蓋圖片預覽（`ProgressView(value: uploadProgress)` + 百分比文字），表單其他欄位可繼續填寫。5 個消費端 Sheet（`EditProfileSheet`、`CreateItinerarySheet`、`EditItineraryMetaSheet`、`CreateSpotSheet`、`EditSpotInfoSheet`）各自宣告 `@State private var imageUploading = false` 並傳入 `isUploadInProgress: $imageUploading`，儲存/建立按鈕 disabled 加入 `!imageUploading` 條件。`.onDisappear { uploadTask?.cancel() }` 確保 sheet 關閉時釋放上傳資源。

### 22. 自訂底部面板取代系統 Sheet（DraggableBottomPanelContainer）

**問題**：`ItineraryMapView` 原本使用 `.sheet(isPresented: $showPanel)` 呈現底部景點面板。系統 sheet 使用 UIKit `UISheetPresentationController`，獨立於 SwiftUI navigation transition 動畫層，導致：(1) navigation pop 時面板延遲消失（visible lag）；(2) 父層 toolbar 按鈕被遮擋；(3) 同一 presentation context 不能再 present 其他 sheet。

**解法** — 自訂 `DraggableBottomPanelContainer` overlay：
- **面板作為 overlay** → `.overlay(alignment: .bottom)` 內嵌面板，隨父 view 同步 pop，無延遲。
- **`DragGesture(coordinateSpace: .global)`** → 面板底部對齊、向上成長，使用 `.local`（預設）座標系會因 view 移動導致 jitter（gesture reference frame 與 view 一起移動，translation 被折半）。`.global` 固定參考座標系解決此問題。
- **Velocity-based 方向吸附** → 快速 flick up/down 移至相鄰 detent（`flickThreshold: 150`），慢拖吸附至最近 detent。使用 `predictedEndTranslation - translation` 計算速度。
- **橡皮筋阻力** → `rubberClamped()` 在邊界外套用 iOS 風格衰減公式 `(1 - 1/(x * 0.55/limit + 1)) * limit`。**重要**：`resolveDetent()` 使用原始 `rawHeight` 而非橡皮筋後的 `currentHeight`，避免邊界附近吸附距離失真。
- **Author 導航回呼** → 面板不在獨立 NavigationStack 內，作者點擊透過 `onAuthorTap: ((Int) -> Void)?` callback 冒泡至父層 `ItineraryDetailView` / `EditItineraryView`，由 `@State authorUserID` + `.navigationDestination(item:)` → `UserProfileView` 處理。
- **面板內容** → `ItinerarySpotPanelView` 使用 `.scrollContentBackground(.hidden)` 搭配面板 `.ultraThinMaterial` 背景，`.contentMargins(.vertical, 8)` 縮小頂部與底部間距。

此模式應用於 `ItineraryDetailView` 和 `EditItineraryView`。`EditItineraryView` 地圖模式仍保留 floating overlay 按鈕（切回列表 + 設定）。

---

## 版本紀錄

| 版本 | 日期 | 說明 |
|------|------|------|
| 0.1 | 2026-02-27 | 初版 scaffold：認證、探索、行程 CRUD、個人頁基本實作完成 |
| 0.2 | 2026-02-27 | ExploreView 改用 CompactItineraryCard（底部面板縮至 ~148px）；修復 ScrollView 高度撐大、地圖無初始 pins、Spot DECIMAL 解碼失敗等問題 |
| 0.3 | 2026-02-27 | 修復行程詳情解碼（ItineraryDay.id = "day_id"）；新增 AddToItinerarySheet（3步驟加入行程流程）；SpotDetailView 加入行程按鈕對未登入用戶顯示登入 Alert；SpotSearchView 開啟時顯示分類 Grid |
| 0.4 | 2026-02-27 | Google OAuth 設定 serverClientID（Web Client ID）；copy 端點加入 user_id；LoginView 改用自訂 GoogleButton/GoogleGMark（移除 GoogleSignInSwift）；ProfileView 未登入直接回傳 LoginView；ItineraryDetailView 複製按鈕對未登入用戶也顯示；AddToItinerarySheet 整合建立行程步驟（Step.createItinerary，內嵌 CreateItineraryForm）；Itinerary/ItineraryDay/ItinerarySpot Flexible / Dual-key 解碼；HTTPClient 只要傳入 authManager 就附加 token；UserService.getUserItineraries 支援可選 auth；ServiceContainer 新增 myItineraries 共用快取；MyItinerariesViewModel 改用 services: ServiceContainer 初始化並採 MERGE 策略；CreateItinerarySheet isPublic 預設改為 false |
| 0.5 | 2026-02-27 | 接上 4 個未使用 API endpoint：(1) 編輯景點屬性 — EditSpotSheet（LabeledContent 永久標籤）+ EditDaySpotsView 點擊景點列直接開啟編輯、左上角改為「排序」自訂按鈕取代 EditButton；(2) SpotDetailLoadingView 用 spotID 載入景點詳情（三態 loading/error/detail）；(3) cover_image_url 支援 — CreateItinerarySheet/EditItineraryMetaSheet/APIEndpoint/ItineraryService/MyItinerariesVM/EditItineraryVM 全鏈路傳遞封面圖片網址；(4) UserProfileView 查看其他使用者 — UserProfileViewModel 用 async let 平行拉取 user + itineraries、ItineraryDetailView 作者區塊改為 NavigationLink(.buttonStyle(.plain)) 導航至 UserProfileView |
| 0.6 | 2026-02-27 | Apple HIG UI/UX 全面優化 + API v1.3 對齊：(1) 4 處破壞性操作加 `.confirmationDialog` 二次確認（刪除行程/天數、移除景點、取消收藏）；(2) HapticManager 統一觸覺回饋（success/warning/error）用於 8 處互動；(3) AuthGateView splash 動畫（app icon + 品牌文字 + opacity transition）；(4) ExploreViewModel `searchNearbySpots` 500ms debounce 防抖；(5) SpotDetailView 收藏失敗顯示 alert 錯誤回饋；(6) `SpotCategory.color` 統一分類顏色（取代 3 處重複定義）；(7) EditSpotSheet 到達/離開時間改用 DatePicker + Toggle；(8) ExploreView 新增重新整理按鈕；(9) 空狀態文字微調；(10) 列表行 padding 統一為 `.vertical, 4`；(11) Accessibility 標籤（地圖標注、收藏按鈕、複製按鈕、CategoryBadge）；(12) API v1.3：`copyItinerary` 移除 `userID` 參數，後端從 JWT Token 自動取得；(13) APIConfig baseURL 更新為 `https://v1api.samuelray.net/api` |
| 0.7 | 2026-02-27 | 包車預訂 & 金流系統（API v1.4）：(1) 4 個新 Model — Vehicle+VehicleType、Order+OrderStatus、CharterBooking、Payment；(2) Decimal+Formatting 擴充（NT$ 千分位格式化）；(3) APIEndpoint 新增 10 個 case（vehicles 2 + orders 8）；(4) VehicleService + OrderService；(5) 4 個新 ViewModel — VehicleListVM、OrderListVM、OrderDetailVM、CharterBookingVM（多步驟表單）；(6) 包車 Tab — CharterTabView（車型 FilterChip 篩選 + 可用性 Toggle + 2 欄 LazyVGrid）、VehicleCardView、VehicleDetailView（hero 圖 + safeAreaInset CTA）、CharterBookingSheet（fillForm→review→submitting→success/error 五步驟）；(7) 訂單管理 — OrderListView（狀態 FilterChip 篩選 + 分頁 + 返回自動刷新）、OrderDetailView（付款/取消/退款操作 + confirmationDialog + 中文化付款狀態）；(8) MainTabView 新增第 4 Tab「包車」；(9) ProfileView 新增「我的訂單」Section；(10) 共用元件 — FilterChip、VehicleTypeBadge、OrderStatusBadge；(11) Order/Payment resilient decoding（try? + 預設值防止巢狀解碼失敗）；(12) HTTPClient 詳細解碼錯誤訊息（顯示具體缺少欄位名稱 + coding path） |
| 0.8 | 2026-02-28 | 包車預約表單驗證 + inline 錯誤提示：CharterBookingViewModel 新增 `showValidation` flag 模式 + 4 個 error computed properties（pickupLocation/contactName/contactPhone/pickupDateTime）；表單驗證失敗觸發 `HapticManager.notification(.warning)`；所有文字欄位 trim whitespace；ISO 8601 時間合併（`combinedPickupDate` 合併日期+時間 DatePicker） |
| 0.9 | 2026-02-28 | 行程 × 包車深度整合：(1) 新增 `CharterItineraryContext` 模型（行程→包車預填資料，工廠方法提取首尾景點地址）；(2) 新增 `ItineraryCharterSheet`（多階段 sheet：車輛選擇 grid + 類型篩選 → 預約表單預填天數/地址 → 確認送出，更換車輛保留表單資料）；(3) `CharterBookingViewModel` 新增 `itineraryID`/`itineraryTitle` + context 初始化器，`submit()` 傳遞 itineraryID；(4) `ItineraryDetailViewModel.didCopy` 改為 `copiedItinerary: Itinerary?`（computed `didCopy`），保存複製後行程 ID；(5) `ItineraryDetailView` 底部按鈕邏輯重構（擁有者→包車 CTA / 複製成功 1.5s 過渡→包車 CTA / 未複製→複製按鈕），`charterContext(for:)` 依身份選擇行程 ID；(6) `EditItineraryView` 新增「為此行程預約包車」Section；(7) `OrderDetailView` 新增「連結行程」Section（NavigationLink → ItineraryDetailView） |
| 1.7 | 2026-02-28 | UI 多語系 i18n（en / ja）：(1) `Localizable.xcstrings` 填入 260 組 UI 字串的 en / ja 翻譯（sourceLanguage: zh-Hant）；涵蓋 Tab 標題、按鈕、表單欄位、Section 標題、Alert/Dialog、enum displayName、錯誤訊息、空狀態文字等；(2) 含插值字串使用 `%lld` / `%@` format specifier；(3) HTTPClient 新增 `prepareRequest()` 集中處理 `Accept-Language`（init 快取）+ auth token，API 回傳內容自動本地化；(4) iOS 專案 `knownRegions` 已包含 zh-Hant / en / ja / Base；(5) 修正 11 處 `??` / ternary 導致 `String` 型別解析繞過 `LocalizedStringKey` 翻譯的問題（改用 `String(localized:)`）；(6) `Decimal.formattedPrice` 改用 `.currency` 樣式（NT$ 符號由 formatter 統一處理），移除 View 層手動 `"NT$ "` 前綴；(7) `DateFormatter.displayDate/displayTime` locale 從硬編碼 `zh_TW` 改為 `.current`；(8) HTTPClient 解碼錯誤 detail 改為英文純文字（開發者除錯用，不走 i18n） |
| 1.8 | 2026-02-28 | 登入 sheet 生命週期修正：(1) AuthGateView 改用 `@State hasRestored` 控制 splash→MainTabView 切換（取代 `authManager.isLoading`），防止子頁面登入時 `isLoading` 重置導致 MainTabView 銷毀、LoginView sheet 提前消失；(2) ItineraryDetailView / SpotDetailView / VehicleDetailView 新增 `.onChange(of: authManager.isAuthenticated)` 監聽，登入成功後自動 dismiss LoginView sheet |
| 1.9 | 2026-03-02 | UI 多語系新增越南文 (vi)：(1) `Localizable.xcstrings` 為全部 257 個翻譯 key 新增 vi 條目（state: translated），涵蓋 Tab 標題、按鈕、表單欄位、Section 標題、Alert/Dialog、enum displayName、錯誤訊息、空狀態文字等；(2) 3 個 key 正確跳過（`%lld` 純格式符、`Hello, world!` 測試用、`點兩下查看詳情` 無翻譯條目）；(3) 語義精煉 4 處重複翻譯：「移除」→ Gỡ bỏ（區分 Remove/Delete）、「尚無行程」→ Không có lịch trình（區分 No.../Not yet）、「確定移除此景點？」對應 remove 而非 delete、「訂單編號：#%lld」→ Mã đơn hàng: #%lld（區分帶標籤/簡短版）；(4) 所有含 `%@`/`%lld` 格式說明符的 key 數量與順序完全一致；(5) 無需任何 Swift 程式碼或專案設定變更 — SwiftUI `LocalizedStringKey` 自動查詢、`HTTPClient` `Accept-Language` 動態取得、String Catalogs 自動偵測新語言；(6) 對齊後端 API v1.8（`SUPPORTED_LOCALES` += vi、`Accept-Language: vi-VN` → vi、越南文搜尋） |
| 2.0 | 2026-03-03 | 圖片上傳 + UI 元件 + Sheet crash 修正：(1) 新增 `UploadService`（圖片壓縮 + S3 multipart 上傳）、`UploadFolder`/`UploadResponse` 模型、`ImagePickerUploadView` 整合元件（PhotosPicker + 壓縮 + 上傳 + 預覽）；(2) `EditProfileSheet` 支援大頭照上傳、`CreateItinerarySheet`/`EditItineraryMetaSheet` 支援封面圖片上傳（取代 URL 文字輸入）；(3) 修正 6 個 Sheet 的 `EXC_BAD_ACCESS` crash — 將 async closure stored property 改為直接接收 ViewModel reference，button action 中以 local constant 捕獲 reference type 再進入 Task；(4) 新增可重用 UI 元件：`CardStyle`（卡片樣式 ViewModifier）、`BottomBarBackground`（底部操作列背景）、`HeroImageView`（hero 圖片 + 漸層遮罩）、`ShimmerView`（載入佔位動畫）；(5) `CachedAsyncImage` 改寫為 phase-based（empty/loading/loaded/failed），cache hit 不動畫、network 載入時 shimmer + fade-in；(6) `ImageCache` 新增 in-flight 去重（`NSLock` + `inFlight` dictionary）、`totalCostLimit` 100MB、像素面積 cost；(7) `EditItineraryViewModel.updateMeta` batch mutation 減少 `@Observable` 通知次數；(8) `HTTPClient` session timeout 維持 20s，upload per-request timeout 60s + `Data(capacity:)` 預分配；(9) `ProfileViewModel.updateProfile` 新增 `avatarURL` 參數；(10) `ServiceContainer` 新增 `uploadService`；(11) `APIEndpoint` 新增 `.upload` case；(12) CLAUDE.md 更新 API 文件與 Sheet crash 防範模式 |
| 2.1 | 2026-03-04 | 圖片上傳效能與慢網路 UX 優化：(1) 新增 `UploadProgressDelegate`（URLSessionTaskDelegate），`HTTPClient.uploadWithProgress()` 搭配暫存檔 + 獨立 URLSession 實現即時進度回報；(2) `UploadService` 新增 `uploadWithProgress()` + `uploadWithRetry()`（指數退避 2s/4s，僅 networkError 重試）；(3) `compressImage()` 新增 `aggressive` 參數（cellular: 1024px/0.5，Wi-Fi: 1920px/0.8）；(4) 新增 `NetworkMonitor`（`@Observable` + `NWPathMonitor` 偵測 isCellular/isConnected）；(5) `ImagePickerUploadView` 改用 `@Binding isUploadInProgress` 單一狀態源 + `ProgressView(value:)` 百分比進度條 + `.onDisappear` 自動取消；(6) 5 個 Sheet 新增 `@State imageUploading`，儲存按鈕 disabled 加入上傳中判斷（非阻塞 UI，表單可繼續填寫）；(7) i18n 新增「上傳中... %lld%%」四語翻譯 |
| 2.2 | 2026-03-07 | 行程地圖視圖 + 3 項 bug 修正：(1) 新增 `ItineraryMapView`（全螢幕行程地圖：天數色路線 MapPolyline + NumberedSpotMarker 編號標記 + daySelectorBar FilterChip 篩選 + 底部 ItinerarySpotPanelView 半高面板 sheet）；(2) 新增 `ItineraryDetail+MapHelpers` extension（`MappableSpot` 結構 + `allMappableSpots/spotsForDay/region` 方法，從 ViewModel 解耦供共用）；(3) 新增 `DayColorPalette`（8 色循環）+ `NumberedSpotMarker`（地圖標記元件）；(4) `ItineraryDetailView` 改為全螢幕地圖 UI，包車/登入 sheet 改用 `.background` 隔離模式解決 sheet 衝突（Bug fix）；(5) `EditItineraryView` 新增地圖/列表雙模式切換（`@State showMap` + toolbar toggle），地圖模式使用 floating overlay 按鈕替代被 panel sheet 阻擋的 toolbar 按鈕，meta/charter sheet 改用 `.background` 隔離模式；(6) `ItineraryMapView` 使用 `@State showPanel = true` 替代 `.constant(true)`，修正 navigation pop 卡頓（Bug fix）；(7) `MyItinerariesView` 加上 `.listStyle(.plain)`（Bug fix）；(8) `ItineraryDay` 新增 `sortedSpots` computed property 統一 orderIndex 排序；(9) `ItineraryDetailViewModel` 精簡為純業務邏輯（移除地圖方法）；(10) `ItinerarySpotPanelView` 解耦自 ViewModel，改用值參數（isCopying/didCopy/showActions） |
| 2.3 | 2026-03-09 | 自訂底部面板取代系統 Sheet + UI/UX 全面優化：(1) 新增 `DraggableBottomPanelContainer`（自訂拖曳面板：三段吸附 collapsed/medium/large、velocity-based 方向吸附、iOS 橡皮筋阻力、spring 動畫），取代 `.sheet()` 解決 navigation pop 面板延遲問題；(2) `ItineraryMapView` 改用 `.overlay(alignment: .bottom)` 內嵌面板，`DragGesture(coordinateSpace: .global)` 解決拖曳 jitter；(3) `ItinerarySpotPanelView` 移除 NavigationStack，作者導航改為 `onAuthorTap` callback → 父層 `.navigationDestination(item:)`；(4) `ItineraryDetailView` / `EditItineraryView` 新增 `@State authorUserID` 接收面板作者點擊；(5) `MappableSpot` 新增 `name` 欄位、`daysForIndex()` 集中篩選方法；(6) `allMappableSpots` N+1 重算優化（一次計算 + 本地篩選）；(7) 切換天數清除 `selectedSpotID` 防止 ghost selection；(8) daySelectorBar shadow → `Divider()` 修正不等距問題；(9) `resolveDetent` 使用 `rawHeight` 而非橡皮筋高度；(10) Accessibility：`FilterChip` / `CategoryChipButton` `.isSelected` trait、`NumberedSpotMarker` label、`PanelSpotRow` `.combine` + `.isSelected`、地圖 Annotation label、面板拖曳手柄 label + hint；(11) `FilterChip` `.padding(.vertical, 10)` 增大觸控目標；(12) 面板 detent 切換觸發 `HapticManager.impact(.light)` |
| 2.4 | 2026-03-09 | UI/UX 審查修正 + 空狀態優化 + 日期格式化統一：(1) 空狀態 CTA 按鈕 — `MyItinerariesView`（"建立行程" → `showCreate`）、`EditDaySpotsView`（"新增景點" → `showAddSpot`）使用 `ContentUnavailableView` `actions:` 閉包取代純文字提示；(2) 錯誤狀態統一 — `OrderListView`、`OrderDetailView`、`CharterTabView` 改用 `ContentUnavailableView` + 重試按鈕（取代 raw VStack）；(3) `OrderListView` 空狀態加上 `ContentUnavailableView` + 說明文字；(4) 表單儲存觸覺回饋 — `EditProfileSheet`、`EditItineraryMetaSheet`、`EditDaySheet`、`AddSpotSheet`（兩條路徑）新增 `HapticManager.notification(.success)`；(5) `ExploreView` `.task` force-unwrap 修正（`viewModel!` → `viewModel?` + `?? ()`）；(6) Accessibility — `ExploreView` 重新整理按鈕 `.accessibilityLabel`、`EditDaySpotsView` 排序按鈕 `.accessibilityHint`（依模式切換描述）、`VehicleDetailView` CTA `.accessibilityHint`（依可用狀態）；(7) 鍵盤管理 — `AddSpotSheet`、`SpotSearchView` 新增 `@FocusState`，搜尋送出/toolbar 搜尋按鈕時自動收起鍵盤；(8) 日期格式化統一 — 新增 `ISODateParser` enum（ISO 8601 解析 + locale-aware 格式化），新增 `DateFormatter.displayDateTime`；`OrderDetailView` 3 處、`OrderListView` 1 處改用 `ISODateParser`（取代 inline `.prefix().replacingOccurrences()`）；(9) `AddSpotSheet` 重構 — 提取 `addSpotToDay(_:)` 統一兩處重複的 add-spot 邏輯；(10) `ISODateParser` 移除 `nonisolated`（`DateFormatter` 非 thread-safe，保持 MainActor 隔離）；(11) i18n — 新增 8 組四語翻譯（建立行程/新增景點/預約包車說明/重新整理行程列表/排序模式 hint ×2/車輛不可預約/點擊預約），更新 2 組描述文字（移除"點右上角 +"指示語） |
| 1.0 | 2026-02-28 | 景點建立 + 我的景點篩選 + 地圖 Annotation 差異化：(1) `Spot` model 新增 `creatorId`/`isPublic`/`source` 欄位；(2) 新增 `CreateSpotSheet`（建立景點，支援帶入經緯度）、`EditSpotInfoSheet`（景點擁有者編輯/刪除）；(3) ExploreView 地圖長按手勢建立景點 — 未登入顯示 Google 登入 sheet（共用 `GoogleButton`），登入成功後 sheet 內容自動切換為 CreateSpotSheet（無需重新操作）；(4) 「我的景點」正交篩選 — SpotSearchView/AddSpotSheet 新增 indigo chip + 垂直 `Divider` 視覺分隔（owner filter × category filter 可同時啟用）；(5) `SpotAnnotationView` 三種差異化標記：我的景點（42px indigo 外環）、收藏景點（紅色愛心 badge）、兩者疊加；(6) `ExploreViewModel` 新增 `userService`、`favoriteSpotIDs`、`loadFavorites()`，三時機同步：初始化/登入登出/SpotDetailView dismiss；(7) `SpotDetailView` 支援 `initialIsFavorited`（從 `favoriteSpotIDs` 帶入）、景點擁有者 toolbar Menu（編輯/刪除）；(8) `UserService.getUserFavorites` 新增可選 `authManager` 參數（Bearer token 確保完整資料）；(9) `GoogleButton`/`GoogleGMark` 改為 internal 供 ExploreView 共用 |
