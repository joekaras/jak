# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A self-contained date/datetime picker (`DatePicker.Web/Scripts/bootstrap-datetimepicker.js` + `Content/bootstrap-datetimepicker.css`) that replaces the legacy `Bootstrap.v3.Datetimepicker.CSS` NuGet package. It targets Bootstrap 5, jQuery is optional, and there is no Moment.js: every date in or out is a native `Date`. The ASP.NET project (`DatePicker.Web.csproj`, .NET Framework 4.7.2, IIS Express on https://localhost:44349/) only hosts static files. There is no server-side code and no JS build tooling (no package.json).

There is one copy of the component and no minified build. Don't reintroduce hand-maintained `.min` or alias copies; an earlier generated `.min.css` was silently corrupt. New files have to be added to `DatePicker.Web.csproj` as `<Content Include=...>` to ship.

## Running / testing

- Demo: `DatePicker.Web/index.html`. Tests: `DatePicker.Web/test.html`. Manual test bench: `DatePicker.Web/playground.html` (options form → rebuilds the picker; the setup is JSON in the URL hash). All three load the unminified files.
- Page CSS must not reuse the picker's class names (`dow`, `day`, `month`, `year`, `picker-switch`, `timepicker-*`, …). The widget is appended to `document.body`, so page rules like `.dow {}` restyle it.
- `test.html` runs on load, renders PASS/FAIL lines plus a summary into `#results`, and sets `window.testResults` / `window.testsDone`.
- Headless: `chrome --headless=new --dump-dom file:///C:/src/DatePicker/DatePicker.Web/test.html`, then grep for `FAIL`/`ERROR`/`Summary`. Inside Claude Code's sandbox this returns empty output. Instead, serve the folder on localhost (a few-line Node `http` server works; the Chrome extension can't open `file://`) and read `window.testResults` through the claude-in-chrome MCP.
- Tests are hand-rolled `assert(cond, msg)` calls in one `runTests()`, not a framework. There are helpers for `fire(elem, type)`, `act(widget, action, attrs)` (injects and clicks a `data-action` cell), and `ymd(date)`. To add a test, append asserts inside `runTests()`.

## Architecture of the component

It's one UMD IIFE (AMD / CommonJS / globals `DateTimePicker` and `DatePicker`). The factory receives `$`, which may be null.

- **Module-level helpers**: `parseCustomFormat` (token-to-regex parser), `parseStandardDateString` (ISO and plain-time only; deliberately no `Date.parse` fallback), `toNativeDate` (the single entry point for any input: Date, number, or string), and `formatDate`.
- **`DateTimePicker(element, options)`**: a closure-based constructor, not a prototype. State (`currentDate`, `viewDate`, the current view, `hasDate`/`hasTime`/`use24Hours` from `detectParts()`) lives in closure variables. Public methods are assigned onto `self`. The instance is stored on `el._dateTimePicker` and on jQuery data under both `'DateTimePicker'` and `'datepicker'`.
- **Rendering**: `render()` rebuilds the widget's HTML from `buildDaysView` / `buildMonthsView` / `buildYearsView` / `buildDecadesView` / `buildTimePicker` / `buildToolbar`. Clicks are delegated: elements carry `data-action` (plus `data-day` and similar), and `handleAction(action, target)` is the one dispatcher.
- **Value writes** all go through `setValue()`. For date-only formats it truncates the time to midnight. It checks `isDateDisabled` and then no-ops if the time is unchanged. Otherwise it writes the input, fires native `input`/`change`, and triggers `dp.change`. The input's own `change` listener ignores the echo of that write, so `dp.change` fires once per real change.
- **Element listeners** are registered through `listen()` so `destroy()` can remove them.
- **Keyboard**: `onInputKeydown` handles Enter/Escape, and while the day grid is open maps arrows/PageUp/PageDown to `stepDate(days, months)`, which keeps the time of day, clamps month-end, and goes through `setValue`.
- **Events**: `triggerEvent` fires each `dp.*` event as a jQuery event (`e.date`, `e.oldDate`) and as a DOM `CustomEvent` (`e.detail.date`). They don't double up: jQuery parses `'dp.change'` as type `dp` with namespace `change`, so jQuery handlers never see the native `dp.change` event.
- **jQuery bridge** (end of file): `$.fn.datetimepicker(options | 'method', ...args)`, aliased as `$.fn.datepicker`. `$.fn.datetimepicker.defaults` is the shared `DEFAULTS` object.

The option, method, and event reference is in `README.md`.
