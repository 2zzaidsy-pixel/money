# Build Fix Report

## Summary

Fixed 7 compiler errors across 7 files (4 reported + 3 preventive). 
After these fixes, `compileDebugKotlin` should pass cleanly.

---

## Errors Fixed

### 1. `PremiumScreen.kt` — Unresolved reference: `ModernCard`

**Root cause:** Missing import for `ModernCard` composable. File imported `com.paywise.app.ui.theme.*` but not `com.paywise.app.ui.components.ModernCard`.

**Fix:** Added:
```kotlin
import com.paywise.app.ui.components.ModernCard
```

The cascade error "@Composable invocations can only happen from the context of a @Composable function" was a secondary symptom of the unresolved `ModernCard` reference — the compiler couldn't verify the composable context because it couldn't resolve the symbol. It resolves automatically once the import is present.

---

### 2. `ProfileScreen.kt` — Unresolved reference: `Color`

**Root cause:** Missing `import androidx.compose.ui.graphics.Color`. Used `Color.White` on line 210.

**Fix:** Added:
```kotlin
import androidx.compose.ui.graphics.Color
```

---

### 3. `SimulatorScreen.kt` — `ScenarioPresetChip` signature mismatch

**Root cause:** Parameter ordering conflict with trailing lambda syntax. The original signature had `onClick: () -> Unit` as the 4th parameter and `modifier: Modifier = Modifier` as the 5th (last). The trailing lambda `{ ... }` always maps to the **last** parameter. Since `modifier` (Modifier type, not a function) was last, the trailing lambda could not map to it, leaving `onClick` unfilled.

```kotlin
// Broken signature: onClick (4th), modifier (5th/last)
// Trailing lambda tries to fill modifier — type mismatch
// onClick remains unfilled — "No value passed for parameter onClick"

// Call site:
ScenarioPresetChip("...", icon, isSelected, modifier = weight) { onClick }
//                                                         ^^^^^^^^^^^^^^
//                                      trailing lambda can't map to modifier (wrong type)
```

**Fix:** Reordered parameters so `onClick` is the last parameter. This matches standard Compose conventions (lambda/event callbacks are last).

```kotlin
// Fixed signature: modifier (4th, with default), onClick (5th/last)
fun ScenarioPresetChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit       // last — trailing lambda maps here
)
```

The 4 call sites require **no changes** — they already pass `modifier = Modifier.weight(1f)` as a named argument, and the trailing lambda now correctly maps to `onClick`.

---

### 4. `OnboardingScreen.kt` — Experimental Foundation API usage

**Root cause:** Uses `androidx.compose.foundation.pager.HorizontalPager` and `rememberPagerState` which are annotated with `@ExperimentalFoundationApi`. The composable lacked the required opt-in annotation.

**Fix:** Added:
```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi

// Changed from:
@OptIn(ExperimentalMaterial3Api::class)
// To:
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
```

---

## Preventive Fixes (same error class, would fail next)

### 5. `AuthScreen.kt` — Missing `Color` import

Used `Color.White` on line 331. Added:
```kotlin
import androidx.compose.ui.graphics.Color
```

### 6. `AddGoalScreen.kt` — Missing `Color` import

Used `Color.White` on line 218. Added:
```kotlin
import androidx.compose.ui.graphics.Color
```

### 7. `AddExpenseScreen.kt` — Missing `Color` import

Used `Color.White` on line 294. Added:
```kotlin
import androidx.compose.ui.graphics.Color
```

---

## File Change Summary

| File | Import Added | API Change |
|------|-------------|------------|
| PremiumScreen.kt | `ModernCard` | — |
| ProfileScreen.kt | `Color` | — |
| SimulatorScreen.kt | — | Reordered `ScenarioPresetChip` params: `modifier` before `onClick` |
| OnboardingScreen.kt | `ExperimentalFoundationApi` | `@OptIn(... ExperimentalFoundationApi::class)` |
| AuthScreen.kt | `Color` | — |
| AddGoalScreen.kt | `Color` | — |
| AddExpenseScreen.kt | `Color` | — |

## Verification

- Full project scan for `Color.White` usage: all 7 files now have the required `import androidx.compose.ui.graphics.Color`
- Full project scan for `ModernCard` usage: all 12 files importing it have the correct import
- `@OptIn` annotations scanned across all screens: no missing experimental API opt-ins
- All imports verified to appear only at top of files (before any declarations)

## Remaining Build Prerequisites

- JDK 17+
- Android SDK (API 34 / build-tools 34.0.0)
- `app/google-services.json`
