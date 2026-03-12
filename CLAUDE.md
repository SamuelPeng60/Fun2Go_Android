# Fun2Go — 開發記錄

## 專案概覽
- **名稱**: Fun2Go — Android 旅遊行程規劃 App
- **套件名稱**: `com.funTrip.fun2go`（注意大寫 T）
- **minSdk**: 26 / targetSdk & compileSdk: 36
- **語言**: Kotlin
- **架構**: MVVM (AndroidViewModel + LiveData + Repository)
- **後端 API**: `https://v1api.samuelray.net/`
- **GitHub**: https://github.com/SamuelPeng60/Fun2Go_Android

---

## 重要檔案路徑

| 用途 | 路徑 |
|------|------|
| MainActivity | `app/src/main/java/com/funTrip/fun2go/MainActivity.kt` |
| MainViewModel | `app/src/main/java/com/funTrip/fun2go/ui/viewmodel/MainViewModel.kt` |
| ItineraryListActivity | `app/src/main/java/com/funTrip/fun2go/ui/ItineraryListActivity.kt` |
| ItineraryDetailActivity | `app/src/main/java/com/funTrip/fun2go/ui/ItineraryDetailActivity.kt` |
| ItineraryViewModel | `app/src/main/java/com/funTrip/fun2go/ui/viewmodel/ItineraryViewModel.kt` |
| ItineraryAdapter | `app/src/main/java/com/funTrip/fun2go/ui/adapter/ItineraryAdapter.kt` |
| TripRepository | `app/src/main/java/com/funTrip/fun2go/data/repository/TripRepository.kt` |
| ApiService | `app/src/main/java/com/funTrip/fun2go/data/remote/ApiService.kt` |
| RetrofitClient | `app/src/main/java/com/funTrip/fun2go/data/remote/RetrofitClient.kt` |
| TokenManager | `app/src/main/java/com/funTrip/fun2go/data/local/TokenManager.kt` |
| AuthModels | `app/src/main/java/com/funTrip/fun2go/data/model/AuthModels.kt` |
| ApiRequests | `app/src/main/java/com/funTrip/fun2go/data/model/ApiRequests.kt` |
| User 模型 | `app/src/main/java/com/funTrip/fun2go/data/model/User.kt` |
| Spot 模型 | `app/src/main/java/com/funTrip/fun2go/data/model/Spot.kt` |
| Itinerary 模型 | `app/src/main/java/com/funTrip/fun2go/data/model/Itinerary.kt` |
| Room DB | `app/src/main/java/com/funTrip/fun2go/data/local/AppDatabase.kt` |
| SavedSpotDao | `app/src/main/java/com/funTrip/fun2go/data/local/SavedSpotDao.kt` |
| SavedSpotEntity | `app/src/main/java/com/funTrip/fun2go/data/local/SavedSpotEntity.kt` |
| SavedSpotAdapter | `app/src/main/java/com/funTrip/fun2go/ui/adapter/SavedSpotAdapter.kt` |
| AndroidManifest | `app/src/main/AndroidManifest.xml` |
| activity_main.xml | `app/src/main/res/layout/activity_main.xml` |
| activity_itinerary_list.xml | `app/src/main/res/layout/activity_itinerary_list.xml` |
| activity_itinerary_detail.xml | `app/src/main/res/layout/activity_itinerary_detail.xml` |
| bottom_sheet_login.xml | `app/src/main/res/layout/bottom_sheet_login.xml` |
| bottom_sheet_edit_itinerary.xml | `app/src/main/res/layout/bottom_sheet_edit_itinerary.xml` |
| item_itinerary.xml | `app/src/main/res/layout/item_itinerary.xml` |
| bottom_sheet_create_edit_spot.xml | `app/src/main/res/layout/bottom_sheet_create_edit_spot.xml` |
| BaseRepository | `app/src/main/java/com/funTrip/fun2go/data/repository/BaseRepository.kt` |
| Vehicle 模型 | `app/src/main/java/com/funTrip/fun2go/data/model/Vehicle.kt` |
| VehicleListActivity | `app/src/main/java/com/funTrip/fun2go/ui/VehicleListActivity.kt` |
| VehicleViewModel | `app/src/main/java/com/funTrip/fun2go/ui/viewmodel/VehicleViewModel.kt` |
| VehicleAdapter | `app/src/main/java/com/funTrip/fun2go/ui/adapter/VehicleAdapter.kt` |
| activity_vehicle_list.xml | `app/src/main/res/layout/activity_vehicle_list.xml` |
| item_vehicle.xml | `app/src/main/res/layout/item_vehicle.xml` |
| Order 模型 | `app/src/main/java/com/funTrip/fun2go/data/model/Order.kt` |
| OrderListActivity | `app/src/main/java/com/funTrip/fun2go/ui/OrderListActivity.kt` |
| OrderViewModel | `app/src/main/java/com/funTrip/fun2go/ui/viewmodel/OrderViewModel.kt` |
| OrderAdapter | `app/src/main/java/com/funTrip/fun2go/ui/adapter/OrderAdapter.kt` |
| activity_order_list.xml | `app/src/main/res/layout/activity_order_list.xml` |
| item_order.xml | `app/src/main/res/layout/item_order.xml` |
| bottom_sheet_booking.xml | `app/src/main/res/layout/bottom_sheet_booking.xml` |
| bottom_sheet_order_detail.xml | `app/src/main/res/layout/bottom_sheet_order_detail.xml` |
| API Key（不進 git）| `local.properties` → `MAPS_API_KEY=...` |
| PublicItineraryListActivity | `app/src/main/java/com/funTrip/fun2go/ui/PublicItineraryListActivity.kt` |
| PublicItineraryAdapter | `app/src/main/java/com/funTrip/fun2go/ui/adapter/PublicItineraryAdapter.kt` |
| activity_public_itinerary_list.xml | `app/src/main/res/layout/activity_public_itinerary_list.xml` |
| item_public_itinerary.xml | `app/src/main/res/layout/item_public_itinerary.xml` |
| menu_public_itinerary_list.xml | `app/src/main/res/menu/menu_public_itinerary_list.xml` |

---

## API Key 設定方式

- 真實 Key 存在 `local.properties`（已加入 .gitignore，不會上傳）
- `app/build.gradle.kts` 透過 `resValue` 注入到 `strings.xml`：
  ```kotlin
  resValue("string", "google_maps_key", localProperties["MAPS_API_KEY"] as? String ?: "")
  ```
- Manifest 使用 `android:value="@string/google_maps_key"` 讀取

---

## 已完成功能

### 1. Google Maps 地圖顯示
- 全螢幕 `SupportMapFragment`，預設鏡頭台北市中心（25.0330, 121.5654），zoom 14
- 從 API 載入所有景點，依分類過濾後打點（Marker）
- 點擊 Marker 顯示景點詳情 BottomSheet
- 右下角自訂 +/− 縮放按鈕（`btnZoomIn` / `btnZoomOut`）

### 2. 分類 Chips 篩選
- 分類：全部 / 景點 / 餐廳 / 夜市 / 購物 / 咖啡廳
- 點擊切換，重新過濾地圖 Marker

### 3. 我的列表（儲存景點）
- 景點詳情 BottomSheet 有「加入行程」按鈕（3 步驟閘門）：
  1. 未登入 → 彈出登入 Sheet
  2. 未建立行程 → AlertDialog 提示前往建立
  3. 已有行程 → 行程選擇器，選取後加入本地列表並呼叫 `addSpotToExistingItinerary`
- 使用 Room DB 持久化儲存（`saved_spots` 資料表）
- App 啟動時從 Room 載入已儲存景點（`hasLoadedFromDb` flag 防止重複載入）

### 4. 景點間距離估算（Haversine 公式）
- 行程詳情頁中，同一天相鄰景點之間顯示距離與預估開車時間
- **不使用任何 API**，本地計算（原 Distance Matrix API 因 REQUEST_DENIED 棄用）
- 計算邏輯：直線距離 × 1.3（路程係數），平均時速 **25 km/h**（台灣市區）
- 距離格式：`X 公尺` / `X.X 公里` / `X 公里`
- 時間格式：`約 X 分鐘` / `約 Xh Xm`
- 位置：`ItineraryDayAdapter.kt` → `calcDistance()` companion object

### 5. 個人資料頁
- 點擊左上角頭像開啟 `bottom_sheet_profile.xml`
- 顯示：頭像（Coil 載入，圓形裁切）、姓名、Email、加入日期（唯讀）
- 可編輯姓名與 Email，按「儲存修改」呼叫 `PUT api/users/{id}`
- 成功後自動更新頂部名稱、關閉 BottomSheet
- 「登出」按鈕：清除 TokenManager + GoogleSignInClient.signOut()

### 6. 目前位置定位按鈕
- 地圖右下角新增「定位」按鈕（`btnMyLocation`）
- 使用 `FusedLocationProviderClient.getCurrentLocation()` 取得位置
- 點擊後地圖動畫移動到目前位置（zoom 16）
- `googleMap.isMyLocationEnabled = true` 顯示持續的藍色定位點
- 首次進入地圖時自動請求 `ACCESS_FINE_LOCATION` 權限

### 7. 建立行程入口
- **首頁（MainActivity）**：已移除 FAB，建立行程統一透過行程列表頁操作
- **行程列表頁**：右下角粉紅色 FAB → `showCreateSheet()` → `POST /api/itineraries`
  - 表單：標題（必填）、目的地（下拉）、天數、起始日期（DatePickerDialog，可選）
  - 建立成功後自動呼叫 `initItineraryDays()`，依 totalDays 逐一建立天數並設定日期
  - 完成後導向 `ItineraryDetailActivity`（天數已全部預建）
- `MainViewModel.cachedUserItineraries`：`onResume` + `loginResult` 成功後刷新快取

### 8. 行程列表（ItineraryListActivity）
- 底部導航「探索」按鈕（`btnNavExplore`）→ `requireLogin` → 開啟 `ItineraryListActivity`
- `ItineraryViewModel` 獨立管理行程列表與建立/編輯/刪除
- `onResume` 每次進入都刷新 `getUserItineraries(userId)`
- 列表項目支援：點擊進入詳情、編輯鉛筆按鈕、**垃圾桶刪除按鈕**（AlertDialog 確認後刪除，成功自動刷新列表）
- 右下角 FAB 建立新行程
- `ItineraryAdapter`：顯示標題、目的地、天數（`onItemClick` / `onEditClick` / `onDeleteClick`）

### 9. 行程詳情（ItineraryDetailActivity）
- `ItineraryDayAdapter` 顯示天數列表（天數在建立行程時已自動初始化）
- `POST /api/itineraries/{id}/days` 需帶 `{ "day_number": N }` body（**重要**：沒有 body 會報錯）
- 天數 header 顯示日期，點擊可透過 DatePickerDialog 修改（`viewModel.updateDayDate()`）
- 刪除行程按鈕已移至**行程列表頁**每筆旁的垃圾桶（詳情頁 toolbar 不再有刪除 menu）

### 10. Google 登入 + JWT 認證
- 登入流程：App 開啟 → 直接進主畫面，需要登入才能使用保護功能（FAB 新增行程、個人資料）
- 使用 `play-services-auth:21.2.0`（舊版 GoogleSignIn API）
- Web Client ID：`36737576573-fepgqksogcshgpe0cshfg7ufoch3sv50.apps.googleusercontent.com`（存於 `strings.xml`）
- 登入後端流程：Google id_token → `POST /api/auth/google` → accessToken（1hr）+ refreshToken（30天）
- Token 持久化：`TokenManager`（SharedPreferences singleton），儲存 user_json、access_token、refresh_token
- **重要設計**：`isLoggedIn()` 檢查 `getSavedUser() != null`（非 accessToken），Google 登入成功後立刻呼叫 `saveGoogleAccount()` → isLoggedIn 立即為 true，不依賴後端回應
- Header（名稱 + 頭像）由 `currentUserLiveData` 驅動，Google 登入後立刻更新
- `requireLogin(desc, action)` 閘門：未登入時彈出 `bottom_sheet_login.xml`，已登入直接執行
- OkHttp 攔截器自動在每個請求帶入 `Authorization: Bearer <accessToken>`

---

## 資料模型重點

### User
```kotlin
data class User(
    val id: Int,
    val name: String,
    val email: String?,
    val avatarUrl: String? = null,   // API: avatar_url
    val createdAt: String? = null    // API: created_at，格式 "2026-01-30T..."
)
```

### SavedListItem（Adapter 用）
```kotlin
sealed class SavedListItem {
    data class SpotItem(val spot: Spot) : SavedListItem()
    // DistanceSeparator 已移除（距離改在 ItineraryDayAdapter 中顯示）
}
```

---

## 依賴函式庫重點

| 函式庫 | 用途 |
|--------|------|
| `play-services-maps:18.2.0` | Google Maps SDK |
| `play-services-location:21.3.0` | FusedLocationProviderClient（定位） |
| `play-services-auth:21.2.0` | Google Sign-In（舊版，有棄用警告但仍可用） |
| `retrofit2:3.0.0` + `converter-gson` | API 呼叫 |
| `okhttp3 logging-interceptor` | HTTP log + Bearer token 自動注入 |
| `room:2.6.1` + KSP | 本地 DB |
| `coil:2.6.0` | 頭像圖片載入 |
| `material:1.12.0` | UI 元件 |
| `kotlinx-coroutines-android:1.7.3` | 協程 |

---

### 11. 自訂景點建立 / 編輯 / 刪除（v1.6）
- **長按地圖** → `requireLogin` → `showCreateSpotSheet(latLng)`（`bottom_sheet_create_edit_spot.xml`）
  - 預填緯度 / 經度；分類 ExposedDropdownMenu；是否公開 SwitchMaterial（預設 off）
  - 按「儲存」→ `viewModel.createSpot(SpotRequest)` → `POST /api/spots`
  - 成功後直接將 API response 的 Spot 加入 `allSpots` + `filterAndPlaceMarkers()`（**不呼叫** `fetchAllSpots()`，避免 id=0 問題）
- **景點詳情 Sheet** 新增擁有者操作列（`llOwnerActions`，預設 GONE）
  - 判斷條件：`spot.creatorId != null && spot.creatorId == currentUser?.id`
  - `btnEditSpot` → `showEditSpotSheet(spot)`：預填所有欄位，PUT 後直接以 response 更新 `allSpots`
  - `btnDeleteSpot` → `confirmDeleteSpot(spot, dialog)`：AlertDialog 確認 → `viewModel.deleteSpot(spot.id)` → 成功後從 `allSpots` 移除 + `filterAndPlaceMarkers()` + 若在 savedSpots 也一併移除
- 新增 `SpotRequest` data class（`ApiRequests.kt`）
- `ApiService`：`PUT api/spots/{id}` / `DELETE api/spots/{id}`；`createSpot` 改接 `SpotRequest`
- `MainViewModel`：`updateSpotResult` / `deleteSpotResult` LiveData + 對應方法
- 新增 Drawable：`ic_edit.xml`（鉛筆）、`ic_delete.xml`（垃圾桶）

---

### 13. 租車（車輛瀏覽）（v1.8，2026-03-02）
- **底部導航**新增第 3 個按鈕 `btnNavCharter`（`ic_car.xml`），排列：地圖 | 探索 | 租車 | 個人
- 點擊 → 直接開啟 `VehicleListActivity`（**公開頁面，不需登入**）
- **`Vehicle` data class**：`id` / `name` / `type` / `capacity` / `pricePerDay`（String） / `imageUrl` / `description` / `isAvailable`
- **`ApiService`**：`GET api/vehicles?type&available`、`GET api/vehicles/{id}`（無需 Token）
- **`TripRepository`**：`getVehicles(type, available)`、`getVehicleDetail(id)`
- **`VehicleViewModel`**：`fetchVehicles(type?, available?)` → `_vehicles: LiveData<NetworkResult<List<Vehicle>>>`
- **`VehicleListActivity`**：
  - `MaterialToolbar`「租車」標題 + 返回按鈕
  - `ChipGroup`（singleSelection）篩選：全部 / 轎車（sedan_4）/ 九人座（van_9）/ 巴士（bus_20）
  - 選非「全部」時自動帶 `available=true` 查詢
  - `onResume` 每次進入刷新（依 `currentType`）
- **`VehicleAdapter`**：Coil 載圖（placeholder 灰色），型別中文化，`isAvailable=false` 顯示紅色「暫不可用」
- `pricePerDay` 格式化：`NT$ X,XXX / 天`（NumberFormat）

---

### 12. 行程建立流程重構（v1.7，2026-03-01）
- **建立行程表單新增起始日期欄位**（`bottom_sheet_create_itinerary.xml`，DatePickerDialog）
- **`TripRepository.initItineraryDays()`**：依 totalDays 迴圈呼叫 `addDay()` + `updateDay()` 設定日期
- `ItineraryViewModel` / `MainViewModel` 各自新增 `initDaysResult` LiveData + `initItineraryDays()`
- `ItineraryListActivity`：新增 `pendingItinerary` 欄位；建立成功後呼叫 `initItineraryDays`，完成後導向詳情頁
- **FAB 整理**：
  - `MainActivity` FAB（建立行程）→ 已移除
  - `ItineraryDetailActivity` FAB（新增天數）→ 已移除（天數在建立時自動初始化）
  - `ItineraryListActivity` FAB（建立行程）→ 保留，右下角粉紅色
- **刪除行程按鈕搬移**：從 `ItineraryDetailActivity` toolbar → `item_itinerary.xml` 編輯按鈕旁
  - `ItineraryAdapter` 新增 `onDeleteClick` callback
  - `ItineraryListActivity` 觀察 `deleteResult`，成功後刷新列表
- **`order_index` 修正**：`addSpotToExistingItinerary` / `importSpotsToNewItinerary` 改用 1-based index
- **景點加入行程流程**：`btnAddToList` → 3步驟閘門（登入 → 有行程 → 選擇器），加入後即時 Toast

---

### 14. 包車預訂（訂單 / 付款）（v1.9，2026-03-02）
- **`Order` / `CharterBooking` / `Payment` data class**（`Order.kt`）：完整 SerializedName 對應後端欄位
- **`ApiService`**：新增 `POST api/orders`、`GET api/orders`、`GET api/orders/{id}`、`POST api/orders/{id}/cancel`、`POST api/orders/{id}/pay`（均需 Token）
- **`TripRepository`**：新增 `createOrder` / `getOrders` / `getOrderDetail` / `cancelOrder` / `payOrder`
- **`OrderViewModel`**：`createOrderResult` / `orders` / `orderDetail` / `cancelResult` / `payResult` LiveData + 對應方法
- **`VehicleListActivity` 修改**：
  - toolbar 新增 `ic_receipt` menu icon（`menu_vehicle_list.xml`）→ 登入確認 → 開啟 `OrderListActivity`
  - 點擊車輛 → 登入確認 + `isAvailable` 確認 → `showBookingSheet(vehicle)`
  - `showBookingSheet()`：DatePickerDialog + TimePickerDialog 組合上車時間；天數變動即時計算預估金額；表單驗證；`viewModel.createOrder()` → 成功 AlertDialog「立即付款？」→ `viewModel.payOrder()`
  - `setupOrderObservers()`：透過 `_bookingSheetRef` lambda 讓 observer 能控制 sheet 引用
- **`OrderListActivity`**：ChipGroup 篩選（全部/待付款/已確認/已完成/已取消）；`onResume` 刷新；點擊 → `showOrderDetailSheet()`
- **`OrderDetailSheet`（`bottom_sheet_order_detail.xml`）**：顯示車輛資訊、預訂明細、聯絡資訊、總金額；`pending` 狀態顯示付款/取消按鈕；操作成功後關閉 Sheet + 刷新列表
- **`OrderAdapter`**：狀態 Badge 顏色（待付款橘、已確認綠、已完成灰、已取消紅）
- 新增 `ic_receipt.xml` drawable、`menu_vehicle_list.xml` menu
- `AndroidManifest.xml` 新增 `OrderListActivity`（parentActivity = VehicleListActivity）

---

---

### 15. 景點收藏同步後端（v2.0，2026-03-03）
- **加入行程（步驟三）** → 同步呼叫 `viewModel.addFavorite(uid, spot.id)` → `POST /api/users/{id}/favorites`
- **移除已加入景點** → 同步呼叫 `viewModel.removeFavorite(spot.id, uid)` → `DELETE /api/spots/{id}/favorites`
- **刪除自訂景點（confirmDeleteSpot）** → 若景點在 savedSpots，也同步 `removeFavorite`
- **登入成功後** → 呼叫 `viewModel.fetchUserFavorites(userId)` → observer 將後端 favorites 補入 Room DB（跳過本地已有的景點）
- `MainViewModel` 的 `addFavorite` / `removeFavorite` / `fetchUserFavorites` / `userFavoritesResponse` 均已在前版備好，本版只補 `MainActivity` 呼叫點

---

### 16. 公開行程探索 + 行程複製（v2.0，2026-03-03）
- **底部導航「探索」** (`btnNavExplore`) 改為直接開啟 `PublicItineraryListActivity`（**不再需要登入**）
- **`ItineraryViewModel`** 新增：
  - `publicItineraries` LiveData + `fetchPublicItineraries()` → `GET /api/itineraries`（limit=20, offset=0）
  - `copyResult` LiveData + `copyItinerary(id)` → `POST /api/itineraries/{id}/copy`
- **`PublicItineraryListActivity`**：
  - `onResume` 每次刷新公開行程列表
  - Toolbar 右側「我的行程」圖示 → 未登入 Toast / 已登入開啟 `ItineraryListActivity`
  - 點擊行程卡片 → 開啟 `ItineraryDetailActivity`（傳 id + title，唯讀瀏覽；若嘗試修改他人行程後端回 403，UI 顯示 Snackbar）
  - 點「複製行程」→ 未登入 Toast；已登入呼叫 `viewModel.copyItinerary(id)` → 成功 AlertDialog「前往我的行程？」
- **`PublicItineraryAdapter`**：顯示標題、目的地、天數、作者名稱（by xxx）、複製次數、複製按鈕（粉紅色）
- 新增 Drawable：`ic_copy.xml`（content_copy）、`ic_list.xml`（清單）
- `AndroidManifest.xml` 新增 `PublicItineraryListActivity`（parentActivity = MainActivity）

---

### 17. 英文版本（Android i18n）+ 修復 Maps 冷啟動 ANR（v2.3，2026-03-03）
- **新增 `res/values-en/strings.xml`**：~140 個 key，系統語言設英文時自動切換，不需手動選擇
- **更新 `res/values/strings.xml`**：補齊所有新 key（含 `msg_add_spot_failed`、`msg_signing_in`、`msg_booking_failed`、`msg_order_created_detail`、`format_total_amount` 等）
- **12 個 layout XML**：所有 hardcoded 中文文字改為 `@string/` 參照
- **6 個 Activity**：`MainActivity`、`ItineraryListActivity`、`ItineraryDetailActivity`、`VehicleListActivity`、`OrderListActivity`、`PublicItineraryListActivity` 全部改用 `getString(R.string.xxx)`
- **3 個 Adapter**：`ItineraryDayAdapter`、`OrderAdapter`、`VehicleAdapter` 改用 `itemView.context.getString()`
- **`categoryMap` 架構調整**：從 property 初始化改為 `private lateinit var`，在 `onCreate()` 中呼叫 `getString()` 初始化（因 getString 需要 Context）
- **String key 命名規則**：`label_`（按鈕）、`title_`（頁面/對話框）、`hint_`（輸入框）、`msg_`（Toast/Snackbar/Dialog）、`nav_`（導覽）、`empty_`（空狀態）、`category_`（景點分類）、`status_`（訂單狀態）、`format_`（含格式化變數）
- **注意**：`ItineraryDayAdapter.calcDistance()` 中的距離單位（`公尺`/`公里`）在 companion object 無 Context，**刻意保留中文**（不在 i18n 範圍）
- **修復 Maps 冷啟動 ANR**：
  - 根本原因：`SupportMapFragment` 寫在 XML layout 中，`setContentView()` 時 Maps SDK 同步初始化阻塞主線程 1-5 秒，造成 ANR
  - `activity_main.xml`：將 `<fragment>` 改為 `<FrameLayout android:id="@+id/mapContainer">`（純容器）
  - `MainActivity.kt`：`onCreate()` 中改用 `window.decorView.post { initMapFragment() }` 延後至第一幀繪製後執行
  - `initMapFragment()`：用 `SupportMapFragment.newInstance()` + `commit()`（非同步，不阻塞主線程）取代原本的 `commitNow()`
  - `App.kt`：加入 `MapsInitializer.initialize(this)` 在 Application 啟動時預熱 Maps SDK

---

### 18. 底部導航持久化 + UI 調整（v2.4，2026-03-06）
- **底部導航持久化**：`ItineraryListActivity` / `VehicleListActivity` 各自加入底部導航列
  - 當前頁 icon 高亮粉紅（#F44062），其餘灰色（#AAAAAA）
  - 使用 `FLAG_ACTIVITY_REORDER_TO_FRONT` 切換 tab，避免 back stack 堆疊
- **底部 icon 調整**：第 1 個改為探索羅盤圖示（`ic_explore_map.xml`），第 2 個改為地圖 icon
- **車子 icon 放大**：`ic_car.xml` width/height 從 24dp → 30dp
- **探索按鈕改為「我的行程」**：`btnNavExplore` 點擊 → requireLogin → `ItineraryListActivity`
- **我的行程過濾**：`ItineraryListActivity` 客端過濾 `author_id != currentId` 的行程
- **首頁公開行程 Panel**：改為 `MaterialCardView`（16dp 圓角、左右下留白 12dp/10dp）
- **`PublicItineraryPanelAdapter`**：新增橫向捲動公開行程卡片（MainAcitvity 底部 Panel）

---

### 19. 複製行程自動設定日期（v2.4，2026-03-06）
- **`ItineraryViewModel.setDatesAfterCopy(itineraryId, startDate)`**：
  - 先 `getItineraryDetail` 取得已複製的天數與 ID
  - 逐一 `updateDay` 設定日期（Day 1 = startDate, Day 2 = +1 天，依此類推）
  - 使用 `_initDaysResult` LiveData 回報進度
  - **注意**：不呼叫 `addDay()`，因複製 API 已建立天數；與 `initItineraryDays`（新建行程用）不同
- **`PublicItineraryListActivity`**：點「複製」→ DatePickerDialog 選出發日 → copyItinerary → setDatesAfterCopy → 直接進 `ItineraryDetailActivity`
- **`ItineraryDetailActivity` 自動偵測**：載入詳情後若所有天 `date == null`
  → 顯示 Toast「請設定出發日期」→ DatePickerDialog → `setDatesAfterCopy` → reload
  → `datePromptShown` flag 防止重複詢問
  - 涵蓋：MainAcivity Panel 複製、舊版複製、任何路徑進入的未設日期行程

---

### 20. Code Review 改善 + Bug Fixes（v2.5，2026-03-12）

#### i18n 補完
- **`ItineraryAdapter`**：移除 hardcoded 中文，改用 `ctx.getString(R.string.empty_destination)` / `R.string.format_day_count`
- **`PublicItineraryAdapter`**：移除 hardcoded 中文，改用 `ctx.getString()` 取得所有顯示字串
- **`item_itinerary.xml`** / **`item_public_itinerary.xml`**：hardcoded text → `@string/` 參照
- 新增字串 key：`label_edit`、`label_copy_itin`、`empty_destination`、`format_day_count`、`format_copy_count`、`badge_public`（中英文雙版本）

#### UI 改善
- **`item_itinerary.xml`** 重寫為 `MaterialCardView`：頂部新增 `ivCover`（ImageView 120dp，GONE）、`tvPublicBadge`（已發佈標示，`bg_badge_public.xml` 圓角背景，GONE）
- **`item_public_itinerary.xml`** 新增 `ivCover`（140dp）+ `ivAuthorAvatar`（24dp 圓形）
- **`ItineraryAdapter`** 新增封面圖（Coil `crossfade`）+ 已發佈 badge 顯示邏輯
- **`PublicItineraryAdapter`** 新增封面圖 + 作者頭像（`CircleCropTransformation`）
- 新增 Drawable：`bg_badge_public.xml`（粉紅圓角背景）

#### DiffUtil 優化
- **`ItineraryAdapter`** / **`PublicItineraryAdapter`** 的 `submitList()` 改用 `DiffUtil.calculateDiff()` + `dispatchUpdatesTo(this)`，取代 `notifyDataSetChanged()`，支援動畫且效能更佳

#### Debug Log 清除
- **`ItineraryListActivity`** / **`ItineraryViewModel`**：移除所有 `Log.d("ILA_DEBUG", ...)` 共 6 處 + 移除 `import android.util.Log`

#### Bug Fix：無行程時新增景點 → 建立行程後自動加入
- **根本原因**：建立行程 → `initItineraryDays` 完成後只導航到行程詳情，景點未自動加入
- **修復**：`MainActivity` 新增 `pendingSpotForItinerary: Spot?` 狀態變數；「前往建立行程」AlertDialog 點擊時儲存 `pendingSpotForItinerary = spot`；`initDaysResult` Success 後偵測是否有 pending spot，有則呼叫 `addPendingSpotToNewItinerary()`
- **`addPendingSpotToNewItinerary(itinerary, spot)`**：加入 Room DB + favorites → `getItineraryDetail` 取得天數 → 0 天直接 `addSpotToExistingItinerary`；1 天直接加入；多天 AlertDialog 選天數 → `addSpotToDayById` → 導向詳情頁

#### Bug Fix：刪除行程後景點 icon 恢復未加入狀態
- **根本原因**：刪除行程後 Room DB 未同步移除景點，`savedSpots` 仍保有已加入記錄，地圖 icon 不更新
- **`ItineraryViewModel.deleteItinerary()`** 修改：刪除前先 `getItineraryDetail` 取得所有景點 `spot_id`，刪除成功後批次 `dao.deleteById(spotId)` 從 Room DB 移除
- **`MainActivity.savedSpotsLiveData` observer** 修改：移除 `hasLoadedFromDb` 一次性保護旗標，改為每次 DB 變動都同步 `savedSpots` 並呼叫 `filterAndPlaceMarkers()`，確保地圖 icon 即時反映最新狀態

#### Bug Fix：無行程時景點仍顯示 ADDED 狀態（v2.5 補丁）
- **根本原因（雙重）**：
  1. Room DB 保有舊行程的殘留景點（fix 部署前刪除的行程未清除 DB）
  2. 登入時 `userFavoritesResponse` observer 會把後端 favorites 重新寫入 Room DB，即使用戶已無任何行程
- **`SavedSpotDao`** 新增 `deleteAll()`（`DELETE FROM saved_spots`）
- **`MainViewModel.fetchUserItineraries()`**：API 回傳 Success 且列表為空時呼叫 `savedSpotDao.deleteAll()`，確保「無行程 = 無 ADDED 景點」
- **`MainActivity.userFavoritesResponse` observer**：加入 `viewModel.cachedUserItineraries.isNotEmpty()` 檢查，防止後端 favorites 在無行程時把景點重新寫入 Room DB

---

## 已知待完成功能
- 無

## 已知 API 注意事項
- `POST /api/itineraries/{id}/days` 必須帶 `{ "day_number": N }` body，否則後端報 destructure 錯誤
- `GET /api/users/{id}/itineraries` 用 userId 查詢，userId 須為後端真實 ID（> 0）
- `RetrofitClient.tokenManager` 由 `MainViewModel.init` 注入，`ItineraryViewModel` 共用同一 singleton
- `GET /api/spots` 回傳的 user-created spot 可能缺少 `"id"` 欄位（後端問題），Gson 會將 `val id: Int` 設為 0 → 建立/編輯後不呼叫 `fetchAllSpots()`，改以 POST/PUT response 直接更新 `allSpots`
- `DELETE /api/spots/{id}` 成功回傳 **204 No Content**（無 body）；已修復 `BaseRepository.safeApiCall` 的 204 處理：`body()=null` 時改回傳 `NetworkResult.Success(Unit)` 而非 Error
- **v1.5**：`POST /api/itineraries` 的 `user_id` 現在從 JWT 取得（忽略 body 中的 user_id），Android 端不需傳也不應傳 user_id；新增 `is_public` 欄位支援
- **v1.6**：`GET /api/spots` 改用 optionalAuth（有 Token 顯示自己私人景點，無 Token 不回 401）；Spot 回應新增 `source`（"official"/"user"）和 `creator_id` 欄位；`PUT/DELETE /api/spots/{id}` 僅限建立者（403 Forbidden），系統景點不可改刪（403）；DELETE 引用保護：其他用戶行程已引用時回 400
- **v1.7（多語系 i18n）**：景點名稱/地址、行程標題/目的地、車輛名稱/描述、上下車地點等欄位後端改為 JSONB 儲存；**Android 端完全向後相容**（API 預設回傳 zh-TW 純文字字串，無需修改）；若未來需切換語言可加 `?lang=en` query 或 `Accept-Language` header
