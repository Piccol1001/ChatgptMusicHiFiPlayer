# CODEX PROMPT — COMPLETE ANDROID MUSIC PLAYER UI/UX REDESIGN

## Project: HiFi Player
## Task: Major Interface Redesign and Layout Correction

The current Android music player has functioning screens, but the overall UI/UX is visually unsatisfactory.

The supplied five screenshots demonstrate the current interface.

I want a substantial, professional redesign inspired by successful Android music applications. Do not simply adjust the colors, corner radius, or typography while retaining the existing layout.

**This is a UI/UX overhaul, not a request to rebuild the playback system.**

Preserve all existing functionality, including:

- Music library and folder access
- Local playback and playlists
- Background audio service
- All three equalizer modes
- Equalizer presets and AutoEq
- All six visualizers
- Lyrics
- Audio information
- USB DAC support
- Widgets and settings
- Offline storage and privacy

Do not remove existing features to simplify the new interface.

---

# 1. DESIGN REFERENCES

Research and study the following applications and their publicly available interface screenshots.

### Poweramp
https://powerampapp.com/

Use as inspiration for:
- Now Playing layout
- Professional audio-control presentation
- Graphic and parametric equalizer
- Visualizer appearance
- Hi-Res audio information
- Playback control hierarchy

### Oto Music
https://play.google.com/store/apps/details?id=com.piyush.music

Use as inspiration for:
- Modern Android design
- Album and playlist organization
- Clean typography
- Balanced spacing
- Minimal UI
- Dark-theme appearance

### Symfonium
https://symfonium.app/

Use as inspiration for:
- Customizable Now Playing
- Mini-player
- Adaptive layouts
- Album artwork presentation
- Dynamic colors
- Music browsing

### Musicolet
https://play.google.com/store/apps/details?id=in.krosbits.musicolet

Use as inspiration for:
- Efficient music library navigation
- Folder organization
- Playlist management
- Compact song listings
- Offline-first workflow

Do not directly copy copyrighted graphics, proprietary layouts, icons, or brand assets.

Combine successful interface principles into a cohesive, original design.

**Target design: Poweramp audio capabilities + Oto Music visual simplicity + Symfonium customization + Musicolet library efficiency.**

---

# 2. GLOBAL VISUAL DESIGN

## 2.1 Color system

Maintain a dark AMOLED-oriented interface.

Suggested design tokens:

- Main background: #080A0F
- Elevated surface: #151922
- Secondary surface: #202633
- Primary text: #F4F6FA
- Secondary text: #A4ADBF
- Main accent: #58A6FF
- Error: #FF6B6B

These are starting points. Refine them for visual balance and accessible contrast.

Use the blue accent selectively for active controls, audio sliders, progress bars, and important actions.

Avoid coloring every text action blue.

Do not overuse large gray-purple pill-shaped backgrounds.

Use subtle gradients where they materially enhance the interface, particularly behind album artwork.

## 2.2 Typography

Establish a consistent typographic hierarchy.

Suggested sizes:
- Main screen title: 26–30sp
- Section headings: 18–22sp
- Song title: 16–18sp
- Body text: 14–16sp
- Supporting labels: 12–14sp

Use appropriate weight rather than excessive size differences.

Avoid huge labels that occupy unnecessary vertical space.

## 2.3 Spacing

Use a consistent spacing system based on 4dp increments.

Recommended:
- Horizontal screen padding: 16–20dp
- Small spacing: 8dp
- Component spacing: 12–16dp
- Section spacing: 20–24dp

Do not create excessive blank areas through hardcoded spacers or screen-height fractions.

Use responsive layouts.

## 2.4 Shared components

Create reusable components for:

- App header
- Icon buttons
- Album artwork
- Song rows
- Section headers
- Playlist cards
- Mini-player
- Playback controls
- Audio-format badges
- Equalizer controls
- Empty states
- Bottom navigation
- Dialogs and bottom sheets

Implement them consistently throughout the application.

---

# 3. GLOBAL NAVIGATION REDESIGN

Keep the existing five main destinations:

1. Library
2. Playlists
3. Playing
4. Equalizer
5. Visualizer

However, redesign the bottom navigation.

Requirements:
- Compact height.
- Consistent icons.
- Consistent spacing.
- Small readable labels.
- Clear active state.
- No oversized selection pill.
- No excessive bottom padding.
- Respect Android gesture-navigation insets.

The navigation should feel similar to a polished commercial music player.

### Header correction

The current UI repeats:
"HiFi Player · on your device"

Remove this repetitive subtitle from every screen.

Use a simple header with a clear screen title and context-relevant actions.

For example:
- Library: Search, Rescan, Settings
- Playlists: Create, Import, More
- Equalizer: Presets, Settings
- Visualizer: Full-screen, Settings
- Playing: Queue, More

Do not display refresh buttons on screens where refreshing has no useful meaning.

---

# 4. GLOBAL MINI-PLAYER

Implement a persistent mini-player above the bottom navigation whenever a track is loaded.

The mini-player should contain:

- Small album artwork
- Song title
- Artist
- Play/Pause
- Next track
- Thin playback-progress indicator

Tapping the mini-player opens Now Playing.

Suggested height: 64–76dp, excluding any system insets.

Use a subtly elevated dark background.

Do not display the mini-player when no track is available.

Do not duplicate it on the full Now Playing screen.

Ensure the mini-player and navigation do not cover scrollable content.

---

# 5. LIBRARY SCREEN — COMPLETE REDESIGN

## Current problems

The screenshot shows:
- Oversized music-permission card.
- Excessively large search bar.
- Crowded category buttons.
- Poor visual hierarchy.
- Large empty region.
- Unattractive empty state.

## Required new layout

### Header

Display:

Library

Use context-appropriate search and settings actions.

### Search

Create a modern rounded search field with a leading search icon and clear-action support.

Avoid thick outlines and excessive height.

Search song titles, artists, albums, and supported metadata.

### Library categories

Create horizontally scrollable category chips:

- Songs
- Albums
- Artists
- Genres
- Folders
- Favorites
- Recently Added

The selected category should use a compact accent-colored background.

Category chips must not be cut off incorrectly at the right edge.

Horizontal scrolling should be natural and accessible.

### Music listing

For Songs, display a compact list.

Each row should contain:

- Album artwork
- Song title
- Artist and album
- Optional audio-quality label
- Three-dot action menu

Use consistent artwork sizing and row height.

For Albums, use a two-column artwork grid with album name and artist underneath.

For Artists, show artist artwork or a meaningful placeholder with artist name and song count.

For Folders, provide a proper folder-tree navigation system.

### Music permissions

Do not permanently occupy a large portion of the screen with a permission card.

If music access has not been granted, show a compact onboarding state with:

- Clear explanation
- Grant Music Access
- Choose Folder

After permission is granted, remove that onboarding component automatically.

Display a progress indicator during library scanning.

### Empty state

If no music exists, display a visually balanced, compact empty state.

Use a simple music illustration or icon.

Text:

"No music found"

"Add a local music folder or grant storage access to start listening."

Provide a prominent Add Music button.

Do not fill the screen with large amounts of empty space or fake song entries.

---

# 6. PLAYLIST SCREEN — COMPLETE REDESIGN

## Current problems

The screenshot contains:
- Large blank space.
- Disconnected Create Playlist button.
- Technical-looking import text.
- Very little visual structure.

## New playlist layout

Header:

Playlists

Top actions:
- Create Playlist
- Import
- More

Display sections for:

### Your Playlists

Use artwork-based playlist cards.

Show:
- Playlist name
- Artwork or collage
- Track count
- Total duration where available

Use a responsive two-column grid when suitable.

### Smart Playlists

Present built-in smart playlists such as:
- Recently Played
- Most Played
- Favorites
- Recently Added

Use compact attractive cards.

### Import

Move M3U/M3U8 import into a menu or bottom sheet.

Do not use a large technical text link reading "Import M3U8 from indexed content URIs" as the primary user-facing label.

Use natural text such as "Import playlist".

Internally retain the correct URI and file-selection behavior.

### Empty playlist state

Display a concise illustration and:

"Create your first playlist"

"Organize your favorite songs into collections."

Provide a Create Playlist button.

When playlists exist, use the available space for actual playlist cards instead of large empty margins.

---

# 7. NOW PLAYING — MAJOR REDESIGN

This should be the most visually polished screen in the application.

Use Poweramp and Oto Music as the primary design references.

## 7.1 When a song is playing

Arrange the screen vertically:

### Top bar
- Back/minimize
- Now Playing label
- Queue or More

### Album artwork
- Large square artwork
- Centered horizontally
- Approximately 70–85% of available screen width, constrained by height
- Moderate rounded corners
- Optional subtle background gradient derived from artwork

Do not crop the artwork unnecessarily.

### Track information
- Song title
- Artist
- Album
- Favorite button

Use visual hierarchy without excessive spacing.

### Audio-quality badge

Display a compact row such as:

FLAC · 24-bit · 96 kHz

Include detailed audio information behind a tap action.

### Seek bar
- Clear progress indication
- Elapsed time
- Remaining or total duration
- Responsive seeking

### Main controls
- Shuffle
- Previous
- Large Play/Pause
- Next
- Repeat

Align controls symmetrically and maintain adequate touch targets.

### Additional controls
- Lyrics
- Equalizer
- Visualizer
- Queue
- Audio information

Use compact icons or a secondary action row.

Do not crowd the main playback controls.

## 7.2 When no music is loaded

The current screen is almost completely empty.

Replace it with an intentional empty state.

Display:
- Large but restrained music-disc illustration
- "Ready to listen?"
- "Choose music from your library to start playing."
- Browse Library button

Do not show fake playback information or pretend a track exists.

## 7.3 Optional visual personalization

Provide settings for:
- Artwork-based background gradient
- Pure black background
- Artwork corner radius
- Compact/detailed audio information
- Optional embedded spectrum visualization

Keep animations lightweight.

---

# 8. EQUALIZER SCREEN — COMPLETE REDESIGN

This screen requires particular attention.

The current screenshot shows:
- Poor use of vertical space.
- A large graph separating related controls.
- Unbalanced buttons.
- Awkward preamp control.
- Overlapping/clipped frequency sliders.
- Important band controls close to the navigation bar.

Redesign it as a professional mobile audio-control interface inspired by Poweramp and foobar2000.

## 8.1 Header

Display:

Equalizer

On the right:
- EQ On/Off switch
- Preset selector

Place technical engine information in a compact expandable information panel rather than permanently taking space.

## 8.2 EQ mode selection

Use a compact segmented control:

10-Band | Parametric | 31-Band

Do not use oversized buttons.

The active mode must be immediately recognizable.

## 8.3 Frequency response graph

Display an attractive frequency-response curve.

Suggested portrait height: approximately 180–220dp, adapted to available space.

Features:
- Logarithmic frequency axis
- Gain/dB labels
- Subtle grid
- Bright blue response line
- Current response
- Accurate axis markings

Do not overlap frequency labels.

Make sure the response graph fits within the screen width.

For parametric mode, show editable filter control points.

When EQ is bypassed, display an accurate indication that processing is bypassed.

## 8.4 Equalizer bands

Immediately below the graph, provide the frequency controls.

### 10-Band mode
- Ten vertical sliders
- Horizontal scrolling where needed
- Frequency labels
- Gain labels
- Smooth interactions

### 31-Band mode
- Horizontally scrollable bands
- Consistent band width
- Appropriate slider height
- Visible selected-band gain
- Clear frequency labels
- Ability to enter an exact value

### Parametric mode
- Frequency response graph
- Filter list
- Add Filter
- Remove Filter
- Frequency/Gain/Q controls
- Filter type selector

Allow editing through a bottom sheet or compact dialog.

**Critical:** Every band must be accessible. No control may be covered by the bottom navigation or clipped by the display edge.

## 8.5 Presets and custom settings

Present a compact toolbar:

Preset selector | Save | Reset

Preset selection should open a searchable bottom sheet containing:

- Built-in presets
- Favorites
- Custom presets
- Recently used presets

Keep advanced preset operations in the bottom sheet or overflow menu.

## 8.6 Preamp

Replace the awkward current preamp slider with a clean horizontal control.

Show:

Preamp    +7.5 dB

[Slider]

Include automatic headroom as a compact switch below.

Provide a short expandable explanation of headroom.

Clearly distinguish:
- User-requested preamp
- Automatically calculated headroom compensation
- Effective applied preamp
- EQ bypass state

Avoid displaying confusing or contradictory gain values.

## 8.7 Responsive screen structure

The screen must scroll vertically.

The band controls must support independent horizontal scrolling.

Use nested scrolling correctly.

Keep important controls away from bottom-system and navigation insets.

Do not use hardcoded heights that cause the sliders to disappear on smaller phones.

---

# 9. VISUALIZER SCREEN — COMPLETE REDESIGN

## Current problems

The screenshot shows:
- A large empty black visualization box.
- Two rows of bulky icon selectors.
- Disconnected settings controls.
- Technical information occupying valuable screen space.
- Too little visual emphasis on the visualization itself.

## 9.1 Primary visualizer

Make the visualization the central element of the screen.

When music is playing, dedicate the largest reasonable area to the selected visualizer.

Support full-screen immersive mode.

Use real audio data from the existing PCM/FFT pipeline.

Do not replace real analysis with random animation.

## 9.2 Visualizer selector

Replace the existing grid of six large circular icons with a horizontally scrollable selector.

Modes:
- Spectrum
- Oscilloscope
- Stereo Meter
- Spectrogram
- Circular
- Reactive

Use compact labeled icons or preview thumbnails.

Display the active mode clearly.

## 9.3 Bottom controls

Provide:
- Full Screen
- Settings
- Color Theme

Use a compact toolbar.

Move lengthy technical information into an About/Technical Details section.

## 9.4 During playback

Show a small overlay containing:
- Song title
- Artist
- Optional audio quality

Allow the overlay to disappear automatically in full-screen mode.

## 9.5 When no music is playing

Do not leave a meaningless blank black rectangle.

Display a designed placeholder with a visualizer icon and:

"Play music to start visualizing"

Provide a Browse Music button.

An optional demonstration animation is acceptable only if explicitly labeled Preview and is not presented as actual audio analysis.

## 9.6 Visualizer quality

Preserve:
- 30/60 FPS settings
- Smoothing
- FFT resolution
- Color customization
- Peak hold
- Performance optimization

The redesign must not change the underlying DSP pipeline unnecessarily.

---

# 10. SETTINGS REDESIGN

Use grouped settings pages with a consistent hierarchy.

Categories:
- Playback
- Audio Output
- Equalizer
- Visualizer
- Library
- Appearance
- Widgets
- Privacy & Data
- About

Use list rows with:
- Icon
- Title
- Short description where useful
- Relevant switch, selected value, or navigation arrow

Avoid unnecessary dialogs for simple settings.

Offer an Appearance section with:
- AMOLED black theme
- Dark gray theme
- Accent color
- Dynamic album-art color
- Artwork corner radius
- Reduce animations
- Compact music-list mode

---

# 11. ADAPTIVE LAYOUT REQUIREMENTS

Support:
- Common small Android phones
- Standard phones
- Large phones
- Landscape orientation
- Display size adjustments
- Larger font scales

Use dp/sp and actual layout constraints.

Do not design solely for the dimensions of the supplied screenshots.

Minimum requirements:
- No horizontal clipping of unrelated controls.
- No buttons hidden behind bottom navigation.
- No overlapping texts.
- No excessive empty gaps caused by fixed spacers.
- No controls extending beyond the screen.
- All scrollable areas remain accessible.
- Respect status bar, navigation bar, and keyboard insets.
- Maintain touch targets of at least approximately 48dp for primary interactions.

Use Compose's adaptive measurement and WindowInsets facilities.

---

# 12. UI IMPLEMENTATION STRATEGY

Before making changes:

1. Examine the existing Compose navigation and screen structures.
2. Identify reusable components.
3. Identify the cause of excessive whitespace.
4. Identify fixed dimensions that cause clipping.
5. Review how empty states are rendered.
6. Check whether bottom navigation overlays screen content.
7. Identify duplicate header components or actions.
8. Create an updated design system.

Create an implementation plan and then perform the redesign.

Recommended implementation order:

### Stage A — Design System
- Colors
- Typography
- Spacing
- Buttons
- Cards
- Navigation
- Header
- Mini-player

### Stage B — Core Screens
- Library
- Playlists
- Now Playing

### Stage C — Advanced Screens
- Equalizer
- Visualizer
- Settings

### Stage D — Polish
- Empty states
- Transitions
- Responsive layouts
- UI consistency
- Accessibility
- Screenshot testing

Do not stop after planning. Implement the changes in the existing codebase.

---

# 13. VISUAL QUALITY ACCEPTANCE CRITERIA

The redesign is only complete when:

1. All five screens use a consistent visual language.
2. Screen headers are compact and relevant.
3. The library looks organized when populated.
4. Empty states look intentional rather than unfinished.
5. The Now Playing screen prioritizes artwork and playback controls.
6. Equalizer sliders are fully visible and usable.
7. The frequency-response graph has accurate, readable axes.
8. The visualizer occupies the primary content area.
9. The mini-player integrates with bottom navigation.
10. No screen has overlapping or clipped controls.
11. Scrolling works correctly.
12. Layouts adapt to different phone sizes.
13. Dark-theme contrast is readable.
14. Playback and DSP functionality remain unchanged unless a real bug must be fixed.
15. All existing features remain accessible.

## Screenshot validation

Where the environment supports emulator screenshots or Compose screenshot tests:

Capture and inspect:
- Library with songs
- Library without music
- Playlists populated
- Playlists empty
- Now Playing with music
- Now Playing without music
- 10-band Equalizer
- Parametric Equalizer
- 31-band Equalizer
- Visualizer during playback
- Visualizer when idle

Compare layouts visually to the target quality of established Android music players.

Check screenshots for proportions, visual alignment, whitespace, overlap, clipping, and color consistency.

Do not report screenshot validation as completed if the environment cannot render the application.

---

# 14. REGRESSION PREVENTION

Do not:
- Rebuild the audio engine.
- Delete the current database.
- Reset user playlists.
- Remove existing DSP modes.
- Disable current visualizers.
- Remove AutoEq.
- Break MediaSessionService.
- Introduce network dependencies.
- Replace functional features with mockups.
- Introduce fake song entries into the live library.

Preserve user data through any necessary migrations.

Run compilation, lint, and relevant tests after implementation.

---

# 15. FINAL DELIVERY

After finishing:

- Summarize the major UI improvements.
- Identify modified screen files.
- Explain the new shared design components.
- Report build and test results.
- List any unverified functionality.
- Update application version information appropriately according to the project's existing versioning policy.
- Update relevant project documentation.

**The final result should look like a professionally designed Android music player, not a default Material 3 demonstration application.**

Prioritize layout quality, visual hierarchy, professional music-player ergonomics, and responsive interactions.

Use the supplied screenshots as before-state references and Poweramp, Oto Music, Symfonium, and Musicolet as design inspiration.

Implement a cohesive, original UI while retaining every existing audio and offline feature.
