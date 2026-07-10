## 1. Project Structure (Layer-First)

```
com.sunflower.pinpic
│
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   └── PlacesApi.kt
│   │   ├── dto/
│   │   │   ├── PlaceDto.kt
│   │   │   └── PlacesResponse.kt
│   │   └── interceptor/
│   │       └── AuthInterceptor.kt
│   │
│   ├── local/
│   │   ├── dao/
│   │   │   └── PlaceDao.kt
│   │   ├── entity/
│   │   │   └── PlaceEntity.kt
│   │   └── AppDatabase.kt
│   │
│   ├── mapper/
│   │   ├── PlaceDtoMapper.kt
│   │   └── PlaceEntityMapper.kt
│   │
│   └── repository/
│       └── PlacesRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   ├── Place.kt
│   │   └── BoundingBox.kt
│   ├── repository/
│   │   └── PlacesRepository.kt          # interface
│   └── usecase/
│       ├── GetNearbyPlacesUseCase.kt
│
├── services/
│   ├── location/
│   │   └── LocationTrackingService.kt
│   ├── notification/
│   │   ├── NotificationHelper.kt
│   │   └── PlacesNotificationService.kt
│   └── sync/
│       └── PlacesSyncService.kt
│
├── worker/
│   ├── PlacesSyncWorker.kt
│   ├── AnnotationCachePruneWorker.kt
│
├── presentation/
│   │
│   │── common/
│   │
│   ├── navigation/
│   │   │
│   │   ├── NavDisplay.kt
│   │   ├── Screens.kt
│   │
│   ├── splash/
│   │   ├── SplashScreen.kt
│   │
│   │
│   ├── onboarding/
│   │   │
│   │   ├── OnboardingScreen.kt
│   │
│   ├── auth/
│   │   │
│   │   ├── login/
│   │   │   ├── LoginScreen.kt
│   │
│   ├── places/
│   │   │
│   │   ├── PlacesScreen.kt
│   │   ├── PlacesViewModel.kt
│   │   ├── PlacesUiState.kt
│   │   ├── PlacesUiEvent.kt
│   │   │
│   │   └── components/
│   │       ├── PlaceMarker.kt
│   │       ├── ClusterMarker.kt
│   │       └── MapControls.kt
│   │
│   └── theme/
│       ├── Color.kt
│       ├── Typography.kt
│       └── Theme.kt
│
├── di/
│   ├── NetworkModule.kt
│   ├── DatabaseModule.kt
│
├── common/
│   ├── Result.kt
│   ├── Constants.kt
│   └── extensions/
│       └── FlowExtensions.kt
│
├── MainActivity.kt
└── MyApplication.kt                      # @HiltAndroidApp
```

---

## 2. Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| DI | Dagger Hilt | Dependency injection across all layers |
| Networking | Retrofit + OkHttp | REST API calls |
| Async state | Kotlin Coroutines + StateFlow | Reactive state exposed from ViewModel to UI |
| One-time events | Channel / SharedFlow | Navigation, snackbars, toasts (non-persisted state) |
| Image loading | Coil | Compose-native image loading |

---

## 3. Naming Conventions

### Files & Classes

| Type | Convention | Example |
|---|---|---|
| ViewModel | `<Feature>ViewModel` | `PlacesViewModel.kt` |
| UI State | `<Feature>UiState` | `PlacesUiState.kt` |
| UI Event | `<Feature>UiEvent` | `PlacesUiEvent.kt` |
| Side Effect | `<Feature>SideEffect` | `PlacesSideEffect.kt` |
| Screen composable | `<Feature>Screen` | `PlacesScreen.kt` |
| Reusable composable | Noun, PascalCase | `PlaceMarker.kt`, `ClusterMarker.kt` |
| UseCase | `<Verb><Noun>UseCase` | `GetNearbyPlacesUseCase.kt` |
| Repository interface | `<Feature>Repository` | `PlacesRepository.kt` |
| Repository impl | `<Feature>RepositoryImpl` | `PlacesRepositoryImpl.kt` |
| DTO (network model) | `<Noun>Dto` | `PlaceDto.kt` |
| Entity (DB model) | `<Noun>Entity` | `PlaceEntity.kt` |
| Domain model | Plain noun, no suffix | `Place.kt` |
| Mapper | `<Source>To<Target>Mapper` or `<Noun>Mapper` | `PlaceDtoMapper.kt` |
| Worker | `<Task>Worker` | `PlacesSyncWorker.kt` |
| Hilt module | `<Concern>Module` | `NetworkModule.kt` |
| API interface | `<Feature>Api` | `PlacesApi.kt` |
| DAO | `<Noun>Dao` | `PlaceDao.kt` |

### Variables & Functions

| Type | Convention | Example |
|---|---|---|
| Private mutable state | `_camelCase` (underscore prefix) | `_uiState`, `_sideEffect` |
| Public exposed state | `camelCase` (no underscore) | `uiState`, `sideEffect` |
| Composable functions | PascalCase (they act as types) | `PlacesScreen()`, `MapControls()` |
| Regular functions | camelCase, verb-first | `loadPlaces()`, `onMarkerClicked()` |
| Event handler params in Composables | `on<Action>` | `onMarkerClick`, `onQueryChange` |
| Boolean variables | `is`/`has`/`should` prefix | `isLoading`, `hasError`, `shouldRetry` |
| Constants | `UPPER_SNAKE_CASE` | `const val DEFAULT_ZOOM_LEVEL = 12f` |
| UseCase invocation | `operator fun invoke(...)` so it's called like a function | `getNearbyPlacesUseCase(bbox)` |

### Package naming
All lowercase, no underscores.

---

## 4. Interaction Flow

### Step-by-step: UI → ViewModel

The UI never touches business logic directly. It only:
1. Collects `uiState` as Compose state
2. Sends `UiEvent`s through a single `onEvent()` function

### Step-by-step: ViewModel → UseCase → Repository

ViewModel receives the event, launches a coroutine in `viewModelScope`, delegates to a UseCase (or Repository directly if there's no business logic to isolate), and updates `_uiState`.

UseCase combines Repository calls and applies business rules — stays pure Kotlin, no Android imports:

Repository interface (domain layer) hides *where* data comes from:

Repository implementation (data layer) does the actual fetch + mapping:

```kotlin
class PlacesRepositoryImpl @Inject constructor(
    private val api: PlacesApi,
    private val dao: PlaceDao,
    private val dispatcher: CoroutineDispatcher
) : PlacesRepository {
    override suspend fun getNearbyPlaces(bbox: BoundingBox): Result<List<Place>> =
        withContext(dispatcher) {
            runCatching { api.getPlaces(bbox.toQuery()).toDomain() }
        }
}
```

### Rules of thumb

- **UI → ViewModel:** always via `UiEvent`, never direct method calls with many params scattered across the Composable.
- **ViewModel → UseCase:** skip UseCase only when the operation is a single unmodified Repository call with no business logic.
- **UseCase → Repository:** UseCase depends on the Repository *interface*, never the impl — enables easy testing with fakes/mocks.
- **State flows one way up:** ViewModel → UI via `StateFlow`. UI never mutates state directly, only triggers events.
- **One-time actions (navigation, snackbar) never live in `UiState`** — use a `Channel`/`SharedFlow`-based `SideEffect` instead, to avoid re-triggering on recomposition/rotation.