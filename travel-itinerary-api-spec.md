# 旅遊行程規劃 API 規格文件

## 目錄
- [概述](#概述)
- [環境設定](#環境設定)
- [ER 關係圖](#er-關係圖)
- [資料表結構](#資料表結構)
- [認證機制](#認證機制)
- [API 端點](#api-端點)
- [錯誤處理](#錯誤處理)
- [Seed 資料](#seed-資料)
- [測試](#測試)
- [常用查詢範例](#常用查詢範例)
- [注意事項](#注意事項)
- [版本紀錄](#版本紀錄)

---

## 概述

本系統為旅遊行程規劃應用，支援以下核心功能：
- 用戶建立/瀏覽多天行程
- 每天可安排多個景點（含順序、時間）
- 景點間記錄交通方式與時間
- 景點支援關鍵字、分類、地理位置搜尋（Haversine 公式）
- 用戶可收藏景點、複製他人公開行程
- 支援官方推薦行程
- 行程發佈功能
- **Google OAuth 登入**（Access Token + Refresh Token，支援真正登出）

---

## 環境設定

### 系統需求
- Node.js
- PostgreSQL
- npm

### 環境變數 (.env)

```env
# 資料庫
DB_USER=dbmasteruser
DB_HOST=<your-db-host>
DB_NAME=postgres
DB_PASSWORD=<your-db-password>
DB_PORT=5432
PORT=5487

# Google OAuth & JWT（v1.2 新增）
GOOGLE_CLIENT_ID=<your-google-client-id>.apps.googleusercontent.com
JWT_SECRET=<random-64-char-hex>
JWT_EXPIRES_IN=1h
REFRESH_TOKEN_EXPIRES_DAYS=30
```

### 啟動指令

```bash
npm run migrate       # 執行初始資料庫 migration
npm run migrate:auth  # 執行 Google Auth migration（v1.2）
npm start             # 啟動 server (http://localhost:5487)
npm test              # 執行測試 (Jest + Supertest)
```

---

## 專案結構

```
fun2Go/
├── server.js                  # 伺服器入口
├── app.js                     # Express 應用 & 路由掛載
├── db.js                      # PostgreSQL 連線池
├── package.json
├── .env
├── controllers/               # 商業邏輯層
│   ├── authController.js      # Google 登入、refresh、logout（v1.2 新增）
│   ├── usersController.js
│   ├── spotsController.js
│   ├── itinerariesController.js
│   ├── itineraryDaysController.js
│   ├── itinerarySpotsController.js
│   └── favoritesController.js
├── routes/                    # API 路由
│   ├── auth.js                # /api/auth/*（v1.2 新增）
│   ├── users.js
│   ├── spots.js
│   ├── itineraries.js
│   ├── itinerarySpots.js
│   └── favorites.js
├── middleware/
│   ├── auth.js                # JWT 驗證 middleware（v1.2 新增）
│   └── errorHandler.js        # 集中式錯誤處理
├── migrations/
│   ├── 001_init.sql           # 資料庫初始化
│   └── 002_add_google_auth.sql # Google Auth（users.google_id + refresh_tokens）（v1.2 新增）
├── seeds/
│   └── seed.sql               # 測試用種子資料（台灣真實景點）
├── tests/                     # Jest 測試套件
│   ├── setup.js
│   ├── users.test.js
│   ├── spots.test.js
│   ├── itineraries.test.js
│   ├── itineraryDays.test.js
│   ├── itinerarySpots.test.js
│   └── favorites.test.js
└── docs/
    └── travel-itinerary-api-spec.md
```

---

## ER 關係圖

```
┌─────────────────┐       ┌─────────────────┐
│     users       │       │ refresh_tokens  │
│─────────────────│       │─────────────────│
│ id (PK)         │──1:N─►│ id (PK)         │
│ name            │       │ user_id (FK)    │
│ email           │       │ token           │
│ avatar_url      │       │ expires_at      │
│ google_id       │       └─────────────────┘
└────────┬────────┘
         │
         │ 1:N
         ▼
┌─────────────────┐       ┌─────────────────┐
│   itineraries   │       │     spots       │
│─────────────────│       │─────────────────│
│ id (PK)         │       │ id (PK)         │
│ user_id (FK)    │       │ name            │
│ title           │       │ address         │
│ destination     │       │ latitude        │
│ total_days      │       │ longitude       │
│ is_official     │       │ category        │
│ is_public       │       │ image_url       │
│ copy_count      │       │ google_place_id │
└────────┬────────┘       └────────┬────────┘
         │                         │
         │ 1:N                     │
         ▼                         │
┌─────────────────┐                │
│ itinerary_days  │                │
│─────────────────│                │
│ id (PK)         │                │
│ itinerary_id(FK)│                │
│ day_number      │                │
│ date            │                │
└────────┬────────┘                │
         │                         │
         │ 1:N                     │ 1:N
         ▼                         │
┌─────────────────┐                │
│ itinerary_spots │◄───────────────┘
│─────────────────│
│ id (PK)         │
│ itinerary_day_id│
│ spot_id (FK)    │
│ order_index     │
│ arrival_time    │
│ departure_time  │
└────────┬────────┘
         │
         │ 1:1 (from/to)
         ▼
┌─────────────────┐
│spot_transitions │
│─────────────────│
│ from_spot_id    │
│ to_spot_id      │
│ transport_mode  │
│ duration_minutes│
└─────────────────┘
```

### 關聯表

```
users ◄──N:M──► spots          (透過 user_favorites)
users ◄──N:M──► itineraries    (透過 user_itinerary_copies)
```

---

## 資料表結構

### 1. `users` - 用戶

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `name` | TEXT | ✓ | - | 用戶名稱 |
| `email` | TEXT | - | - | 電子郵件（唯一） |
| `avatar_url` | TEXT | - | - | 頭像網址 |
| `google_id` | TEXT | - | - | Google 帳號 ID（唯一，v1.2 新增，不對外回傳） |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |

---

### 2. `spots` - 景點/地點

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `name` | TEXT | ✓ | - | 景點名稱 |
| `address` | TEXT | - | - | 地址 |
| `latitude` | DECIMAL(10,8) | - | - | 緯度 |
| `longitude` | DECIMAL(11,8) | - | - | 經度 |
| `category` | TEXT | - | - | 類別（見下方列舉） |
| `image_url` | TEXT | - | - | 圖片網址 |
| `google_place_id` | TEXT | - | - | Google Places API ID |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |

**category 建議值：**
- `restaurant` - 餐廳
- `attraction` - 景點
- `night_market` - 夜市
- `accommodation` - 住宿
- `cafe` - 咖啡廳
- `shopping` - 購物

---

### 3. `itineraries` - 行程

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `user_id` | INTEGER | - | - | 建立者 (FK → users) |
| `title` | TEXT | ✓ | - | 行程標題 |
| `cover_image_url` | TEXT | - | - | 封面圖片 |
| `destination` | TEXT | - | - | 目的地 |
| `total_days` | INTEGER | ✓ | 1 | 總天數 |
| `is_official` | BOOLEAN | - | false | 是否官方推薦 |
| `is_public` | BOOLEAN | - | true | 是否公開 |
| `copy_count` | INTEGER | - | 0 | 被複製次數 |
| `published_at` | TIMESTAMP | - | - | 發佈時間 |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |
| `updated_at` | TIMESTAMP | - | now() | 更新時間（自動觸發） |

---

### 4. `itinerary_days` - 行程天數

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `itinerary_id` | INTEGER | ✓ | - | 所屬行程 (FK → itineraries) |
| `day_number` | INTEGER | ✓ | - | 第幾天 (1, 2, 3...) |
| `date` | DATE | - | - | 實際日期（選填） |
| `note` | TEXT | - | - | 當天備註 |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |

**唯一約束：** `(itinerary_id, day_number)`

---

### 5. `itinerary_spots` - 行程景點（核心關聯表）

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `itinerary_day_id` | INTEGER | ✓ | - | 所屬天 (FK → itinerary_days) |
| `spot_id` | INTEGER | ✓ | - | 景點 (FK → spots) |
| `order_index` | INTEGER | ✓ | - | 當天順序 (1, 2, 3...) |
| `arrival_time` | TIME | - | - | 抵達時間 (HH:MM) |
| `departure_time` | TIME | - | - | 離開時間 (HH:MM) |
| `is_custom_time` | BOOLEAN | - | false | 是否為用戶自訂時間 |
| `duration_minutes` | INTEGER | - | - | 預計停留時間（分鐘） |
| `note` | TEXT | - | - | 備註 |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |

**唯一約束：** `(itinerary_day_id, order_index)`

---

### 6. `spot_transitions` - 景點間交通

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `from_itinerary_spot_id` | INTEGER | ✓ | - | 起點 (FK → itinerary_spots) |
| `to_itinerary_spot_id` | INTEGER | ✓ | - | 終點 (FK → itinerary_spots) |
| `transport_mode` | TEXT | - | 'driving' | 交通方式 |
| `duration_minutes` | INTEGER | - | - | 交通時間（分鐘） |
| `distance_km` | DECIMAL(8,2) | - | - | 距離（公里） |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |

**transport_mode 建議值：**
- `driving` - 開車
- `walking` - 步行
- `transit` - 大眾運輸
- `cycling` - 騎車

**唯一約束：** `(from_itinerary_spot_id, to_itinerary_spot_id)`

---

### 7. `user_favorites` - 用戶收藏景點

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `user_id` | INTEGER | ✓ | - | 用戶 (FK → users) |
| `spot_id` | INTEGER | ✓ | - | 景點 (FK → spots) |
| `created_at` | TIMESTAMP | - | now() | 收藏時間 |

**唯一約束：** `(user_id, spot_id)`

---

### 8. `user_itinerary_copies` - 行程複製記錄

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `user_id` | INTEGER | ✓ | - | 複製者 (FK → users) |
| `original_itinerary_id` | INTEGER | ✓ | - | 原始行程 (FK → itineraries) |
| `copied_itinerary_id` | INTEGER | ✓ | - | 複製後行程 (FK → itineraries) |
| `created_at` | TIMESTAMP | - | now() | 複製時間 |

---

### 9. `refresh_tokens` - Refresh Token（v1.2 新增）

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `user_id` | INTEGER | ✓ | - | 用戶 (FK → users，CASCADE) |
| `token` | TEXT | ✓ | - | Refresh Token（唯一） |
| `expires_at` | TIMESTAMP | ✓ | - | 到期時間（預設 30 天） |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |

**唯一約束：** `token`

---

## 認證機制

### 流程概覽

```
Android App
  └─► Google Sign-In SDK → 取得 id_token
        └─► POST /api/auth/google { id_token }
              └─► 後端驗證 → 找或建立 User
                    └─► 回傳 { user, accessToken, refreshToken }

之後每個 API 請求：
  Authorization: Bearer <accessToken>

accessToken 過期時：
  POST /api/auth/refresh { refreshToken }
    └─► 回傳新的 { accessToken, refreshToken }（舊 refreshToken 立即失效）

登出：
  POST /api/auth/logout { refreshToken }
    └─► 刪除 DB 中的 refreshToken
```

### Token 規格

| Token | 演算法 | 有效期 | 說明 |
|-------|--------|--------|------|
| Access Token | JWT (HS256) | 1 小時 | 每次 API 請求帶在 Header |
| Refresh Token | Random Hex (80 chars) | 30 天 | 儲存於 DB，用於換新 Token |

### 路由保護規則

| 類型 | 路由 | 說明 |
|------|------|------|
| 公開 | `GET /api/spots` | 不需要 Token |
| 公開 | `GET /api/itineraries` | 不需要 Token |
| 公開 | `GET /api/itineraries/:id` | 不需要 Token |
| 公開 | `GET /api/spots/:id` | 不需要 Token |
| 公開 | `GET /api/users/:id` | 不需要 Token |
| 公開 | `GET /api/users/:id/itineraries` | 不需要 Token |
| 公開 | `GET /api/users/:id/favorites` | 不需要 Token |
| **需認證** | `POST /api/itineraries` | 需要 Bearer Token |
| **需認證** | `PUT/DELETE /api/itineraries/:id` | 需要 Bearer Token |
| **需認證** | `POST /api/itineraries/:id/copy` | 需要 Bearer Token |
| **需認證** | `POST /api/itineraries/:id/publish` | 需要 Bearer Token |
| **需認證** | `POST/PUT/DELETE /api/itineraries/:id/days` | 需要 Bearer Token |
| **需認證** | 所有 `/api/days/:dayId/spots` | 需要 Bearer Token |
| **需認證** | `POST /api/spots` | 需要 Bearer Token |
| **需認證** | `POST/DELETE /api/favorites` | 需要 Bearer Token |
| **需認證** | `PUT /api/users/:id` | 需要 Bearer Token |

> **注意**：`google_id` 欄位為內部欄位，所有 `/api/users` 回應均不包含此欄位。
>
> **上線前待辦**：`POST /api/users` 目前為公開端點，僅供開發/測試用。正式環境用戶應透過 `POST /api/auth/google` 建立，上線前請移除此 route（`routes/users.js` 第一行）。

---

## API 端點

### 認證 (Auth)（v1.2 新增）

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/auth/google` | Google 登入，回傳 accessToken + refreshToken | ❌ | ✅ |
| POST | `/api/auth/refresh` | 換新 Token（Rotation，舊 refreshToken 立即失效） | ❌ | ✅ |
| POST | `/api/auth/logout` | 登出，刪除 refreshToken | ❌ | ✅ |

**POST /api/auth/google**
```json
// Request
{ "id_token": "Google 回傳的 JWT" }

// Response 200
{
  "user": { "id": 1, "name": "王小明", "email": "...", "avatar_url": "..." },
  "accessToken": "eyJhbGci...",
  "refreshToken": "a3f8c2..."
}
```

**POST /api/auth/refresh**
```json
// Request
{ "refreshToken": "a3f8c2..." }

// Response 200
{ "accessToken": "eyJhbGci...", "refreshToken": "b9d1e4..." }
```

**POST /api/auth/logout**
```json
// Request
{ "refreshToken": "a3f8c2..." }

// Response 200
{ "message": "Logged out" }
```

### 用戶 (Users)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/users` | 建立用戶（開發/測試用，上線前應移除） | ❌ | ✅ |
| GET | `/api/users/:id` | 取得用戶資料 | ❌ | ✅ |
| PUT | `/api/users/:id` | 更新用戶資料（支援部分更新） | ✅ | ✅ |
| GET | `/api/users/:id/itineraries` | 取得用戶的行程列表 | ❌ | ✅ |
| GET | `/api/users/:id/favorites` | 取得用戶收藏的景點 | ❌ | ✅ |

### 行程 (Itineraries)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| GET | `/api/itineraries?limit=20&offset=0` | 列出公開行程（依 copy_count 排序，支援分頁） | ❌ | ✅ |
| POST | `/api/itineraries` | 建立新行程 | ✅ | ✅ |
| GET | `/api/itineraries/:id` | 取得行程詳情（含巢狀天數、景點、作者資訊） | ❌ | ✅ |
| PUT | `/api/itineraries/:id` | 更新行程基本資料（支援部分更新） | ✅ | ✅ |
| DELETE | `/api/itineraries/:id` | 刪除行程（CASCADE 刪除天數和景點） | ✅ | ✅ |
| POST | `/api/itineraries/:id/copy` | 複製行程（含天數、景點完整複製，Transaction） | ✅ | ✅ |
| POST | `/api/itineraries/:id/publish` | 發佈行程（設定 is_public + published_at） | ✅ | ✅ |

### 行程天數 (Itinerary Days)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/itineraries/:id/days` | 新增一天 | ✅ | ✅ |
| PUT | `/api/itineraries/:id/days/:dayId` | 更新某天資料 | ✅ | ✅ |
| DELETE | `/api/itineraries/:id/days/:dayId` | 刪除某天 | ✅ | ✅ |

### 行程景點 (Itinerary Spots)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/days/:dayId/spots` | 新增景點到某天 | ✅ | ✅ |
| PUT | `/api/days/:dayId/spots/:spotId` | 更新景點資料（時間、備註） | ✅ | ✅ |
| DELETE | `/api/days/:dayId/spots/:spotId` | 移除景點 | ✅ | ✅ |
| PUT | `/api/days/:dayId/spots/reorder` | 重新排序景點（Transaction，負數迴避唯一約束） | ✅ | ✅ |

### 景點 (Spots)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| GET | `/api/spots?keyword=&category=&lat=&lng=&radius=` | 搜尋景點（支援關鍵字 ILIKE、分類、地理位置 Haversine） | ❌ | ✅ |
| GET | `/api/spots/:id` | 取得景點詳情 | ❌ | ✅ |
| POST | `/api/spots` | 新增景點 | ✅ | ✅ |

### 收藏 (Favorites)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/favorites` | 收藏景點（body: user_id, spot_id） | ✅ | ✅ |
| DELETE | `/api/favorites/:spotId` | 取消收藏（body: user_id） | ✅ | ✅ |

---

## 錯誤處理

所有 API 統一回傳 JSON 格式錯誤：

```json
{ "error": "錯誤訊息" }
```

| HTTP 狀態碼 | 說明 | 範例場景 |
|:-----------:|------|----------|
| 400 | 缺少必填欄位 | 建立用戶缺少 name、建立行程缺少 title、auth 缺少 id_token |
| 401 | 未授權 | 未帶 Token、Token 無效或過期 |
| 404 | 資源不存在 | 查詢不存在的用戶、行程、景點 |
| 409 | 資源衝突（重複） | 重複收藏、重複 day_number、重複 order_index |
| 500 | 伺服器內部錯誤 | 資料庫連線失敗等非預期錯誤 |

---

## Seed 資料

執行 `seeds/seed.sql` 可載入台灣真實景點測試資料：

```bash
psql -h <DB_HOST> -U <DB_USER> -d <DB_NAME> -f seeds/seed.sql
```

### 內含資料

| 資料 | 筆數 | 說明 |
|------|:----:|------|
| 用戶 | 3 | 小明、小美、阿傑 |
| 景點 | 25 | 台北(8)、台中(5)、台南(4)、高雄(3)、花蓮(3)、住宿(2) |
| 行程 | 5 | 台北三日遊、台中兩日遊、南台灣四日遊、花蓮兩日遊、複製行程 |
| 天數 | 11 | 各行程的每日安排 |
| 行程景點 | 23 | 含抵達/離開時間、停留分鐘、備註 |
| 景點間交通 | 5 | 步行、大眾運輸，含距離公里數 |
| 用戶收藏 | 8 | 各用戶收藏的景點 |
| 複製記錄 | 1 | 小美複製小明的台北三日遊 |

---

## 測試

### 測試框架
- **Jest** - 測試執行器
- **Supertest** - HTTP 端點測試

### 執行測試

```bash
npm test
```

### 測試覆蓋（6 套件 / 56 測試）

| 測試套件 | 測試數 | 覆蓋範圍 |
|----------|:------:|----------|
| users.test.js | 8 | CRUD、404、部分更新、空列表 |
| spots.test.js | 8 | CRUD、關鍵字搜尋、分類搜尋、地理位置搜尋、複合搜尋、404 |
| itineraries.test.js | 12 | CRUD、分頁、巢狀查詢、複製(含驗證)、發佈、排序、400/404 |
| itineraryDays.test.js | 5 | 新增/更新/刪除、400/404/409 |
| itinerarySpots.test.js | 8 | 新增/更新/刪除/排序、400/404/409 |
| favorites.test.js | 5 | 收藏/取消收藏、400/404/409 |

### 已修復的 Bug

- **地理位置搜尋**：原本使用 `HAVING` 搭配 `SELECT *` 在 PostgreSQL 會報錯（缺少 `GROUP BY`），已改為子查詢 (subquery) 方式過濾 `distance_km`

---

## 常用查詢範例

### 1. 取得行程完整資料（含所有天數和景點）

```sql
SELECT
    i.*,
    u.name AS author_name,
    u.avatar_url AS author_avatar,
    COALESCE(
      json_agg(
        json_build_object(
            'day_id', d.id,
            'day_number', d.day_number,
            'date', d.date,
            'note', d.note,
            'spots', (
                SELECT COALESCE(json_agg(
                    json_build_object(
                        'itinerary_spot_id', isp.id,
                        'order_index', isp.order_index,
                        'arrival_time', isp.arrival_time,
                        'departure_time', isp.departure_time,
                        'duration_minutes', isp.duration_minutes,
                        'note', isp.note,
                        'spot', json_build_object(
                            'id', s.id,
                            'name', s.name,
                            'address', s.address,
                            'image_url', s.image_url,
                            'latitude', s.latitude,
                            'longitude', s.longitude,
                            'category', s.category
                        )
                    ) ORDER BY isp.order_index
                ), '[]'::json)
                FROM itinerary_spots isp
                JOIN spots s ON s.id = isp.spot_id
                WHERE isp.itinerary_day_id = d.id
            )
        ) ORDER BY d.day_number
      ) FILTER (WHERE d.id IS NOT NULL),
      '[]'::json
    ) AS days
FROM itineraries i
LEFT JOIN users u ON u.id = i.user_id
LEFT JOIN itinerary_days d ON d.itinerary_id = i.id
WHERE i.id = :itinerary_id
GROUP BY i.id, u.name, u.avatar_url;
```

### 2. 取得熱門公開行程（依複製次數排序）

```sql
SELECT
    i.*,
    u.name AS author_name,
    u.avatar_url AS author_avatar
FROM itineraries i
LEFT JOIN users u ON u.id = i.user_id
WHERE i.is_public = true
ORDER BY i.copy_count DESC, i.created_at DESC
LIMIT 20 OFFSET 0;
```

### 3. 複製行程（Transaction）

```sql
BEGIN;

-- 1. 複製行程主體
INSERT INTO itineraries (user_id, title, cover_image_url, destination, total_days, is_public)
SELECT :new_user_id, title, cover_image_url, destination, total_days, false
FROM itineraries WHERE id = :original_id
RETURNING id AS new_itinerary_id;

-- 2. 複製天數
INSERT INTO itinerary_days (itinerary_id, day_number, date, note)
SELECT :new_itinerary_id, day_number, NULL, note
FROM itinerary_days WHERE itinerary_id = :original_id;

-- 3. 複製景點（需要映射新的 day_id，由程式端處理）

-- 4. 更新原行程複製次數
UPDATE itineraries SET copy_count = copy_count + 1 WHERE id = :original_id;

-- 5. 記錄複製關係
INSERT INTO user_itinerary_copies (user_id, original_itinerary_id, copied_itinerary_id)
VALUES (:new_user_id, :original_id, :new_itinerary_id);

COMMIT;
```

### 4. 搜尋附近景點（Haversine 子查詢）

```sql
SELECT * FROM (
    SELECT *,
        (6371 * acos(
            cos(radians(:lat)) * cos(radians(latitude)) *
            cos(radians(longitude) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(latitude))
        )) AS distance_km
    FROM spots
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL
) AS sub
WHERE distance_km < :radius_km
ORDER BY distance_km
LIMIT 20;
```

### 5. 取得某天的景點與交通資訊

```sql
SELECT
    isp.order_index,
    isp.arrival_time,
    isp.departure_time,
    s.name AS spot_name,
    s.image_url,
    s.latitude,
    s.longitude,
    st.transport_mode,
    st.duration_minutes AS travel_time
FROM itinerary_spots isp
JOIN spots s ON s.id = isp.spot_id
LEFT JOIN spot_transitions st ON st.from_itinerary_spot_id = isp.id
WHERE isp.itinerary_day_id = :day_id
ORDER BY isp.order_index;
```

---

## 注意事項

1. **刪除行為**：所有外鍵都設定 `ON DELETE CASCADE`，刪除行程會自動刪除其下所有天數和景點關聯

2. **時間欄位**：`arrival_time` 和 `departure_time` 使用 TIME 類型，格式為 `HH:MM:SS`

3. **座標精度**：
   - 緯度 `DECIMAL(10,8)` 可精確到約 1.1mm
   - 經度 `DECIMAL(11,8)` 可精確到約 1.1mm

4. **自動更新**：`itineraries.updated_at` 會在更新時自動觸發更新

5. **索引**：已建立常用查詢的索引，如需額外索引請評估查詢模式

6. **景點重排序**：使用負數暫存策略避免唯一約束衝突（先設為負數，再設回正確值）

7. **地理搜尋**：使用 Haversine 公式計算球面距離，預設搜尋半徑 10km

---

## 版本紀錄

| 版本 | 日期 | 說明 |
|------|------|------|
| 1.0 | 2026-01-20 | 初版：資料表結構與 API 端點規劃 |
| 1.1 | 2026-01-30 | 所有 API 實作完成、56 項測試通過、Seed 資料、Bug 修復（地理搜尋查詢）、Port 改為 5487 |
| 1.2 | 2026-02-24 | Google OAuth 登入、Access Token + Refresh Token、路由保護（protected/public 分離）、users.google_id、refresh_tokens 表、migration 002 |
