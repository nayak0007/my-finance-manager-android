# axio-Style UI Redesign Specification
## My Finance Manager (Android) - Jetpack Compose

**Reference app:** `axio: Income & Expense Tracker` (Axio Digital Pvt. Ltd., formerly Walnut Money Manager / Capital Float)
**Reference package:** `com.daamitt.walnut.app` (Google Play, 10M+ downloads)
**Target app:** `com.myfinancemanager.app` (Kotlin, Jetpack Compose, Material 3)
**Spec version:** 1.0
**Status:** Ready for implementation
**Color values:** sampled directly from axio marketing + Play Store screenshots (see [Appendix A](#appendix-a--sampling-method))

---

## 1. Design System Identification

axio does **not** use stock Material 3. It is a **custom, dark-first design system** that borrows Material 3's *structural* primitives (bottom nav, FAB-less card feeds, bottom sheets) but replaces the *visual* layer almost entirely:

| Layer | axio's choice | Material 3 default | Action for us |
| --- | --- | --- | --- |
| Base theme | Dark-only canvas `#181818` with light content cards `#EBEBEB` | Light-first, tonal surfaces | Invert default; dark is the brand |
| Primary color | Electric lime `#B4E300` | Purple `#6750A4` | Replace completely |
| Data color | Indigo blue `#4050E0` for rings/bars, lime line overlay | Single tonal primary | Add a dedicated data palette |
| Shape | Large radii: 20-28dp cards, full pill buttons/chips | 12dp / 16dp | Increase radius scale |
| Type | Geometric rounded sans (Circular Std / Gilroy family) | Roboto | Ship a custom font family |
| Depth | Flat surfaces separated by color steps + occasional lime glow | Tonal elevation + shadow | Replace shadows with surface stepping |
| Category/merchant color | A fixed 8-color "brand chip" palette | N/A | Add a category palette |

**One-line thesis:** axio feels like a *premium dark dashboard* - a near-black canvas, one acid-lime brand accent, one indigo data accent, oversized rounded numbers, and white "receipt" cards that pop out of the dark.

---

## 2. Design DNA - What Actually Makes axio Distinctive

These eight traits are the character of the app. If any is dropped, the redesign stops looking like axio.

1. **Dark canvas, white receipt cards.** The shell is `#181818`. The transaction feed and "add" surfaces are light `#EBEBEB`/white cards that jump forward. This inversion is axio's signature and is what your current `#F7FAF8` light theme is missing.
2. **One hero number.** The dashboard is built around a giant donut ring with the month's spend in the center at ~34-40sp, semibold. Not a grid of four equal stat cards.
3. **The tri-stat strip.** Directly under the ring: `Income | Budget | Safe to spend`, separated by 1px vertical dividers, label on top in muted gray, value below in white semibold. Income is masked (`₹***` with an eye toggle).
4. **Acid-lime accent, used sparingly.** Lime `#B4E300` appears in the logo, the active segment indicator, the "positive/under-budget" marker, and one CTA. It is never a large fill behind body text.
5. **Indigo data accent.** All charts default to indigo `#4050E0`: ring arcs, bar columns, category ring strokes. Lime is the *overlay/second* series (e.g. the trend line over the bars).
6. **Rounded-rect vertical bars.** The trend chart is not a line chart - it is fat, fully rounded vertical capsules (radius = half the bar width), with the current month highlighted in a taller/brighter treatment.
7. **Colored circular category glyphs.** Every list row has a 40-44dp filled circular badge in a category color (cyan cart, gray suitcase, green basket, indigo star) with a white glyph. This does more for perceived polish than any other single element.
8. **Tiny circular action buttons.** Rows end with a 36-40dp outlined/filled circle containing a 14-16dp glyph (up-right arrow, refresh, chevron, split). It is a consistent, recognizable axio affordance.

---

## 3. Color System

### 3.1 Brand colors (sampled)

| Token | Hex | Usage |
| --- | --- | --- |
| `AxioLime` | `#B4E300` | Brand accent, active indicators, "positive" markers, selected chips, one CTA |
| `AxioLimeDeep` | `#6EC800` | Gradient end for logo marks / lime-to-green fills |
| `AxioLimeSoft` | `#DFF79A` | Lime at 20% - selected chip backgrounds on light |
| `InkBlack` | `#000000` | Logo glyph on lime; button text on lime |

> Brand gradient (icon / splash): linear 135deg `#B3E301` -> `#6EC800`.
> Website wordmark gradient: `#B1D235` -> `#72BF44`.

### 3.2 Dark theme (default / primary)

| Token | Hex | Role |
| --- | --- | --- |
| `background` | `#181818` | App canvas |
| `surface` | `#1A1A1A` | Default surface (sheets, bars) |
| `surfaceContainer` | `#212121` | Cards, list groups |
| `surfaceContainerHigh` | `#292929` | Pressed/hover, inset tiles, secondary cards |
| `surfaceContainerHighest` | `#333333` | Dividers-on-dark when a fill is needed |
| `cardLight` | `#EBEBEB` | The white "receipt" card (transaction feed, key content) |
| `onCardLight` | `#181818` | Text on the receipt card |
| `onBackground` | `#F5F5F5` | Primary text |
| `onSurfaceVariant` | `#9E9E9E` | Secondary/meta text, labels |
| `onSurfaceMuted` | `#6B6B6B` | Tertiary text, disabled |
| `outline` | `#2F2F2F` | Hairlines, dividers |
| `outlineStrong` | `#3D3D3D` | Chip borders, outlined buttons |

### 3.3 Light theme (derived - the app is dark-first, ship light as secondary)

| Token | Hex | Role |
| --- | --- | --- |
| `background` | `#FFFFFF` | Canvas |
| `surface` | `#F4F5F1` | Page section background |
| `surfaceContainer` | `#FFFFFF` | Cards |
| `surfaceContainerHigh` | `#EDEFE9` | Inset tiles |
| `onBackground` | `#181818` | Primary text |
| `onSurfaceVariant` | `#6B6B6B` | Secondary text |
| `outline` | `#E2E4DE` | Dividers |
| `primary` (lime on light) | `#5A7A00` | Lime is not accessible on white - darken it for text/icons |

**Accessibility rule:** `#B4E300` on white is ~1.7:1 and **fails**. Never use raw lime as text on light surfaces. Use `#5A7A00` (lime-900) for lime text/icons in light mode, or put lime on `#181818` (contrast ~11:1, passes AAA).

### 3.4 Semantic / money colors

| Token | Dark value | Light value | Usage |
| --- | --- | --- | --- |
| `positive` / income | `#7AE04A` | `#1F7A38` | Income amounts, under-budget, success |
| `negative` / expense | `#F06060` | `#C4362E` | Expense/overspend, destructive, over-budget ring |
| `invest` | `#5B7CFA` | `#3A52C8` | Investment amounts |
| `warning` | `#F5A623` | `#B87400` | Approaching budget, due soon |
| `info` | `#00B0D0` | `#00839B` | Neutral info, bills |

> Money amounts are **always** colored by sign/type and use tabular numerals (see 4.3). This is consistent across axio.

### 3.5 Data-visualization palette

Charts use indigo first, then the rest in order. This is the exact sequence to copy:

| Index | Hex | Name |
| --- | --- | --- |
| 1 | `#4050E0` | Indigo (default series) |
| 2 | `#B4E300` | Lime (overlay / second series) |
| 3 | `#00B0D0` | Cyan |
| 4 | `#9050D0` | Purple |
| 5 | `#F06060` | Coral |
| 6 | `#802040` | Berry |
| 7 | `#4CAF50` | Green |
| 8 | `#9E9E9E` | Gray (residual / "other") |

Ring background track on dark: `#2A2A2A`. Ring on light: `#E8E8E8`.

### 3.6 Category / merchant chip palette

The circular category badges. Assign deterministically (hash category name -> index) so a given category keeps its color everywhere:

| Hex | Example mapping |
| --- | --- |
| `#00B0D0` | Shopping / card |
| `#9050D0` | Transport / travel |
| `#7AE04A` | Grocery / food |
| `#4050E0` | Entertainment / subscriptions |
| `#F06060` | Health / dining |
| `#F5A623` | Bills / utilities |
| `#9E9E9E` | Transfer / uncategorized |
| `#B4E300` | Income / salary |

---

## 4. Typography

### 4.1 Font family

axio uses a **geometric rounded sans** in the **Circular Std / Gilroy** family (double-storey `a`, single-storey `g`, straight `y` tail, geometric `o`). This is a licensed font and cannot be redistributed, so ship a close free substitute:

| Priority | Family | Why | Source |
| --- | --- | --- | --- |
| **Primary** | **DM Sans** | Closest free match to Circular Std; double-storey `a`, geometric humanist, excellent tabular figures | Google Fonts |
| Alternative | Plus Jakarta Sans | Rounded geometric, strong fintech numerals, slightly wider | Google Fonts |
| Fallback | Poppins | More geometric but single-storey `a` - only if the above are rejected | Google Fonts |

Do **not** use Roboto for headings if you want the axio feel; Roboto's proportions are the single biggest giveaway that an app is "stock Android". Body/numbers should also use the custom family for consistency, with `FontFeatureSetting("tnum")` for figures.

### 4.2 Type scale

Sizes in `sp`; line-height as a multiple. Weights: Regular 400, Medium 500, SemiBold 600, Bold 700.

| Token | Size / line | Weight | Tracking | Usage |
| --- | --- | --- | --- | --- |
| `heroNumber` | 40 / 1.05 | SemiBold | -1.0sp | The donut center amount, portfolio totals |
| `displaySmall` | 32 / 1.1 | Bold | -0.5sp | Marketing/splash headline |
| `headlineMedium` | 24 / 1.2 | Bold | -0.3sp | Screen titles ("Spent in May") |
| `titleLarge` | 20 / 1.25 | SemiBold | -0.2sp | Card titles, sheet titles |
| `titleMedium` | 16 / 1.3 | SemiBold | 0 | List group headers, section titles |
| `bodyLarge` | 16 / 1.5 | Regular | 0 | Long-form / onboarding body |
| `bodyMedium` | 14 / 1.45 | Regular | 0 | Row secondary lines, descriptions |
| `bodySmall` | 12 / 1.4 | Regular | 0.1sp | Timestamps, merchant meta, notes |
| `labelLarge` | 14 / 1.2 | Medium | 0.2sp | Buttons, chips |
| `labelMedium` | 12 / 1.2 | Medium | 0.3sp | Stat-strip labels ("Income", "Budget") |
| `labelSmall` | 11 / 1.2 | Medium | 0.4sp | All-caps eyebrow ("DUE TODAY", "AUTO DEBIT IN 2 DAYS") |

**Distinctive rules to copy:**
- The hero number is **very large and semibold**, never bold-black - weight 600 max.
- Stat labels are **muted gray, 12sp, regular/medium**, and always sit *above* their value.
- Eyebrow labels over bill cards are **uppercase, 11-12sp, wide tracking, purple `#9050D0`**.
- Amounts in lists are **semibold, right-aligned**, same size as the title (16sp) - not smaller.

### 4.3 Tabular numerals (important)

All money values, dates in columns, and percentages must use tabular figures so digits do not jitter as values change:

```kotlin
val TabularNums = TextStyle(
    fontFeatureSettings = "tnum",
    fontFamily = AppFontFamily
)
```

Apply via a helper composable/`TextStyle` wrapper to every amount, percentage, and the donut center number.

### 4.4 Font resources to implement

Option A - bundled (recommended for brand consistency, no network):

```
app/src/main/res/font/dm_sans_regular.ttf
app/src/main/res/font/dm_sans_medium.ttf
app/src/main/res/font/dm_sans_semibold.ttf
app/src/main/res/font/dm_sans_bold.ttf
```

Option B - downloadable (smaller APK, needs Play Services):

```kotlin
val Provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
val AppFontFamily = FontFamily(
    Font(GoogleFont("DM Sans"), Provider, FontWeight.Normal),
    Font(GoogleFont("DM Sans"), Provider, FontWeight.Medium),
    Font(GoogleFont("DM Sans"), Provider, FontWeight.SemiBold),
    Font(GoogleFont("DM Sans"), Provider, FontWeight.Bold)
)
```

---

## 5. Spacing, Shape, Elevation

### 5.1 Spacing scale (4dp base, 8dp rhythm)

| Token | dp | Usage |
| --- | --- | --- |
| `xs` | 4 | Icon-to-text inside tight chips |
| `sm` | 8 | Chip gaps, inline gaps |
| `md` | 12 | Row inner padding, chip padding |
| `lg` | 16 | **Screen horizontal gutter**, card inner padding |
| `xl` | 20 | Card inner padding (large cards) |
| `2xl` | 24 | Section gap |
| `3xl` | 32 | Gap between major blocks |
| `4xl` | 48 | Empty-state top spacing |

**Screen gutter is 16dp** on phones, 24dp on >=600dp width. Your current code already uses 16dp - keep it.

### 5.2 Radius scale

| Token | dp | Usage |
| --- | --- | --- |
| `radiusXs` | 8 | Small chips, tags |
| `radiusSm` | 12 | Input fields, small tiles |
| `radiusMd` | 16 | Secondary cards |
| `radiusLg` | 20 | **Primary cards**, list group containers |
| `radiusXl` | 28 | Bottom sheets, modals, hero cards |
| `radiusPill` | 100 (Full) | Buttons, filter chips, segmented tabs, search bar |
| `radiusCircle` | 50% | Category badges, icon buttons, ring caps |

**Rule:** cards are 20-24dp; every interactive control (button, chip, tab, search) is a **full pill**. Never mix a 16dp card with a 16dp button - buttons must be pills.

### 5.3 Elevation

Drop shadows almost entirely. Depth is communicated by **surface color steps** (`#181818` -> `#1A1A1A` -> `#212121` -> `#292929`) and by light cards on dark. Where a lift is needed:

- Bottom sheets / modals: `ModalBottomSheetDefaults` + 28dp radius, no shadow, scrim `#000000` at 60%.
- The one exception: axio's marketing uses a **lime glow** (`#B4E300` at 35%, blur 24dp) around the "featured" card. Use it for exactly one element per screen (e.g. the current month card or the primary CTA row) as an accent - never as general elevation.

---

## 6. Component Library

### 6.1 Buttons

| Variant | Spec | Usage |
| --- | --- | --- |
| **Primary (dark)** | Pill, bg `#000000`, text `#FFFFFF` 14sp Medium, height 48dp, padding 24dp horizontal, optional trailing arrow-right in a 20dp circle | "Pay now", "Add", main CTA on light cards |
| **Primary (lime)** | Pill, bg `#B4E300`, text `#181818` 14sp SemiBold, height 48dp | The single onboarding/confirm CTA (use sparingly) |
| **Secondary / outlined** | Pill, transparent bg, 1dp border `#3D3D3D` (dark) / `#DADCD6` (light), text `onBackground` 14sp Medium, height 48dp | "Mark as paid", "Not now" |
| **Tertiary / text** | No container, text 14sp Medium, `onSurfaceVariant`; pressed = `#292929` background with pill radius | Cancel, "See all" |
| **Icon button** | 40dp circle; dark = `#212121` fill or `#3D3D3D` outline; glyph 18-20dp | Row-end arrows, refresh, share |
| **Small pill button** | Height 36dp, horizontal padding 16dp, 13sp Medium | Inline row actions ("Split") |

Pressed feedback: Material ripple with `#FFFFFF` at 12% on dark, `#000000` at 10% on light; plus a 0.97 scale on the container for 80ms. Never shift layout.

**Implementation note:** your current `QuickAddTile` and `Button`s are default Material shapes. Wrap them in a `PillButton` composable so every CTA is consistent.

### 6.2 Text fields / inputs

axio's inputs are **minimal**: label above the field (not floating inside), a filled rounded container, no visible border until focus.

| State | Spec |
| --- | --- |
| Default | Container `#212121` (dark) / `#F1F2EE` (light), radius 12dp, height 56dp, no border; placeholder `#6B6B6B` 15sp |
| Focused | 1.5dp border `#B4E300`, container unchanged (no tint flood) |
| Filled | Text `onBackground` 16sp Medium |
| Error | 1.5dp border `#F06060`, helper text `#F06060` 12sp below |
| Label | `labelMedium` 12sp `onSurfaceVariant`, positioned **above** the field with 6dp gap (not Material's floating label) |
| Amount field | Prefix currency symbol in `onSurfaceVariant`, value 24sp SemiBold, right-aligned; numeric keyboard |

```
Label (12sp, muted)
+--------------------------------------+
|  ₹   1,250.00                        |   <- 56dp, 12dp radius, #212121
+--------------------------------------+
Helper / error (12sp)
```

**Change required:** switch `OutlinedTextField` (Shared.kt:91, :111) to this filled/12dp style. The visible outline box is the biggest "generic Material" tell in the current build.

### 6.3 Cards

| Card | Spec |
| --- | --- |
| **Surface card** | bg `#212121`, radius 20dp, padding 16-20dp, no border, no shadow |
| **Receipt card (light)** | bg `#EBEBEB`, radius 24dp, padding 20dp, dark text; used for the transaction feed and primary content - the "white pops out of dark" signature |
| **Stat tile** | bg `#212121`, radius 16dp, padding 14dp, label 12sp muted on top, value 18sp SemiBold below, colored by type |
| **Bill / due card** | radius 24dp, a soft 135deg gradient from mint `#E9F7E4` to lilac `#E7DDF5` (light-on-dark), 20dp padding; eyebrow purple 11sp caps, title 16sp SemiBold, amount 20sp SemiBold, right-side pill action |
| **Account card** | horizontal split: left 40% a brand color panel (berry `#802040`, sky `#BFE6F5`, rose `#E8C4C8`) with logo + name + masked number; right 60% light `#F4F4F4` with balance and a refresh circle button; radius 20dp, overflow clipped |
| **Featured card** | any card + lime glow accent (one per screen) |

### 6.4 Navigation

**Bottom navigation bar** (5 items max) - axio does not use Material's tonal pill indicator. Instead:

- Container: `#181818`, 1px top hairline `#2F2F2F`, height 64dp + gesture inset, **no** floating pill.
- Item: icon 24dp + label 11sp Medium, stacked, 4dp gap.
- Selected: icon + label `#FFFFFF`; unselected `#9E9E9E`.
- A 3dp lime dot or a short 16dp lime underline sits under the selected item (not a filled pill).
- Use **rounded-outline** icons when unselected and **rounded-filled** when selected (consistent with axio's dual icon rule).

**Top app bar:** transparent, no elevation, `#181818`. Left = avatar (28dp circle) + greeting 16sp SemiBold. Right = search icon and/or notification icon (24dp outline). Title case/sentence case, never all caps.

**Segmented tabs** ("Transactions | Categories | Merchants" / "Trends | Categories"): a pill track `#212121` containing pill segments; selected segment bg `#2E2E2E` with white text + a small colored inline glyph (lime bar-chart, purple pie). Unselected `#9E9E9E`.

### 6.5 Lists & rows

The axio row is the highest-leverage component to copy:

```
[44dp colored circle  ]  Title (16sp Medium, #F5F5F5)          26, May          (12sp muted, right)
[  white glyph        ]  #tag · subtitle (12sp muted)           ₹220            (16sp SemiBold, right)
                                                                        (40dp circle arrow button)
                               optional: [ Split ] small pill
```

Spec:
- Height: 68-76dp, horizontal padding 16dp, vertical 12dp.
- Divider: none. Separate rows by 1px `#2F2F2F` hairline **inset to start at the text column**, or by a `#212121` group card with 20dp radius.
- Leading badge: 44dp circle, category color fill, white glyph 20dp.
- Trailing: amount semibold + a 40dp circular outline button with an up-right arrow (expense), down-left (income), or chevron (detail).
- Income rows show amount in `positive`; expenses in `onBackground` (white) - not red - except when emphasizing; axio keeps most expense amounts white and colors only income. Match that restraint.
- On the **light receipt card** (transaction feed), text is `#181818`, meta `#6B6B6B`, and the trailing circle is `#FFFFFF` on `#EBEBEB`.

### 6.6 Chips

- Filter chip: pill, height 36dp, bg `#212121` unselected / `#B4E300`+`#181818` text selected (dark), 1dp border `#3D3D3D` unselected.
- Stat/category chip with inline glyph: pill, bg `#212121`, glyph 16dp colored, label 14sp `#F5F5F5`.
- Your existing `FilterChip`/`AssistChip` usage (Shared.kt:134, :209) should be re-skinned to these values.

### 6.7 Modals, sheets, dialogs

- **Bottom sheet** for add/edit (axio's primary input surface): `ModalBottomSheet`, radius 28dp top corners, bg `#1A1A1A`, drag handle 40x4dp `#3D3D3D`, 24dp top padding, title 20sp SemiBold, scrim `#000000` 60%, `skipPartiallyExpanded = true` for forms.
- **Dialog** (confirmations only): bg `#212121`, radius 24dp, title 18sp SemiBold, body 14sp `onSurfaceVariant`, buttons are pills - destructive on the right in `negative`.
- Bottom sheets animate in with a 300ms `FastOutSlowInEasing` slide; exit 200ms.

### 6.8 Charts (reuse your Canvas charts, restyle)

| Chart | Spec |
| --- | --- |
| **Donut ring (hero)** | Stroke width 16-20dp, rounded caps, track `#2A2A2A`, progress `#4050E0`; a 10dp lime dot marks the progress tip; center shows a small 28dp circle `#212121` containing an up-right arrow, then the hero number, then a lime `%` pill (`#B4E300` at 15% bg, lime text). Animate sweep 0->target over 900ms with `FastOutSlowInEasing`. |
| **Trend bars** | Vertical capsules, width 12-16dp, radius = width/2, color `#4050E0`, selected month taller and `#5B7CFA`; 6-12 columns; baseline labels 12sp muted, month abbreviations. |
| **Trend overlay** | Lime polyline `#B4E300` 2dp stroke over the bars with 6dp lime dots at each point. |
| **Category ring (list)** | 36dp ring per row, 4dp stroke, rounded cap, color = category color; red `#F06060` when over budget; percentage centered 10sp. |
| **Sparkline** | 2dp line, no fill or a 10% gradient fill of the line color. |

Your `PieChart` (Shared.kt:163) should become a **donut** (add an inner cutout via `Stroke` instead of `useCenter=true`) to match axio, and `LineChart` (:182) should gain the bar+line combo.

### 6.9 Empty states

Centered, 96dp top gap: a 64dp circle `#212121` with a 28dp outline glyph (muted), title 16sp SemiBold, body 14sp `#9E9E9E` max width 280dp, then an optional outlined pill CTA. Never a bare gray sentence.

### 6.10 Feedback

- Snackbar: bg `#292929`, radius 12dp, text 14sp `#F5F5F5`, action in lime `#B4E300`, 4s duration, anchored above the bottom bar.
- Loading: skeletons (`#212121` base + a `#292929`->`#333333` shimmer) for lists; a lime `CircularProgressIndicator` only for full-screen initial load.
- Success: brief lime check confirm + optional haptic (`HapticFeedbackType.LongPress` or `CONFIRM`).

---

## 7. Iconography

**Dual system - this is deliberate:**

1. **UI / chrome icons = rounded outline**, 24dp default (20dp dense, 28dp feature), stroke ~2dp with rounded caps/joins. Use **Material Symbols Rounded** (`Outlined` weight 400 with `Rounded` style), or Lucide/Phosphor for a softer feel. Icons: search, filter, bell, back arrow, chevron, settings, plus, arrow-up-right, refresh, eye.
2. **Category / merchant icons = filled glyph inside a colored circle**, white glyph on a category-colored 40-44dp badge. These are the colorful "chip" glyphs (cart, suitcase, basket, star, fork-knife, plane, bolt, heart).

Rules:
- **No emoji as icons, ever.** (This also aligns with the repo's `no-emoji` rule.)
- One family only; do not mix filled and outline at the same hierarchy level (outline = chrome, filled = category badges).
- The arrow-in-a-circle is the canonical axio action affordance: a 36-40dp circle, 1dp border (or `#FFFFFF` fill on light cards), 16dp `arrow_outward`/`arrow_forward` glyph.
- Sizes are tokens: `iconSm=16`, `iconMd=20`, `iconLg=24`, `iconXl=28`, `badgeMd=40`, `badgeLg=44`.

Your current nav icons (MainActivity.kt:180) are arbitrary Material defaults (`Star` for Income, `ShoppingCart` for Spend, `Info` for Insights). Replace with a coherent set: Home=`home`, Income=`payments`/`south_west`, Spend=`shopping_bag`/`receipt_long`, Invest=`trending_up`, Insights=`auto_awesome`.

---

## 8. Motion & Micro-interactions

| Interaction | Spec |
| --- | --- |
| Ring fill | Sweep 0 -> target, 900ms `FastOutSlowInEasing`; count-up the center number in sync (600-900ms) |
| Number count-up | Animate `Double` with `animateDoubleAsState`; 700ms; tabular nums prevent jitter |
| Card press | Scale 0.97 for 80ms in, restore 120ms out; ripple on top; never move layout |
| Row press | Ripple + container -> `#292929` for 120ms |
| Screen transition | Forward: slide-in from right 300ms + fade; back: slide-out 240ms. Or a shared-element transition on the hero number/card |
| Bottom sheet | Slide up 300ms `FastOutSlowInEasing`; dismiss swipe-down; scrim fades 200ms |
| Segmented tab change | Selected pill moves with a 250ms spring (`spring(dampingRatio=0.8)`) + crossfade of content |
| Chart entrance | Bars grow from baseline, staggered 40ms per bar, 500ms |
| Masked income reveal | Eye toggle crossfades `₹***` -> value 200ms |
| Budget threshold | When crossing 80%, warning color; 100%, negative color + a subtle single shake (2dp, 3 cycles) once |
| List item entrance | Fade + 8dp translate-up, staggered 30ms, only on first load (respect reduced motion) |

**Easing tokens:** enter `FastOutSlowInEasing`; exit `FastOutLinearInEasing`; springs for physics-y moves. Always respect `Settings.Global.ANIMATOR_DURATION_SCALE == 0` (reduced motion) by snapping.

Haptic: light tick on category select, confirm tick on save/confirm payment.

---

## 9. Dark Mode Implementation

axio is **dark-first**. Make dark the default regardless of system setting (or default to dark and allow light), and design tokens so both themes share structure.

```kotlin
@Composable
fun MyFinanceTheme(
    darkTheme: Boolean = true,                 // dark-first, not isSystemInDarkTheme()
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AxioDarkColors else AxioLightColors,
        typography = AxioTypography,
        shapes = AxioShapes,
        content = content
    )
}
```

Rules:
- **Do not use `dynamicColorScheme`.** axio's identity depends on fixed brand colors; dynamic color would destroy it. (Your current theme already omits it - keep it that way.)
- Use `#000000`-based scrims at 60% for sheets/modals so the dark canvas stays dark.
- Light cards on dark are intentional - `cardLight` is a **theme token**, not "surfaceVariant".
- Contrast targets: primary text >= 4.5:1, secondary >= 3:1, on **both** themes. Lime text only on dark or darkened to `#5A7A00` on light.
- Test the receipt card (`#EBEBEB`) text (`#181818`) = 15.5:1 (passes).

### 9.1 Enable edge-to-edge (already on) and system bars

- Status bar: transparent, icons light on dark (`isAppearanceLightStatusBars = false`), navigation bar transparent with light icons.
- Reserve insets: bottom bar height 64dp + `navigationBarsPadding()`; lists get `contentPadding` bottom = bar height + 16dp so nothing hides behind it.

---

## 10. Jetpack Compose Implementation

Create `ui/theme/Color.kt`, `Type.kt`, `Shape.kt` and rewrite `Theme.kt`. Drop the current ad-hoc `IncomeGreen`/`ExpenseRed`/`InvestBlue` top-level constants (Theme.kt:10-15) in favour of the semantic tokens below.

### 10.1 Color.kt

```kotlin
package com.myfinancemanager.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand
val AxioLime = Color(0xFFB4E300)
val AxioLimeDeep = Color(0xFF6EC800)
val AxioLimeDark = Color(0xFF5A7A00)   // lime for light-mode text/icons
val InkBlack = Color(0xFF000000)

// Dark neutrals
val Ink900 = Color(0xFF181818)  // background
val Ink850 = Color(0xFF1A1A1A)  // surface
val Ink800 = Color(0xFF212121)  // surfaceContainer
val Ink700 = Color(0xFF292929)  // surfaceContainerHigh
val Ink600 = Color(0xFF333333)  // surfaceContainerHighest
val InkOutline = Color(0xFF2F2F2F)
val InkOutlineStrong = Color(0xFF3D3D3D)
val CardLight = Color(0xFFEBEBEB)
val TextPrimaryDark = Color(0xFFF5F5F5)
val TextSecondaryDark = Color(0xFF9E9E9E)
val TextMutedDark = Color(0xFF6B6B6B)

// Light neutrals
val Paper = Color(0xFFFFFFFF)
val PaperSurface = Color(0xFFF4F5F1)
val PaperContainerHigh = Color(0xFFEDEFE9)
val TextPrimaryLight = Color(0xFF181818)
val TextSecondaryLight = Color(0xFF6B6B6B)
val PaperOutline = Color(0xFFE2E4DE)

// Semantic
val MoneyPositiveDark = Color(0xFF7AE04A)
val MoneyNegativeDark = Color(0xFFF06060)
val MoneyInvestDark = Color(0xFF5B7CFA)
val MoneyWarning = Color(0xFFF5A623)
val MoneyInfoDark = Color(0xFF00B0D0)

val MoneyPositiveLight = Color(0xFF1F7A38)
val MoneyNegativeLight = Color(0xFFC4362E)
val MoneyInvestLight = Color(0xFF3A52C8)
val MoneyInfoLight = Color(0xFF00839B)

// Data-viz sequence (index 0 is the default series)
val ChartPalette = listOf(
    Color(0xFF4050E0), // indigo
    Color(0xFFB4E300), // lime
    Color(0xFF00B0D0), // cyan
    Color(0xFF9050D0), // purple
    Color(0xFFF06060), // coral
    Color(0xFF802040), // berry
    Color(0xFF4CAF50), // green
    Color(0xFF9E9E9E)  // gray
)

// Category badge palette (deterministic by category name hash)
val CategoryPalette = listOf(
    Color(0xFF00B0D0), Color(0xFF9050D0), Color(0xFF7AE04A), Color(0xFF4050E0),
    Color(0xFFF06060), Color(0xFFF5A623), Color(0xFF9E9E9E), Color(0xFFB4E300)
)
```

### 10.2 Theme.kt color schemes

```kotlin
private val AxioDarkColors = darkColorScheme(
    primary = AxioLime,
    onPrimary = Ink900,
    primaryContainer = Color(0xFF2E3A00),
    onPrimaryContainer = AxioLime,
    secondary = MoneyInvestDark,
    onSecondary = Color.White,
    tertiary = MoneyInfoDark,
    background = Ink900,
    onBackground = TextPrimaryDark,
    surface = Ink850,
    onSurface = TextPrimaryDark,
    surfaceVariant = Ink800,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = Ink800,
    surfaceContainerHigh = Ink700,
    outline = InkOutline,
    outlineVariant = InkOutlineStrong,
    error = MoneyNegativeDark,
    onError = Ink900
)

private val AxioLightColors = lightColorScheme(
    primary = AxioLimeDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF6C8),
    onPrimaryContainer = Color(0xFF33420A),
    background = Paper,
    onBackground = TextPrimaryLight,
    surface = PaperSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = PaperContainerHigh,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = Paper,
    surfaceContainerHigh = PaperContainerHigh,
    outline = PaperOutline,
    outlineVariant = PaperOutline,
    error = MoneyNegativeLight,
    onError = Color.White
)
```

Read semantic money colors through a small composition-local so components do not branch on `darkTheme`:

```kotlin
data class MoneyColors(
    val positive: Color, val negative: Color, val invest: Color,
    val warning: Color, val info: Color
)

val LocalMoneyColors = staticCompositionLocalOf {
    MoneyColors(MoneyPositiveDark, MoneyNegativeDark, MoneyInvestDark, MoneyWarning, MoneyInfoDark)
}
```

### 10.3 Type.kt

```kotlin
private val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold)
)

private val base = Typography()
val AxioTypography = Typography(
    displaySmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 35.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 29.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.4.sp)
)
```

Add a dedicated hero style since Material's scale tops out at displayLarge:

```kotlin
val HeroNumber = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
    fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1).sp,
    fontFeatureSettings = "tnum"
)
val TabularAmount = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp, fontFeatureSettings = "tnum"
)
```

### 10.4 Shape.kt

```kotlin
val AxioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
val PillShape = RoundedCornerShape(percent = 50)
```

### 10.5 Example: restyled hero ring card

```kotlin
@Composable
fun HeroSpendCard(spent: Double, budget: Double, currency: String, monthLabel: String) {
    val progress = (spent / budget).toFloat().coerceIn(0f, 1f)
    val animated by animateFloatAsState(progress, tween(900, easing = FastOutSlowInEasing))
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Spent in $monthLabel", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(220.dp)) {
                val stroke = 18.dp.toPx()
                drawArc(Color(0xFF2A2A2A), -90f, 360f, false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(ChartPalette[0], -90f, animated * 360f, false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowOutward, null, Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(8.dp))
                Text(Money.format(spent, currency), style = HeroNumber,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.background(AxioLime.copy(alpha = 0.15f), PillShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp)) {
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium,
                        color = AxioLime)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatColumn("Income", "₹***", Modifier.weight(1f), showEye = true)
            VerticalDivider(Modifier.height(28.dp), color = MaterialTheme.colorScheme.outline)
            StatColumn("Budget", Money.format(budget, currency), Modifier.weight(1f))
            VerticalDivider(Modifier.height(28.dp), color = MaterialTheme.colorScheme.outline)
            StatColumn("Safe to spend", Money.format((budget - spent) / 30, currency) + "/day",
                Modifier.weight(1f))
        }
    }
}
```

### 10.6 Dependency / config changes

- Add `implementation("androidx.compose.material:material-icons-extended")` if you need the rounded icon set, or ship Material Symbols Rounded as drawables to keep the APK lean.
- Add the four DM Sans `.ttf` files under `res/font/` (or configure the downloadable-font provider).
- Keep `enableEdgeToEdge()` (MainActivity.kt:80) and update `themes.xml` to a `Theme.Material3.DayNight.NoActionBar` base with transparent bars, or continue using the Compose theme and set `WindowCompat` bars in code.
- No new architecture is required - this is a pure theming + component restyle of the existing Compose UI.

---

## 11. Before / After and Redesign Priorities

### 11.1 axio's defining screens (the character references)

| # | axio screen | What it defines | Our closest screen |
| --- | --- | --- | --- |
| 1 | **Money Manager home** | Dark canvas, hero donut, tri-stat strip, Trends/Categories pills, white receipt feed with colored badges | `DashboardScreen.kt` |
| 2 | **Categories breakdown** | Ring + per-row colored badge / amount / mini ring, over-budget in coral | `RecordScreens.kt` ExpenseList |
| 3 | **Bill / due cards** | Gradient cards, purple eyebrow caps, pill "Pay now"/"Mark as paid" | `MoreScreens.kt` BudgetScreen + queue |
| 4 | **Trends** | Rounded indigo bars + lime overlay line, filter chip | `DashboardScreen.kt` charts |
| 5 | **Accounts & Deposits** | Split color-panel account cards, berry/magenta credit cards | `RecordScreens.kt` InvestmentList |
| 6 | **Add transaction** (implicit via "+ Add") | Black pill CTA, minimal filled fields, category badge picker | `RecordScreens.kt` Add* |

### 11.2 Priority roadmap for My Finance Manager

**P0 - Establish the identity (do first, ~1 sprint)**
1. `ui/theme/` - implement `Color.kt`, `Type.kt`, `Shape.kt`, dark-first `Theme.kt`, ship DM Sans. Everything else inherits this.
2. `ui/components/Shared.kt` - restyle `SummaryCard`, `MoneyField`, `EnumDropdown`, `FilterRow`, `EmptyState`, `SectionTitle`; add `PillButton`, `CategoryBadge`, `CircleIconButton`, `StatTile`, `ReceiptCard`, `FilledTextField`. This single file unblocks every screen.
3. `MainActivity.kt:165-196` - restyle the bottom nav (dark container, lime indicator, coherent icon set) and scaffold background.

**P1 - Hero screens (highest visual payoff)**
4. `DashboardScreen.kt` - rebuild as: greeting top bar -> hero donut + tri-stat strip -> Trends/Categories segmented pills -> white receipt feed with colored badges. Replace the 2x2 `SummaryCard` grid and the two separate `LineChart`s.
5. `RecordScreens.kt` Expense/Income lists (`:68`, `:120`) - convert to grouped rows with category badges and trailing circle buttons; move add/edit into a bottom sheet with a black pill CTA.

**P2 - Supporting screens**
6. `MoreScreens.kt` BudgetScreen (`:383`) - per-category rings (4dp, coral when over) + gradient budget cards.
7. `RecordScreens.kt` `Add*` screens (`:224`, `:248`, `:274`) - filled 12dp fields, amount field as a 24sp hero input, category badge selector, sticky black pill save.
8. `MoreScreens.kt` InsightsScreen (`:224`) - card feed with icon bubbles and a lime "Refresh" text button.
9. `MoreScreens.kt` QueueScreen (`:186`) / ImportScreen (`:82`) - review list in the row style, confirm actions as pills.

**P3 - Chrome / auth**
10. `AuthScreens.kt` Login/Signup/Onboarding - lime-to-green gradient logo mark, 40sp headline, filled fields, one lime CTA.
11. `MoreScreens.kt` Settings (`:262`), SenderScreen (`:424`) - grouped `#212121` cards with 20dp radius.

### 11.3 Quick before/after summary

| Element | Before (current) | After (axio-style) |
| --- | --- | --- |
| Canvas | `#F7FAF8` light mint, light-first | `#181818` dark-first |
| Accent | Forest green `#0F6E56` | Lime `#B4E300` + indigo `#4050E0` |
| KPI display | 4 equal `SummaryCard`s in a 2x2 grid | 1 hero donut + tri-stat strip |
| Money colors | `IncomeGreen`/`ExpenseRed`/`InvestBlue` literals | Semantic `positive`/`negative`/`invest` tokens, tabular nums |
| Type | System Roboto (Material defaults) | DM Sans custom family + 40sp hero number |
| Inputs | `OutlinedTextField` boxes | Filled `#212121`, 12dp, label above, lime focus |
| Buttons/chips | Material default shapes | Full pills, black/lime/outlined variants |
| Cards | 1dp elevation white/mint | Flat `#212121` + `#EBEBEB` receipt cards, 20-24dp |
| Charts | Solid pie + thin line | Donut ring + rounded indigo bars with lime overlay |
| Icons | Ad-hoc Material filled (`Star`, `ShoppingCart`, `Info`) | Rounded outline chrome + colored filled category badges |
| Nav | Default `NavigationBar` tonal pill | Flat dark bar, lime dot indicator, outline/filled swap |

---

## 12. Pre-Delivery QA Checklist

- [ ] Dark theme is the default and looks correct; light theme derived and tested separately.
- [ ] No raw lime text on white; lime-on-light uses `#5A7A00`.
- [ ] Primary text >= 4.5:1, secondary >= 3:1 in both themes (receipt card passes).
- [ ] All money/percent values use tabular numerals; digits do not shift on update.
- [ ] Every tappable target >= 48dp; icon-only buttons have content descriptions.
- [ ] Buttons and chips are pills; cards are 20-24dp; shapes are not mixed arbitrarily.
- [ ] One lime CTA / one glow accent per screen maximum.
- [ ] No emoji used as icons; icons come from one family per hierarchy level.
- [ ] Bottom nav has <= 5 items, each with icon **and** label, selected state in lime/white.
- [ ] Charts have legends/tooltips, are not color-only (icon or label accompanies), and have an empty state.
- [ ] Dark-mode scrim is 60% black; sheets are 28dp radius and dismissible by swipe.
- [ ] Micro-interactions are 150-300ms, interruptible, and disabled under reduced motion.
- [ ] Lists have bottom content padding so nothing hides behind the nav bar (edge-to-edge insets).
- [ ] Verified on a 375dp-wide phone and in landscape; no horizontal scroll.
- [ ] Empty/loading/error states exist for every list and chart.

---

## Appendix A - Sampling Method

Colors and dimensions in this spec were derived from:
- axio Play Store listing assets (`com.daamitt.walnut.app`), 1080x1920 marketing screens, 15 Jul 2026 build.
- axio.co.in product screenshots (Personal Finance Manager modules).
- The axio launcher icon (512x512) and website logo SVG.
- Pixel sampling (Pillow, dominant + hue-filtered clustering) and direct visual measurement of the UI shown in-frame.

Observed key values: lime `#B4E300`, icon gradient `#B3E301` -> `#6EC800`, dark canvas `#181818`, elevated surfaces `#1A1A1A`/`#212121`/`#292929`, light card `#EBEBEB`, ring/bar indigo `#4050E0`, cyan `#00B0D0`, purple `#9050D0`, coral `#F06060`, berry `#802040`, income greens `#7AE04A` and deep `#207838`, website wordmark gradient `#B1D235` -> `#72BF44`.

## Appendix B - Source Notes

- axio is dark-first; the light theme here is a derived variant, not an officially published axio theme.
- The exact typeface is a licensed geometric rounded sans (Circular Std / Gilroy family). DM Sans is a substitute, not the original; swap in the licensed font if the brand team obtains it.
- Borderline non-cash values (e.g. exact ring stroke widths) are estimated from the rendered screenshots at 1080px width and should be tuned visually on-device.
