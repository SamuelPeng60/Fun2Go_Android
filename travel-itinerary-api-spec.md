# 旅遊行程規劃 API 規格文件

## 目錄
- [概述](#概述)
- [環境設定](#環境設定)
- [ER 關係圖](#er-關係圖)
- [資料表結構](#資料表結構)
- [認證機制](#認證機制)
- [API 端點](#api-端點)
- [圖片上傳](#圖片上傳-upload)
- [錯誤處理](#錯誤處理)
- [Seed 資料](#seed-資料)
- [測試](#測試)
- [常用查詢範例](#常用查詢範例)
- [注意事項](#注意事項)
- [多語系 (i18n)](#多語系-i18n)
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
- **安全 Middleware**：Helmet、CORS、Rate Limit、Body Size Limit（v1.3）
- **資源所有權驗證**：修改/刪除行程僅限擁有者（v1.3）
- **Health Check**：`GET /health`（v1.3）
- **包車預訂**：車輛瀏覽 + 建立包車訂單 + 訂單管理（v1.4）
- **金流系統**：Mock 付款/退款，可擴充至真實支付閘道（v1.4）
- **景點擁有權**：creator_id 區分官方/使用者景點，is_public 控制可見性，PUT/DELETE 僅限建立者（v1.6）
- **多語系 (i18n)**：內容欄位支援 zh-TW / en / ja / vi 四種語言，透過 JSONB 儲存、`?lang=` 或 `Accept-Language` 切換語言（v1.7）
- **圖片上傳**：行動端上傳圖片至 S3，支援 sharp 壓縮 + WebP 轉換（v1.9）

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

# S3 圖片上傳（v1.9 新增）
S3_BUCKET=<your-s3-bucket>
S3_REGION=us-east-1
```

### 相依套件（v1.3 新增）
- `cors` - 跨域資源共享
- `helmet` - HTTP 安全標頭
- `express-rate-limit` - API 請求頻率限制
- `morgan` - HTTP 請求日誌
- `multer` - multipart/form-data 解析（圖片上傳，v1.9 新增）
- `sharp` - 圖片縮放 + WebP 轉換（v1.9 新增）
- `@aws-sdk/client-s3` - AWS S3 上傳（v1.9 新增）

### 啟動指令

```bash
npm install           # 安裝相依套件
npm run migrate       # 執行初始資料庫 migration
npm run migrate:auth  # 執行 Google Auth migration（v1.2）
npm run migrate:charter # 執行包車 & 金流 migration（v1.4）
npm run migrate:spots   # 執行景點 creator + 可見性 migration（v1.6）
npm run migrate:i18n    # 執行多語系 migration — TEXT→JSONB（v1.7）
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
│   ├── favoritesController.js
│   ├── vehiclesController.js   # 車輛 CRUD（v1.4 新增）
│   ├── ordersController.js     # 訂單 CRUD + 取消（v1.4 新增）
│   ├── paymentsController.js   # Mock 付款/退款（v1.4 新增）
│   └── uploadController.js    # 圖片上傳 handler（v1.9 新增）
├── utils/
│   ├── i18n.js                # i18n 工具函式：localize, localizeRow, toI18nValue, mergeI18nValue（v1.7 新增）
│   ├── uploadConfig.js        # 上傳常數：folder、大小限制、MIME type、寬度（v1.9 新增）
│   └── s3.js                  # S3 client + uploadToS3（v1.9 新增）
├── routes/                    # API 路由
│   ├── auth.js                # /api/auth/*（v1.2 新增）
│   ├── users.js
│   ├── spots.js
│   ├── itineraries.js
│   ├── itinerarySpots.js
│   ├── favorites.js
│   ├── vehicles.js             # /api/vehicles/*（v1.4 新增）
│   ├── orders.js               # /api/orders/* + 付款路由（v1.4 新增）
│   └── upload.js               # /api/upload（v1.9 新增）
├── middleware/
│   ├── auth.js                # JWT 驗證 middleware（v1.2 新增）
│   ├── optionalAuth.js        # Optional JWT — 有 token 解析，無 token 不擋（v1.6 新增）
│   ├── locale.js              # 語言偵測 middleware：?lang= → Accept-Language → zh-TW（v1.7 新增）
│   └── errorHandler.js        # 集中式錯誤處理
├── migrations/
│   ├── 001_init.sql           # 資料庫初始化
│   ├── 002_add_google_auth.sql # Google Auth（users.google_id + refresh_tokens）（v1.2 新增）
│   ├── 003_add_charter_and_payments.sql # 包車 & 金流（vehicles, orders, charter_bookings, payments）（v1.4 新增）
│   ├── 004_add_spot_creator.sql  # 景點 creator_id + is_public（v1.6 新增）
│   ├── 005_add_i18n.sql         # 多語系：10 個 TEXT 欄位轉 JSONB（v1.7 新增）
│   └── 006_add_vietnamese.sql   # 越南文：spots.name->>'vi' 索引（v1.8 新增）
├── seeds/
│   └── seed.sql               # 測試用種子資料（台灣真實景點）
├── tests/                     # Jest 測試套件
│   ├── setup.js
│   ├── users.test.js
│   ├── spots.test.js
│   ├── itineraries.test.js
│   ├── itineraryDays.test.js
│   ├── itinerarySpots.test.js
│   ├── favorites.test.js
│   ├── vehicles.test.js        # 車輛 CRUD 測試（v1.4 新增）
│   ├── orders.test.js          # 訂單 CRUD + 取消測試（v1.4 新增）
│   ├── payments.test.js        # 付款/退款測試（v1.4 新增）
│   ├── i18n.test.js           # 多語系測試：locale middleware、CRUD、搜尋、巢狀（v1.7 新增）
│   └── upload.test.js         # 圖片上傳測試：auth、驗證、S3 URL（v1.9 新增）
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
│ user_id (FK)    │       │ name (JSONB)    │
│ title (JSONB)   │       │ address (JSONB) │
│ destination(JB) │       │ latitude        │
│ total_days      │       │ longitude       │
│ is_official     │       │ category        │
│ is_public       │       │ image_url       │
│ copy_count      │       │ google_place_id │
└────────┬────────┘       │ creator_id (FK) │◄── users (v1.6)
                          │ is_public       │
                          └────────┬────────┘
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

### 包車 & 金流模組（v1.4 新增）

```
┌─────────────────┐
│    vehicles     │
│─────────────────│
│ id (PK)         │
│ name (JSONB)    │
│ type            │
│ capacity        │
│ price_per_day   │
│ is_available    │
└────────┬────────┘
         │
         │ 1:N
         │
┌────────┴────────┐       ┌─────────────────┐
│ charter_bookings│       │     orders      │
│─────────────────│       │─────────────────│
│ id (PK)         │  1:1  │ id (PK)         │
│ order_id (FK) ──┼───────┤ user_id (FK)    │──► users
│ vehicle_id (FK) │       │ itinerary_id(FK)│──► itineraries (nullable)
│ pickup_loc(JB)  │       │ order_type      │
│ pickup_time     │       │ status          │
│ days            │       │ total_amount    │
│ passenger_count │       └────────┬────────┘
│ contact_name    │                │
│ contact_phone   │                │ 1:N
└─────────────────┘                ▼
                          ┌─────────────────┐
                          │    payments     │
                          │─────────────────│
                          │ id (PK)         │
                          │ order_id (FK)   │
                          │ amount          │
                          │ method          │
                          │ status          │
                          │ transaction_id  │
                          │ paid_at         │
                          └─────────────────┘
```

### 關聯表

```
users ◄──N:M──► spots          (透過 user_favorites)
users ◄──N:M──► itineraries    (透過 user_itinerary_copies)
users ──1:N──► orders          (用戶的訂單)
orders ──1:1──► charter_bookings (包車明細)
orders ──1:N──► payments       (付款記錄)
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
| `name` | JSONB | ✓ | - | 景點名稱（多語系，v1.7 由 TEXT 轉換） |
| `address` | JSONB | - | - | 地址（多語系，v1.7 由 TEXT 轉換） |
| `latitude` | DECIMAL(10,8) | - | - | 緯度 |
| `longitude` | DECIMAL(11,8) | - | - | 經度 |
| `category` | TEXT | - | - | 類別（見下方列舉） |
| `image_url` | TEXT | - | - | 圖片網址 |
| `google_place_id` | TEXT | - | - | Google Places API ID |
| `creator_id` | INTEGER | - | NULL | 建立者 (FK → users，ON DELETE SET NULL)；NULL 代表系統預設景點（v1.6 新增） |
| `is_public` | BOOLEAN | - | true | 是否公開；使用者建立景點時預設 false（v1.6 新增） |
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
| `title` | JSONB | ✓ | - | 行程標題（多語系，v1.7 由 TEXT 轉換） |
| `cover_image_url` | TEXT | - | - | 封面圖片 |
| `destination` | JSONB | - | - | 目的地（多語系，v1.7 由 TEXT 轉換） |
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
| `note` | JSONB | - | - | 當天備註（多語系，v1.7 由 TEXT 轉換） |
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
| `note` | JSONB | - | - | 備註（多語系，v1.7 由 TEXT 轉換） |
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

### 10. `vehicles` - 可預訂車輛（v1.4 新增）

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `name` | JSONB | ✓ | - | 車輛名稱（多語系，如 `{"zh-TW":"豪華九人座","en":"Luxury Van"}`，v1.7 由 TEXT 轉換） |
| `type` | TEXT | ✓ | - | 車輛類型（van_9, sedan_4, bus_20） |
| `capacity` | INTEGER | ✓ | - | 座位數 |
| `price_per_day` | NUMERIC(10,2) | ✓ | - | 每日價格 |
| `image_url` | TEXT | - | - | 車輛圖片 |
| `description` | JSONB | - | - | 車輛描述（多語系，v1.7 由 TEXT 轉換） |
| `is_available` | BOOLEAN | - | true | 是否可預訂 |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |

---

### 11. `orders` - 通用訂單表（v1.4 新增）

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `user_id` | INTEGER | ✓ | - | 下單用戶 (FK → users，CASCADE) |
| `itinerary_id` | INTEGER | - | - | 關聯行程 (FK → itineraries，SET NULL)，可選 |
| `order_type` | TEXT | ✓ | - | 訂單類型（'charter'，未來: 'ticket', 'hotel'） |
| `status` | TEXT | ✓ | 'pending' | 訂單狀態（pending → confirmed → completed / cancelled） |
| `total_amount` | NUMERIC(10,2) | ✓ | - | 訂單總金額 |
| `note` | TEXT | - | - | 訂單備註 |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |
| `updated_at` | TIMESTAMP | - | now() | 更新時間（自動觸發） |

**訂單狀態流程：**
```
pending ──pay──→ confirmed ──complete──→ completed
   │                 │
   └──cancel──→ cancelled ←──refund──┘
```

---

### 12. `charter_bookings` - 包車訂單明細（v1.4 新增）

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `order_id` | INTEGER | ✓ | - | 所屬訂單 (FK → orders，CASCADE，UNIQUE) |
| `vehicle_id` | INTEGER | ✓ | - | 預訂車輛 (FK → vehicles，RESTRICT) |
| `pickup_location` | JSONB | ✓ | - | 上車地點（多語系，v1.7 由 TEXT 轉換） |
| `dropoff_location` | JSONB | - | - | 下車地點（多語系，v1.7 由 TEXT 轉換） |
| `pickup_time` | TIMESTAMP | ✓ | - | 上車時間 |
| `dropoff_time` | TIMESTAMP | - | - | 下車時間 |
| `days` | INTEGER | ✓ | 1 | 包車天數 |
| `passenger_count` | INTEGER | ✓ | - | 乘客人數 |
| `contact_name` | TEXT | ✓ | - | 聯絡人姓名 |
| `contact_phone` | TEXT | ✓ | - | 聯絡人電話 |
| `special_requests` | TEXT | - | - | 特殊需求 |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |

**唯一約束：** `order_id`（1:1 對應 order）

---

### 13. `payments` - 付款記錄（v1.4 新增）

| 欄位 | 類型 | 必填 | 預設值 | 說明 |
|------|------|:----:|--------|------|
| `id` | SERIAL | ✓ | auto | 主鍵 |
| `order_id` | INTEGER | ✓ | - | 所屬訂單 (FK → orders，CASCADE) |
| `amount` | NUMERIC(10,2) | ✓ | - | 金額 |
| `method` | TEXT | ✓ | 'mock' | 付款方式（'mock'，未來: 'credit_card', 'line_pay'） |
| `status` | TEXT | ✓ | 'pending' | 付款狀態（pending → paid / refunded / failed） |
| `transaction_id` | TEXT | - | - | 外部交易 ID（mock 時自動產生 UUID） |
| `metadata` | JSONB | - | '{}' | 保留給未來閘道回傳資料 |
| `paid_at` | TIMESTAMP | - | - | 付款時間 |
| `created_at` | TIMESTAMP | - | now() | 建立時間 |

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
| 公開 | `GET /health` | Health Check，不需要 Token（v1.3） |
| 公開 | `GET /api/spots` | 不需要 Token |
| 公開 | `GET /api/itineraries` | 不需要 Token |
| 公開 | `GET /api/itineraries/:id` | 不需要 Token |
| 公開 | `GET /api/spots/:id` | 不需要 Token |
| 公開 | `GET /api/users/:id` | 不需要 Token |
| 公開 | `GET /api/users/:id/itineraries` | 不需要 Token |
| 公開 | `GET /api/users/:id/favorites` | 不需要 Token |
| **需認證** | `POST /api/itineraries` | 需要 Bearer Token |
| **需認證+Owner** | `PUT/DELETE /api/itineraries/:id` | 需要 Bearer Token，**僅行程擁有者**（v1.3） |
| **需認證** | `POST /api/itineraries/:id/copy` | 需要 Bearer Token，自動使用 Token 中的 user_id（v1.3） |
| **需認證+Owner** | `POST /api/itineraries/:id/publish` | 需要 Bearer Token，**僅行程擁有者**（v1.3） |
| **需認證+Owner** | `POST/PUT/DELETE /api/itineraries/:id/days` | 需要 Bearer Token，**僅行程擁有者**（v1.3） |
| **需認證+Owner** | 所有 `/api/days/:dayId/spots` | 需要 Bearer Token，**透過 day→itinerary 驗證擁有者**（v1.3） |
| **Optional Auth** | `GET /api/spots` | 有 Token 時可見自己的私人景點（v1.6） |
| **需認證** | `POST /api/spots` | 需要 Bearer Token |
| **需認證+Creator** | `PUT/DELETE /api/spots/:id` | 需要 Bearer Token，**僅限景點建立者**，系統景點不可改刪（v1.6） |
| **需認證** | `POST/DELETE /api/favorites` | 需要 Bearer Token |
| **需認證+本人** | `PUT /api/users/:id` | 需要 Bearer Token，**僅能更新自己** (403)（v1.3） |
| 公開 | `GET /api/vehicles` | 不需要 Token（v1.4） |
| 公開 | `GET /api/vehicles/:id` | 不需要 Token（v1.4） |
| **需認證** | `POST/PUT/DELETE /api/vehicles` | 需要 Bearer Token（v1.4） |
| **需認證+Owner** | 所有 `/api/orders/*` | 需要 Bearer Token，**僅限訂單擁有者**（v1.4） |
| **需認證** | `POST /api/upload` | 需要 Bearer Token（v1.9） |

> **注意**：`google_id` 欄位為內部欄位，所有 `/api/users` 回應均不包含此欄位。
>
> **上線前待辦**：`POST /api/users` 目前為公開端點，僅供開發/測試用。正式環境用戶應透過 `POST /api/auth/google` 建立，上線前請移除此 route（`routes/users.js` 第一行）。

---

## API 端點

### Health Check（v1.3 新增）

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| GET | `/health` | 健康檢查 | ❌ | ✅ |

```json
// Response 200
{ "status": "ok", "timestamp": "2026-02-27T12:12:13.583Z" }
```

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
| PUT | `/api/users/:id` | 更新用戶資料（支援部分更新，**僅限本人** v1.3） | ✅ | ✅ |
| GET | `/api/users/:id/itineraries` | 取得用戶的行程列表 | ❌ | ✅ |
| GET | `/api/users/:id/favorites` | 取得用戶收藏的景點 | ❌ | ✅ |

**POST /api/users（開發/測試用）**

```json
// Request
{
  "name": "王小明",
  "email": "ming@example.com",
  "avatar_url": "https://example.com/avatar.jpg"
}

// Response 201
{
  "id": 1,
  "name": "王小明",
  "email": "ming@example.com",
  "avatar_url": "https://example.com/avatar.jpg",
  "created_at": "..."
}
```

**GET /api/users/:id**

```json
// Response 200
{
  "id": 1,
  "name": "王小明",
  "email": "ming@example.com",
  "avatar_url": "https://example.com/avatar.jpg",
  "created_at": "..."
}
```

**PUT /api/users/:id（僅限本人，支援部分更新）**

```json
// Request — 僅需傳送要更新的欄位
// Authorization: Bearer <token>
{
  "name": "新名字",
  "avatar_url": "https://cdn.example.com/new-avatar.webp"
}

// Response 200
{
  "id": 1,
  "name": "新名字",
  "email": "ming@example.com",
  "avatar_url": "https://cdn.example.com/new-avatar.webp",
  "created_at": "..."
}
```

> **可更新欄位**：`name`、`email`、`avatar_url`（皆為選填，未傳的欄位保持不變）

### 行程 (Itineraries)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| GET | `/api/itineraries?limit=20&offset=0&lang=en` | 列出公開行程（依 copy_count 排序，limit 上限 100，支援 `&format=v2` 分頁格式 v1.3，`&lang=` 切換語系 v1.7） | ❌ | ✅ |
| POST | `/api/itineraries` | 建立新行程 | ✅ | ✅ |
| GET | `/api/itineraries/:id` | 取得行程詳情（含巢狀天數、景點、作者資訊） | ❌ | ✅ |
| PUT | `/api/itineraries/:id` | 更新行程基本資料（支援部分更新，**僅限擁有者** v1.3） | ✅ | ✅ |
| DELETE | `/api/itineraries/:id` | 刪除行程（CASCADE 刪除天數和景點，**僅限擁有者** v1.3） | ✅ | ✅ |
| POST | `/api/itineraries/:id/copy` | 複製行程（自動使用 Token user_id，不再需要 body 傳 user_id v1.3） | ✅ | ✅ |
| POST | `/api/itineraries/:id/publish` | 發佈行程（設定 is_public + published_at，**僅限擁有者** v1.3） | ✅ | ✅ |

**GET /api/itineraries 分頁格式（v1.3）**

```
# 預設格式（向下相容）：回傳 array
GET /api/itineraries?limit=20&offset=0
→ [{
    id: 1, title: "...", cover_image_url: "https://...",
    author_name: "王小明", author_avatar: "https://..."
  }, ...]

# v2 格式：回傳含分頁資訊的 object
GET /api/itineraries?limit=20&offset=0&format=v2
→ {
    "data": [{
      id: 1, title: "...", cover_image_url: "https://...",
      author_name: "王小明", author_avatar: "https://..."
    }, ...],
    "pagination": { "total": 50, "limit": 20, "offset": 0 }
  }
```

> **列表欄位說明**：每筆行程額外包含 `author_name`（作者名稱）、`author_avatar`（作者頭像 URL）、`cover_image_url`（行程封面圖 URL），皆可能為 `null`。

**POST /api/itineraries（v1.5 變更，v1.7 i18n）**

```json
// Request — user_id 從 JWT 取得，不需在 body 傳送
// Authorization: Bearer <token>

// 寫法 A：純字串（自動包成 {"zh-TW": "台北三日遊"}，依 ?lang= 或 Accept-Language 決定語系）
{
  "title": "台北三日遊",
  "total_days": 3,
  "destination": "台北",
  "cover_image_url": "https://cdn.example.com/images/taipei.webp"
}

// 寫法 B：多語系 JSONB 物件（一次寫入多個語言）
{
  "title": {"zh-TW": "台北三日遊", "en": "3-Day Taipei Trip", "ja": "台北3日間の旅"},
  "total_days": 3,
  "destination": {"zh-TW": "台北", "en": "Taipei", "ja": "台北"},
  "cover_image_url": "https://cdn.example.com/images/taipei.webp"
}

// Response 201（回傳純文字，依請求語言解析）
{
  "id": 1,
  "user_id": 1,
  "title": "台北三日遊",
  "cover_image_url": "https://cdn.example.com/images/taipei.webp",
  "total_days": 3,
  "destination": "台北",
  "is_public": false,
  "copy_count": 0,
  "created_at": "..."
}
```

> **安全性**：即使 body 中帶了 `user_id`，server 也會忽略，一律使用 JWT Token 中的 `req.user.id`。

**PUT /api/itineraries/:id（支援部分更新，僅限擁有者）**

```json
// Request — 僅需傳送要更新的欄位
// Authorization: Bearer <token>
{
  "title": "台北四日遊",
  "cover_image_url": "https://cdn.example.com/images/new-cover.webp",
  "total_days": 4
}

// Response 200
{
  "id": 1,
  "user_id": 1,
  "title": "台北四日遊",
  "cover_image_url": "https://cdn.example.com/images/new-cover.webp",
  "total_days": 4,
  "destination": "台北",
  "is_public": false,
  "copy_count": 0,
  "created_at": "..."
}
```

> **可更新欄位**：`title`、`cover_image_url`、`destination`、`total_days`、`is_public`（皆為選填，未傳的欄位保持不變）。`title` 和 `destination` 支援 i18n merge（傳入的語言覆寫，其他語言保留）。

**POST /api/itineraries/:id/copy（v1.3 變更）**

```json
// v1.2 之前：需要在 body 傳 user_id
{ "user_id": 123 }

// v1.3 起：自動使用 Token 中的 user_id，body 可為空
// Authorization: Bearer <token>
{}
```

### 行程天數 (Itinerary Days)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/itineraries/:id/days` | 新增一天（**僅限擁有者** v1.3） | ✅ | ✅ |
| PUT | `/api/itineraries/:id/days/:dayId` | 更新某天資料（**僅限擁有者** v1.3） | ✅ | ✅ |
| DELETE | `/api/itineraries/:id/days/:dayId` | 刪除某天（**僅限擁有者** v1.3） | ✅ | ✅ |

### 行程景點 (Itinerary Spots)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/days/:dayId/spots` | 新增景點到某天（**僅限擁有者** v1.3） | ✅ | ✅ |
| PUT | `/api/days/:dayId/spots/:spotId` | 更新景點資料（時間、備註）（**僅限擁有者** v1.3） | ✅ | ✅ |
| DELETE | `/api/days/:dayId/spots/:spotId` | 移除景點（**僅限擁有者** v1.3） | ✅ | ✅ |
| PUT | `/api/days/:dayId/spots/reorder` | 重新排序景點（批次 CASE 語句優化 v1.3，**僅限擁有者**） | ✅ | ✅ |

### 景點 (Spots)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| GET | `/api/spots?keyword=&category=&lat=&lng=&radius=&lang=en` | 搜尋景點（可見性過濾 v1.6，`&lang=` 切換語系 v1.7，關鍵字搜尋所有語系） | Optional | ✅ |
| GET | `/api/spots/:id` | 取得景點詳情（含 `source` 欄位） | ❌ | ✅ |
| POST | `/api/spots` | 新增景點（自動設 `creator_id`，預設 `is_public=false`） | ✅ | ✅ |
| PUT | `/api/spots/:id` | 更新景點（支援部分更新，**僅限建立者**，系統景點不可改 v1.6） | ✅ | ✅ |
| DELETE | `/api/spots/:id` | 刪除景點（**僅限建立者**，系統景點不可刪，他人行程引用中拒絕 v1.6） | ✅ | ✅ |

**GET /api/spots 可見性邏輯（v1.6）**

| 呼叫者 | 可見範圍 |
|--------|----------|
| 未登入（無 Token） | 系統景點（creator_id=NULL）+ 公開景點（is_public=true） |
| 已登入（有 Token） | 上述 + 自己的私人景點（creator_id=自己, is_public=false） |

**Spot 回應範例（v1.6，v1.7 i18n：name/address 依語言回傳純文字）**
```json
{
  "id": 1,
  "name": "Taipei 101",           // ?lang=en 時回傳英文
  "address": "Xinyi District",    // ?lang=en 時回傳英文
  "latitude": "25.03390000",
  "longitude": "121.56450000",
  "category": "attraction",
  "image_url": null,
  "google_place_id": null,
  "creator_id": 5,
  "is_public": false,
  "source": "user",
  "created_at": "..."
}
```

> `source` 為 computed field：`creator_id IS NULL` → `"official"`，否則 `"user"`

**PUT /api/spots/:id（v1.6，v1.7 i18n）**
```json
// Request（部分更新，僅送需改的欄位；name 可傳純字串或 JSONB）
// 純字串：只更新當前語言，保留其他語系翻譯
{ "name": "新名稱", "is_public": true }
// JSONB：一次更新多個語言
// { "name": {"zh-TW": "新名稱", "en": "New Name"}, "is_public": true }

// Response 200
{ "id": 1, "name": "新名稱", "is_public": true, "source": "user", "..." }
```

| 狀態碼 | 條件 |
|:------:|------|
| 200 | 成功更新 |
| 401 | 未帶 Token |
| 403 | 系統景點（"Cannot modify a system default spot"）或非建立者（"Forbidden"） |
| 404 | 景點不存在 |

**DELETE /api/spots/:id（v1.6）**

| 狀態碼 | 條件 |
|:------:|------|
| 204 | 成功刪除（無回傳 body） |
| 400 | 其他用戶的行程引用此景點（"Cannot delete spot referenced by other users' itineraries"） |
| 401 | 未帶 Token |
| 403 | 系統景點（"Cannot delete a system default spot"）或非建立者（"Forbidden"） |
| 404 | 景點不存在 |

### 收藏 (Favorites)

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/favorites` | 收藏景點（body: user_id, spot_id） | ✅ | ✅ |
| DELETE | `/api/favorites/:spotId` | 取消收藏（body: user_id） | ✅ | ✅ |

### 車輛 (Vehicles)（v1.4 新增）

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| GET | `/api/vehicles?type=van_9&available=true&lang=en` | 列出車輛（支援類型、可用性篩選，`&lang=` 切換語系 v1.7） | ❌ | ✅ |
| GET | `/api/vehicles/:id` | 取得車輛詳情 | ❌ | ✅ |
| POST | `/api/vehicles` | 新增車輛（未來限管理員） | ✅ | ✅ |
| PUT | `/api/vehicles/:id` | 更新車輛（支援部分更新） | ✅ | ✅ |
| DELETE | `/api/vehicles/:id` | 刪除車輛 | ✅ | ✅ |

**vehicle type 建議值：**
- `sedan_4` - 四人轎車
- `van_9` - 九人座
- `bus_20` - 20人座巴士

### 訂單 (Orders)（v1.4 新增）

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/orders` | 建立訂單 + 包車明細（一次完成，自動計算金額） | ✅ | ✅ |
| GET | `/api/orders?status=pending&limit=20&offset=0` | 我的訂單列表（自動篩 user_id，支援 status 篩選、分頁） | ✅ | ✅ |
| GET | `/api/orders/:id` | 訂單詳情（含包車明細 + 車輛資訊 + 付款記錄） | ✅ | ✅ |
| PUT | `/api/orders/:id` | 更新訂單（僅 pending 狀態可改，**僅限擁有者**） | ✅ | ✅ |
| POST | `/api/orders/:id/cancel` | 取消訂單（僅 pending 狀態，**僅限擁有者**） | ✅ | ✅ |

**POST /api/orders（v1.7 i18n：pickup_location / dropoff_location 支援多語系）**
```json
// Request — pickup_location / dropoff_location 可傳純字串或 JSONB 物件
{
  "order_type": "charter",
  "itinerary_id": 1,
  "charter": {
    "vehicle_id": 2,
    "pickup_location": "台北車站",
    "dropoff_location": "九份老街",
    "pickup_time": "2026-03-15T09:00:00",
    "days": 1,
    "passenger_count": 6,
    "contact_name": "小明",
    "contact_phone": "0912345678"
  }
}

// Response 201（i18n 欄位回傳純文字，依請求語言解析）
{
  "id": 1,
  "user_id": 1,
  "itinerary_id": 1,
  "order_type": "charter",
  "status": "pending",
  "total_amount": "5000.00",
  "charter_booking": {
    "id": 1,
    "order_id": 1,
    "vehicle_id": 2,
    "pickup_location": "台北車站",
    "dropoff_location": "九份老街",
    "pickup_time": "2026-03-15T09:00:00",
    "days": 1,
    "passenger_count": 6,
    "contact_name": "小明",
    "contact_phone": "0912345678"
  },
  "created_at": "..."
}
```

**GET /api/orders/:id**（詳情含車輛資訊 + 付款記錄）
```json
{
  "id": 1,
  "status": "confirmed",
  "total_amount": "5000.00",
  "charter_booking": {
    "vehicle_name": "豪華九人座",
    "vehicle_type": "van_9",
    "vehicle_capacity": 9,
    "pickup_location": "台北車站",
    "..."
  },
  "payments": [
    { "id": 1, "amount": "5000.00", "status": "paid", "transaction_id": "mock_xxx", "..." }
  ]
}
```

### 付款 (Payments)（v1.4 新增）

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/orders/:id/pay` | Mock 付款（立即成功，order → confirmed） | ✅ | ✅ |
| GET | `/api/orders/:id/payments` | 該訂單的付款紀錄 | ✅ | ✅ |
| POST | `/api/orders/:id/refund` | Mock 退款（僅限 confirmed 狀態，order → cancelled） | ✅ | ✅ |

**POST /api/orders/:id/pay**
```json
// Response 200
{
  "id": 1,
  "order_id": 1,
  "amount": "5000.00",
  "method": "mock",
  "status": "paid",
  "transaction_id": "mock_xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "paid_at": "2026-03-01T10:30:00.000Z",
  "created_at": "..."
}
```

**POST /api/orders/:id/refund**
```json
// Response 200
{
  "id": 2,
  "order_id": 1,
  "amount": "5000.00",
  "method": "mock",
  "status": "refunded",
  "transaction_id": "mock_refund_xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "paid_at": "2026-03-02T14:00:00.000Z",
  "created_at": "..."
}
```

### 圖片上傳 (Upload)（v1.9 新增）

| Method | Endpoint | 說明 | Auth | 狀態 |
|--------|----------|------|:----:|:----:|
| POST | `/api/upload` | 上傳圖片至 S3 | ✅ | ✅ |

**Request**：`multipart/form-data`

| 欄位 | 類型 | 必填 | 說明 |
|------|------|:----:|------|
| `file` | File | ✅ | 圖片檔案（JPEG/PNG/WebP/GIF，≤ 5 MB） |
| `folder` | String | ✅ | 上傳目錄：`spots`、`vehicles`、`itineraries` |

```bash
# 範例
curl -X POST http://localhost:5487/api/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@photo.jpg" \
  -F "folder=spots"
```

```json
// Response 201
{
  "url": "https://bucket-kk61j7.s3.us-east-1.amazonaws.com/images/spots/1709366400000-a1b2c3d4.webp"
}
```

**處理流程**：
1. JWT 認證
2. 驗證 file 存在、folder 有效、MIME type 允許、檔案大小 ≤ 5 MB
3. sharp 處理：resize（spots/vehicles: 1200px, itineraries: 1600px）→ WebP quality 80 → `withoutEnlargement: true`
4. 上傳至 S3（`images/{folder}/{timestamp}-{random}.webp`，`public-read`）
5. 回傳公開 URL

**錯誤回應**：

```json
// 401 — 未帶 Token
{ "error": "Unauthorized" }

// 400 — 缺少檔案
{ "error": "No file uploaded" }

// 400 — 缺少 folder
{ "error": "folder is required" }

// 400 — 無效 folder
{ "error": "Invalid folder. Must be one of: spots, vehicles, itineraries" }

// 400 — 無效檔案類型
{ "error": "Invalid file type. Accepted: image/jpeg, image/png, image/webp, image/gif" }

// 400 — 檔案過大
{ "error": "File too large. Maximum size is 5 MB" }
```

> **行動端使用流程**：先呼叫 `POST /api/upload` 上傳圖片取得 URL，再將 URL 帶入 `POST /api/spots`（`image_url` 欄位）或其他建立/更新 API。

---

## 錯誤處理

所有 API 統一回傳 JSON 格式錯誤：

```json
{ "error": "錯誤訊息" }
```

| HTTP 狀態碼 | 說明 | 範例場景 |
|:-----------:|------|----------|
| 400 | 缺少必填欄位 / 輸入驗證失敗 | 缺少 name、title、id_token；lat/lng/radius 超出範圍（v1.3）；FK 違反；型別驗證失敗（非整數/非正數）；無效 JSON body；無效 input 格式（PG 22P02）；CHECK 約束違反（PG 23514）；檔案過大/無效檔案類型/缺少上傳欄位（multer，v1.9） |
| 401 | 未授權 | 未帶 Token、Token 無效或過期 |
| 403 | 禁止操作（v1.3） | 修改/刪除他人行程、更新他人用戶資料 |
| 404 | 資源不存在 | 查詢不存在的用戶、行程、景點；嘗試操作非自己的行程也回 404（避免洩漏資源存在） |
| 409 | 資源衝突（重複） | 重複收藏、重複 day_number、重複 order_index |
| 429 | 請求過於頻繁（v1.3） | 超過 Rate Limit（每 IP 15 分鐘 300 次） |
| 500 | 伺服器內部錯誤 | 資料庫連線失敗等非預期錯誤（production 不洩漏 stack trace） |

---

## Seed 資料

執行 `seeds/seed.sql` 可載入台灣真實景點測試資料：

```bash
psql -h <DB_HOST> -U <DB_USER> -d <DB_NAME> -f seeds/seed.sql
```

### 內含資料

| 資料 | 筆數 | 說明 |
|------|:----:|------|
| 用戶 | 4 | 小明、小美、阿傑、OpenClaw Agent（id=6，系統用戶） |
| 景點 | 28 | 台北(8)、台中(5)、台南(4)、高雄(3)、花蓮(3)、住宿(2)、**使用者建立(3)**（v1.6：含公開+私人） |
| 行程 | 5 | 台北三日遊、台中兩日遊、南台灣四日遊、花蓮兩日遊、複製行程 |
| 天數 | 11 | 各行程的每日安排 |
| 行程景點 | 23 | 含抵達/離開時間、停留分鐘、備註 |
| 景點間交通 | 5 | 步行、大眾運輸，含距離公里數 |
| 用戶收藏 | 8 | 各用戶收藏的景點 |
| 複製記錄 | 1 | 小美複製小明的台北三日遊 |
| 車輛 | 3 | 豪華九人座、舒適轎車、中型巴士（v1.4，v1.7 改為多語 JSONB） |

> **v1.7**：所有 Seed 資料的 i18n 欄位（景點名稱/地址、行程標題/目的地、天數/景點備註、車輛名稱/描述）已改為 JSONB 格式，包含 zh-TW / en / ja / vi 四種語言翻譯。

---

## 測試

### 測試框架
- **Jest** - 測試執行器
- **Supertest** - HTTP 端點測試

### 執行測試

```bash
npm test
```

### 測試覆蓋（11 套件 / 190 測試）

| 測試套件 | 測試數 | 覆蓋範圍 |
|----------|:------:|----------|
| users.test.js | 9 | CRUD、404、部分更新、空列表、**所有權驗證 (403)** |
| spots.test.js | 41 | CRUD、關鍵字搜尋、分類搜尋、地理位置搜尋、複合搜尋、404、**lat/lng/radius 輸入驗證**、**PUT/DELETE（所有權+系統景點+他人引用）**、**source/is_public 可見性**、**latitude=0 邊界、address 清空、name null 驗證、空 body、whitelist 安全（id/creator_id/created_at 不可改）、空字串正規化、geo+visibility、系統景點搜尋**（v1.6） |
| itineraries.test.js | 19 | CRUD、分頁（含 v2 格式）、巢狀查詢、複製(含驗證)、發佈、排序、400/404、**所有權驗證**、**JWT user_id 驗證**（body 不帶/body 帶假 user_id/is_public）（v1.5） |
| itineraryDays.test.js | 5 | 新增/更新/刪除、400/404/409 |
| itinerarySpots.test.js | 8 | 新增/更新/刪除/排序、400/404/409 |
| favorites.test.js | 5 | 收藏/取消收藏、400/404/409 |
| vehicles.test.js | 13 | CRUD、類型篩選、可用性篩選、401/404、**負數 capacity/price_per_day 驗證**（v1.4） |
| orders.test.js | 19 | 建立/列表/詳情/更新/取消、金額計算、行程關聯、容量驗證（含更新時）、分頁、**所有權驗證 (403)**、狀態限制、**型別驗證**（v1.4） |
| payments.test.js | 11 | Mock 付款/退款、狀態轉換驗證、付款紀錄列表、**所有權驗證 (403)**、重複付款防止（v1.4） |
| i18n.test.js | 22 | locale middleware（預設/query/header/fallback）、工具函式（localize/toI18nValue/mergeI18nValue）、**CRUD 多語系**（純字串寫入+JSONB 寫入+不同語言讀取+fallback）、**搜尋**（各語系關鍵字）、**巢狀 localize**（itinerary getById）、**車輛/訂單 i18n**（v1.7 新增） |
| upload.test.js | 14 | 401 未授權、400 驗證（缺 file/缺 folder/無效 folder/無效 MIME/檔案過大）、201 上傳成功、folder 正確傳遞至 S3（spots/vehicles/itineraries）、sharp resize 寬度驗證（spots 1200/itineraries 1600）、500 sharp 處理失敗、500 S3 上傳失敗（v1.9 新增） |

> 所有測試已更新為包含 JWT Token（v1.3），確保 Protected 路由在測試中也通過認證。

### 已修復的 Bug

- **地理位置搜尋**：原本使用 `HAVING` 搭配 `SELECT *` 在 PostgreSQL 會報錯（缺少 `GROUP BY`），已改為子查詢 (subquery) 方式過濾 `distance_km`
- **權限漏洞（v1.3）**：任何已登入用戶可修改/刪除他人行程，已加入資源所有權驗證
- **行程 user_id 為 null（v1.5）**：`POST /api/itineraries` 的 `user_id` 從 `req.body` 取得，但 iOS/Android 端不傳此欄位（僅帶 JWT Token），導致新建行程的 `user_id` 寫入 `null`。已改為從 `req.user.id`（JWT）取得，同時新增 `is_public` 欄位支援，並阻止 body 中的 `user_id` 偽造身份

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

1. **刪除行為**：所有外鍵都設定 `ON DELETE CASCADE`，刪除行程會自動刪除其下所有天數和景點關聯。景點的 `creator_id` 使用 `ON DELETE SET NULL`（建立者帳號刪除 → 景點保留變為「官方」景點）

2. **時間欄位**：`arrival_time` 和 `departure_time` 使用 TIME 類型，格式為 `HH:MM:SS`

3. **座標精度**：
   - 緯度 `DECIMAL(10,8)` 可精確到約 1.1mm
   - 經度 `DECIMAL(11,8)` 可精確到約 1.1mm

4. **自動更新**：`itineraries.updated_at` 會在更新時自動觸發更新

5. **索引**：已建立常用查詢的索引，如需額外索引請評估查詢模式

6. **景點重排序**：使用負數暫存策略避免唯一約束衝突，v1.3 優化為批次 CASE 語句（2 次 SQL 取代 2N 次）

7. **地理搜尋**：使用 Haversine 公式計算球面距離，預設搜尋半徑 10km，v1.3 加入 lat(-90~90)、lng(-180~180)、radius(0~500) 輸入驗證

8. **安全 Middleware（v1.3）**：
   - `helmet` — HTTP 安全標頭（X-Content-Type-Options, X-Frame-Options 等）
   - `cors` — 跨域資源共享（預設允許所有 origin）
   - `express-rate-limit` — 每 IP 15 分鐘 300 次請求上限（v1.4 從 100 調高，回傳 JSON 格式錯誤）
   - `express.json({ limit: '10kb' })` — Request body 大小限制
   - `morgan('combined')` — HTTP 請求日誌（test 環境停用）
   - 未知路由統一回傳 `404 JSON`（v1.4，確保 mobile client 收到 JSON）

9. **資源所有權驗證（v1.3）**：修改/刪除行程及其子資源時，server 端驗證 `req.user.id` 與資源的 `user_id` 是否一致。行程操作直接在 SQL WHERE 加 `user_id` 條件；天數/景點操作透過 JOIN 鏈（day → itinerary → user_id）驗證

10. **過期 Token 清理（v1.3）**：登入和 refresh 時順便清除該用戶的過期 refresh token，避免 DB 堆積

11. **訂單並發安全（v1.4）**：訂單更新（update）和取消（cancel）使用 `BEGIN` + `SELECT ... FOR UPDATE` 防止與同時間的付款/退款操作產生 race condition。車輛查詢在建立訂單時也使用 `FOR UPDATE` 確保價格一致性

12. **型別安全（v1.4）**：所有 ownership 比對使用 `Number()` 強制轉型（JWT payload 可能為 string），避免 `===` 比較失敗。車輛/訂單欄位（capacity、price_per_day、passenger_count、days）在 controller 層做完整型別驗證（typeof + 正數 + 整數）

13. **錯誤處理強化（v1.4, v1.6.1 重構）**：errorHandler 使用 `PG_ERRORS` 命名常數（`INVALID_INPUT_SYNTAX`、`UNIQUE_VIOLATION`、`FOREIGN_KEY_VIOLATION`、`NOT_NULL_VIOLATION`、`CHECK_VIOLATION`）取代 magic string。`console.error(err.stack)` 僅在非預期 5xx 錯誤時輸出，已知 4xx client 錯誤不記錄 stack trace。所有錯誤一律回傳 `{ "error": "..." }` JSON 格式

14. **車輛刪除保護（v1.4）**：刪除車輛前檢查是否有既存包車訂單（charter_bookings），有則回傳 400 阻止刪除

15. **景點擁有權與可見性（v1.6）**：`creator_id` 區分官方（NULL）與使用者景點。`is_public` 控制搜尋可見性（官方景點永遠可見，使用者景點預設私人）。PUT/DELETE 僅限建立者，系統景點不可修改/刪除。DELETE 前檢查其他用戶行程引用，有引用則拒絕刪除。`source` 為 computed field（"official"/"user"），不存於 DB

16. **Optional Auth（v1.6）**：`GET /api/spots` 使用 `optionalAuth` middleware — 有 JWT Token 時解析 `req.user`（可看到自己的私人景點），無 Token 或 Token 失效時 `req.user = null`（僅看到官方+公開景點），不回 401

17. **景點 PUT/DELETE 並發安全（v1.6, v1.6.1 重構）**：使用 `BEGIN` + `SELECT ... FOR UPDATE` 鎖定 spot row，確保 ownership 檢查與修改/刪除的原子性。ownership 驗證邏輯提取為共用 `verifySpotOwnership(client, res, spotId, userId, action)` helper，消除 update/remove 間的重複程式碼。PUT 使用動態欄位構建（僅更新 body 中出現的欄位，欄位白名單 `ALLOWED_UPDATE_FIELDS` 與字串欄位集合 `STRING_FIELDS` 提升至 module scope），正確處理 `latitude: 0`、`address: null`、`is_public: false` 等邊界值

18. **NOT NULL 違反處理（v1.6）**：errorHandler 新增 PG 23502（not_null_violation）→ `400 { "error": "<column> is required" }`，攔截如 `PUT /api/spots/:id { name: null }` 的無效更新

19. **`GET /api/spots/:id` 不檢查可見性（v1.6 設計決策）**：景點詳情頁（detail page）需要能透過 ID 顯示任何景點（包含被加入他人行程中的私人景點），因此 `getById` 不做 visibility 過濾。私人景點的保護在於搜尋端（`GET /api/spots`）不會出現在他人結果中，而非隱藏已知 ID 的存取

20. **多語系 JSONB 儲存格式（v1.7）**：i18n 欄位在 DB 中以 `{"zh-TW": "值", "en": "value", "ja": "値", "vi": "giá trị"}` 格式儲存。API 回傳時自動解析為請求語言的純文字字串。寫入時接受純字串（自動包成 `{locale: value}`）或 JSONB 物件（直接存入）。更新時使用 JSONB `||` 合併（僅更新當前語言，保留其他語系翻譯）

21. **語言偵測優先順序（v1.7）**：`?lang=en` query param → `Accept-Language: ja` header → 預設 `zh-TW`。支援的語系：`zh-TW`、`en`、`ja`、`vi`。Accept-Language 解析：`en-US` → `en`、`zh` → `zh-TW`、`ja-JP` → `ja`、`vi-VN` → `vi`

22. **i18n Fallback 機制（v1.7）**：讀取時若請求語言無翻譯，依序 fallback：requested locale → `zh-TW` → 第一個可用語言 → `null`。確保即使只有部分翻譯，仍能回傳內容

23. **JSONB 搜尋（v1.7）**：`GET /api/spots?keyword=` 同時搜尋所有語系（`name->>'zh-TW' ILIKE $N OR name->>'en' ILIKE $N OR name->>'ja' ILIKE $N OR name->>'vi' ILIKE $N`），關鍵字不需指定語言即可搜到任一語系的結果

24. **向後相容（v1.7）**：現有客戶端不需任何修改。API 預設語言為 zh-TW，回傳格式維持純文字字串。寫入端仍接受純字串。Migration 005 自動將現有 TEXT 資料包裝為 `{"zh-TW": "原始值"}`

25. **`spots.creator_id ON DELETE SET NULL` 的行為（v1.6 設計決策）**：當使用者帳號被刪除時，其建立的景點 `creator_id` 變為 `NULL`，效果等同變為「官方景點」（`source: "official"`）。此景點從此不可修改/刪除（403 Cannot modify a system default spot）。選擇 `SET NULL` 而非 `CASCADE` 是為了保留景點資料（可能已被其他用戶行程引用）；選擇不另設 `is_system` 欄位是為了保持 schema 精簡。若未來需要區分「真正官方」與「孤兒景點」，可加入 `is_system BOOLEAN DEFAULT false`

---

## 多語系 (i18n)

### 概覽

v1.7 起，內容欄位（景點名稱、行程標題、車輛描述等）支援 **zh-TW / en / ja / vi** 四種語言。DB 層使用 JSONB 格式儲存，API 層自動解析為請求語言的純文字。

### 語言偵測

優先順序：
1. Query parameter：`?lang=en`
2. HTTP Header：`Accept-Language: ja`
3. 預設：`zh-TW`

```bash
# 繁體中文（預設）
curl /api/spots

# 英文
curl /api/spots?lang=en

# 日文（透過 Accept-Language）
curl -H "Accept-Language: ja" /api/spots

# 越南文
curl /api/spots?lang=vi
```

### i18n 欄位列表

| Table | Column | 說明 |
|-------|--------|------|
| spots | name | 景點名稱 |
| spots | address | 地址 |
| itineraries | title | 行程標題 |
| itineraries | destination | 目的地 |
| itinerary_days | note | 當天備註 |
| itinerary_spots | note | 景點備註 |
| vehicles | name | 車輛名稱 |
| vehicles | description | 車輛描述 |
| charter_bookings | pickup_location | 上車地點 |
| charter_bookings | dropoff_location | 下車地點 |

> **不轉換的欄位**（enum 或使用者自由輸入）：`spots.category`、`vehicles.type`、`charter_bookings.special_requests`、`orders.note`、`users.name`

### DB 儲存格式

```jsonc
// JSONB 欄位範例
{
  "zh-TW": "台北101",
  "en": "Taipei 101",
  "ja": "台北101",
  "vi": "Đài Bắc 101"
}
```

### 寫入行為

**新建（POST）**：

```jsonc
// 方式 A：傳純字串 → 自動包成 {"<locale>": "值"}
{ "name": "新景點" }
// ?lang=zh-TW 時存為 → {"zh-TW": "新景點"}

// 方式 B：傳 JSONB 物件 → 直接存入
{ "name": {"zh-TW": "新景點", "en": "New Spot"} }
```

**更新（PUT）**：

```jsonc
// 只更新當前語言，保留其他語系翻譯
// 原始資料：{"zh-TW": "舊名稱", "en": "Old Name"}
// PUT ?lang=en { "name": "New Name" }
// 結果：{"zh-TW": "舊名稱", "en": "New Name"}

// 傳 JSONB 物件也可一次更新多個語言
// PUT { "name": {"en": "New Name", "ja": "新しい名前"} }
// 結果：{"zh-TW": "舊名稱", "en": "New Name", "ja": "新しい名前"}
```

### 讀取行為

API 回傳時自動將 JSONB 解析為請求語言的純文字：

```bash
# 預設 zh-TW
GET /api/spots/1
→ { "name": "台北101", "address": "台北市信義區信義路五段7號" }

# 切換英文
GET /api/spots/1?lang=en
→ { "name": "Taipei 101", "address": "No. 7, Section 5, Xinyi Rd, Xinyi District" }

# Fallback：若 en 無翻譯，回傳 zh-TW
GET /api/spots/1?lang=en
→ { "name": "台北101" }  // en 無翻譯時 fallback 到 zh-TW
```

### 巢狀 Localize

`GET /api/itineraries/:id` 回傳的巢狀結構中，所有 i18n 欄位（行程 title/destination、天數 note、景點 note、景點 spot.name/address）皆會依語言解析：

```jsonc
{
  "title": "3-Day Taipei Trip",      // itinerary.title
  "cover_image_url": "https://...",   // itinerary.cover_image_url
  "destination": "Taipei",            // itinerary.destination
  "author_name": "王小明",             // users.name (JOIN)
  "author_avatar": "https://...",     // users.avatar_url (JOIN)
  "days": [{
    "note": "Day 1 note",             // day.note
    "spots": [{
      "note": "Must try beef noodle", // itinerary_spot.note
      "spot": {
        "name": "Taipei 101",         // spot.name
        "address": "Xinyi District"   // spot.address
      }
    }]
  }]
}
```

### 搜尋

`GET /api/spots?keyword=taipei` 會同時搜尋所有語系，不需指定語言：

```sql
-- 搜尋邏輯
(name->>'zh-TW' ILIKE '%taipei%' OR name->>'en' ILIKE '%taipei%' OR name->>'ja' ILIKE '%taipei%' OR name->>'vi' ILIKE '%taipei%')
```

### Migration

```bash
npm run migrate:i18n   # 執行 migrations/005_add_i18n.sql
```

Migration 005 將 10 個 TEXT 欄位轉為 JSONB，現有資料自動包裝為 `{"zh-TW": "原始值"}`。此操作為 **冪等**（可重複執行），不會破壞已有 JSONB 資料。

```bash
npm run migrate:vi    # 執行 migrations/006_add_vietnamese.sql
```

Migration 006 新增 `spots.name->>'vi'` 索引，支援越南文搜尋效能。

---

## 版本紀錄

| 版本 | 日期 | 說明 |
|------|------|------|
| 1.0 | 2026-01-20 | 初版：資料表結構與 API 端點規劃 |
| 1.1 | 2026-01-30 | 所有 API 實作完成、56 項測試通過、Seed 資料、Bug 修復（地理搜尋查詢）、Port 改為 5487 |
| 1.2 | 2026-02-24 | Google OAuth 登入、Access Token + Refresh Token、路由保護（protected/public 分離）、users.google_id、refresh_tokens 表、migration 002 |
| 1.3 | 2026-02-27 | **安全性強化**：Helmet/CORS/Rate Limit/Body Limit middleware、資源所有權驗證（行程/天數/景點僅 owner 可改刪）、用戶僅能更新自己、輸入驗證（lat/lng/radius/limit/offset）。**API 品質**：Health Check (`GET /health`)、Morgan 日誌、分頁 v2 格式（`?format=v2`）、PG 錯誤碼統一處理。**程式碼優化**：Copy 改用 Token user_id、Reorder 批次 CASE 語句、過期 Refresh Token 自動清理。測試 56→61 項（含所有權驗證測試、JWT Token 整合）。所有變更向下相容。 |
| 1.4 | 2026-02-27 | **包車預訂**：vehicles（車輛管理 CRUD）、orders（通用訂單表，支援 charter 類型，可擴充 ticket/hotel）、charter_bookings（包車明細 1:1 訂單）。**金流系統**：payments（Mock 付款/退款，transaction_id 自動產生 UUID）、訂單狀態流轉（pending→confirmed→completed/cancelled）。**訂單功能**：自動計算金額（vehicle.price_per_day × days）、乘客容量驗證、可選行程關聯（itinerary_id nullable）、所有權驗證。Migration 003、3 輛範例車輛 seed。測試 61→103 項。 |
| 1.4.1 | 2026-02-27 | **Production Hardening**：Rate Limit 100→300（JSON 格式回應）、404 catch-all JSON handler、errorHandler 新增 PG 22P02/23514/entity.parse.failed。**並發安全**：訂單 update/cancel 加入 Transaction + FOR UPDATE、車輛建單 FOR UPDATE。**型別安全**：ownership 比對 `Number()` 轉型、controller 層完整型別驗證（capacity/price/passenger_count/days）。**訂單 API**：新增分頁（limit/offset）、更新時容量驗證、列表回傳巢狀 charter_booking。**車輛刪除保護**：檢查既存訂單。DB CHECK 約束（capacity>0, price>0, days>0 等）。測試 103→107 項。 |
| 1.5 | 2026-02-28 | **Bug Fix — 行程 user_id 為 null**：`POST /api/itineraries` 原本從 `req.body` 取 `user_id`，但 iOS/Android 端不傳此欄位（僅帶 JWT Token），導致新建行程 `user_id` 寫入 `null`。改為從 `req.user.id`（JWT）取得。同時新增 `is_public` 欄位支援（body 可傳 `is_public`，未傳時 DB 預設 `true`）。**安全性**：body 中的 `user_id` 一律忽略，防止偽造身份。測試 107→110 項（+3：JWT user_id 驗證、is_public、body user_id 忽略）。 |
| 1.6 | 2026-02-28 | **景點擁有權與可見性**：spots 表新增 `creator_id`（FK → users, ON DELETE SET NULL）區分官方/使用者景點、`is_public`（使用者景點預設 false）。GET 回應含 computed `source`（"official"/"user"）。新增 `optionalAuth` middleware（有 JWT 解析 user、無 JWT 不擋）套用於 `GET /api/spots`。**搜尋可見性**：未登入→官方+公開；登入→額外顯示自己的私人景點。**PUT /api/spots/:id**：動態部分更新（僅更新 body 中出現的欄位，正確處理 `0`/`null`/`false`），僅限建立者、系統景點 403。**DELETE /api/spots/:id**：僅限建立者、系統景點 403、他人行程引用 400 拒絕。**並發安全**：PUT/DELETE 使用 Transaction + `FOR UPDATE`。**錯誤處理**：errorHandler 新增 PG 23502（NOT NULL violation）→ 400。**latitude/longitude 修正**：`create` 改用 `??`（nullish coalescing）取代 `||`，正確處理值為 0 的座標。**update 字串正規化**：空字串 `""` 統一轉為 `NULL`（與 create 行為一致）。**update whitelist**：僅允許 8 個欄位更新，`id`/`creator_id`/`created_at` 等欄位自動忽略。**Seed data**：新增 3 筆使用者景點（小明私房咖啡廳/小美甜點店/阿傑釣點），展示公開+私人景點。Migration 004。測試 110→140 項。 |
| 1.6.1 | 2026-02-28 | **Code Quality 重構**：（1）提取共用 `verifySpotOwnership` helper，消除 update/remove 間 ~20 行重複的 ownership 驗證邏輯（404/403/system spot 檢查）。（2）`ALLOWED_UPDATE_FIELDS` 和 `STRING_FIELDS` 提升至 module scope 常數，避免每次 PUT 請求重新建立。（3）消除 `search` 中 `parseFloat` 重複呼叫（lat/lng/radius 各解析兩次→一次），改用已驗證的 `latNum`/`lngNum`/`radiusNum`。（4）errorHandler PG 錯誤碼改用 `PG_ERRORS` 命名常數（取代 magic string `'22P02'` 等）。（5）`console.error(err.stack)` 移至已知 4xx client 錯誤判斷之後，僅對非預期 5xx 錯誤輸出 stack trace。無 API 行為變更，全 140 項測試通過。 |
| 1.7 | 2026-02-28 | **多語系 (i18n)**：支援 zh-TW / en / ja 三種語言。10 個內容欄位從 TEXT 轉為 JSONB（spots.name/address、itineraries.title/destination、itinerary_days.note、itinerary_spots.note、vehicles.name/description、charter_bookings.pickup_location/dropoff_location）。**語言偵測**：`locale.js` middleware（`?lang=` → `Accept-Language` → 預設 zh-TW）。**i18n 工具**：`utils/i18n.js`（localize/localizeRow/localizeRows/toI18nValue/mergeI18nValue）。**寫入**：純字串自動包 `{locale: value}`，JSONB 物件直接存入；更新用 `||` 合併保留其他語系。**讀取**：回傳純文字（JSONB→指定語言字串），fallback zh-TW→第一個可用。**搜尋**：`keyword` 同時搜尋所有語系（`name->>'zh-TW' ILIKE` OR `->>'en'` OR `->>'ja'`）。**巢狀**：itinerary getById 遞迴 localize 所有子層（days.note、spots.note、spot.name/address）。**Seed**：所有 i18n 欄位改為三語 JSONB。**向後相容**：預設 zh-TW，回傳格式不變，現有客戶端無需修改。Migration 005（冪等）。測試 140→171 項（+31，新增 i18n.test.js 22 項）。 |
| 1.8 | 2026-03-02 | **新增越南文 (vi)**：`SUPPORTED_LOCALES` 加入 `vi`，自動支援 `?lang=vi`、`Accept-Language: vi-VN`、越南文搜尋。Migration 006（`idx_spots_name_vi` 索引）。Seed 資料全面補充越南文翻譯（spots 25 筆、itineraries 5 筆、itinerary_days 11 筆、itinerary_spots 20 筆、vehicles 3 筆）。測試新增 5 項（middleware vi/vi-VN、搜尋 vi、CRUD vi 讀寫）。 |
| 1.9 | 2026-03-02 | **圖片上傳 API**：新增 `POST /api/upload` 端點，行動端可上傳圖片至 S3 取得公開 URL。**上傳流程**：multer memory storage 接收 multipart/form-data → 驗證（file 存在、folder ∈ {spots, vehicles, itineraries}、MIME type ∈ {JPEG, PNG, WebP, GIF}、≤ 5 MB）→ sharp 處理（resize max width + WebP quality 80 + withoutEnlargement）→ S3 上傳（`images/{folder}/{timestamp}-{random}.webp`，public-read ACL）→ 回傳 201 `{ url }`。**新增檔案**：`controllers/uploadController.js`、`routes/upload.js`、`utils/uploadConfig.js`、`utils/s3.js`、`tests/upload.test.js`。**錯誤處理**：errorHandler 新增 multer `LIMIT_FILE_SIZE` → 400、`MulterError` → 400。**相依套件**：multer、sharp、@aws-sdk/client-s3。**環境變數**：S3_BUCKET、S3_REGION。測試 176→190 項（+14）。 |
