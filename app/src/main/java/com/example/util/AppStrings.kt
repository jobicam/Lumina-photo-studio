package com.example.util

import com.example.model.AppLanguage
import java.util.Locale

object AppStrings {

    fun isKhmer(language: AppLanguage): Boolean {
        return when (language) {
            AppLanguage.KHMER -> true
            AppLanguage.ENGLISH -> false
            AppLanguage.SYSTEM -> Locale.getDefault().language.equals("km", ignoreCase = true)
        }
    }

    // --- Header & Navigation ---
    fun appName(lang: AppLanguage) = "Lumina Photo"
    fun appSubtitle(lang: AppLanguage) = if (isKhmer(lang)) "ស្ទូឌីយោកែរូប & តម្រងពណ៌កម្រិតខ្ពស់" else "Pro Retouching & Filter Studio"
    fun tabStudio(lang: AppLanguage) = if (isKhmer(lang)) "ស្ទូឌីយោ" else "Studio"
    fun tabEditor(lang: AppLanguage) = if (isKhmer(lang)) "កែសម្រួល" else "Editor"
    fun tabSettings(lang: AppLanguage) = if (isKhmer(lang)) "ការកំណត់" else "Settings"
    fun preferences(lang: AppLanguage) = if (isKhmer(lang)) "ចំណូលចិត្ត ការរក្សាទុក & ស្ទូឌីយោ" else "Preferences, Export & Studio"

    // --- Home Screen ---
    fun activeSession(lang: AppLanguage) = if (isKhmer(lang)) "វគ្គកំពុងដំណើរការ" else "ACTIVE SESSION"
    fun continueEditing(lang: AppLanguage) = if (isKhmer(lang)) "បន្តការកែសម្រួល" else "Continue Editing"
    fun createAndEdit(lang: AppLanguage) = if (isKhmer(lang)) "បង្កើត & កែសម្រួល" else "CREATE & EDIT"
    fun cameraCapture(lang: AppLanguage) = if (isKhmer(lang)) "ថតរូបថ្មី" else "Camera Capture"
    fun cameraSubtitle(lang: AppLanguage) = if (isKhmer(lang)) "ថតរូប & កែភ្លាមៗ" else "Snap & Edit"
    fun importPhoto(lang: AppLanguage) = if (isKhmer(lang)) "បញ្ចូលរូបភាព" else "Import Photo"
    fun importSubtitle(lang: AppLanguage) = if (isKhmer(lang)) "ពីវិចិត្រសាល" else "From Gallery"
    fun faceRetouch(lang: AppLanguage) = if (isKhmer(lang)) "កែសម្រួលផ្ទៃមុខ" else "Face Retouch"
    fun faceRetouchSubtitle(lang: AppLanguage) = if (isKhmer(lang)) "ស្បែក កែទម្រង់ & ធ្មេញស" else "Skin, Sculpt & Teeth"
    fun curvesAndFilters(lang: AppLanguage) = if (isKhmer(lang)) "ខ្សែកោង & តម្រង" else "Curves & Filters"
    fun curvesSubtitle(lang: AppLanguage) = if (isKhmer(lang)) "HSL & ម៉ូដពណ៌ ១៥+" else "HSL & 15+ Looks"
    fun trySamplePhotos(lang: AppLanguage) = if (isKhmer(lang)) "សាកល្បងរូបភាពគំរូ" else "TRY SAMPLE PHOTOS"
    fun naturalPortrait(lang: AppLanguage) = if (isKhmer(lang)) "រូបថតបញ្ឈរធម្មជាតិ" else "Natural Portrait"
    fun portraitBadge(lang: AppLanguage) = if (isKhmer(lang)) "មុខ & ស្បែក" else "Face & Skin"
    fun mountainSunset(lang: AppLanguage) = if (isKhmer(lang)) "ថ្ងៃលិចលើភ្នំ" else "Mountain Sunset"
    fun sunsetBadge(lang: AppLanguage) = if (isKhmer(lang)) "ពណ៌ & តម្រង" else "Color & Filter"
    fun studioCapabilities(lang: AppLanguage) = if (isKhmer(lang)) "សមត្ថភាពស្ទូឌីយោ v2.9" else "STUDIO CAPABILITIES v2.9"

    // Feature Banners
    fun skinSmoothBannerTitle(lang: AppLanguage) = if (isKhmer(lang)) "រលោងស្បែក Gaussian & រក្សាភាពលម្អិត" else "Gaussian Mask Skin Smoothing"
    fun skinSmoothBannerDesc(lang: AppLanguage) = if (isKhmer(lang)) "របាំង Gaussian ផ្តោតលើពណ៌ស្បែក ដោយរក្សារន្ធញើស និងគែមមុខឱ្យច្បាស់ធម្មជាតិ។" else "Localized Gaussian blur masks soften skin tones while preserving micro-texture and sharp edges."
    fun updateBannerTitle(lang: AppLanguage) = if (isKhmer(lang)) "ពិនិត្យរកកំណែថ្មី" else "Smart Update Checker"
    fun updateBannerDesc(lang: AppLanguage) = if (isKhmer(lang)) "ពិនិត្យកំណែថ្មី ប្រវត្តិអាប់ដេត និងការផ្ទៀងផ្ទាត់កំណែពី GitHub ដោយផ្ទាល់។" else "Instant version verification, release highlights & built-in GitHub release checks."
    fun cropBannerTitle(lang: AppLanguage) = if (isKhmer(lang)) "កាត់តម្រឹមរូបភាព & សមាមាត្រ" else "Interactive Crop & Aspect Presets"
    fun cropBannerDesc(lang: AppLanguage) = if (isKhmer(lang)) "អូសជ្រុងកាត់តម្រឹមដោយផ្ទាល់ជាមួយសមាមាត្រ 1:1 4:5 16:9 និងក្រឡា 1/3។" else "Drag corners and pan freely on canvas with locked 1:1, 4:5, 16:9 & rule-of-thirds guides."
    fun cameraFilterBannerTitle(lang: AppLanguage) = if (isKhmer(lang)) "មើលតម្រងផ្ទាល់ពេលថតកាមេរ៉ា" else "Live Camera Filter Preview"
    fun cameraFilterBannerDesc(lang: AppLanguage) = if (isKhmer(lang)) "សាកល្បងតម្រង ២០+ ផ្ទាល់លើអេក្រង់កាមេរ៉ាមុនពេលចុចថត និងកែសម្រួលបន្ត។" else "Preview 20+ real-time film and color looks directly in the viewfinder before you snap."
    fun autoEnhanceBannerTitle(lang: AppLanguage) = if (isKhmer(lang)) "កែពន្លឺស្វ័យប្រវត្តិ (Auto Enhance)" else "1-Tap Smart Auto Enhance"
    fun autoEnhanceBannerDesc(lang: AppLanguage) = if (isKhmer(lang)) "វិភាគក្រាហ្វពន្លឺដើម្បីលៃតម្រូវ Exposure Contrast Shadows និងពណ៌ភ្លាមៗ។" else "Analyzes histogram percentiles to balance exposure, shadows, and vibrance in one tap."
    fun studioFramesBannerTitle(lang: AppLanguage) = if (isKhmer(lang)) "ស៊ុមរូបថតស្ទូឌីយោ & Polaroid" else "Studio & Cinema Frames"
    fun studioFramesBannerDesc(lang: AppLanguage) = if (isKhmer(lang)) "បន្ថែមស៊ុមស វិចិត្រសាល Polaroid ភាពយន្ត 21:9 និងស៊ុមមាស Obsidian។" else "Museum white, Polaroid, 21:9 Cinema bars, and Obsidian gold frames ready for export."
    fun histogramBannerTitle(lang: AppLanguage) = if (isKhmer(lang)) "ក្រាហ្វពណ៌ RGB ផ្ទាល់ (Histogram)" else "Live RGB & Luminance Histogram"
    fun histogramBannerDesc(lang: AppLanguage) = if (isKhmer(lang)) "ពិនិត្យពន្លឺ និងពណ៌ភ្លាមៗនៅលើផ្ទាំងរូបភាព និងក្រោមខ្សែកោងពណ៌។" else "Real-time tonal distribution HUD and interactive tone curve underlay."
    fun retouchStudioTitle(lang: AppLanguage) = if (isKhmer(lang)) "ស្ទូឌីយោ Retouch & Restore Brush" else "Retouch Studio & Restore Brush"
    fun retouchStudioDesc(lang: AppLanguage) = if (isKhmer(lang)) "ធ្វើឱ្យស្បែករលោង ធ្មេញស ភ្នែកភ្លឺ កែទម្រង់មុខ និងជក់ស្ដារផ្ទៃរូបភាពដើម។" else "Skin smoothing, teeth whiten, eye pop, directional sculpt warp, and original restore brush."
    fun curvesTitle(lang: AppLanguage) = if (isKhmer(lang)) "ខ្សែកោង & ពណ៌ HSL" else "Tone Curves & HSL"
    fun curvesDesc(lang: AppLanguage) = if (isKhmer(lang)) "ខ្សែកោង RGB កម្រិតវិជ្ជាជីវៈ និងការលៃតម្រូវបណ្តាញពណ៌ចំនួន ៨។" else "Professional RGB curve nodes and 8 selective color channels."
    fun filtersTitle(lang: AppLanguage) = if (isKhmer(lang)) "តម្រងហ្វីល & ពណ៌ចម្រុះ" else "Pro Tonal & Film Looks"
    fun filtersDesc(lang: AppLanguage) = if (isKhmer(lang)) "ពន្លឺខ្ពស់ ស្រមោល Clarity Soft Glow និងម៉ូដពណ៌ភាពយន្ត ២០+។" else "Highlights, Shadows, Soft Glow, Sharpness & 20+ cinematic presets."

    // --- Editor Top Bar ---
    fun backToHome(lang: AppLanguage) = if (isKhmer(lang)) "ត្រឡប់ទៅទំព័រដើម" else "Back to Home"
    fun undo(lang: AppLanguage) = if (isKhmer(lang)) "មិនធ្វើវិញ" else "Undo"
    fun redo(lang: AppLanguage) = if (isKhmer(lang)) "ធ្វើឡើងវិញ" else "Redo"
    fun splitView(lang: AppLanguage) = if (isKhmer(lang)) "ទិដ្ឋភាពប្រៀបធៀប" else "Split View"
    fun holdOriginal(lang: AppLanguage) = if (isKhmer(lang)) "សង្កត់៖ រូបដើម" else "Hold: Original"
    fun share(lang: AppLanguage) = if (isKhmer(lang)) "ចែករំលែក" else "Share"
    fun save(lang: AppLanguage) = if (isKhmer(lang)) "រក្សាទុក" else "Save"
    fun moreOptions(lang: AppLanguage) = if (isKhmer(lang)) "ជម្រើសបន្ថែម" else "More options"
    fun histogramHud(lang: AppLanguage) = if (isKhmer(lang)) "ក្រាហ្វ RGB" else "Histogram"
    fun zoomIn(lang: AppLanguage) = if (isKhmer(lang)) "ពង្រីករូប" else "Zoom In"
    fun zoomOut(lang: AppLanguage) = if (isKhmer(lang)) "បង្រួមរូប" else "Zoom Out"
    fun resetZoom(lang: AppLanguage) = if (isKhmer(lang)) "កំណត់ទំហំដើម 1.0x" else "Reset Zoom (1.0x)"

    // --- Editor Tabs ---
    fun tabFiltersLabel(lang: AppLanguage) = if (isKhmer(lang)) "តម្រងពណ៌" else "Filters"
    fun tabRetouchLabel(lang: AppLanguage) = if (isKhmer(lang)) "កែសម្រួល" else "Retouch"
    fun tabAdjustLabel(lang: AppLanguage) = if (isKhmer(lang)) "លៃតម្រូវ" else "Adjust"
    fun tabCurvesLabel(lang: AppLanguage) = if (isKhmer(lang)) "ខ្សែកោង" else "Curves"
    fun tabHslLabel(lang: AppLanguage) = if (isKhmer(lang)) "ពណ៌ HSL" else "HSL"
    fun tabCropLabel(lang: AppLanguage) = if (isKhmer(lang)) "កាត់តម្រឹម" else "Crop"
    fun autoSmoothSkin(lang: AppLanguage) = if (isKhmer(lang)) "រលោងស្បែកស្វ័យប្រវត្តិ" else "Auto Smooth"
    fun autoSmoothSubtitle(lang: AppLanguage) = if (isKhmer(lang)) "រកឃើញផ្ទៃស្បែក & ធ្វើឱ្យរលោងភ្លាមៗ" else "Auto-detect skin tones & smooth portrait"
    fun restoreBrushLabel(lang: AppLanguage) = if (isKhmer(lang)) "ស្ដារដើម" else "Restore"
    fun restoreBrushSubtitle(lang: AppLanguage) = if (isKhmer(lang)) "អូសដើម្បីបង្ហាញផ្ទៃរូបភាពដើមឡើងវិញ" else "Brush to restore original unretouched pixels"
    fun retouchRestoreModeBadge(lang: AppLanguage) = if (isKhmer(lang)) "ទម្រង់ស្ដារដើម • អូសដើម្បីស្ដាររូបដើម" else "RESTORE BRUSH • PAINT TO REVEAL ORIGINAL"
    fun sculptDragHint(lang: AppLanguage) = if (isKhmer(lang)) "អូសស្រាលៗតាមគែមមុខដើម្បីរុញកែទម្រង់" else "Drag gently along contours to nudge & shape"

    // --- Tool Panels ---
    fun strength(lang: AppLanguage) = if (isKhmer(lang)) "កម្រិត" else "Strength"
    fun sizeLabel(lang: AppLanguage) = if (isKhmer(lang)) "ទំហំ" else "Size"
    fun intensityLabel(lang: AppLanguage) = if (isKhmer(lang)) "កម្រិត" else "Intensity"
    fun texturePreserveLabel(lang: AppLanguage) = if (isKhmer(lang)) "រក្សាភាពលម្អិត" else "Detail"
    fun catLight(lang: AppLanguage) = if (isKhmer(lang)) "ពន្លឺ" else "Light"
    fun catColor(lang: AppLanguage) = if (isKhmer(lang)) "ពណ៌" else "Color"
    fun catDetail(lang: AppLanguage) = if (isKhmer(lang)) "ភាពច្បាស់" else "Detail"
    fun catEffects(lang: AppLanguage) = if (isKhmer(lang)) "បែបផែន" else "Effects"
    fun curvesHint(lang: AppLanguage) = if (isKhmer(lang)) "អូសចំណុច • ចុចបន្ទាត់ដើម្បីបន្ថែម • ចុចចំណុចដើម្បីលុប" else "Drag points to bend curve • Tap line to add • Tap point to delete"
    fun autoEnhance(lang: AppLanguage) = if (isKhmer(lang)) "ស្វ័យប្រវត្តិ" else "Auto"
    fun rotate90(lang: AppLanguage) = if (isKhmer(lang)) "បង្វិល 90°" else "Rotate 90°"
    fun flipH(lang: AppLanguage) = if (isKhmer(lang)) "ត្រឡប់ដេក" else "Flip H"
    fun flipV(lang: AppLanguage) = if (isKhmer(lang)) "ត្រឡប់ឈរ" else "Flip V"
    fun aspectRatio(lang: AppLanguage) = if (isKhmer(lang)) "សមាមាត្រ" else "Aspect Ratio"
    fun applyCrop(lang: AppLanguage) = if (isKhmer(lang)) "អនុវត្តការកាត់" else "Apply Crop"
    fun resetCrop(lang: AppLanguage) = if (isKhmer(lang)) "កំណត់ឡើងវិញ" else "Reset"
    fun cropHint(lang: AppLanguage) = if (isKhmer(lang)) "អូសជ្រុងដើម្បីប្តូរទំហំ • អូសខាងក្នុងដើម្បីផ្លាស់ទី" else "Drag handles to resize • Drag inside to move"
    fun studioFrames(lang: AppLanguage) = if (isKhmer(lang)) "ស៊ុមស្ទូឌីយោ" else "Studio Frames"

    // --- Photo Picker Modal ---
    fun choosePhotoToEdit(lang: AppLanguage) = if (isKhmer(lang)) "ជ្រើសរើសរូបភាពដើម្បីកែសម្រួល" else "Choose Photo to Edit"
    fun takePhotoWithCamera(lang: AppLanguage) = if (isKhmer(lang)) "ថតរូបដោយប្រើកាមេរ៉ា" else "Take Photo with Camera"
    fun openPhotoFromGallery(lang: AppLanguage) = if (isKhmer(lang)) "បើករូបភាពពីវិចិត្រសាល" else "Open Photo from Gallery"
    fun orTrySamplePhotos(lang: AppLanguage) = if (isKhmer(lang)) "ឬសាកល្បងរូបភាពគំរូកម្រិតច្បាស់" else "OR TRY HIGH-RES SAMPLE PHOTOS"

    // --- Camera Capture View ---
    fun cameraPermTitle(lang: AppLanguage) = if (isKhmer(lang)) "ត្រូវការសិទ្ធិប្រើកាមេរ៉ា" else "Camera Permission Required"
    fun cameraPermDesc(lang: AppLanguage) = if (isKhmer(lang)) "សូមអនុញ្ញាតឱ្យប្រើកាមេរ៉ា ដើម្បីថតរូប និងកែសម្រួលផ្ទាល់ក្នុង Lumina Photo។" else "Please allow camera access to snap photos and edit them directly in Lumina Photo."
    fun grantPermission(lang: AppLanguage) = if (isKhmer(lang)) "ផ្តល់សិទ្ធិ" else "Grant Permission"
    fun liveFilters(lang: AppLanguage) = if (isKhmer(lang)) "តម្រងផ្ទាល់" else "Live Filters"

    // --- Save Modal ---
    fun saveToDeviceGallery(lang: AppLanguage) = if (isKhmer(lang)) "រក្សាទុកក្នុងវិចិត្រសាលទូរសព្ទ" else "Save to Device Gallery"
    fun galleryAlbumHeader(lang: AppLanguage) = if (isKhmer(lang)) "អាល់ប៊ុម Pictures/Lumina" else "Pictures/Lumina Album"
    fun galleryAlbumLocation(lang: AppLanguage) = if (isKhmer(lang)) "អាល់ប៊ុមវិចិត្រសាល៖ Pictures/Lumina" else "Gallery Album: Pictures/Lumina"
    fun visibleInGallery(lang: AppLanguage) = if (isKhmer(lang)) "បង្ហាញក្នុង Google Photos & វិចិត្រសាលទូរសព្ទ" else "Visible in Google Photos & Device Gallery"
    fun fileNameHeader(lang: AppLanguage) = if (isKhmer(lang)) "ឈ្មោះឯកសារ" else "FILE NAME"
    fun enterPhotoName(lang: AppLanguage) = if (isKhmer(lang)) "បញ្ចូលឈ្មោះរូបថត" else "Enter photo name"
    fun formatHeader(lang: AppLanguage) = if (isKhmer(lang)) "ទម្រង់" else "FORMAT"
    fun compactWeb(lang: AppLanguage) = if (isKhmer(lang)) "ទំហំតូច / Web" else "Compact / Web"
    fun losslessPrint(lang: AppLanguage) = if (isKhmer(lang)) "ច្បាស់ល្អ / Print" else "Lossless / Print"
    fun qualityHeader(lang: AppLanguage) = if (isKhmer(lang)) "គុណភាព JPEG" else "JPEG QUALITY"
    fun resolutionHeader(lang: AppLanguage) = if (isKhmer(lang)) "ទំហំរូបភាព" else "RESOLUTION"
    fun cancel(lang: AppLanguage) = if (isKhmer(lang)) "បោះបង់" else "Cancel"
    fun saveToGalleryBtn(lang: AppLanguage) = if (isKhmer(lang)) "រក្សាទុកក្នុងវិចិត្រសាល" else "Save to Gallery"

    // --- Save Success ---
    fun savedSuccessTitle(lang: AppLanguage) = if (isKhmer(lang)) "បានរក្សាទុកក្នុងវិចិត្រសាល!" else "Saved to Device Gallery!"
    fun savedSuccessSubtitle(lang: AppLanguage) = if (isKhmer(lang)) "បានបន្ថែមទៅអាល់ប៊ុម Pictures/Lumina" else "Added to Pictures/Lumina album"
    fun openInGallery(lang: AppLanguage) = if (isKhmer(lang)) "បើកមើលក្នុងកម្មវិធីវិចិត្រសាល" else "Open in Gallery App"
    fun done(lang: AppLanguage) = if (isKhmer(lang)) "រួចរាល់" else "Done"

    // --- Settings Screen ---
    fun languageSection(lang: AppLanguage) = if (isKhmer(lang)) "ភាសា / LANGUAGE" else "LANGUAGE / ភាសា"
    fun appDisplayLanguage(lang: AppLanguage) = if (isKhmer(lang)) "ភាសាបង្ហាញកម្មវិធី" else "App Display Language"
    fun exportAndFormat(lang: AppLanguage) = if (isKhmer(lang)) "ការរក្សាទុក & ទម្រង់" else "EXPORT & FORMAT"
    fun saveFormat(lang: AppLanguage) = if (isKhmer(lang)) "ទម្រង់រក្សាទុក" else "Save Format"
    fun maxResolution(lang: AppLanguage) = if (isKhmer(lang)) "ទំហំកែសម្រួលអតិបរមា" else "Max Processing Resolution"
    fun cameraAndWorkspace(lang: AppLanguage) = if (isKhmer(lang)) "កាមេរ៉ា & កន្លែងធ្វើការ" else "CAMERA & WORKSPACE"
    fun defaultFrontCam(lang: AppLanguage) = if (isKhmer(lang)) "បើកកាមេរ៉ាមុខជាមុន" else "Default to Front Camera"
    fun defaultFrontCamSub(lang: AppLanguage) = if (isKhmer(lang)) "បើកកាមេរ៉ាក្នុងទម្រង់សែលហ្វី" else "Open camera in selfie mode"
    fun gridLines(lang: AppLanguage) = if (isKhmer(lang)) "បន្ទាត់ក្រឡាច្បាប់ 1/3" else "Composition Grid Lines"
    fun gridLinesSub(lang: AppLanguage) = if (isKhmer(lang)) "បន្ទាត់ណែនាំលើផ្ទាំងរូបភាព" else "Rule-of-thirds guide on photo canvas"
    fun hapticFeedback(lang: AppLanguage) = if (isKhmer(lang)) "រំញ័រពេលអូសរបារ" else "Slider Haptic Feedback"
    fun hapticFeedbackSub(lang: AppLanguage) = if (isKhmer(lang)) "រំញ័រស្រាលៗពេលកែសម្រួល" else "Vibration ticks when adjusting sliders"
    fun storageAndMemory(lang: AppLanguage) = if (isKhmer(lang)) "ទំហំផ្ទុក & ឃ្លាំងសម្ងាត់" else "STORAGE & MEMORY"
    fun tempCache(lang: AppLanguage) = if (isKhmer(lang)) "ឃ្លាំងសម្ងាត់រូបភាពបណ្តោះអាសន្ន" else "Temporary Image Cache"
    fun cacheSizePrefix(lang: AppLanguage) = if (isKhmer(lang)) "ទំហំឃ្លាំងសម្ងាត់៖" else "Cache Size:"
    fun clear(lang: AppLanguage) = if (isKhmer(lang)) "សម្អាត" else "Clear"
    fun aboutAndPrivacy(lang: AppLanguage) = if (isKhmer(lang)) "អំពីកម្មវិធី & ភាពឯកជន" else "ABOUT & PRIVACY"
    fun whatsNew(lang: AppLanguage) = if (isKhmer(lang)) "អ្វីដែលថ្មី" else "What's New"
    fun latestRelease(lang: AppLanguage) = if (isKhmer(lang)) "កំណែចុងក្រោយបំផុត" else "Latest Release"
    fun gotItLetsEdit(lang: AppLanguage) = if (isKhmer(lang)) "យល់ព្រម តោះកែរូបថត!" else "Got It, Let's Edit!"
    fun previousUpdates(lang: AppLanguage) = if (isKhmer(lang)) "កំណែមុនៗ" else "Previous Updates"
    fun checkForUpdates(lang: AppLanguage) = if (isKhmer(lang)) "ពិនិត្យរកកំណែថ្មី" else "Check for Updates"
    fun checkingForUpdates(lang: AppLanguage) = if (isKhmer(lang)) "កំពុងពិនិត្យរកកំណែថ្មី..." else "Checking for updates..."
    fun upToDateTitle(lang: AppLanguage) = if (isKhmer(lang)) "អ្នកកំពុងប្រើកំណែចុងក្រោយបំផុត" else "You're Up to Date"
    fun updateAvailableTitle(lang: AppLanguage) = if (isKhmer(lang)) "មានកំណែថ្មីរួចរាល់!" else "New Update Available!"
    fun updateCheckFailedTitle(lang: AppLanguage) = if (isKhmer(lang)) "ការពិនិត្យរកកំណែថ្មីបរាជ័យ" else "Update Check Failed"
    fun installedVersionLabel(lang: AppLanguage) = if (isKhmer(lang)) "កំណែបច្ចុប្បន្ន" else "Installed Version"
    fun latestVersionLabel(lang: AppLanguage) = if (isKhmer(lang)) "កំណែថ្មី" else "Latest Version"
    fun lastCheckedLabel(lang: AppLanguage) = if (isKhmer(lang)) "ពិនិត្យចុងក្រោយ" else "Last checked"
    fun neverChecked(lang: AppLanguage) = if (isKhmer(lang)) "មិនទាន់បានពិនិត្យ" else "Not checked yet"
    fun checkAgain(lang: AppLanguage) = if (isKhmer(lang)) "ពិនិត្យម្តងទៀត" else "Check Again"
    fun downloadUpdate(lang: AppLanguage) = if (isKhmer(lang)) "បើកទាញយកកំណែថ្មី" else "Get Update"
    fun downloadApkUpdate(lang: AppLanguage) = if (isKhmer(lang)) "ទាញយក APK កំណែថ្មីផ្ទាល់" else "Download APK Update"
    fun directUpdateHint(lang: AppLanguage) = if (isKhmer(lang)) "បើកតំណទាញយកដើម្បីដំឡើងលើកំណែចាស់។ ប្រសិនបើ Android បដិសេធការអាប់ដេតដោយសារហត្ថលេខាកម្មវិធីខុសគ្នា សូមលុបកំណែចាស់ចេញម្តងសិនមុនដំឡើង។" else "Downloads directly in your browser so you can install right over your current version. If Android rejects the update due to a mismatched signing key, uninstall the previous build once before installing."
    fun linkCopiedToast(lang: AppLanguage) = if (isKhmer(lang)) "បានចម្លងតំណទាញយកហើយ" else "Download link copied"
    fun noDownloadLinkHint(lang: AppLanguage) = if (isKhmer(lang)) "រកឃើញកំណែថ្មី ប៉ុន្តែមិនទាន់មានតំណ APK ភ្ជាប់មកជាមួយទេ។" else "New version detected, but this release has no APK download link attached yet."
    fun autoCheckUpdates(lang: AppLanguage) = if (isKhmer(lang)) "ពិនិត្យកំណែថ្មីស្វ័យប្រវត្តិ" else "Auto-Check on Launch"
    fun autoCheckUpdatesSub(lang: AppLanguage) = if (isKhmer(lang)) "ពិនិត្យរកកំណែថ្មីពី GitHub ដោយស្វ័យប្រវត្តិពេលបើកកម្មវិធី" else "Check GitHub releases automatically when app starts"
    fun updateSourceLabel(lang: AppLanguage) = if (isKhmer(lang)) "ប្រភពអាប់ដេតផ្លូវការ" else "Update Source"
    fun customLinkDisabledNote(lang: AppLanguage) = if (isKhmer(lang)) "តំណភ្ជាប់អាប់ដេតត្រូវបានភ្ជាប់មកជាស្រេច (ការបញ្ចូលតំណផ្ទាល់ខ្លួនត្រូវបានបិទ)។" else "Built-in GitHub update link enabled (custom link input disabled)."
    fun updatedFromLabel(fromVer: String, toVer: String, lang: AppLanguage) =
        if (isKhmer(lang)) "បានអាប់ដេតពី v$fromVer ទៅ v$toVer" else "Updated from v$fromVer → v$toVer"
    fun privacyNote(lang: AppLanguage) = if (isKhmer(lang)) "ដំណើរការលើទូរសព្ទ ១០០%។ គ្មានរូបភាព ឬទិន្នន័យផ្ទៃមុខត្រូវបានបញ្ជូនទៅម៉ាស៊ីនមេខាងក្រៅឡើយ។ រូបថតរបស់អ្នករក្សាការសម្ងាត់យ៉ាងតឹងរ៉ឹងនៅលើទូរសព្ទរបស់អ្នក។" else "100% On-Device Processing. No images or facial metrics are uploaded to external servers. Your photos stay strictly private on your phone."

    // --- Status Messages ---
    fun statusPhotoReady(lang: AppLanguage) = if (isKhmer(lang)) "រូបភាពរួចរាល់សម្រាប់កែសម្រួល" else "Photo ready to edit"
    fun statusSaving(lang: AppLanguage) = if (isKhmer(lang)) "កំពុងរក្សាទុកទៅ Pictures/Lumina..." else "Saving to Pictures/Lumina..."
    fun statusSaved(lang: AppLanguage) = if (isKhmer(lang)) "បានរក្សាទុករូបភាពទៅ Pictures/Lumina!" else "Photo saved to Pictures/Lumina!"
    fun statusAutoSmoothApplied(lang: AppLanguage) = if (isKhmer(lang)) "បានធ្វើឱ្យស្បែករលោងស្វ័យប្រវត្តិ" else "Auto Skin Smoothing applied"
    fun statusTransformApplied(lang: AppLanguage) = if (isKhmer(lang)) "បានអនុវត្តការកាត់តម្រឹម" else "Transform applied"
    fun statusCropApplied(lang: AppLanguage) = if (isKhmer(lang)) "បានកាត់តម្រឹមរូបភាពរួចរាល់" else "Crop applied"
    fun statusCacheCleared(lang: AppLanguage) = if (isKhmer(lang)) "បានសម្អាតឃ្លាំងសម្ងាត់រួចរាល់" else "App cache cleared"
    fun statusCacheClearFailed(lang: AppLanguage) = if (isKhmer(lang)) "មិនអាចសម្អាតឃ្លាំងសម្ងាត់បានទេ" else "Failed to clear cache"
    fun statusNoPhotoToSave(lang: AppLanguage) = if (isKhmer(lang)) "មិនមានរូបភាពសម្រាប់រក្សាទុកទេ" else "No photo loaded to save"
    fun statusPreparingShare(lang: AppLanguage) = if (isKhmer(lang)) "កំពុងរៀបចំរូបភាពសម្រាប់ចែករំលែក..." else "Preparing image..."
    fun statusSaveFailed(lang: AppLanguage, reason: String?) = if (isKhmer(lang)) "ការរក្សាទុកបរាជ័យ៖ ${reason.orEmpty()}" else "Save failed: ${reason.orEmpty()}"
    fun statusShareFailed(lang: AppLanguage, reason: String?) = if (isKhmer(lang)) "ការចែករំលែកបរាជ័យ៖ ${reason.orEmpty()}" else "Share failed: ${reason.orEmpty()}"
    fun statusLoadFailed(lang: AppLanguage) = if (isKhmer(lang)) "មិនអាចផ្ទុករូបភាពបានទេ" else "Could not load image"
    fun statusRetouchApplied(tool: com.example.model.RetouchType, lang: AppLanguage): String =
        if (isKhmer(lang)) "បានអនុវត្ត ${retouchTitle(tool, lang)}" else "${tool.title} applied"
    fun statusRetouchRestored(lang: AppLanguage) =
        if (isKhmer(lang)) "បានស្ដារផ្ទៃរូបភាពដើម" else "Restored original pixels"
    fun cameraPermToast(lang: AppLanguage) = if (isKhmer(lang)) "ត្រូវការសិទ្ធិប្រើកាមេរ៉ា ដើម្បីថតរូប" else "Camera permission is required to capture photos"
    fun cameraCaptureFailed(lang: AppLanguage, reason: String?) = if (isKhmer(lang)) "ថតរូបបរាជ័យ៖ ${reason.orEmpty()}" else "Capture failed: ${reason.orEmpty()}"
    fun storagePermDeniedToast(lang: AppLanguage) = if (isKhmer(lang)) "ត្រូវការសិទ្ធិរក្សាទុកឯកសារ ដើម្បីរក្សាទុកក្នុងវិចិត្រសាល" else "Storage permission is required to save to gallery"
    fun statusAutoEnhanceApplied(lang: AppLanguage) = if (isKhmer(lang)) "បានកែលម្អពន្លឺ និងពណ៌ស្វ័យប្រវត្តិ" else "Smart Auto Enhance applied"
    fun statusFrameApplied(frame: com.example.model.FrameStyle, lang: AppLanguage): String =
        if (isKhmer(lang)) "បានជ្រើសរើសស៊ុម៖ ${frameLabel(frame, lang)}" else "Frame: ${frameLabel(frame, lang)}"

    // --- Filter Categories ---
    fun filterCategory(cat: com.example.model.FilterCategory, lang: AppLanguage): String = when (cat) {
        com.example.model.FilterCategory.ALL -> if (isKhmer(lang)) "ទាំងអស់" else "All"
        com.example.model.FilterCategory.PORTRAIT -> if (isKhmer(lang)) "រូបមនុស្ស" else "Portrait"
        com.example.model.FilterCategory.CINEMATIC -> if (isKhmer(lang)) "ភាពយន្ត" else "Cinematic"
        com.example.model.FilterCategory.VINTAGE -> if (isKhmer(lang)) "បុរាណ" else "Vintage"
        com.example.model.FilterCategory.BLACK_WHITE -> if (isKhmer(lang)) "ស-ខ្មៅ" else "B&W"
        com.example.model.FilterCategory.AESTHETIC -> if (isKhmer(lang)) "សោភ័ណ" else "Aesthetic"
        com.example.model.FilterCategory.LANDSCAPE -> if (isKhmer(lang)) "ធម្មជាតិ" else "Nature"
    }

    // --- Retouch Tools ---
    fun retouchTitle(tool: com.example.model.RetouchType, lang: AppLanguage): String = when (tool) {
        com.example.model.RetouchType.SKIN_SMOOTH -> if (isKhmer(lang)) "ស្បែករលោង" else "Skin Smooth"
        com.example.model.RetouchType.TEETH_WHITEN -> if (isKhmer(lang)) "ធ្មេញស" else "Teeth Whiten"
        com.example.model.RetouchType.EYE_POP -> if (isKhmer(lang)) "ភ្នែកភ្លឺថ្លា" else "Eye Pop"
        com.example.model.RetouchType.SCULPT -> if (isKhmer(lang)) "កែទម្រង់មុខ" else "Sculpt & Warp"
        com.example.model.RetouchType.DETAIL_SHARPEN -> if (isKhmer(lang)) "បង្កើនភាពច្បាស់" else "Detail Brush"
        com.example.model.RetouchType.GLOW -> if (isKhmer(lang)) "ពន្លឺភ្លឺរលោង" else "Highlighter"
    }

    fun retouchSubtitle(tool: com.example.model.RetouchType, lang: AppLanguage): String = when (tool) {
        com.example.model.RetouchType.SKIN_SMOOTH -> if (isKhmer(lang)) "ធ្វើឱ្យស្បែករលោង និងបន្ថយរន្ធញើស" else "Softens skin texture & evens pores"
        com.example.model.RetouchType.TEETH_WHITEN -> if (isKhmer(lang)) "កាត់បន្ថយពណ៌លឿង និងធ្វើឱ្យធ្មេញស" else "Removes yellow tint & brightens teeth"
        com.example.model.RetouchType.EYE_POP -> if (isKhmer(lang)) "បង្កើនពន្លឺប្រស្រីភ្នែកឱ្យភ្លឺថ្លា" else "Enhances iris sparkle & eye clarity"
        com.example.model.RetouchType.SCULPT -> if (isKhmer(lang)) "រុញ និងកែសម្រួលទម្រង់មុខយ៉ាងរលូន" else "Nudge & refine contours smoothly"
        com.example.model.RetouchType.DETAIL_SHARPEN -> if (isKhmer(lang)) "បង្កើនភាពច្បាស់ភ្នែក ចិញ្ចើម និងបបូរមាត់" else "Sharpen eyes, brows, lips & hair"
        com.example.model.RetouchType.GLOW -> if (isKhmer(lang)) "បន្ថែមពន្លឺភ្លឺរលោងលើផ្ទៃមុខ" else "Adds a subtle radiant beauty glow"
    }

    // --- Adjust Sliders ---
    fun sliderExposure(lang: AppLanguage) = if (isKhmer(lang)) "កម្រិតពន្លឺ" else "Exposure"
    fun sliderContrast(lang: AppLanguage) = if (isKhmer(lang)) "កម្រិតខុសគ្នា" else "Contrast"
    fun sliderHighlights(lang: AppLanguage) = if (isKhmer(lang)) "ពន្លឺខ្ពស់" else "Highlights"
    fun sliderShadows(lang: AppLanguage) = if (isKhmer(lang)) "ស្រមោល" else "Shadows"
    fun sliderWhites(lang: AppLanguage) = if (isKhmer(lang)) "ពណ៌ស" else "Whites"
    fun sliderBlacks(lang: AppLanguage) = if (isKhmer(lang)) "ពណ៌ខ្មៅ" else "Blacks"
    fun sliderWarmth(lang: AppLanguage) = if (isKhmer(lang)) "ភាពកក់ក្តៅ" else "Warmth"
    fun sliderTint(lang: AppLanguage) = if (isKhmer(lang)) "ពណ៌បៃតង/ស្វាយ" else "Tint"
    fun sliderSaturation(lang: AppLanguage) = if (isKhmer(lang)) "ដង់ស៊ីតេពណ៌" else "Saturation"
    fun sliderVibrance(lang: AppLanguage) = if (isKhmer(lang)) "ពណ៌ស្រស់ថ្លា" else "Vibrance"
    fun sliderClarity(lang: AppLanguage) = if (isKhmer(lang)) "ភាពច្បាស់ល្អិត" else "Clarity"
    fun sliderSharpness(lang: AppLanguage) = if (isKhmer(lang)) "ភាពមុតស្រួច" else "Sharpness"
    fun sliderBackgroundBlur(lang: AppLanguage) = if (isKhmer(lang)) "ព្រាលផ្ទៃក្រោយ" else "Blur BG"
    fun sliderFocusSize(lang: AppLanguage) = if (isKhmer(lang)) "ទំហំផ្ដោត" else "Focus Size"
    fun protectSubjectLabel(lang: AppLanguage) = if (isKhmer(lang)) "រក្សាប្រធានបទច្បាស់" else "Protect Subject"
    fun blurFocusHint(lang: AppLanguage) = if (isKhmer(lang)) "ចុចលើរូបដើម្បីប្ដូរចំណុចផ្ដោត" else "Tap photo to move focus"
    fun quickBlurBg(lang: AppLanguage) = if (isKhmer(lang)) "ព្រាលផ្ទៃក្រោយ" else "Blur BG"
    fun statusBackgroundBlurEnabled(lang: AppLanguage) = if (isKhmer(lang)) "បានបើកភាពព្រាលផ្ទៃខាងក្រោយ — ចុចលើរូបដើម្បីកំណត់ចំណុចផ្ដោត" else "Background blur active — tap photo to set focus point"
    fun sliderSoftGlow(lang: AppLanguage) = if (isKhmer(lang)) "ពន្លឺភ្លឺស្រទន់" else "Soft Glow"
    fun sliderVignette(lang: AppLanguage) = if (isKhmer(lang)) "ស្រមោលគែម" else "Vignette"
    fun sliderGrain(lang: AppLanguage) = if (isKhmer(lang)) "គ្រាប់ហ្វីល" else "Film Grain"
    fun sliderFade(lang: AppLanguage) = if (isKhmer(lang)) "ពណ៌ស្លេកហ្វីល" else "Film Fade"
    fun sliderHue(lang: AppLanguage) = if (isKhmer(lang)) "ពណ៌ Hue" else "Hue"
    fun sliderLuminance(lang: AppLanguage) = if (isKhmer(lang)) "ពន្លឺ" else "Luminance"

    // --- HSL Channels ---
    fun hslChannel(ch: com.example.model.HslColorChannel, lang: AppLanguage): String = when (ch) {
        com.example.model.HslColorChannel.RED -> if (isKhmer(lang)) "ក្រហម" else "Red"
        com.example.model.HslColorChannel.ORANGE -> if (isKhmer(lang)) "ទឹកក្រូច" else "Orange"
        com.example.model.HslColorChannel.YELLOW -> if (isKhmer(lang)) "លឿង" else "Yellow"
        com.example.model.HslColorChannel.GREEN -> if (isKhmer(lang)) "បៃតង" else "Green"
        com.example.model.HslColorChannel.CYAN -> if (isKhmer(lang)) "ខៀវស្រាល" else "Cyan"
        com.example.model.HslColorChannel.BLUE -> if (isKhmer(lang)) "ខៀវ" else "Blue"
        com.example.model.HslColorChannel.PURPLE -> if (isKhmer(lang)) "ស្វាយ" else "Purple"
        com.example.model.HslColorChannel.MAGENTA -> if (isKhmer(lang)) "ផ្កាឈូក" else "Magenta"
    }

    // --- Crop Ratios & Studio Frames ---
    fun cropRatioLabel(r: com.example.model.CropAspectRatio, lang: AppLanguage): String = when (r) {
        com.example.model.CropAspectRatio.FREE -> if (isKhmer(lang)) "សេរី" else "Free"
        com.example.model.CropAspectRatio.ORIGINAL -> if (isKhmer(lang)) "ដើម" else "Original"
        else -> r.label
    }

    fun frameLabel(frame: com.example.model.FrameStyle, lang: AppLanguage): String = when (frame) {
        com.example.model.FrameStyle.NONE -> if (isKhmer(lang)) "គ្មាន" else "None"
        com.example.model.FrameStyle.CLASSIC_WHITE -> if (isKhmer(lang)) "ស៊ុមស" else "White"
        com.example.model.FrameStyle.POLAROID -> if (isKhmer(lang)) "Polaroid" else "Polaroid"
        com.example.model.FrameStyle.CINEMA -> if (isKhmer(lang)) "ភាពយន្ត" else "Cinema"
        com.example.model.FrameStyle.OBSIDIAN -> if (isKhmer(lang)) "ខ្មៅមាស" else "Obsidian"
    }
}
