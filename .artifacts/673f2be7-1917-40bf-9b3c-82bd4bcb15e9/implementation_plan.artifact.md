# Implementation Plan - Dynamic Female Breeder Categories ("Primipare" & "Multipare")

Add two female-specific breeder category chips (`"Primipare"` and `"Multipare"`) in the Add/Edit Rabbit form when `"Femelle"` is selected.

## User Review Required

> [!IMPORTANT]
> - `"Primipare"` (first-time mother doe) and `"Multipare"` (experienced mother doe) will dynamically appear in the Category section of the form whenever `"Femelle"` is selected.
> - If `"Mâle"` is selected, these two categories will automatically hide (`View.GONE`), and if one of them was checked, selection will revert to `"Reproducteur"`.
> - Both `"Primipare"` and `"Multipare"` are categorized as breeding stock in counts, lists, badges, and filters throughout the application.

## Proposed Changes

---

### String Resources Component

#### [MODIFY] [strings.xml](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/res/values/strings.xml)
- Add string keys for `cat_primiparous` ("Primipare") and `cat_multiparous` ("Multipare").

#### [MODIFY] [strings.xml (en)](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/res/values-en/strings.xml)
- Add English translations for `cat_primiparous` ("Primiparous") and `cat_multiparous` ("Multiparous").

---

### UI Form Layout Component

#### [MODIFY] [fragment_add_edit_animal.xml](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/res/layout/fragment_add_edit_animal.xml)
- Add `chipPrimipare` and `chipMultipare` chips inside `chipGroupCategory` (default visibility `gone`).

---

### UI Fragment & Adapter Component

#### [MODIFY] [AddEditAnimalFragment.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/ui/animals/AddEditAnimalFragment.kt)
- Update `updateSexSelection(sex)`: Show `chipPrimipare` and `chipMultipare` when sex is `"F"`, and hide them when sex is `"M"`.
- Update `observeAnimalToEdit()` and `setupSaveButton()` to handle `"PRIMIPARE"` and `"MULTIPARE"` categories.

#### [MODIFY] [AnimalAdapter.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/ui/adapter/AnimalAdapter.kt) & [AnimalDetailFragment.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/ui/animals/AnimalDetailFragment.kt)
- Support `"PRIMIPARE"` and `"MULTIPARE"` badge display and detail screen text formatting.

#### [MODIFY] [AnimalsFragment.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/ui/animals/AnimalsFragment.kt) & [AnimalDao.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/data/local/dao/AnimalDao.kt)
- Ensure breeder counts and filters include `"PRIMIPARE"` and `"MULTIPARE"`.

---

## Verification Plan

### Automated Tests
- Run Gradle assemble debug task to ensure project builds cleanly:
  `gradle_build("app:assembleDebug")`

### Manual Verification
1. Open the "Ajouter un lapin" (Add Rabbit) form.
2. Toggle sex to "Femelle": verify "Primipare" and "Multipare" chips appear.
3. Select "Primipare" or "Multipare" and save the rabbit.
4. Verify the new rabbit displays with the "Primipare" or "Multipare" badge in the list and detail screens.
5. Toggle sex to "Mâle": verify "Primipare" and "Multipare" chips hide, and category reverts to "Reproducteur".
