---
name: RMO Reporting
description: Self-contained migration readiness reports for bank stakeholders, built to work as a file, in email and on paper.
colors:
  navy: "#2d3561"
  navy-deep: "#232a4e"
  navy-ink: "#dfe3f5"
  ready: "#0a7d32"
  ready-tint: "#e6f3ea"
  impact: "#8a5300"
  impact-fill: "#b26a00"
  impact-tint: "#fff3e0"
  fail: "#b00020"
  fail-tint: "#fbe9ec"
  ink: "#1a1a2e"
  muted: "#5f5f70"
  line: "#e2e3ea"
  header-cell: "#fafafc"
  canvas: "#f4f5f7"
  card: "#ffffff"
typography:
  figure:
    fontFamily: "'Segoe UI', system-ui, -apple-system, Arial, sans-serif"
    fontSize: "34px"
    fontWeight: 600
    lineHeight: 1
    fontFeature: "tnum"
  headline:
    fontFamily: "'Segoe UI', system-ui, -apple-system, Arial, sans-serif"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: "-0.01em"
  stat:
    fontFamily: "'Segoe UI', system-ui, -apple-system, Arial, sans-serif"
    fontSize: "22px"
    fontWeight: 600
    fontFeature: "tnum"
  lede:
    fontFamily: "'Segoe UI', system-ui, -apple-system, Arial, sans-serif"
    fontSize: "18px"
    fontWeight: 400
  title:
    fontFamily: "'Segoe UI', system-ui, -apple-system, Arial, sans-serif"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: 1.3
  body:
    fontFamily: "'Segoe UI', system-ui, -apple-system, Arial, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.45
  label:
    fontFamily: "'Segoe UI', system-ui, -apple-system, Arial, sans-serif"
    fontSize: "12px"
    fontWeight: 600
  mono:
    fontFamily: "Consolas, 'Cascadia Mono', monospace"
    fontSize: "12.5px"
rounded:
  key: "2px"
  focus: "3px"
  button: "6px"
  bar: "7px"
  card: "10px"
  pill: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "14px"
  lg: "20px"
  xl: "24px"
components:
  page-head:
    backgroundColor: "{colors.navy}"
    textColor: "{colors.card}"
    typography: "{typography.headline}"
    rounded: "{rounded.card}"
    padding: "22px 24px 20px"
  card:
    backgroundColor: "{colors.card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "18px 20px"
  impact-strip:
    backgroundColor: "{colors.impact-tint}"
    textColor: "{colors.impact}"
    rounded: "{rounded.card}"
    padding: "16px 20px"
  impact-strip-clear:
    backgroundColor: "{colors.ready-tint}"
    textColor: "{colors.ready}"
  button-open:
    backgroundColor: "{colors.navy}"
    textColor: "{colors.card}"
    rounded: "{rounded.button}"
    padding: "7px 14px"
  button-open-hover:
    backgroundColor: "{colors.navy-deep}"
  tag:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.muted}"
    rounded: "{rounded.pill}"
    padding: "1px 8px"
  tag-impact:
    backgroundColor: "{colors.impact-tint}"
    textColor: "{colors.impact}"
  tag-ok:
    backgroundColor: "{colors.ready-tint}"
    textColor: "{colors.ready}"
  tag-fail:
    backgroundColor: "{colors.fail-tint}"
    textColor: "{colors.fail}"
  count:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.muted}"
    rounded: "{rounded.pill}"
    padding: "1px 8px"
  table-header-cell:
    backgroundColor: "{colors.header-cell}"
    textColor: "{colors.muted}"
    typography: "{typography.label}"
    padding: "8px 10px"
---

# Design System: RMO Reporting

## Overview

**Creative North Star: "The Sign-off Brief"**

A quiet, businesslike report that a bank stakeholder can read in one viewport and sign. A navy header band names the page and its provenance; white cards sit on a cool grey canvas; colour is spent almost entirely on status. Green means a report converts, amber means a delivery is affected, red means a report will not convert. Everything else is ink, muted grey and hairlines, so the status colours read as data, not decoration.

Density is moderate and table-led: 14px body, tabular figures, hairline row dividers and pill tags that annotate a cell without shouting. Depth is a soft ambient lift on cards and nothing more. Every page is a single self-contained HTML file with inline CSS, no JavaScript, no external fonts or images, and inline SVG for the few icons, so the system relies on the platform font stack and CSS-only disclosure. Paper is a first-class target: print rules strip the canvas and shadows, force status colour to print, and repeat collapsed detail in a printable form.

**Key Characteristics:**
- Navy header band (rounded, full width of the 1120px column) carrying title and a muted provenance line.
- White cards on a cool grey canvas, one 14px rhythm for card gaps and grid gutters.
- Status colours used only for status: ready, impact, not converting, each with a pale tint for fills.
- Tabular figures everywhere a number is compared.
- Pill tags and counts, hairline tables, CSS-only disclosure with an inline-SVG chevron.
- No JavaScript, no external assets; prints legibly with collapsed content repeated for paper.

## Colors

A restrained navy-and-grey frame whose only saturated colours are the three status hues.

### Primary
- **Ledger Navy** (`navy`): the header band, section titles, links, the focus ring, text selection and the solid "open" button. It is the frame's voice, never a status.
- **Ledger Navy Deep** (`navy-deep`): hover state of the solid navy button only.
- **Navy Mist** (`navy-ink`): the provenance line on the navy header band.

### Secondary (status)
- **Ready Green** (`ready`): reports that convert; the convert half of the split bar, the headline converted figure, ready tags and legend keys. **Ready Wash** (`ready-tint`) fills ready tags and the all-clear strip.
- **Impact Amber** (`impact`): text on amber fills (impact strip, enabled-schedule tag, trailing-space marks). It is darker than the world's nominal amber so text clears contrast on the tint. **Signal Amber** (`impact-fill`) is the nominal amber; on the new surfaces it appears only as the translucent ring around the impact strip. **Amber Wash** (`impact-tint`) is the strip and tag fill.
- **Stop Red** (`fail`): reports that will not convert; the fail half of the split bar, reason bars, reason-code links in the jobs table, fail tags. **Stop Wash** (`fail-tint`) is the track behind reason bars and the fail tag fill.

### Neutral
- **Report Ink** (`ink`): body text and h3.
- **Pencil Grey** (`muted`): secondary text, table header text, folders, notes, footer, neutral tags and counts, chevron stroke.
- **Hairline** (`line`): row dividers, disclosure dividers, the split-bar track.
- **Header Wash** (`header-cell`): table header cells.
- **Canvas Grey** (`canvas`): page background and the fill of neutral tags and counts.
- **Card White** (`card`): card surfaces and text on navy.

### Named Rules
**The Status Owns Colour Rule.** Green, amber and red are reserved for ready, impact and not converting. Never use them for decoration, branding or emphasis unrelated to status, and never use navy to signal status.

**The Tint-and-Ink Rule.** A status fill always pairs its pale tint with its own ink (amber wash with impact amber, ready wash with ready green, stop wash with stop red). Status text never sits on a saturated fill.

## Typography

**Display Font:** Segoe UI (with system-ui, -apple-system, Arial, sans-serif)
**Body Font:** Segoe UI (same stack)
**Label/Mono Font:** Consolas (with Cascadia Mono, monospace) for expressions and code values

**Character:** One platform sans throughout, chosen because the files may not load external fonts. Hierarchy comes from size and weight 600 alone, with tabular figures carrying the numbers.

### Hierarchy
- **Figure** (600, 34px, line-height 1, tabular): the one headline number on a dashboard (reports that convert), in ready green.
- **Headline** (600, 24px, 1.2, -0.01em): page title in the navy band; 21px under 720px.
- **Stat** (600, 22px, tabular): figures in each detail section's summary stats, navy.
- **Lede** (400, 18px): the summary sentence that opens a detail report; the impact strip title uses 18px at weight 600.
- **Title** (600, 16px, 1.3): card section titles in navy. Disclosure group summaries use 15px at 600.
- **Body** (400, 14px, 1.45): running text and table cells. Sub-heads (h3) are 13px at 600 in ink.
- **Label** (600, 12px): table header cells in Pencil Grey; tags are 11.5px at 600; notes are 12.5px at 400.
- **Mono** (400, 12.5px): query expressions and literal values.

### Named Rules
**The Tabular Figures Rule.** Any number a reader compares (counts, percentages, IDs in tables, figures) uses `font-variant-numeric: tabular-nums` and right-aligns in tables.

**The Sentence Case Rule.** Titles, labels and table headers are sentence case at their natural tracking. No uppercase tracked labels.

## Layout

A single centred column, max 1120px, with 24px page padding (12px under 720px). Header band, then content, then a centred muted footer stating when and from which discovery the file was generated.

The 14px step (`spacing.md`) is the rhythm: gap between cards, grid gutter, header-to-content gap. Cards pad 18px by 20px (14px on small screens); table cells pad 8px by 10px (7px by 8px on small screens).

The dashboard uses an asymmetric two-column grid, 7fr main and 5fr side, collapsing to one column at 860px. When nothing is affected the grid collapses to one column and the side cards sit two-up. The detail sections below the summary are a single column, each opening with a section heading (20px, hairline rule, colour key). Under 720px, key-value fact lists stack to one column and appendix tables reflow into labelled blocks (each cell prefixed with its column name). Wide tables scroll horizontally inside a wrapper instead of shrinking.

Print: white background, no page padding, no max width, cards avoid page breaks, interactive drill sections are hidden and replaced by print-only repeats; "open" buttons print as navy text followed by their link target.

## Elevation & Depth

Flat surfaces with a single soft ambient lift on cards; the header band and impact strip carry no drop shadow. The impact strip is edged with a 1px inset ring in its own status hue instead of a border. All shadows are removed in print.

### Shadow Vocabulary
- **Card lift** (`box-shadow: 0 1px 2px rgba(26,26,46,.06), 0 2px 8px rgba(26,26,46,.05)`): every white card.
- **Status ring** (`box-shadow: inset 0 0 0 1px rgba(178,106,0,.22)`, or `rgba(10,125,50,.2)` for the all-clear variant): the impact strip's edge.

### Named Rules
**The One Lift Rule.** Cards get the card lift and nothing gets more. No hover elevation, no hard offset shadows, no stacked depth levels.

## Shapes

Gently rounded containers (10px) for the header band, cards and the impact strip; 6px for solid buttons; full pills for tags and counts; 7px ends on the split bar and 2px on legend keys and reason bars. Separation inside a card is by 1px hairlines, never by coloured side or top stripes. The split bar uses a 2px gap between segments instead of a divider.

## Components

### Buttons
- **Shape:** gently rounded (6px).
- **Primary ("open" link-button):** solid navy, white 13px weight-600 text, padding 7px by 14px, no underline. Used on the summary to jump to a detail section; hidden in print.
- **Hover / Focus:** background deepens to Ledger Navy Deep; focus uses the global 2px navy outline offset 2px.
- **Print:** loses its fill and prints as navy text followed by the link target in muted grey.

### Chips (tags and counts)
- **Style:** pill (999px), 11.5px weight-600 text, padding 1px by 8px, sitting 6px after the text it annotates.
- **Variants:** neutral (Canvas Grey on Pencil Grey), impact (amber wash on impact amber, e.g. an enabled schedule that will break), ok, fail.
- **Count:** the same pill at 12px, tabular, minimum 24px wide, placed after a card title or at the far end of a disclosure summary.

### Cards / Containers
- **Corner Style:** 10px.
- **Background:** Card White on Canvas Grey.
- **Shadow Strategy:** card lift (see Elevation & Depth).
- **Border:** none.
- **Internal Padding:** 18px by 20px; 14px on small screens.

### Navigation
There is no navigation chrome. Everything is one page: the summary jumps to its detail sections with solid "open" buttons and in-page anchor links (navy, 1px underline offset 3px, thickening to 2px on hover).

### Page head
The navy band: 24px weight-600 title in white, then a 13px Navy Mist provenance line (holding company, wave, environment, discovery time) separated by middots. No label or tagline above the title.

### Impact strip
A full-width amber-wash band directly under the page head, with a 26px inline-SVG warning triangle in currentColor, an 18px title stating the delivery impact and a one-line explanation in dark amber. The all-clear variant swaps to ready wash, ready green and a check-circle icon.

### Readiness split bar
A 14px pill track split into ready and fail segments with a 2px gap, each at least 4px wide, with a legend of 10px square keys, labels and muted percentages. Paired with the figure-sized ready count.

### Reason list
Rows of reason name (an anchor) and a right-aligned weight-600 tabular count, each over a 4px Stop Wash track filled in Stop Red proportional to the count, separated by hairlines.

### Entry card
A card whose title is preceded by a 10px status key square (ready or fail) matching the legend, a muted one-line summary and a solid "open" button.

### Disclosure (details/summary)
CSS-only `<details>`. The native marker is hidden and replaced by a 14px inline-SVG chevron (Pencil Grey stroke) that rotates 90 degrees when open over .18s `cubic-bezier(.2,.8,.2,1)`; the summary title underlines on hover. Group summaries are 15px weight-600 navy with the count pushed to the far end; nested items are divided by hairlines and their bodies indent 22px (no indent on small screens). Collapsed content always has a print-only equivalent.

### Data table
Full width, collapsed borders, hairline row dividers, last row undivided, header cells in Header Wash with 12px weight-600 Pencil Grey labels, numeric columns right-aligned and non-wrapping.

### Trailing-space mark
Invisible trailing spaces inside literals are shown as middots in an amber-wash mark with a 2px radius, titled with the space count.

## Do's and Don'ts

### Do:
- **Do** ship every customer page as one HTML file with inline CSS, inline SVG icons, the Segoe UI system stack and no JavaScript, external fonts, scripts or images.
- **Do** give every collapsed `<details>` block a print-only equivalent and force status colours to print with `print-color-adjust: exact`.
- **Do** use the 14px rhythm for card gaps and grid gutters, 10px corners on cards and bands, and pills for tags and counts.
- **Do** pair each status tint with its own status ink, and use impact amber (not the nominal fill amber) for text on amber wash.
- **Do** use tabular figures for every compared number and right-align numeric table columns.
- **Do** mark disclosure with the rotating inline-SVG chevron.

### Don't:
- **Don't** use green, amber or red for anything but ready, impact and not converting.
- **Don't** put a label, eyebrow or tagline above the page title in the navy band.
- **Don't** use coloured border-left or border-top stripes on cards, callouts or tiles; separate with hairlines and tint fills.
- **Don't** use Unicode glyphs (such as a triangle character) as disclosure markers or icons; use inline SVG.
- **Don't** add shadows beyond the card lift, hover elevation or hard offset shadows.
- **Don't** use uppercase tracked labels.
