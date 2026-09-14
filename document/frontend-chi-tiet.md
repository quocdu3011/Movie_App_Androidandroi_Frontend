# Chi tiết Frontend Android — App xem phim
### (Viết lại theo đúng contract `backend_api.md` — thay thế bản thiết kế trước)

**Bản đồng bộ 2026-09-14 / revision 3.** Thứ tự đối chiếu: `backend_api.md` > file này > `todo-prompt-frontend.md` > `thiet-ke-app-xem-phim.md`. Toàn bộ DTO public cần cho luồng app hiện tại đã được chốt trong `backend_api.md`; không tự thêm field hoặc endpoint ngoài contract.

Trước khi tạo project production cần chốt `applicationId/namespace`, minSdk, compileSdk/targetSdk, JDK/AGP/Kotlin và base URL dev/staging/prod. `http://127.0.0.1:3000` trong contract là địa chỉ nhìn từ máy chạy backend; Android emulator dùng `10.0.2.2:3000` hoặc `adb reverse`, thiết bị thật dùng địa chỉ LAN/tunnel. Chỉ bật cleartext HTTP ở debug; staging/prod dùng HTTPS.

## 0. Điểm khác biệt quan trọng so với bản thiết kế cũ

Trước khi đọc tiếp, lưu ý các điểm này vì chúng thay đổi cách viết code so với thiết kế trước:

| Chủ đề | Trước đây giả định | Theo contract thật |
|---|---|---|
| Phát video | Gọi 1 API `play-url` là xong | Phải **tạo playback session** (`POST /streaming/playback-sessions`), rồi duy trì bằng heartbeat/progress/event theo vòng đời |
| Auth owned HLS | Signed URL trong query param | **Cookie `HttpOnly`** do Gateway forward — ExoPlayer cần cấu hình cookie jar riêng |
| Tải offline | Có tính năng tải xuống | **Chưa hỗ trợ** (`offlineSupported` luôn `false`) — bỏ khỏi phạm vi hiện tại |
| Home | 1 API duy nhất | **2 API**: `/catalog/home` (public) và `/home?profileId=` (cá nhân hóa, có "tiếp tục xem") |
| Favorites | `POST`/`body { movie_id }` | `PUT /profiles/:id/favorites/:movieId` và `DELETE` tương ứng, không có body |
| Refresh token | Refresh nhiều lần tùy ý | **Dùng 1 lần, tự rotate** — dùng lại token cũ sẽ bị revoke cả phiên đăng nhập |
| Response envelope | `{success, data, error}` | Có thêm `requestId` — cần log lại khi báo lỗi cho support |
| Chọn nguồn phát | Ẩn, BE tự chọn | **Người dùng chọn** `playableId` + `sourceItemId` từ danh sách `sources` ở màn Detail |

---

## 1. Danh sách chức năng đầy đủ (đã điều chỉnh theo contract)

### 1.1. Tài khoản & hồ sơ
- Đăng ký (`email, password 12-128 ký tự, fullName`), đăng nhập (kèm `deviceId` ổn định theo máy)
- Khôi phục phiên khi mở lại app qua `GET /auth/session`
- CRUD tối đa **5 hồ sơ**/tài khoản; hồ sơ trẻ em (`isKids`) lọc nội dung không phù hợp
- Đăng ký chỉ tạo tài khoản và trả thông tin user, **không trả token**; đăng ký thành công phải chuyển về đăng nhập, không coi là đã đăng nhập
- Không có API quên mật khẩu/OAuth trong contract hiện tại — **không thiết kế màn hình này** cho tới khi backend bổ sung

### 1.2. Khám phá nội dung
- Trang chủ public (chưa chọn hồ sơ): `newReleases`, `topRated`, `trending`
- Trang chủ cá nhân hóa (đã chọn hồ sơ): thêm `continue_watching`, gợi ý (có fallback khi Recommendation service không sẵn sàng)
- Duyệt/lọc: `genre`, `country`, `year`, `type`, `contentKind`, `sourceType`, `provider`, `sort`
- Tìm kiếm theo tên (không tìm trực tiếp KKPhim, chỉ tìm nội dung đã import vào Catalog)
- Chi tiết phim: metadata + danh sách `playableItems` (tập phim) + danh sách `sources` (server phát) để người dùng chọn trước khi phát

### 1.3. Xem phim
- Chọn `playableId` + `sourceItemId` (chỉ hiện nguồn có `sourceStatus` là `available` hoặc `unknown` và `sourceItemId != null`)
- Tạo playback session, xử lý xác nhận resume nếu `resumeNeedsConfirmation`
- Gửi heartbeat (giữ lease), progress (lưu tiến độ), sự kiện lifecycle (`started`/`qualified`/`stopped`/`failed`)
- Owned HLS: phát qua cookie tự động kèm theo request và renew bằng `/media-auth` trước khi hết hạn; KKPhim: phát trực tiếp URL trả về, không cache dài hạn
- **Không có** chọn thủ công độ phân giải, offline, DRM ở giai đoạn hiện tại (không có trong contract)

### 1.4. Tương tác
- Yêu thích: `PUT`/`DELETE` theo `movieId`, chỉ dùng được khi đã chọn hồ sơ
- Lịch sử xem: đọc từ `watch-history`, bỏ qua item có `tombstone: true`

### 1.5. Gói cước & thanh toán (mock)
- Xem gói cước, đăng ký gói (`Idempotency-Key` bắt buộc)
- Xem gói hiện tại — **thanh toán là mock**, app không tự gọi webhook hay giả lập thành công; cần màn hình "đang chờ xử lý"

---

## 2. Network layer — chi tiết theo contract thật

### 2.1. Response envelope
```kotlin
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError?,
    val requestId: String
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: JsonElement? = null
)

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(
        val code: String,
        val message: String,
        val requestId: String? = null,
        val throwable: Throwable? = null
    ) : Result<Nothing>
}
```
`requestId` luôn có mặt kể cả khi lỗi — lưu lại và hiển thị trong màn "Báo lỗi/Gửi phản hồi" để đối chiếu log phía backend.

### 2.2. Xử lý theo HTTP status (bảng quyết định cho `ApiErrorMapper`)

| Status | Hành vi client |
|---|---|
| 200/201/202 | Đọc `data`. Với `202`, hiển thị trạng thái "đang xử lý", polling nếu API có cung cấp endpoint kiểm tra (ví dụ `sync-runs`) |
| 204 | Không parse JSON, coi là thành công |
| 400 | Hiển thị `error.message`, không tự retry |
| 401 | Gọi `/auth/refresh` đúng 1 lần; nếu vẫn 401 → xóa state đăng nhập, điều hướng Login |
| 403 | Hiển thị "Không có quyền truy cập" — không hiển thị màn quản trị |
| 404 | Hiển thị "Nội dung không tồn tại", quay lại màn trước |
| 409 | **Không tự tạo request mới** khi chưa hiểu `error.code` — hiển thị message, để người dùng chủ động thử lại |
| 422 | Dữ liệu hợp lệ nhưng không dùng được (ví dụ nguồn chưa sẵn sàng) — hiển thị message rõ ràng, không coi là lỗi hệ thống |
| 429 | Đọc header `Retry-After` nếu có, backoff trước khi cho phép thử lại (áp dụng cho các endpoint Auth) |
| 503 | Cho phép retry giới hạn với GET; với POST idempotent thì retry bằng **cùng** `Idempotency-Key` |

### 2.3. Idempotency-Key
Bắt buộc cho: `POST /streaming/playback-sessions`, `POST /subscriptions/subscribe` (và các endpoint admin upload). Quy tắc: 8-120 ký tự, tập `[A-Za-z0-9._:-]`. Sinh 1 lần khi bắt đầu 1 thao tác logic (ví dụ: 1 lần bấm "Phát"), **giữ nguyên key khi retry cùng payload**, sinh key mới nếu người dùng đổi lựa chọn (đổi server/tập phim) trước khi bấm lại.

```kotlin
object IdempotencyKeyGenerator {
    fun generate(): String = UUID.randomUUID().toString() // đã nằm trong charset + độ dài cho phép
}
```

### 2.4. deviceId
Sinh 1 lần, lưu bền (`DataStore`), dùng lại cho mọi lần đăng nhập trên cùng máy — không sinh mới mỗi lần login vì ảnh hưởng tới giới hạn thiết bị/thống kê phía backend.
```kotlin
class DeviceIdProvider(private val dataStore: DataStore<Preferences>) {
    suspend fun getOrCreate(): String { /* đọc từ DataStore, nếu chưa có thì UUID.randomUUID() rồi lưu lại */ }
}
```

### 2.5. Refresh token — rotate, dùng 1 lần
```kotlin
class TokenAuthenticator(
    // RefreshTokenApi dùng Retrofit/OkHttp client "trần": không AuthInterceptor,
    // không TokenAuthenticator, để /auth/refresh không đệ quy khi chính nó trả 401.
    private val refreshApi: RefreshTokenApi,
    private val tokenStorage: TokenStorage,
    private val mutex: Mutex = Mutex()
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val failedAccessToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")

        return runBlocking {
            mutex.withLock {
                // Request khác có thể đã refresh trong lúc request này chờ Mutex.
                // Khi token đã đổi, retry bằng token mới và KHÔNG gọi refresh lần nữa.
                val latestAccessToken = tokenStorage.getAccessToken()
                if (latestAccessToken != null && latestAccessToken != failedAccessToken) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $latestAccessToken")
                        .build()
                }

                val currentRefreshToken = tokenStorage.getRefreshToken() ?: return@withLock null
                try {
                    val result = refreshApi.refresh(RefreshRequest(currentRefreshToken))
                    // QUAN TRỌNG: server rotate refresh token — phải lưu token MỚI, không giữ token cũ
                    tokenStorage.save(result.data.accessToken, result.data.refreshToken)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${result.data.accessToken}")
                        .build()
                } catch (e: Exception) {
                    tokenStorage.clear() // refresh thất bại → không thử lại, buộc đăng nhập lại
                    null
                }
            }
        }
    }
}
```
**Lưu ý an toàn:** `Mutex` chỉ tuần tự hóa, tự nó không tạo single-flight. Kiểm tra `failedAccessToken` với access token hiện tại bên trong lock mới đảm bảo ba response 401 cùng token cũ chỉ gọi refresh một lần. `RefreshTokenApi` bắt buộc dùng client trần riêng để tránh refresh đệ quy.

### 2.6. Ba cấu hình HTTP tách vai trò

| Client | Bearer/Auth interceptor | Authenticator | CookieJar |
|---|---|---|---|
| Backend API | Có, trừ `register/login/refresh/logout/JWKS` | Có | Shared |
| Refresh client | Không | Không | Không bắt buộc |
| Media client | Không | Không | Cùng shared jar với Backend API |

Không bao giờ đưa authenticated Backend API client vào Media3. `401/403` từ media host không được kích hoạt refresh user token.

---

## 3. Luồng Auth chi tiết

```kotlin
// Login
data class LoginRequest(val email: String, val password: String, val deviceId: String, val deviceName: String? = null)

// Khôi phục phiên khi mở app
suspend fun restoreSession(): AuthState {
    // Access token chỉ ở RAM nên thường null sau process death. Refresh token mới là
    // tín hiệu cho phép thử restore; request /auth/session đầu tiên có thể nhận 401
    // và TokenAuthenticator sẽ refresh đúng một lần rồi retry.
    if (tokenStorage.getRefreshToken() == null) return AuthState.LoggedOut
    return when (val result = authApi.getSession()) {
        is Result.Success -> if (result.data.active) AuthState.LoggedIn(result.data) else AuthState.LoggedOut
        is Result.Error -> AuthState.LoggedOut // xóa token nếu session inactive/refresh thất bại
    }
}
```
- Access token: lưu **in-memory** (không log, không đưa vào URL).
- Refresh token: lưu `EncryptedSharedPreferences`, luôn ghi đè bằng giá trị mới nhất sau mỗi lần refresh.
- Khi logout: best-effort gọi `POST /auth/logout` với refresh token hiện tại, rồi **luôn** xóa local state trong `finally` kể cả request lỗi/mất mạng; không giữ người dùng đăng nhập cục bộ vì logout server thất bại.
- `register` không trả `TokenPair`; đăng ký thành công chuyển về Login. Chỉ `login`/`refresh` mới cập nhật `TokenStorage`.

---

## 4. Thiết kế màn hình cập nhật theo contract

### 4.1. Home — 2 nguồn dữ liệu
```kotlin
sealed interface HomeUiState {
    object Loading : HomeUiState
    data class PublicHome(val newReleases: List<Movie>, val topRated: List<Movie>, val trending: List<Movie>) : HomeUiState
    data class PersonalizedHome(val sections: List<HomeSection>) : HomeUiState // gồm cả continue_watching nếu có
    data class Error(val message: String) : HomeUiState
}
```
- Trước khi chọn hồ sơ (hoặc chưa đăng nhập): gọi `GET /catalog/home`, render 3 hàng cố định (mới ra mắt/đánh giá cao/xu hướng).
- Sau khi chọn hồ sơ: gọi `GET /home?profileId=`, render động theo `sections` trả về — **luôn có** `catalog_new_releases`, phần gợi ý là tùy chọn (kiểm tra `type` để hiển thị đúng, xử lý `fallback_new_releases` với `reason` bằng cách hiển thị nhãn nhẹ "Gợi ý cho bạn" thay vì cố tỏ ra đây là gợi ý cá nhân hóa thật khi backend đã báo fallback).
- `continue_watching` render từ `HistoryItem[]`, **bỏ qua item có `tombstone: true`**, bấm vào mở thẳng Player với `playableId`/`sourceItemId` đã lưu sẵn trong history thay vì quay lại màn Detail.

### 4.2. Chi tiết phim — chọn playable + source
```kotlin
data class MovieDetailUiState(
    val movie: MovieDetail,
    val selectedPlayable: PlayableItem? = null,
    val selectableSources: List<SourceItem> = emptyList() // đã filter sourceStatus in [available, unknown]
)
```
Luồng UI:
1. Gọi `GET /catalog/movies/:movieId?profileId=` lấy `MovieDetail` (gồm `playableItems`, `sources`).
2. Nếu `type == 'series'`: hiển thị danh sách tập theo `seasonNumber`/`episodeNumber` (từ `playableItems`), người dùng chọn 1 tập.
3. Sau khi chọn tập (hoặc với phim lẻ, playable duy nhất), lọc `sources` theo `playableId` tương ứng, **chỉ hiển thị source có `sourceStatus` là `available` hoặc `unknown` và `sourceItemId != null`** — ẩn hẳn `unavailable`/`error` hoặc source chưa có selector phát hợp lệ.
4. Hiển thị lựa chọn server bằng `serverLabel` (không hiển thị `provider`/slug kỹ thuật cho người dùng cuối).
5. Bấm "Phát" → điều hướng Player kèm `movieId, playableId, sourceItemId, profileId` — trong đó selector phát là **`SourceItem.sourceItemId`**, không phải `SourceItem.id` (ID content source). Cả 4 ID lấy nguyên từ response, không tự suy ra từ title/thứ tự mảng.

### 4.3. Player — luồng playback session đầy đủ
```kotlin
class PlayerViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val createSessionUseCase: CreatePlaybackSessionUseCase,
    private val heartbeatUseCase: SendHeartbeatUseCase,
    private val progressUseCase: SendProgressUseCase,
    private val eventUseCase: SendPlaybackEventUseCase,
    private val renewMediaAuthUseCase: RenewMediaAuthUseCase,
) : ViewModel() {

    private var session: PlaybackSession? = null
    private var seq = 0L
    private var playedMillis = 0L // tăng theo clock chỉ khi player thật sự Playing
    private var qualifiedSent = false
    private var startedSent = false
    private var terminalSent = false

    fun startPlayback(movieId: String, playableId: String, sourceItemId: String, profileId: String) {
        viewModelScope.launch {
            val payloadKey = "$movieId|$playableId|$sourceItemId|$profileId"
            val idempotencyKey = (if (savedStateHandle["sessionPayloadKey"] == payloadKey) {
                savedStateHandle.get<String>("sessionIdempotencyKey")
            } else null) ?: IdempotencyKeyGenerator.generate().also {
                savedStateHandle["sessionPayloadKey"] = payloadKey
                savedStateHandle["sessionIdempotencyKey"] = it
            }
            when (val result = createSessionUseCase(movieId, playableId, sourceItemId, profileId, idempotencyKey)) {
                is Result.Success -> {
                    session = result.data
                    if (result.data.resumeNeedsConfirmation) {
                        _uiState.update { it.copy(showResumeDialog = true, resumePosition = result.data.resumePositionSeconds) }
                    } else {
                        preparePlayer(result.data, seekTo = result.data.resumePositionSeconds)
                    }
                }
                is Result.Error -> handlePlaybackError(result) // xử lý riêng CONCURRENT_STREAM_LIMIT, PLAYBACK_MODE_UNSUPPORTED
            }
        }
    }

    private fun preparePlayer(session: PlaybackSession, seekTo: Int) {
        playerManager.prepare(session.playbackUrl, startPositionMs = seekTo * 1000L)
        startHeartbeatLoop()
        startProgressLoop()
        if (session.sourceType == "owned") startMediaAuthRenewal(session)
    }

    // Gọi từ collector PlayerState khi ExoPlayer chuyển sang Playing lần đầu.
    fun onPlayerActuallyPlaying() {
        if (!startedSent) {
            startedSent = true
            sendEvent("started")
        }
    }

    private fun startHeartbeatLoop() = viewModelScope.launch {
        while (isActive) {
            delay(30_000)
            session?.let {
                val result = heartbeatUseCase(it.sessionId)
                // cập nhật leaseExpiresAt; nếu lease bị từ chối thì dừng player và kết thúc failed
                if (result is Result.Error) finishSession(StopReason.LeaseRejected)
            }
        }
    }

    private fun startProgressLoop() = viewModelScope.launch {
        while (isActive) {
            delay(12_000)
            val position = playerManager.currentPositionSeconds()
            session?.let { progressUseCase(it.sessionId, seq = (++seq).toString(), positionSeconds = position, durationSeconds = playerManager.durationSeconds()) }
            // playedMillis do collector player cập nhật bằng monotonic clock, không cộng
            // cứng 12 giây vì pause/buffering không được tính.
            val playedSeconds = (playedMillis / 1000L).toInt()
            if (!qualifiedSent && playedSeconds >= 30) {
                qualifiedSent = true
                sendEvent("qualified", playedSeconds = playedSeconds)
            }
        }
    }

    fun finishSession(reason: StopReason) {
        if (terminalSent) return
        terminalSent = true
        viewModelScope.launch {
            val s = session ?: return@launch
            // Cùng một coroutine để bảo đảm thứ tự network: final progress ACK trước event terminal.
            progressUseCase(
                s.sessionId,
                seq = (++seq).toString(),
                positionSeconds = playerManager.currentPositionSeconds(),
                durationSeconds = playerManager.durationSeconds()
            )
            eventUseCase(
                s.sessionId,
                eventId = UUID.randomUUID().toString(),
                type = if (reason.isFailure) "failed" else "stopped",
                reasonCode = reason.code
            )
        }
    }

    private fun sendEvent(type: String, playedSeconds: Int? = null, reasonCode: String? = null) {
        viewModelScope.launch {
            session?.let { eventUseCase(it.sessionId, eventId = UUID.randomUUID().toString(), type = type, playedSeconds = playedSeconds, reasonCode = reasonCode) }
        }
    }

    private fun startMediaAuthRenewal(session: PlaybackSession) = viewModelScope.launch {
        // Lập lịch trước mediaAuth.expiresAt; mỗi response MediaAuth thành công phải cập nhật
        // mốc expiry kế tiếp. Không dùng TTL hard-code vì backend cấu hình TTL theo môi trường.
        // Không gọi endpoint này cho third_party (server sẽ trả MEDIA_AUTH_NOT_APPLICABLE).
    }
}
```

`finishSession` là best-effort khi app còn chạy; process bị kill đột ngột không thể bảo đảm request cuối ra mạng, khi đó lease backend tự hết hạn. Mọi job heartbeat/progress/renew phải được hủy sau terminal event hoặc khi ViewModel kết thúc để không gửi tiếp vào session đã đóng.

Chỉ lưu idempotency key và fingerprint của payload trong `SavedStateHandle`, không lưu `playbackUrl`. Retry cùng payload dùng lại key; người dùng đổi tập/server hoặc bắt đầu thao tác phát mới sau lỗi `IDEMPOTENCY_KEY_REUSED` phải sinh key mới.

**Xử lý lỗi riêng khi tạo session:**
```kotlin
private fun handlePlaybackError(result: Result.Error) {
    when (result.code) {
        "CONCURRENT_STREAM_LIMIT" -> _uiState.update { it.copy(error = "Bạn đang phát tối đa số thiết bị cho phép. Vui lòng dừng một thiết bị khác trước.") } // KHÔNG tự động retry
        "PLAYBACK_MODE_UNSUPPORTED" -> _uiState.update { it.copy(error = "Nguồn này hiện chưa hỗ trợ phát trong app.") } // xảy ra với external_embed
        "IDEMPOTENCY_KEY_REUSED" -> _uiState.update { it.copy(error = "Có lỗi khi khởi tạo phiên phát, vui lòng thử lại.") } // sinh key mới rồi mới cho phép bấm lại
        else -> _uiState.update { it.copy(error = result.message) }
    }
}
```

**Xử lý dialog xác nhận resume:**
```kotlin
fun onResumeConfirmed(resume: Boolean) {
    val s = session ?: return
    val seekTo = if (resume) s.resumePositionSeconds else 0
    preparePlayer(s, seekTo)
    _uiState.update { it.copy(showResumeDialog = false) }
}
```

**Owned HLS — cấu hình cookie cho ExoPlayer (Android, không phải web nên không vướng CORS):**
```kotlin
// Chia sẻ CookieJar, KHÔNG chia sẻ authenticated OkHttpClient.
val cookieManager = CookieManager().apply { setCookiePolicy(CookiePolicy.ACCEPT_ALL) }
val sharedCookieJar = JavaNetCookieJar(cookieManager)

val backendApiClient = OkHttpClient.Builder()
    .cookieJar(sharedCookieJar)
    .addInterceptor(authInterceptor)
    .authenticator(tokenAuthenticator)
    .build()

val mediaClient = OkHttpClient.Builder()
    .cookieJar(sharedCookieJar)
    // TUYỆT ĐỐI không AuthInterceptor/TokenAuthenticator: tránh rò bearer JWT
    // sang CDN/host ngoài và tránh coi 401 media là 401 API.
    .build()

val dataSourceFactory = OkHttpDataSource.Factory(mediaClient)
val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(session.playbackUrl))
```
Vì cookie là `HttpOnly` và có domain/path giới hạn — **không đọc/copy giá trị cookie trong code Kotlin**, chỉ để `CookieJar` tự lưu và tự gắn. Backend API client và media client là hai instance tách biệt nhưng dùng cùng đúng một `CookieJar`. Cấu hình redirect/media allowlist phải ngăn credential media đi sang host không thuộc phạm vi; test integration phải xác nhận cookie từ Gateway thực sự hợp lệ cho host/path trong `playbackUrl`.

Với owned HLS, lập lịch gọi `POST /streaming/playback-sessions/{sessionId}/media-auth` trước `mediaAuth.expiresAt`, để shared `CookieJar` nhận `Set-Cookie` mới và map `data` thành `MediaAuth` mới. Không gọi endpoint này cho `third_party`; nếu renew/heartbeat bị từ chối thì dừng playback và gửi `failed` best-effort. `expiresAt` từ response renew là mốc cho lần lập lịch tiếp theo.

**Nguồn KKPhim (`external_hls`):** không cache `playbackUrl` — dùng thẳng 1 lần cho `MediaItem`, không lưu vào Room/SharedPreferences/log. Nếu người dùng thoát rồi phát lại, phải gọi lại `createSessionUseCase` để lấy URL mới (server có thể trả URL khác lần trước).

### 4.4. Favorites
```kotlin
// Thêm yêu thích
suspend fun addFavorite(profileId: String, movieId: String) =
    profileApi.putFavorite(profileId, movieId) // PUT, không có body, idempotent theo trạng thái cuối

// Bỏ yêu thích
suspend fun removeFavorite(profileId: String, movieId: String) =
    profileApi.deleteFavorite(profileId, movieId)
```
Nút yêu thích **disable** (không phải ẩn) khi chưa chọn hồ sơ, kèm tooltip/snackbar "Chọn hồ sơ để dùng tính năng này".

### 4.5. Gói cước (mock) — màn hình chờ xử lý
Vì thanh toán là mock và app không tự giả lập thành công, cần thêm 1 trạng thái UI mới không có trong thiết kế cũ:
```kotlin
sealed interface SubscribeUiState {
    object SelectingPlan : SubscribeUiState
    object AwaitingPayment : SubscribeUiState // sau khi POST /subscriptions/subscribe thành công
    data class Active(val subscription: Subscription) : SubscribeUiState
}
```
Sau khi gọi `subscribe` thành công, poll `GET /subscriptions/current` định kỳ (ví dụ mỗi 5 giây, tối đa 2 phút) để phát hiện khi trạng thái chuyển `active` — vì webhook xử lý phía backend là bất đồng bộ, **không** coi việc gọi `subscribe` thành công là đã có gói cước.

`subscribe` trả `PaymentOrder` với `paymentId`, `orderId`, `subscriptionId`, `status`, `paymentExpiresAt`, snapshot plan và `idempotentReplay`. `current` trả `CurrentSubscription?`: `pending` có `startAt/endAt = null`, còn `active` có hai thời điểm này. Khi API trả `null`, không có gói pending/active. UI chỉ mở nội dung subscription khi `current.status == "active"`; app không gọi webhook hoặc tự đổi trạng thái thanh toán.

### 4.6. Profile CRUD

- Sau đăng nhập, gọi `GET /profiles`. Nếu rỗng, yêu cầu tạo profile; nếu có, hiển thị màn chọn profile trước Home cá nhân hóa.
- Tạo bằng `POST /profiles`, sửa bằng `PATCH /profiles/{profileId}`, xóa bằng `DELETE /profiles/{profileId}`; ẩn nút tạo khi đã đủ 5 profile.
- Khi đổi/xóa profile đang chọn: hủy request cá nhân cũ, xóa cache nhạy cảm gắn profile và cập nhật một `CurrentProfileStore` dùng chung qua interface, không dùng mutable global `SharedFlow` đặt trong feature.

### 4.7. Search và Browse

- Search dùng `/catalog/search` với `q` tối đa 100 ký tự và phân trang từ 1.
- Browse/filter dùng `/catalog/movies` với đầy đủ `genre`, `country`, `year`, `type`, `contentKind`, `sourceType`, `provider`, `sort`, `profileId`; không nhét filter vào `/catalog/search`.
- Khi có profile đang chọn, luôn truyền `profileId` và bearer để kids filter được backend thực thi.

---

## 5. Cấu trúc dự án Android (cập nhật các điểm khác biệt so với bản trước)

Giữ nguyên cấu trúc multi-module Clean Architecture đã thiết kế trước đó, chỉ điều chỉnh các phần sau:

```
core-network/
  ├── ApiResponseDto.kt          -- thêm field requestId
  ├── AuthInterceptor.kt
  ├── TokenAuthenticator.kt      -- single-flight refresh + chống retry loop (mục 2.5)
  ├── RefreshTokenApi.kt         -- chạy trên bare client, không authenticator
  ├── BackendApiClient.kt        -- có bearer/authenticator
  ├── MediaHttpClient.kt         -- không bearer/authenticator, dùng chung CookieJar
  ├── IdempotencyKeyGenerator.kt -- MỚI
  └── DeviceIdProvider.kt        -- MỚI

core-player/
  ├── PlayerManager.kt
  ├── SharedCookieJarProvider.kt -- MỚI, chia sẻ CookieJar chứ không chia sẻ authenticated client
  └── (bỏ DownloadTracker.kt — chưa có API offline trong contract)

domain/usecase/streaming/
  ├── CreatePlaybackSessionUseCase.kt  -- thay cho GetPlaybackInfoUseCase cũ
  ├── SendHeartbeatUseCase.kt          -- MỚI
  ├── SendProgressUseCase.kt           -- đổi chữ ký: seq, positionSeconds, durationSeconds?
  ├── SendPlaybackEventUseCase.kt      -- MỚI
  └── RenewMediaAuthUseCase.kt         -- MỚI, trả MediaAuth với expiresAt mới

feature-player/
  └── PlayerViewModel.kt  -- viết lại hoàn toàn theo vòng đời session (mục 4.3)

feature-subscription/
  └── SubscriptionViewModel.kt -- thêm trạng thái AwaitingPayment + polling
```

---

## 6. Cấu trúc API client — theo đúng path/method thật

```
AuthApi:
  POST /auth/register        { email, password, fullName }
  POST /auth/login           { email, password, deviceId, deviceName? }
  POST /auth/refresh         { refreshToken }
  POST /auth/logout          { refreshToken }              -> 204
  GET  /auth/session

ProfileApi:
  GET    /profiles
  POST   /profiles                                  { name, avatarId?, isKids? }
  PATCH  /profiles/{profileId}                       { name?, avatarId?, isKids? }
  DELETE /profiles/{profileId}                       -> 204
  GET    /profiles/{profileId}/favorites
  PUT    /profiles/{profileId}/favorites/{movieId}   -- không body
  DELETE /profiles/{profileId}/favorites/{movieId}   -> 204
  GET    /profiles/{profileId}/watch-history

CatalogApi:
  GET /catalog/home?pageSize=
  GET /catalog/movies?page=&pageSize=&q=&genre=&country=&year=&type=&contentKind=&sourceType=&provider=&sort=&profileId=
  GET /catalog/search?q=&page=&pageSize=&profileId=
  GET /catalog/movies/{movieId}?profileId=
  GET /home?profileId=

SubscriptionApi:
  GET  /subscriptions/plans
  POST /subscriptions/subscribe    { planId, paymentMethod }   -- header Idempotency-Key
  GET  /subscriptions/current

StreamingApi:
  POST /streaming/playback-sessions
       header: Idempotency-Key
       body: { movieId, playableId, sourceItemId, profileId }
  POST /streaming/playback-sessions/{sessionId}/heartbeat
  POST /streaming/playback-sessions/{sessionId}/progress   { seq, positionSeconds, durationSeconds? }
  POST /streaming/playback-sessions/{sessionId}/events     { eventId, type, playedSeconds?, reasonCode? }
  POST /streaming/playback-sessions/{sessionId}/media-auth -- owned HLS; data: MediaAuth
```

Tất cả interface Retrofit đặt trong `data/remote/`, DTO khớp field theo tài liệu (camelCase, đúng tên như `movieId`, `playableId`, `sourceItemId`, `profileId` — **không** đổi tên field khi map DTO, tránh nhầm lẫn khi debug log network).

---

## 7. Contract đã chốt và phạm vi chưa có API

- `media-auth` trả `MediaAuth`; heartbeat, progress và events lần lượt trả `PlaybackHeartbeat`, `PlaybackProgressAck`, `PlaybackEventAck`.
- `PaymentOrder` và `CurrentSubscription` đã có schema trong `backend_api.md`; payment vẫn chỉ là mock provider.
- `PlaybackSession.subtitles` là `List<String>` URL đã được backend kiểm tra. Không có nhãn/ngôn ngữ/track nên không làm UI chọn subtitle ở giai đoạn này.
- Trước khi nghiệm thu owned HLS trên Android, chạy instrumentation/integration test với Gateway và media-edge cùng hostname. Test phải xác nhận shared `CookieJar` nhận `Set-Cookie` host-only có path asset/generation và tự gửi nó cho master, variant, segment. Đây là kiểm thử topology runtime, không phải DTO chưa xác định.

Các phần còn lại ngoài phạm vi hiện tại:

- Không thiết kế màn hình tải phim offline — chưa có API, `offlineSupported` luôn `false`.
- Không tự nhúng iframe/WebView cho `link_embed` — backend đã chặn bằng `422 PLAYBACK_MODE_UNSUPPORTED`, làm vậy là đi vòng qua giới hạn backend đặt ra có chủ đích.
- Không cache `playbackUrl` của nguồn `third_party` vào Room/log/analytics dưới bất kỳ hình thức nào.
- Không gọi trực tiếp Notification/Recommendation service — chỉ đọc qua `/home` đã ghép sẵn.
- Không tự dựng màn hình theo dõi tiến trình transcode cho CMS — Gateway chưa có endpoint polling cho việc này.
- Không giả lập/tự gọi payment webhook từ app để "test" thanh toán thành công trong bản thật (chỉ hợp lý trong môi trường dev có mock server riêng).
