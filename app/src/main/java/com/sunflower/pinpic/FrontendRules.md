## 0. General rule (apply this to every screen, including future ones)

Before writing a new screen, answer these four questions in order — the answers decide the whole structure. Section 4.2 shows this rule already applied to Map/Album/Chat; use those as worked examples, not as the only allowed screens.

1. **Does this screen need chrome (title, back button, bottom input, FAB) that the nav-level Scaffold doesn't already give it?**
    - No → no local `Scaffold`. Build directly in a `Box`/`Column`.
    - Yes → one local `Scaffold`, only for the `topBar`/`bottomBar`/`fab` slot it actually needs. Never nest a second `Scaffold` inside a screen that's already inside the nav-level one.

2. **Is the content a single top-to-bottom flow, or does it need floating overlays on top of a full-bleed layer (a map, an image, a video)?**
    - Single flow → `Column` root. Long/scrollable → `LazyColumn` / `LazyVerticalGrid`, never `Column` + `verticalScroll`.
    - Full-bleed + overlays → `Box` root. First child fills the box; every overlay after it is positioned with `Modifier.align(...)`, not by adding padding/spacers.

3. **Does every color and text size trace back to `MaterialTheme.colorScheme.*` / `MaterialTheme.typography.*`?**
    - If you're about to write a hex value or a raw `.sp`, stop — add a named token to `Color.kt`/`Typography.kt` first (section 1.1, 1.2). A screen file should contain zero raw color/size literals.

4. **Does the state branching sit at the very root, before any layout?**
    - `when (uiState) { Loading -> ...; Error -> ...; is Content -> ... }` wraps the Scaffold/Box/Column — never buried inside a child composable. This keeps loading/error handling identical across screens.

If a new screen doesn't cleanly fit an existing pattern from section 4.2, don't force it — but do write the new pattern into section 4.2 so the next screen that looks like it can copy it instead of re-deriving these four questions.

---

## 1. Color System

### 1.1 Never hardcode hex values in a screen or component

All color values live in `presentation/theme/Color.kt` and are exposed through `MaterialTheme.colorScheme.*`. A Composable should never write `Color(0xFFD85A30)` inline — that's how light/dark mode drifts out of sync. If a new color is needed, add it to `Color.kt` first, name it by semantic role, then consume it from the theme.

### 1.2 Which role to use where

| Role | Use for | Never use for |
|---|---|---|
| `primary` | Photo pins on map, camera FAB, primary buttons, active nav icon | Body text, backgrounds |
| `onPrimary` | Text/icons sitting on a `primary` fill | — |
| `secondary` | Map route lines, chat avatar fills, links, selected chip | Error states |
| `background` | Root `Scaffold`/screen background | Cards (use `surface`) |
| `surface` | Cards, bottom sheets, dialogs, TopAppBar/BottomBar containers | Full-screen backgrounds |
| `onBackground` / `onSurface` | Primary text on that surface | — |
| `onSurfaceVariant` | Secondary/muted text, timestamps, captions | Primary headings |
| `surfaceVariant` | Hairline dividers, unselected borders, disabled chip bg | Emphasis borders |
| `error` (standard Material red, add if needed) | Delete confirmations, failed upload state | Anything decorative |

Rule of thumb: **primary = something the user tapped or created (a pin, a photo, a send action). secondary = orientation and connection (map lines, chat, links).**

---

## 2. Typography implementation

Same rule as color: no inline `fontSize = 18.sp` scattered across screens. Define the scale once in `Typography.kt` and reference `MaterialTheme.typography.*`.

If a screen seems to need a one-off size, that's a signal to add a named token to `Typography.kt` instead — never a magic `.sp` value in the Composable.

---

## 3. Scaffold usage


`Scaffold` with `bottomBar` lives **once**, at the `NavDisplay`/nav-graph level — not repeated inside `MapScreen`, `AlbumScreen`, `ChatScreen`. Each screen composable only provides the *content* that goes inside that shared Scaffold's padding. Nesting a second Scaffold inside a screen that's already inside the nav-level Scaffold causes double padding and inconsistent insets.

 A screen gets its own local Scaffold only if it needs a TopAppBar or FAB that the nav-level one doesn't provide

---

## 4. Screen structure & component hierarchy

Every screen composable follows the same shape, top to bottom, so any screen is readable the same way:

```
<Feature>Screen(viewModel)                 // 1. stateless entry, collects uiState
 └─ when(uiState) { Loading / Error / Content }   // 2. state branch, always at the root
     └─ Root container                     // 3. Box (overlay-heavy) or Column (linear) — see 4.1
         └─ Structural children in reading order   // 4. top→bottom, back→front
```

### 4.1 Root container choice

- **`Box` as root** when content needs overlays (map with floating search bar + FAB, photo viewer with overlaid controls). First child = the thing that fills the screen; later children = overlays, positioned with `Modifier.align`.
- **`Column` as root** when content is a single top-to-bottom flow (album grid under a title, chat thread under a header). Wrap scrollable regions in `LazyColumn`/`LazyVerticalGrid`, never a `Column` + `verticalScroll` for long lists.

### 4.3 Rule of thumb

If two screens solve the same layout problem (a scrollable grid, a full-bleed map, a message list) they must use the *same* root container pattern and slot order. A reviewer should be able to open any screen file and immediately know where the Scaffold, the root container, and the overlay layer are, without reading the whole file.