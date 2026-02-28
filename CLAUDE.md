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
| API Key（不進 git）| `local.properties` → `MAPS_API_KEY=...` |

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
- 景點詳情 BottomSheet 有「＋ 加入列表」按鈕，可切換已加入／未加入狀態
- 底部「列表」按鈕開啟 `bottom_sheet_saved_list.xml`，顯示所有已儲存景點
- 列表中每個景點可點 X 刪除
- 使用 Room DB 持久化儲存（`saved_spots` 資料表）
- App 啟動時從 Room 載入已儲存景點（`hasLoadedFromDb` flag 防止重複載入）

### 4. 景點間距離估算（Haversine 公式）
- 列表中相鄰景點之間顯示距離與預估開車時間
- **不使用任何 API**，本地計算（原 Distance Matrix API 因 REQUEST_DENIED 棄用）
- 計算邏輯：直線距離 × 1.3（路程係數），平均時速 **25 km/h**（台灣市區）
- 距離格式：`X 公尺` / `X.X 公里` / `X 公里`
- 時間格式：`約 X 分鐘` / `約 Xh Xm`
- 位置：`MainViewModel.kt` → `calcDistanceInfo()`

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

### 7. FAB 新增行程（含匯入景點 + 加入行程）
- MainActivity FAB → `requireLogin` → `showCreateItinerarySheet()` → `POST /api/itineraries`
- 建立成功後：若「我的列表」有景點，彈出 AlertDialog 詢問是否匯入第一天
  - 「加入」→ `importSpotsToNewItinerary(itineraryId, spots)` → addDay(day_number=1) + 逐一 addSpotToDay → 導向 ItineraryDetailActivity
  - 「略過」→ 直接導向 ItineraryDetailActivity
- 景點詳情 BottomSheet 的「加入列表」按鈕：已登入且有行程時彈出行程選擇器
  - 選現有行程 → 加入本地列表 + `addSpotToExistingItinerary`（取最後一天或新建 Day 1）
  - 選「＋ 新增行程」→ 加入本地列表 + 開啟建立行程 Sheet
  - 選「只加入列表」→ 只加入本地列表
  - 已加入 → 直接移除，不顯示對話框
- `MainViewModel.cachedUserItineraries`：`onResume` + `loginResult` 成功後刷新快取

### 8. 行程列表（ItineraryListActivity）
- 底部導航「探索」按鈕（`btnNavExplore`）→ `requireLogin` → 開啟 `ItineraryListActivity`
- `ItineraryViewModel` 獨立管理行程列表與建立/編輯/刪除
- `onResume` 每次進入都刷新 `getUserItineraries(userId)`
- 列表項目支援：點擊進入詳情、編輯按鈕（修改標題/日期）
- FAB 可在列表頁建立新行程（不含匯入景點功能）
- `ItineraryAdapter`：顯示標題、日期範圍、天數（從 days.size 或日期差計算）

### 9. 行程詳情（ItineraryDetailActivity）
- `ItineraryDayAdapter` 顯示天數列表
- 右下角 FAB（`fabAddDay`）→ `addDay(itineraryId, currentDayCount + 1)` → 自動 reload
- `POST /api/itineraries/{id}/days` 需帶 `{ "day_number": N }` body（**重要**：沒有 body 會報錯）
- 空狀態文字提示「點擊右下角 ＋ 新增第一天」
- Toolbar 右上角刪除按鈕 → 確認 Dialog → `DELETE /api/itineraries/{id}` → finish()

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
    data class DistanceSeparator(val distanceText: String, val durationText: String) : SavedListItem()
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

## 已知待完成功能
- 底部導航「聊天」頁面

## 已知 API 注意事項
- `POST /api/itineraries/{id}/days` 必須帶 `{ "day_number": N }` body，否則後端報 destructure 錯誤
- `GET /api/users/{id}/itineraries` 用 userId 查詢，userId 須為後端真實 ID（> 0）
- `RetrofitClient.tokenManager` 由 `MainViewModel.init` 注入，`ItineraryViewModel` 共用同一 singleton
- `GET /api/spots` 回傳的 user-created spot 可能缺少 `"id"` 欄位（後端問題），Gson 會將 `val id: Int` 設為 0 → 建立/編輯後不呼叫 `fetchAllSpots()`，改以 POST/PUT response 直接更新 `allSpots`
- `DELETE /api/spots/{id}` 成功回傳 **204 No Content**（無 body）；已修復 `BaseRepository.safeApiCall` 的 204 處理：`body()=null` 時改回傳 `NetworkResult.Success(Unit)` 而非 Error
