# To-do & Prompt cho AI Agent — Triển khai Frontend Android
### (Theo đúng contract `backend_api.md` — thay thế bộ prompt frontend trước đó)

**Bản đồng bộ 2026-09-14 / revision 3.** Cách dùng: đưa từng "Prompt" theo đúng thứ tự giai đoạn cho AI coding agent (Claude Code, Cursor...). Luôn đính kèm `backend_api.md` (contract gốc) và `frontend-chi-tiet.md` (thiết kế đã lập) làm ngữ cảnh nền. Thứ tự ưu tiên là `backend_api.md` > `frontend-chi-tiet.md` > file này > `thiet-ke-app-xem-phim.md`; agent không tự đoán field/endpoint ngoài contract. Sau mỗi giai đoạn, yêu cầu agent build/test và báo lỗi trước khi sang giai đoạn kế tiếp.

---

## Giai đoạn 0 — Khởi tạo dự án

- [ ] Tạo multi-module project
- [ ] Cấu hình version catalog
- [ ] Chốt cấu hình build/app còn thiếu

**Prompt:**
```
Hãy tạo một dự án Android Kotlin multi-module tên "MovieApp" theo kiến trúc Clean Architecture + MVVM, dựa trên contract API đính kèm (backend_api.md) và thiết kế frontend đính kèm (frontend-chi-tiet.md).

CONFIG-GATE trước khi sinh project production: yêu cầu chủ dự án chốt `applicationId/namespace`, minSdk, compileSdk/targetSdk, JDK/AGP/Kotlin version và base URL cho dev/staging/prod. Không tự khóa `com.example.*` hay từ "mới nhất" thành quyết định lâu dài. Nếu cần scaffold trước, ghi các giá trị placeholder tập trung và đánh dấu TODO rõ ràng.

Tạo các module: app, core-ui, core-network, core-database, core-player, core-common, domain, data, feature-auth, feature-home, feature-detail, feature-player, feature-search, feature-profile, feature-subscription.

Version catalog (libs.versions.toml) cần có các version tương thích và được pin: Compose BOM, Hilt, Retrofit + OkHttp + kotlinx.serialization, Room, Media3 (bao gồm media3-datasource-okhttp), Coroutines, Coil, Navigation Compose, Android security/Keystore-backed storage, DataStore. Không dùng version động như `+`.

Local networking: `127.0.0.1:3000` chỉ trỏ đúng từ chính máy host. Với Android emulator dùng `10.0.2.2:3000` hoặc `adb reverse tcp:3000 tcp:3000`; thiết bị thật cần địa chỉ LAN/tunnel phù hợp. Chỉ cho phép cleartext HTTP trong debug qua network security config; staging/prod phải HTTPS.

domain không phụ thuộc Android framework. feature-* chỉ phụ thuộc domain + core-ui + core-common, không phụ thuộc data/core-network trực tiếp.

Build thử để đảm bảo project sync thành công.
```

---

## Giai đoạn 1 — core-common

**Prompt:**
```
Trong module core-common, tạo:
1. Result.kt: sealed interface Result<out T> với Success(data: T) và Error(code: String, message: String, requestId: String? = null, throwable: Throwable? = null) — `code` dùng cho logic UI đặc tả; `requestId` phải được truyền xuyên suốt để tra log support.
2. DispatcherProvider.kt: interface + DefaultDispatcherProvider.
3. IdempotencyKeyGenerator.kt: object với hàm generate(): String trả UUID.randomUUID().toString() (khớp charset [A-Za-z0-9._:-] và độ dài 8-120 mà backend yêu cầu).
4. Extensions.kt: hàm format thời lượng giây -> "1h 58m", format ngày giờ ISO string sang hiển thị tiếng Việt.
```

---

## Giai đoạn 2 — core-network (theo đúng envelope và cơ chế auth thật)

**Prompt:**
```
Trong module core-network, dựa CHÍNH XÁC theo mục 1 và mục 3 của backend_api.md, thiết lập:

1. ApiResponseDto<T> (kotlinx.serialization):
   data class ApiResponseDto<T>(val success: Boolean, val data: T?, val error: ErrorDto?, val requestId: String)
   data class ErrorDto(val code: String, val message: String, val details: JsonElement? = null)
   PagedResponseDto<T>(val items: List<T>, val page: Int, val pageSize: Int, val totalItems: Int, val totalPages: Int)
   Field requestId LUÔN có mặt kể cả khi lỗi — expose ra Result.Error để tầng UI có thể hiển thị khi cần báo lỗi cho support.

2. DeviceIdProvider: dùng DataStore, hàm suspend getOrCreate(): String — đọc deviceId đã lưu, nếu chưa có thì sinh UUID mới và lưu lại VĨNH VIỄN (không sinh mới mỗi lần app khởi động hay mỗi lần login).

3. TokenStorage: interface lưu accessToken (in-memory, KHÔNG persist), refreshToken (EncryptedSharedPreferences). Có hàm save(access, refresh) LUÔN ghi đè cả 2 giá trị (vì backend rotate refresh token mỗi lần refresh — token cũ không dùng lại được), và hàm clear().

4. AuthInterceptor: OkHttp Interceptor gắn header "Authorization: Bearer ${tokenStorage.getAccessToken()}" nếu có, bỏ qua với các path public: /auth/register, /auth/login, /auth/refresh, /auth/logout, /auth/.well-known/jwks.json.

5. TokenAuthenticator implements okhttp3.Authenticator, có 1 Mutex (kotlinx.coroutines.sync.Mutex) và single-flight thật sự:
   - Chặn retry loop bằng cách đếm `priorResponse`; không retry quá một vòng refresh.
   - Lấy access token trên request gây 401 (`failedAccessToken`). Sau khi vào Mutex, so sánh nó với access token hiện tại. Nếu token hiện tại đã khác, một request khác đã refresh xong: retry bằng token hiện tại và KHÔNG gọi refresh lần nữa.
   - Chỉ khi token chưa đổi mới lấy refreshToken hiện tại và gọi `RefreshTokenApi.refresh(...)`.
   - `RefreshTokenApi` là interface tối thiểu thuộc core-network và PHẢI chạy trên một Retrofit/OkHttp client trần, không AuthInterceptor và không TokenAuthenticator; không dùng `AuthApi` của data để tránh dependency vòng và refresh đệ quy.
   - Nếu thành công: LƯU LẠI CẢ accessToken VÀ refreshToken MỚI vào TokenStorage (vì server rotate refresh token, không được giữ token cũ), trả request mới có access token mới.
   - Nếu thất bại (kể cả 401 khi refresh): gọi TokenStorage.clear(), trả null (không retry vô hạn).
   - Giải thích trong comment: Mutex chỉ serialize; phép so sánh token bên trong lock mới tạo single-flight.
   Viết unit test xác nhận chỉ gọi API refresh đúng 1 lần khi có 3 request 401 xảy ra đồng thời.

6. Không tái sử dụng `requestId` của response trước làm ID request mới. Có thể để Gateway tự sinh `x-request-id`; nếu client chủ động trace thì sinh UUID mới cho từng request logic và chỉ giữ nguyên khi retry đúng request đó.

7. Cung cấp BA cấu hình tách vai trò:
   - `BackendApiClient`: AuthInterceptor + TokenAuthenticator + shared CookieJar.
   - `RefreshClient`: không AuthInterceptor, không TokenAuthenticator.
   - `MediaHttpClient`: không AuthInterceptor, không TokenAuthenticator, nhưng dùng CHUNG đúng instance CookieJar với BackendApiClient.
   Tạo CookieManager + JavaNetCookieJar singleton trong SharedCookieJarProvider. Không expose authenticated BackendApiClient cho Media3: nếu dùng nó, bearer JWT có thể bị gửi sang host HLS ngoài và 401 media có thể kích hoạt refresh sai.

8. Viết test bảo mật: request MediaHttpClient tới host media/redirect không bao giờ có header Authorization; 401 media không gọi RefreshTokenApi; cookie chỉ được gửi khi domain/path phù hợp.

Build, test, báo cáo lỗi.
```

---

## Giai đoạn 3 — core-database (rút gọn so với bản trước, vì backend đã lưu watch-history/favorites)

**Prompt:**
```
Trong module core-database, dùng Room, tạo MỘT bảng duy nhất phục vụ cache hiển thị nhanh (KHÔNG lưu watch-history/favorites cục bộ vì backend đã là nguồn sự thật cho các dữ liệu này):

CachedMovieEntity: id (PK), title, posterUrl, backdropUrl, cachedAt — dùng để hiển thị tạm khi mở app trong lúc chờ gọi /catalog/home, tránh màn hình trắng.

Tạo AppDatabase + CachedMovieDao (upsert, getAll(): Flow<List<CachedMovieEntity>>, hàm xoá cache cũ hơn N ngày).

KHÔNG tạo bảng WatchHistory/Favorite ở local — hai loại dữ liệu này luôn đọc trực tiếp từ API /profiles/:id/watch-history và /profiles/:id/favorites theo đúng contract, tránh lệch dữ liệu giữa local và server.
```

---

## Giai đoạn 4 — core-player (media client riêng, chia sẻ CookieJar)

**Prompt:**
```
Trong module core-player, dùng Media3 (media3-exoplayer, media3-exoplayer-hls, media3-datasource-okhttp), triển khai:

1. PlayerManager: wrapper ExoPlayer với các hàm:
   - prepare(playbackUrl: String, startPositionMs: Long)
   - play(), pause(), release()
   - currentPositionSeconds(): Int, durationSeconds(): Int?
   - Flow<PlayerState> phát trạng thái (Idle/Buffering/Playing/Paused/Ended/Error)

2. QUAN TRỌNG — cấu hình nguồn dữ liệu:
   PlayerManager phải nhận `MediaHttpClient` được inject từ ngoài. Đây là client RIÊNG, không có AuthInterceptor/TokenAuthenticator, nhưng dùng chung CookieJar singleton với BackendApiClient. KHÔNG nhận authenticated BackendApiClient và không tự tạo OkHttpClient bên trong core-player. Dùng OkHttpDataSource.Factory(mediaHttpClient), rồi HlsMediaSource.Factory(dataSourceFactory).
   Lý do: shared CookieJar nhận cookie HttpOnly cho owned HLS; client tách biệt ngăn rò bearer JWT sang CDN/host ngoài và ngăn 401 media kích hoạt refresh API. Viết comment giải thích trong code.

3. KHÔNG tạo DownloadTracker hay bất kỳ logic tải offline nào — contract backend hiện tại không hỗ trợ offline (offlineSupported luôn false).

Viết test xác nhận PlayerManager nhận đúng qualified MediaHttpClient qua constructor injection; module không `new OkHttpClient()` và media request không có Authorization.
```

---

## Giai đoạn 5 — core-ui

**Prompt:**
```
Trong module core-ui, dùng Jetpack Compose, tạo:
1. Theme.kt/Color.kt/Typography.kt: theme tối làm mặc định, hỗ trợ light mode.
2. Component: MovieCard(title, posterUrl, progressPercent: Float? = null, onClick), LoadingIndicator(), ErrorView(message, requestId: String? = null, onRetry) — ErrorView hiển thị thêm requestId nhỏ phía dưới (dùng để đối chiếu log khi người dùng báo lỗi, theo đúng field requestId trong mọi response lỗi của backend), PrimaryButton(text, onClick, enabled=true), MovieRow(title, movies, onMovieClick).
3. ResumeConfirmDialog(resumePositionSeconds: Int, onConfirm: (Boolean) -> Unit): dialog hỏi "Tiếp tục xem từ {mm:ss}?" với 2 nút "Tiếp tục"/"Xem lại từ đầu" — dùng ở màn Player khi resumeNeedsConfirmation=true.

Viết @Preview cho mỗi component ở cả 2 mode.
```

---

## Giai đoạn 6 — Domain layer (khớp chính xác field theo contract)

**Prompt:**
```
Trong module domain, dựa vào mục 2, 4, 5, 6, 7, 8 của backend_api.md, định nghĩa model VÀ GIỮ NGUYÊN TÊN FIELD camelCase như trong tài liệu (để tránh nhầm lẫn khi map DTO ở tầng data sau này):

1. Model:
   Movie(id, title, originTitle, description, posterUrl, backdropUrl, releaseYear, type, contentKind, status, accessTier, isKidsSafe, averageRating, publishedAt, version, createdAt, updatedAt)
   MovieDetail : Movie + genres: List<GenreRef>, countries: List<CountryRef>, playableItems: List<PlayableItem>, sources: List<SourceItem>
   PlayableItem(id, kind, seasonId, seasonNumber, episodeNumber, label, sortOrder, durationSeconds, archivedAt)
   SourceItem(id, sourceType, provider, sourceStatus, sourceItemId, playableId, serverKey, serverLabel, playbackMode)
   Profile(id, name, avatarId, isKids, createdAt, updatedAt)
   HistoryItem(movieId, playableId, sourceItemId, positionSeconds, durationSeconds, updatedAt, movie: Movie?, tombstone)
   PlaybackSession(sessionId, playableId, sourceItemId, sourceType, protocol, playbackUrl, mediaAuth: MediaAuth?, urlExpiresAt, leaseExpiresAt, resumePositionSeconds, resumeNeedsConfirmation, subtitles: List<String>, offlineSupported, idempotentReplay)
   MediaAuth(expiresAt) ở domain là metadata lập lịch tối thiểu. DTO data parse đủ cookieName, cookieValue, path, expiresAt, nhưng mapper KHÔNG đưa cookieValue vào UI/domain/log/analytics; CookieJar nhận Set-Cookie tự động.
   PlaybackHeartbeat(sessionId, leaseExpiresAt); WatchProgress(profileId, playableId, movieId, sourceItemId, sessionOrdinal, seq, positionSeconds, durationSeconds, updatedAt); PlaybackProgressAck(sessionId, accepted, applied, progress); PlaybackEventAck(sessionId, eventId, duplicate, state, qualified)
   Plan(id, name, price, currency, durationDays, maxConcurrentStreams, maxResolution, version)
   PaymentOrder(paymentId, orderId, subscriptionId, status, paymentExpiresAt, amount, currency, paymentMethod, planSnapshot, provider, idempotentReplay, requestId)
   CurrentSubscription?(subscriptionId, status: pending|active, startAt, endAt, autoRenew=false, plan, payment); toàn bộ `data` của GET current có thể null
   CatalogHome(newReleases: List<Movie>, topRated: List<Movie>, trending: List<Movie>) — đây là domain model đã làm phẳng; DTO ở data vẫn phải giữ wrapper `{type, items}` đúng response backend
   PersonalizedHome(profileId, sections: List<HomeSection>) với HomeSection là sealed class theo type: ContinueWatching(items: List<HistoryItem>), CatalogNewReleases(items: List<Movie>), FallbackNewReleases(reason: String, items: List<Movie>), Other(type: String, items: List<Movie>)

   Map đầy đủ response playback theo các model trên. `subtitles` là URL chuỗi; chưa có metadata để làm subtitle picker. `RenewMediaAuthUseCase` trả MediaAuth mới và chỉ dùng expiresAt để lập lịch; không đưa cookieValue ra UI/log/analytics.

2. Interface repository (KHÔNG implement):
   AuthRepository: register, login(email, password, deviceName?), refresh, logout, getSession. Data implementation tự lấy deviceId từ DeviceIdProvider; feature/domain không phụ thuộc core-network.
   ProfileRepository: getProfiles, createProfile, updateProfile, deleteProfile, getFavorites(profileId), putFavorite(profileId, movieId), deleteFavorite(profileId, movieId), getWatchHistory(profileId)
   CatalogRepository: getPublicHome(pageSize), getPersonalizedHome(profileId), getMovies(filters...), search(q, page, pageSize, profileId?), getMovieDetail(movieId, profileId?)
   SubscriptionRepository: getPlans, subscribe(planId, paymentMethod, idempotencyKey): Result<PaymentOrder>, getCurrent(): Result<CurrentSubscription?>
   StreamingRepository: createPlaybackSession(...), sendHeartbeat(sessionId): Result<PlaybackHeartbeat>, sendProgress(...): Result<PlaybackProgressAck>, sendEvent(...): Result<PlaybackEventAck>, renewMediaAuth(sessionId): Result<MediaAuth>.

3. Use case, mỗi use case 1 class có operator fun invoke():
   RegisterUseCase, LoginUseCase, RestoreSessionUseCase, LogoutUseCase
   GetProfilesUseCase, CreateProfileUseCase, UpdateProfileUseCase, DeleteProfileUseCase, GetFavoritesUseCase, ToggleFavoriteUseCase (nhận thêm tham số isFavorite hiện tại để gọi đúng PUT hoặc DELETE), GetWatchHistoryUseCase (LỌC BỎ item có tombstone=true trước khi trả ra ngoài)
   GetPublicHomeUseCase, GetPersonalizedHomeUseCase, BrowseMoviesUseCase, SearchMoviesUseCase, GetMovieDetailUseCase
   GetPlansUseCase, SubscribeUseCase, GetCurrentSubscriptionUseCase
   CreatePlaybackSessionUseCase, SendHeartbeatUseCase, SendProgressUseCase, SendPlaybackEventUseCase, RenewMediaAuthUseCase

Tất cả hàm trả Result<T> từ core-common (đã có field code để xử lý theo error.code cụ thể ở tầng trên).
```

---

## Giai đoạn 7 — Data layer

**Prompt:**
```
Trong module data, triển khai đúng theo path/method/field trong backend_api.md, KHÔNG tự đổi tên field hay tự thêm/bớt tham số:

1. Retrofit interface (data.remote):
   AuthApi: POST auth/register, POST auth/login, POST auth/refresh, POST auth/logout, GET auth/session
   ProfileApi: GET/POST profiles, PATCH/DELETE profiles/{profileId}, GET profiles/{profileId}/favorites,
               PUT profiles/{profileId}/favorites/{movieId} (KHÔNG có @Body), DELETE profiles/{profileId}/favorites/{movieId},
               GET profiles/{profileId}/watch-history
   CatalogApi: GET catalog/home (query pageSize), GET catalog/movies (đầy đủ query: page, pageSize, q, genre, country, year, type, contentKind, sourceType, provider, sort, profileId),
               GET catalog/search (q, page, pageSize, profileId), GET catalog/movies/{movieId} (query profileId optional),
               GET home (query profileId, method GET, path KHÔNG có prefix /catalog)
   SubscriptionApi: GET subscriptions/plans, POST subscriptions/subscribe (@Header("Idempotency-Key"), body {planId, paymentMethod}) trả PaymentOrder, GET subscriptions/current trả CurrentSubscription?.
   StreamingApi: POST streaming/playback-sessions (@Header("Idempotency-Key"), @Body {movieId, playableId, sourceItemId, profileId}),
                 POST streaming/playback-sessions/{sessionId}/heartbeat,
                 POST streaming/playback-sessions/{sessionId}/progress (@Body {seq, positionSeconds, durationSeconds?}),
                 POST streaming/playback-sessions/{sessionId}/events (@Body {eventId, type, playedSeconds?, reasonCode?}),
                 POST streaming/playback-sessions/{sessionId}/media-auth (chỉ owned HLS, trả MediaAuth mới)

2. DTO (data.remote.dto) khớp CHÍNH XÁC field trong tài liệu (camelCase, đúng optional/nullable). Chú ý các field đặc thù: Movie.originTitle, Movie.contentKind, Movie.accessTier, Movie.isKidsSafe (không phải isKids — đó là field của Profile, khác nhau, không nhầm lẫn); SourceItem KHÔNG có link_m3u8/link_embed/externalSlug — chỉ có id, sourceType, provider, sourceStatus, sourceItemId, playableId, serverKey, serverLabel, playbackMode.
   Decode đầy đủ PlaybackHeartbeat, PlaybackProgressAck, PlaybackEventAck và MediaAuth theo `backend_api.md`; dùng `ignoreUnknownKeys` chỉ để chịu được field được backend bổ sung về sau, không thay cho DTO contract.

3. Mapper (data.mapper) map DTO -> domain model, giữ nguyên field như đã định nghĩa domain ở giai đoạn 6.

4. Implement repository (data.repository, hậu tố Impl):
   - Toàn bộ hàm bắt exception HTTP, đọc error.code/error.message/requestId từ ApiResponseDto khi lỗi, map thành Result.Error(code, message, requestId).
   - ToggleFavoriteUseCase ở domain sẽ gọi ProfileRepositoryImpl.putFavorite hoặc deleteFavorite tương ứng.
   - GetWatchHistoryUseCase filter tombstone nên có thể để logic filter ở use case (domain) thay vì ở đây, ProfileRepositoryImpl chỉ trả nguyên danh sách từ API.

5. Hilt Module (NetworkModule, RepositoryModule) binding Api và Repository.

6. Không khai báo `AuthApi.refresh` theo cách khiến data/core-network phụ thuộc vòng nhau. TokenAuthenticator dùng `RefreshTokenApi` + RefreshClient trần từ giai đoạn 2; AuthApi ở data phục vụ repository auth thông thường.

Build, test, báo cáo lỗi. Đặc biệt kiểm tra kỹ chữ ký PUT/DELETE favorites không có @Body — đây là điểm dễ làm sai nhất so với REST thông thường.
```

---

## Giai đoạn 8 — feature-auth

**Prompt:**
```
Trong module feature-auth, dùng Jetpack Compose + Hilt ViewModel:

1. LoginScreen: email, password, nút "Đăng nhập". Gọi LoginUseCase(email, password, deviceName?) qua domain; AuthRepositoryImpl ở data tự lấy deviceId bền vững từ DeviceIdProvider. Feature KHÔNG inject core-network trực tiếp. Password validate 12-128 ký tự.
2. RegisterScreen: thêm field fullName (bắt buộc theo contract), password 12-128 ký tự.
3. AuthViewModel: StateFlow<AuthUiState> (Idle/Loading/Success/Error(message, code, requestId)). Repository data chịu trách nhiệm lưu token khi login; feature không inject TokenStorage. `register` chỉ trả user, KHÔNG trả TokenPair: đăng ký thành công chuyển về Login (có thể điền sẵn email), không gọi onAuthSuccess.
4. SplashViewModel gọi RestoreSessionUseCase. Luồng restore kiểm tra refreshToken persisted ở data/core-network; KHÔNG yêu cầu access token còn trong RAM. Nếu có refresh token, gọi GET /auth/session; request đầu có thể 401, TokenAuthenticator refresh đúng một lần bằng RefreshClient trần rồi retry. Chỉ `active=true` mới vào app; inactive/refresh thất bại thì clear token và về Login.

Build, test cơ bản, báo cáo lỗi.
```

---

## Giai đoạn 9 — feature-home (2 nguồn dữ liệu)

**Prompt:**
```
Trong module feature-home, triển khai theo đúng 2 API riêng biệt mô tả ở mục 4.1 của frontend-chi-tiet.md:

1. HomeViewModel: nhận currentProfileId: StateFlow<String?> qua interface `CurrentProfileStore` được inject. Không dùng mutable global SharedFlow; implementation/scope đặt ở app hoặc data và hủy dữ liệu/request của profile cũ khi chuyển profile.
   - Nếu currentProfileId == null: gọi GetPublicHomeUseCase(), render CatalogHome (newReleases/topRated/trending).
   - Nếu currentProfileId != null: gọi GetPersonalizedHomeUseCase(profileId), render theo sections trả về — LUÔN render được catalog_new_releases; nếu có continue_watching thì render riêng 1 hàng ở đầu; nếu gặp section type=fallback_new_releases, vẫn hiển thị items đó nhưng gắn nhãn nhẹ (ví dụ chỉ ghi "Có thể bạn thích" thay vì "Dành riêng cho bạn") để không tuyên bố cá nhân hóa khi backend đã báo reason=recommendation_unavailable.
2. Bấm vào item trong hàng "Tiếp tục xem": lấy playableId/sourceItemId/movieId trực tiếp từ HistoryItem đã có, điều hướng THẲNG vào Player, KHÔNG quay lại màn Detail trước (khác hành vi so với các hàng phim khác — bấm vào phim thường thì mở Detail để chọn playable/source).
3. HomeUiState là sealed class có 2 nhánh Public/Personalized rõ ràng, Composable HomeScreen dùng when để render đúng UI cho từng nhánh.

Build, test, báo cáo lỗi.
```

---

## Giai đoạn 10 — feature-detail (chọn playable + source)

**Prompt:**
```
Trong module feature-detail, triển khai MovieDetailScreen theo đúng luồng mục 4.2 của frontend-chi-tiet.md:

1. Gọi GetMovieDetailUseCase(movieId, profileId) lấy MovieDetail gồm playableItems và sources.
2. Nếu movie.type == "series": nhóm playableItems theo seasonNumber, hiển thị dropdown/tab chọn mùa, danh sách tập theo episodeNumber trong mùa đã chọn (sắp theo sortOrder).
3. Sau khi có playableId được chọn (hoặc mặc định với phim lẻ chỉ có 1 playable kind=movie): filter sources theo `playableId`, `sourceStatus in [available, unknown]` và `sourceItemId != null`. Ẩn source unavailable/error hoặc chưa có selector phát hợp lệ.
4. Hiển thị danh sách source đã lọc bằng serverLabel (ví dụ dạng chip/radio button), người dùng chọn 1 source trước khi nút "Phát" được bật.
5. Bấm "Phát": điều hướng sang feature-player kèm `movieId`, `PlayableItem.id` làm `playableId`, **`SourceItem.sourceItemId`** làm selector `sourceItemId`, và `profileId`. Không dùng `SourceItem.id` vì đó là content source ID; không parse ID từ text UI.
6. Nút yêu thích: disable nếu profileId == null (chưa chọn hồ sơ), gọi ToggleFavoriteUseCase khi bật/tắt.

Build, test, báo cáo lỗi.
```

---

## Giai đoạn 11 — feature-player (phần quan trọng nhất, vòng đời session đầy đủ)

**Prompt:**
```
Trong module feature-player, triển khai TOÀN BỘ theo đúng mã Kotlin mẫu ở mục 4.3 của frontend-chi-tiet.md (bám sát logic, có thể điều chỉnh chi tiết implementation nhưng KHÔNG được bỏ qua bất kỳ bước nào trong danh sách sau):

1. Nhận navigation argument: movieId, playableId, sourceItemId, profileId.
2. PlayerViewModel.startPlayback(): sinh 1 Idempotency-Key MỚI (UUID) MỖI LẦN người dùng bấm "Phát" từ màn Detail (không phải mỗi lần PlayerViewModel khởi tạo lại do configuration change — cần giữ key qua SavedStateHandle nếu Activity bị recreate trong lúc đang chờ response tạo session), gọi CreatePlaybackSessionUseCase.
3. Xử lý resumeNeedsConfirmation: nếu true, hiển thị ResumeConfirmDialog (đã tạo ở core-ui) TRƯỚC KHI chuẩn bị player, không tự động seek.
4. Sau khi player bắt đầu phát thật sự (ExoPlayer chuyển sang state Playing lần đầu, không phải ngay sau khi gọi prepare()), gửi event "started".
5. Vòng lặp heartbeat mỗi 30 giây trong suốt thời gian màn hình Player đang mở (kể cả khi pause). Cập nhật leaseExpiresAt từ response; nếu heartbeat bị từ chối thì dừng player và kết thúc failed, không tiếp tục phát trên lease đã mất.
6. Vòng lặp progress mỗi 10-15 giây, seq là số tăng dần dạng String bắt đầu từ "1", KHÔNG reset seq nếu người dùng seek lùi/tua — seq chỉ tăng theo số lần gửi, không liên quan tới positionSeconds.
7. Theo dõi playedSeconds bằng monotonic clock chỉ trong state Playing (KHÔNG cộng cứng theo chu kỳ và không tính pause/buffering); khi đạt >= 30 giây gửi đúng 1 event "qualified".
8. Khi rời màn hình, lỗi nghiêm trọng, hoặc phát xong: dùng một terminal function idempotent chạy trong cùng coroutine để await final progress trước rồi mới gửi "stopped"/"failed". Hủy mọi heartbeat/progress/renew job sau terminal. Đây là best-effort; process death dựa vào lease TTL backend.
9. Xử lý riêng 3 mã lỗi khi tạo session thất bại: CONCURRENT_STREAM_LIMIT (hiển thị thông báo quota, KHÔNG tự động retry), PLAYBACK_MODE_UNSUPPORTED (hiển thị "nguồn chưa hỗ trợ phát"), IDEMPOTENCY_KEY_REUSED (sinh key mới, cho phép người dùng bấm lại thủ công).
10. PlayerManager dùng MediaHttpClient RIÊNG không bearer/authenticator nhưng chia sẻ CookieJar với BackendApiClient. Tuyệt đối không đưa authenticated client vào Media3.
11. Với source có sourceType="third_party": không lưu playbackUrl vào bất kỳ đâu ngoài biến local trong ViewModel (không log, không SavedStateHandle, không Room) — nếu Activity bị kill và restore, PHẢI gọi lại CreatePlaybackSessionUseCase để lấy URL mới thay vì khôi phục URL cũ.
12. Với sourceType="owned": lập lịch RenewMediaAuthUseCase trước `mediaAuth.expiresAt`; response `MediaAuth` thành công cung cấp expiry kế tiếp và shared CookieJar nhận Set-Cookie mới. Không renew cho third_party. Viết integration test xác nhận cookie host-only Gateway trả có path asset/generation, được CookieJar nhận và được gửi cho master, variant, segment của playbackUrl khi Gateway/media-edge dùng cùng hostname.

Viết unit test cho: seq tăng đúng; pause/buffering không tăng playedSeconds; qualified đúng 1 lần; terminal idempotent và progress hoàn tất trước stopped/failed; heartbeat reject dừng phát; renew chỉ chạy cho owned và được hủy khi terminal.

Build, test, báo cáo lỗi.
```

---

## Giai đoạn 12 — feature-search

**Prompt:**
```
Trong module feature-search, triển khai SearchScreen:
1. TextField tìm kiếm, debounce 300ms trước khi gọi SearchMoviesUseCase(q, page=1, pageSize=24, profileId).
2. Kết quả trả Page<Movie> — hiển thị LazyColumn/LazyVerticalGrid, hỗ trợ load thêm khi cuộn gần cuối (dùng page/totalPages trả về để biết còn trang tiếp theo không).
3. Không cần lo về nguồn KKPhim ở màn này — search CHỈ tìm trong Catalog đã import, không có luồng gọi trực tiếp provider bên ngoài từ app.
4. Lưu lịch sử tìm kiếm local (DataStore, tối đa 10 mục) hiển thị khi ô tìm kiếm trống.
5. Thêm Browse/filter dùng BrowseMoviesUseCase -> `GET /catalog/movies`, hỗ trợ đầy đủ genre, country, year, type, contentKind, sourceType, provider, sort và paging. Không gửi các filter này sang `/catalog/search`. Khi có profile đang chọn, truyền profileId + bearer để backend áp dụng kids filter.

Build, test, báo cáo lỗi.
```

---

## Giai đoạn 13 — feature-profile

**Prompt:**
```
Trong module feature-profile, triển khai:

1. ProfileListScreen: danh sách hồ sơ (GetProfilesUseCase), nút "Thêm hồ sơ" ẩn khi đủ 5. Thêm ProfileCreateScreen, ProfileEditScreen và xác nhận xóa, lần lượt gọi CreateProfileUseCase, UpdateProfileUseCase, DeleteProfileUseCase với đúng `{name, avatarId?, isKids?}`/PATCH field thay đổi. Không chỉ làm màn danh sách vì backend hỗ trợ CRUD đầy đủ.
2. Chuyển hồ sơ active qua `CurrentProfileStore` được inject; hủy request/cache cá nhân cũ. Nếu xóa profile đang active, clear selection và điều hướng về chọn profile.
3. FavoritesScreen: GetFavoritesUseCase(profileId) trả `{ items: Movie[] }` — hiển thị dạng lưới poster.
4. WatchHistoryScreen: GetWatchHistoryUseCase(profileId) đã lọc tombstone=true; bấm item mở Player bằng IDs trong HistoryItem.
5. SettingsScreen: chỉ logout và thông tin từ GET /auth/session; không tự dựng API/cài đặt ngôn ngữ, chất lượng mặc định hay quản lý thiết bị.

Build, test, báo cáo lỗi.
```

---

## Giai đoạn 14 — feature-subscription (mock, có polling)

**Prompt:**
```
Trong module feature-subscription, triển khai theo đúng bản chất "mock" của payment trong contract:

1. PlansScreen: GetPlansUseCase() hiển thị { items: Plan[] } dạng card, mỗi card có price/currency/durationDays/maxConcurrentStreams/maxResolution.
2. Chọn gói + phương thức thanh toán (card/wallet/bank_transfer — đúng 3 giá trị enum), bấm "Đăng ký": sinh Idempotency-Key, gọi SubscribeUseCase(planId, paymentMethod, idempotencyKey).
3. Sau khi subscribe thành công, dùng `PaymentOrder.paymentExpiresAt` và `status` để chuyển sang AwaitingPayment — hiển thị "Đang xử lý thanh toán", rồi polling GetCurrentSubscriptionUseCase() mỗi 5 giây, tối đa 24 lần. Chỉ chuyển thành công khi `CurrentSubscription.status == "active"`; `null` nghĩa là không còn gói pending/active. Hết thời gian thì hiển thị "Thanh toán đang được xử lý, vui lòng kiểm tra lại sau".
4. TUYỆT ĐỐI KHÔNG tự gọi /payments/webhook/mock từ app hoặc tự set trạng thái active ở phía client — đây là hành vi giả lập sai lệch với contract thật, chỉ hợp lý trong 1 màn hình debug riêng biệt rõ ràng đánh dấu "DEV ONLY" nếu cần test nhanh trong lúc phát triển.

Build, test, báo cáo lỗi.
```

---

## Giai đoạn 15 — Tích hợp & điều hướng

**Prompt:**
```
Trong module app, dùng Navigation Compose nối toàn bộ feature:

1. Destinations: Splash, Login, Register, PublicHome, ProfileList, ProfileCreate, ProfileEdit(profileId), Home, SearchBrowse, Profile, Favorites, WatchHistory, MovieDetail(movieId), Player(movieId, playableId, sourceItemId, profileId), Plans, SubscriptionStatus.
2. NavGraph: người chưa đăng nhập có thể vào PublicHome; thao tác protected chuyển Login. Sau restore/login thành công, gọi GET /profiles: nếu rỗng vào ProfileCreate, nếu có vào ProfileList để chọn active profile, rồi mới vào Home cá nhân hóa. Không vừa bắt buộc chọn profile vừa cho bỏ qua vào Home public trong cùng nhánh authenticated.
3. Bottom navigation 4 tab (Trang chủ, Tìm kiếm, Yêu thích, Hồ sơ) — Player mở full-screen không hiện bottom bar.
4. Cung cấp `CurrentProfileStore` dưới dạng interface được inject; implementation có scope app/data và expose StateFlow read-only. Không đặt mutable global SharedFlow trong core-common/feature. Home/Detail/Favorites/WatchHistory cùng đọc nguồn này.

Kiểm tra build và điều hướng chạy được từ Login -> chọn hồ sơ -> Home cá nhân hóa -> Detail -> Player -> quay lại đúng bằng nút back.
```

---

## Giai đoạn 16 — Kiểm thử & rà soát cuối

**Prompt:**
```
Viết unit test (JUnit + Turbine + MockK) cho các phần rủi ro cao nhất theo contract thật:

1. TokenAuthenticator: 3 request 401 cùng token cũ chỉ refresh 1 lần; request chờ lock dùng token mới; `/auth/refresh` 401 không đệ quy; priorResponse chặn loop.
2. ToggleFavoriteUseCase: gọi đúng PUT khi thêm, đúng DELETE khi bỏ, không gửi body ở cả 2 trường hợp.
3. GetWatchHistoryUseCase: lọc đúng item có tombstone=true ra khỏi kết quả trả về UI.
4. PlayerViewModel: seq tăng dần; qualified chỉ 1 lần và không tính pause/buffering; terminal idempotent; final progress hoàn tất trước event; heartbeat reject; media-auth renew owned-only; xử lý đúng 3 mã lỗi CONCURRENT_STREAM_LIMIT/PLAYBACK_MODE_UNSUPPORTED/IDEMPOTENCY_KEY_REUSED.
5. SubscriptionViewModel: polling dừng đúng sau khi active hoặc sau khi hết số lần thử, không tự set active khi chưa nhận được từ server.

Viết UI test (Compose Test) cho luồng: Login -> chọn hồ sơ -> Home -> mở Detail -> chọn tập + server -> Phát -> Player hiển thị đúng dialog resume nếu mock resumeNeedsConfirmation=true.

Rà soát toàn bộ codebase: không lưu playbackUrl third_party; Media3 chỉ dùng qualified MediaHttpClient không auth/authenticator; BackendApiClient và MediaHttpClient dùng cùng CookieJar; RefreshClient trần; không có feature phụ thuộc data/core-network trực tiếp. Chạy integration test cookie domain/path cho owned HLS trước khi đánh dấu luồng này hoàn tất.
```

---

## Ghi chú khi giao việc cho AI agent

- Luôn đính kèm cả `backend_api.md` và `frontend-chi-tiet.md` — nhiều field (`accessTier`, `isKidsSafe`, `contentKind`, `seq` dạng String) dễ bị agent tự "sửa cho hợp lý" theo thói quen REST thông thường nếu không có contract gốc trước mặt.
- Giai đoạn 11 (Player) là phần rủi ro cao nhất — nên yêu cầu agent giải thích lại từng bước trước khi code nếu nghi ngờ agent chưa hiểu đúng vòng đời session.
- Vì contract có thể còn thay đổi (tài liệu ghi rõ một số giới hạn ở mục 13 như "CMS chưa có endpoint polling transcode"), nên hỏi lại backend team trước khi agent tự "chế" thêm field/endpoint không có trong tài liệu.
- Có thể triển khai và nghiệm thu G0–G14 theo contract hiện tại. G11 vẫn cần Android integration test CookieJar với topology cùng hostname; G14 chỉ là payment mock, không được diễn giải là tích hợp cổng thanh toán production.
