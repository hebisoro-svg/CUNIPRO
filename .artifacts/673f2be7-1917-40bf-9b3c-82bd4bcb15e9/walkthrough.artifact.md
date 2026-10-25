# Female Breeder Categories ("Primipare" & "Multipare") Walkthrough

Added dynamic support for female-specific breeder categories (**"Primipare"** and **"Multipare"**) in the rabbit creation and edition form.

## Key Changes Made

### 1. String Resources
- **[strings.xml](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/res/values/strings.xml)** & **[strings.xml (en)](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/res/values-en/strings.xml)**:
  Added string keys `cat_primiparous` ("Primipare" / "Primiparous") and `cat_multiparous` ("Multipare" / "Multiparous").

### 2. Layout & Form Logic
- **[fragment_add_edit_animal.xml](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/res/layout/fragment_add_edit_animal.xml)**:
  Added `chipPrimipare` and `chipMultipare` chips inside `chipGroupCategory`.
- **[AddEditAnimalFragment.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/ui/animals/AddEditAnimalFragment.kt)**:
  - Dynamically displays `chipPrimipare` and `chipMultipare` when sex is `"F"` (Femelle) and hides them when sex is `"M"` (Mâle).
  - Handles saving and loading `"PRIMIPARE"` and `"MULTIPARE"` categories.

### 3. Display, Badge Formatting & Herd Statistics
- **[AnimalAdapter.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/ui/adapter/AnimalAdapter.kt)** & **[AnimalDetailFragment.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/ui/animals/AnimalDetailFragment.kt)**:
  Formatted badges and detail cards for `"PRIMIPARE"` and `"MULTIPARE"`.
- **[AnimalsFragment.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/ui/animals/AnimalsFragment.kt)** & **[AnimalDao.kt](file:///C:/Users/DELL VOSTRO/AndroidStudioProjects/CUNIPRO/app/src/main/java/com/example/cunipro/data/local/dao/AnimalDao.kt)**:
  Updated breeder counts and filtering so `"PRIMIPARE"` and `"MULTIPARE"` are counted as breeders.

## Verification Results

### Automated Build Check
- Executed `gradle_build("app:assembleDebug")`: **SUCCESS**. No compilation errors.
