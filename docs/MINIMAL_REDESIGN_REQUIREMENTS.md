# CODEX PROMPT — HIFI PLAYER COMPLETE UI/UX REDESIGN
## Design Direction: Minimalist, Premium, Functional

### 1. Project Objective

Redesign the existing HiFi Player Android application's user interface to deliver a premium, minimalist music-listening experience.

The current UI has several design problems:
- Excessive unused space.
- Poor visual hierarchy.
- Repetitive headers.
- Oversized buttons and selection backgrounds.
- Inconsistent spacing.
- Equalizer controls extending beyond the visible screen.
- Visualizers occupying too little of the useful screen area.
- Empty screens that look unfinished.
- Too many primary navigation destinations.

The goal is to make the application **simple for everyday music playback while keeping all advanced audiophile features available**.

Think of the design as:

- Oto Music for minimalism.
- Poweramp for playback and equalizer ergonomics.
- Symfonium for customization.
- foobar2000 for advanced audio functionality.

Create an original design. Do not directly copy proprietary assets or screen layouts.

**IMPORTANT: This is a UI/UX redesign, not an application rebuild. Preserve the existing playback engine, database, playlists, equalizers, visualizers, AutoEq, lyrics, widgets, and other functional systems.**

---

# 2. Core Design Philosophy

Follow these principles throughout the application.

**Principle 1 — Music comes first**

The music library, artwork, track information, and playback controls should be the primary focus.

**Principle 2 — Simple first, advanced second**

Frequently used functions must be immediately accessible.

Advanced options such as 31-band EQ, preamp, headroom, FFT configuration, and USB DAC information should be available through well-organized expandable sections, secondary screens, or bottom sheets.

Do not permanently display every technical option on the main interface.

**Principle 3 — Minimal visual clutter**

Avoid unnecessary cards, large decorative backgrounds, repeated subtitles, excessive borders, and irrelevant buttons.

**Principle 4 — Functional simplicity**

Do not sacrifice usability for appearance. A minimal interface must still support efficient navigation, precise audio adjustment, and fast access to frequently used controls.

**Principle 5 — Consistency**

All screens must use a common spacing system, typography, controls, colors, and interaction patterns.

---

# 3. Global Design System

## 3.1 Color palette

Use a premium AMOLED-dark theme.

Recommended starting palette:

| Element | Color |
|---|---|
| Main background | #080A0E |
| Elevated surface | #171B23 |
| Secondary surface | #222834 |
| Primary text | #F5F7FA |
| Secondary text | #A4ADBC |
| Accent blue | #5AA6FF |
| Error | #FF6B6B |

Use accent blue for:
- Active navigation indicators.
- Playback progress.
- Equalizer sliders.
- Selected settings.
- Active controls.
- Important interactive actions.

Avoid excessive blue text and large purple/gray button backgrounds.

Album artwork may provide a subtle dynamic background gradient on the Now Playing screen.

All combinations must remain readable with sufficient contrast.

## 3.2 Typography

Use a consistent Material 3-based typography system.

Suggested sizes:
- Main page title: 26sp, semibold.
- Section heading: 20sp, semibold.
- Song title: 16sp, medium.
- Body text: 14–16sp.
- Secondary information: 12–14sp.

Avoid oversized labels.

Use consistent line heights and proper text truncation.

## 3.3 Layout spacing

Use an 8dp-based spacing system with 4dp increments when needed.

Recommended:
- Screen horizontal padding: 16dp.
- Component spacing: 8–12dp.
- Section spacing: 20–24dp.
- Standard interactive height: at least 48dp.
- Artwork corner radius: approximately 12–16dp.

Avoid hardcoded empty spacers that create large unused sections.

Use responsive constraints and Android WindowInsets.

## 3.4 UI components

Create reusable Compose components for:
- Screen headers.
- Music artwork.
- Song rows.
- Album grids.
- Playlist cards.
- Playback controls.
- Mini-player.
- Navigation bar.
- Settings rows.
- Segmented selectors.
- Equalizer sliders.
- Empty states.
- Bottom sheets.

Maintain a centralized theme and design tokens.

---

# 4. Simplify Navigation — Three Primary Sections

Replace the existing five bottom navigation items with three:

1. Library
2. Playing
3. Audio

Do not remove any existing feature.

## 4.1 Library

Combine music discovery and playlist management.

Include these categories:

Songs | Albums | Artists | Playlists | Folders

Provide secondary access to:
- Genres
- Favorites
- Recently Added
- Recently Played

Use horizontally scrollable tabs or category chips.

## 4.2 Playing

Provide the main playback screen.

Include access to:
- Playback controls.
- Queue.
- Lyrics.
- Audio information.
- Equalizer shortcut.
- Visualizer shortcut.

## 4.3 Audio

Combine the existing Equalizer and Visualizer destinations.

Use two prominent but compact top-level tabs:

Equalizer | Visualizer

Keep both features independent and fully functional.

Provide secondary access to:
- AutoEq.
- Audio output profiles.
- USB DAC information.
- Advanced audio settings.

Existing deep links and shortcuts to the former Equalizer and Visualizer routes should resolve to their corresponding Audio tab instead of becoming broken.

## 4.4 Bottom navigation design

Requirements:
- Three evenly spaced navigation items.
- Clean line icons.
- Short text labels.
- Compact height.
- Clear active state using accent color.
- No oversized selection pills.
- Respect gesture navigation.
- Support normal Android back navigation.

Do not use complex animations.

Use a subtle animated transition when changing destinations.

---

# 5. Persistent Mini-Player

Implement a compact mini-player that appears whenever music is loaded.

Place it above the bottom navigation bar.

Display:
- Small album artwork.
- Song title.
- Artist.
- Play/Pause.
- Next.
- Thin progress indicator.

Recommended height: approximately 68dp.

Tapping the mini-player opens the Playing screen.

Do not duplicate the mini-player on the full Playing screen.

Hide it when no current track exists.

Ensure the mini-player does not overlap scrollable screen content.

The mini-player must observe the existing playback session rather than creating a separate player.

---

# 6. Library Screen Redesign

## 6.1 Header

Use a compact title:

Library

Right-side actions:
- Search, if not already exposed directly.
- Settings or overflow menu.

Remove the repetitive "HiFi Player · on your device" subtitle.

Avoid displaying the same refresh button on every screen.

## 6.2 Search

Provide a compact search bar.

Search:
- Songs.
- Artists.
- Albums.
- Playlists where appropriate.

Use rounded corners and minimal borders.

Include a clear-search action.

## 6.3 Categories

Below search, show horizontally scrollable categories:

Songs | Albums | Artists | Playlists | Folders

The selected category should use a subtle accent-colored background.

Preserve access to additional library views through an overflow menu or secondary categories.

## 6.4 Song list

Display music in compact rows.

Each row contains:
- Album artwork.
- Song title.
- Artist.
- Optional audio format badge.
- More-options icon.

Example:

[Artwork]  Blinding Lights        ⋮
           The Weeknd · FLAC

Ensure:
- Efficient scrolling.
- Appropriate text truncation.
- Consistent artwork dimensions.
- Proper row spacing.
- No unnecessarily large cards.

Tapping a song starts playback.

Long pressing opens contextual actions.

## 6.5 Album layout

Use a responsive two-column album grid on standard phone screens.

Display:
- Square artwork.
- Album name.
- Artist.
- Optional track count.

Avoid unnecessary card backgrounds around every album.

## 6.6 Playlist layout inside Library

Show:
- User-created playlists.
- Smart playlists.
- Favorites.

Provide a floating or clearly located Create Playlist action when the Playlists category is active.

Use artwork-based cards where possible.

Playlist import/export should remain accessible through menus.

Do not expose technical file-handling terminology in the main interface.

## 6.7 Empty library

When storage permission is missing, provide an intentional onboarding state:

Title:
"Your music, your way"

Description:
"Choose a music folder or allow access to discover songs stored on your device."

Buttons:
- Allow Music Access
- Choose Folder

Once access is granted, remove this onboarding state.

When permission exists but there are no songs, show:
"No music found"
with a compact illustration and Add Music action.

Do not display fake music to fill empty areas.

---

# 7. Playing Screen Redesign

This should be the visual centerpiece of the application.

## 7.1 Screen structure

Use the following order:

1. Compact top bar.
2. Large album artwork.
3. Song title and artist.
4. Audio format information.
5. Playback progress.
6. Main playback controls.
7. Secondary actions.

## 7.2 Album artwork

Make the artwork the primary visual element.

Requirements:
- Large square display.
- Horizontally centered.
- Responsive size based on available height and width.
- Moderate rounded corners.
- Optional subtle background gradient derived from artwork.

Do not make artwork so large that controls become inaccessible on smaller phones.

Do not crop artwork unnecessarily.

## 7.3 Track information

Show:
- Song title in a prominent font.
- Artist below.
- Album if available.
- Favorite icon beside the title or in the secondary actions.

Avoid large empty gaps between artwork and track details.

## 7.4 Audio information

Provide a compact audio-quality line.

Example:

FLAC · 24-bit · 96 kHz · Stereo

Only show information confirmed by the existing metadata/audio engine.

Tapping this line opens detailed audio information.

Do not confuse source-file quality with hardware output quality.

## 7.5 Playback progress

Display:
- Seek bar.
- Elapsed time.
- Total duration.

Use the accent color for progress.

Ensure seeking is responsive and accessible.

## 7.6 Primary controls

Use the following arrangement:

Shuffle | Previous | Play/Pause | Next | Repeat

Make Play/Pause the most prominent button.

Use balanced sizing and spacing.

Do not surround every icon with a large colored pill.

## 7.7 Secondary actions

Provide compact access to:
- Queue.
- Lyrics.
- Equalizer.
- Visualizer.
- More options.

If space is limited, use a bottom sheet for secondary controls.

## 7.8 Empty Playing state

If no song is loaded:

- Display a centered music-disc illustration or icon.
- Show "Ready to listen?"
- Show "Choose a song from your library."
- Provide Browse Library button.

Do not use an almost entirely blank screen with text floating in the middle.

Do not display fake album artwork or playback progress.

---

# 8. Equalizer Screen Redesign

The equalizer must look professional while remaining usable on a phone.

Default to 10-band mode for simplicity.

## 8.1 Header

At the top of the Equalizer tab:

Equalizer                         [On/Off]

Below:

[Preset selector]          [More]

Use a compact preset selector.

## 8.2 Frequency-response graph

Display the actual configured response curve.

Suggested height: 160–200dp on a standard phone.

Use:
- Dark background.
- Subtle grid lines.
- Blue response curve.
- Clear frequency labels.
- Clear dB labels.

Avoid overlapping axis labels.

Do not display excessive technical text above the graph.

## 8.3 Mode selection

Use a compact segmented control:

10-Band | Parametric | 31-Band

Maintain the user's last selected mode.

## 8.4 Equalizer sliders

Use consistent vertical sliders with:
- Gain value above.
- Slider in the center.
- Frequency below.

Support horizontal scrolling where necessary.

For 31-band mode:
- Use independently scrollable horizontal band controls.
- Provide sufficient slider height.
- Prevent clipping near the bottom navigation.
- Allow exact numeric gain entry.

For Parametric mode:
- Display the response graph.
- Show a compact list of filters.
- Provide frequency, gain, Q, and filter type controls.
- Use an editing bottom sheet when needed.

## 8.5 Advanced settings

Place the following inside a collapsible "Advanced controls" section:

- Preamp.
- Automatic headroom.
- DSP-related settings.
- Output-specific EQ assignment.

Do not hide controls so deeply that they are difficult to discover.

Keep EQ preset selection and reset easy to access.

## 8.6 Presets

Selecting Presets should open a searchable bottom sheet.

Categories:
- General.
- Genres.
- Listening Profiles.
- Favorites.
- Custom.

Support saving and selecting presets without interrupting music playback.

## 8.7 Technical correctness

Preserve the existing DSP processing.

Do not substitute working equalizer sliders with visual mockups.

The response graph must reflect real filter settings.

All bands must remain accessible on smaller screens.

---

# 9. Visualizer Screen Redesign

The visualizer should be immersive and visually focused.

## 9.1 Main display

Make the visualization occupy most of the available space.

Use actual PCM/FFT data from the existing playback engine.

Do not show an empty large black rectangle without a meaningful state.

## 9.2 Visualization modes

Preserve all six modes:

1. Spectrum Analyzer.
2. Oscilloscope.
3. Stereo Meters.
4. Spectrogram.
5. Circular Spectrum.
6. Reactive / MilkDrop-style.

Replace the large two-row icon grid with a compact horizontally scrollable selector.

Use a simple icon and short label for each mode.

## 9.3 Controls

Below or overlaid on the visualizer, provide:

- Full Screen.
- Mode Selection.
- Color Theme.
- Settings.

Move technical descriptions into the settings screen or an information bottom sheet.

## 9.4 Full-screen mode

Provide immersive full-screen visualization.

Hide navigation and unnecessary controls.

Allow a tap to show temporary controls.

Provide an obvious way to exit.

## 9.5 No music playing

When playback is inactive and no usable audio data exists, display:

"Play music to see your sound"

Provide a Browse Library button.

Optionally show a labeled static preview, but do not present it as live audio analysis.

## 9.6 Performance

Retain:
- FFT analysis.
- Adjustable smoothing.
- FPS settings.
- Quality settings.
- Color themes.
- Peak hold.
- Proper resource cleanup.

Do not change the DSP engine unnecessarily during this redesign.

---

# 10. Settings

Redesign Settings using grouped sections.

Categories:

- Playback.
- Audio Output.
- Equalizer.
- Visualizer.
- Library.
- Appearance.
- Widgets.
- Privacy & Data.
- About.

Each settings row should contain:
- Clear title.
- Short optional description.
- Relevant control or navigation arrow.

Do not display lengthy technical explanations unless the user expands an information section.

## Appearance customization

Allow:
- AMOLED Black theme.
- Dark Gray theme.
- Accent color selection.
- Dynamic album-art colors.
- Reduce animations.
- Compact song-list mode.

Default configuration should remain minimalist.

---

# 11. Responsive Design and Accessibility

Support different Android screen sizes.

Test at least:
- 360dp-wide phone.
- 412dp-wide phone.
- Larger Android screen.
- Landscape orientation.
- Increased font scaling.

Requirements:
- No overlapping text.
- No clipped equalizer sliders.
- No controls hidden behind system navigation.
- No oversized fixed spacers.
- Correct nested scrolling.
- Respect WindowInsets.
- Maintain suitable touch targets.
- Provide accessible labels for icon buttons.
- Support text scaling without breaking the interface.

Do not hardcode pixel coordinates or assume the screen size matches the supplied screenshots.

---

# 12. Animation Guidelines

Use subtle motion only where it improves usability.

Recommended:
- Smooth tab transitions.
- Responsive button feedback.
- Smooth mini-player updates.
- Gentle album-art color transitions.
- Smooth equalizer-curve updates.

Avoid:
- Excessive bounce effects.
- Unnecessary screen transitions.
- Constant decorative background animation.
- Heavy blur effects that impact battery life.
- Animations that delay common actions.

Prioritize performance over decoration.

---

# 13. Implementation Instructions

Follow this order.

## Step 1 — Codebase Audit

Inspect:
- Existing Compose screens.
- Navigation architecture.
- Theme and design system.
- Playback state connections.
- Current mini-player implementation.
- Equalizer and visualizer UI.
- Existing settings.
- Widget integration.

Identify which layouts are causing excessive empty space, clipping, or inconsistent spacing.

## Step 2 — Build the Shared Design System

Implement:
- Color palette.
- Typography.
- Spacing.
- Reusable headers.
- Buttons.
- Cards.
- Navigation.
- Mini-player.
- Empty states.

## Step 3 — Navigation Refactor

Convert five primary destinations into three.

Preserve all existing feature routes.

Ensure existing deep links and shortcuts still resolve correctly.

## Step 4 — Redesign Library and Playing

Prioritize everyday music-listening experience.

Verify populated and empty states separately.

## Step 5 — Redesign Audio

Redesign:
- Equalizer.
- Visualizer.
- Audio settings access.

Keep all existing DSP and visualization functionality.

## Step 6 — Redesign Settings

Apply the shared design system.

## Step 7 — Visual Validation

Build the app and capture emulator screenshots if an emulator is available.

Validate:
- Library with music.
- Library without music.
- Playlists.
- Now Playing with music.
- Now Playing without music.
- 10-band EQ.
- 31-band EQ.
- Parametric EQ.
- All six visualizers.
- Settings.

Review alignment, spacing, readability, clipping, and navigation.

If screenshot testing is unavailable, explicitly report that limitation.

## Step 8 — Regression Testing

Verify:
- MP3 and FLAC playback.
- Background playback.
- Playlists.
- Music scanning.
- Equalizer DSP.
- EQ preset loading.
- AutoEq.
- Visualizer rendering.
- Lyrics.
- Widgets.
- Audio-output settings.

Do not modify unrelated business logic without a justified reason.

---

# 14. Acceptance Criteria

The redesign is complete when:

- The app uses a consistent minimalist design.
- Bottom navigation contains Library, Playing, and Audio.
- All original features remain accessible.
- No screen has excessive layout-generated whitespace.
- Music rows and album cards are visually consistent.
- The Now Playing screen focuses on artwork and playback.
- A functional mini-player appears when music is loaded.
- The Equalizer defaults to 10-band mode for new users.
- All EQ bands are accessible and correctly aligned.
- Advanced EQ options remain available.
- The Visualizer has an immersive presentation.
- All six visualization modes remain operational.
- Empty states look intentional and provide useful actions.
- Common phone sizes and text scaling are supported.
- Playback state remains consistent between screens.
- No audio functionality is removed.
- No network permission is introduced.
- The project builds successfully.
- All executed tests and their results are reported accurately.

---

# 15. Final Restrictions

Do NOT:
- Rebuild the entire application from scratch.
- Replace the existing playback engine.
- Delete user playlists or settings.
- Remove advanced audio features.
- Remove widget support.
- Add cloud synchronization.
- Add advertisements.
- Add unnecessary permissions.
- Use fake music entries in production.
- Use nonfunctional UI controls.
- Mark unfinished visual changes as complete.

Preserve all user data.

If database or navigation migrations are needed, implement them safely.

Update the app version according to the existing versioning convention.

---

# 16. Final Delivery

After implementation, provide:

1. Summary of UI changes.
2. Screens redesigned.
3. Shared UI components created.
4. Navigation changes.
5. Modified source files.
6. Build and test results.
7. Known limitations.
8. Remaining UI issues, if any.

Do not stop at a written implementation plan.

Implement the redesign, validate the application, and fix identified layout defects.

**FINAL DESIGN GOAL**

Create a premium offline HiFi Music Player that feels effortless during everyday listening and powerful when accessing advanced audio features.

The interface must be clean, balanced, responsive, intuitive, and visually cohesive.

Remember: Simple on the surface, powerful underneath.
