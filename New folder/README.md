# DatePicker & DateTimePicker (no dependencies)

A self-contained date/datetime picker that replaces the legacy `Bootstrap.v3.Datetimepicker.CSS` NuGet package. It styles itself with Bootstrap 5 classes, works with jQuery 3.7+ (optional), and has no other dependencies. Moment.js is **not** used: every date going in or out is a native `Date`.

---

## Features

- **Two APIs**: jQuery plugin `$('#picker').datetimepicker(options)` (alias `.datepicker`), or `new DateTimePicker(element, options)` (global alias `DatePicker`).
- **Built-in format tokens**: `YYYY` `YY` · `MMMM` `MMM` `MM` `M` · `DD` `D` · `HH` `H` (24h) · `hh` `h` `A` `a` (12h) · `mm` `m` · `ss` `s` · `L` `LT` `LTS` · `[literal text]`. `YY` uses moment's pivot (`00–68` → 20xx, `69–99` → 19xx).
- **Modes**: date only, time only (12h or 24h), date + time (toggle or `sideBySide`).
- **Views**: days, months, years, decades.
- **Constraints**: `minDate`, `maxDate`, `daysOfWeekDisabled`, `disabledDates`, `enabledDates`.
- **Keyboard**: while the popup is open on the day grid, ←/→ move ±1 day, ↑/↓ move ±7 days, PageUp/PageDown move ±1 month (month-end clamped, time of day kept). Enter/Escape close.
- **Events**: `dp.change`, `dp.show`, `dp.hide`, `dp.error`.

---

## Files

```text
DatePicker.Web/
├── Content/bootstrap-datetimepicker.css   # Component stylesheet
├── Scripts/bootstrap-datetimepicker.js    # Component source
├── index.html                             # Interactive demo
├── playground.html                        # Manual test bench for any option combination
└── test.html                              # Test suite
```

No minified builds ship: without a build step they'd be hand-synced copies. Rely on IIS compression, or add a minifier when there's a build.

---

## Quick start

```html
<link rel="stylesheet" href="Content/bootstrap.min.css">
<link rel="stylesheet" href="Content/bootstrap-datetimepicker.css">

<script src="Scripts/jquery-3.7.1.min.js"></script>          <!-- optional -->
<script src="Scripts/bootstrap-datetimepicker.js"></script>

<div class="input-group date" id="myPicker">
    <input type="text" class="form-control">
    <span class="input-group-text"><!-- calendar icon --></span>
</div>
```

```javascript
$('#myPicker').datetimepicker({ format: 'YYYY-MM-DD', showTodayButton: true });
// or
const picker = new DateTimePicker(document.getElementById('myPicker'), { format: 'YYYY-MM-DD' });
```

When the picker element is an `.input-group`, the addon (`.input-group-text`, `.input-group-addon`, or `[data-toggle="datetimepicker"]`) opens it. Focusing or clicking the text box opens it only when `allowInputToggle: true` or when the picker is attached directly to the `<input>`. This matches the legacy plugin.

---

## Options

| Option | Type | Default | Description |
|---|---|---|---|
| `format` | `string` | `'YYYY-MM-DD'` | Display/parse format. Its tokens decide whether date and/or time pickers appear. |
| `stepping` | `number` | `1` | Minute step for the ▲/▼ buttons. The minute grid uses it too (5 when `1`). |
| `minDate` / `maxDate` | `Date\|string\|false` | `false` | Selectable bounds. For date+time formats, picking the boundary day clamps the time into range. |
| `useCurrent` | `boolean` | `true` | On `show()`, fill in the current date/time if the value is empty. |
| `defaultDate` | `Date\|string\|false` | `false` | Initial value. |
| `daysOfWeekDisabled` | `number[]` | `false` | Day indices to disable (0 = Sunday). |
| `disabledDates` / `enabledDates` | `Array` | `false` | Blacklist / whitelist of days. |
| `viewMode` | `'days'\|'months'\|'years'\|'decades'` | `'days'` | View shown on open. |
| `collapse` | `boolean` | `true` | Date+time: switch to the time screen after picking a day. |
| `sideBySide` | `boolean` | `false` | Date+time: show calendar and time together. |
| `showTodayButton` / `showClear` / `showClose` | `boolean` | `false` | Toolbar buttons. |
| `toolbarPlacement` | `'default'\|'top'` | `'default'` | Toolbar above or below the calendar. |
| `inline` | `boolean` | `false` | Render inside the element instead of as a popup. |
| `keepOpen` | `boolean` | `false` | Don't close after picking a date. |
| `allowInputToggle` | `boolean` | `false` | Open on text-box focus/click for input-group pickers. |
| `focusOnShow` | `boolean` | `true` | Focus the input when the popup opens. |
| `widgetParent` | `string\|Element\|null` | `null` | Where to append the popup (default `document.body`). |
| `datepickerInput` | `string` | `'.datepickerinput'` | Selector for the input inside a non-input element (falls back to the first `input`). |
| `icons` | `object` | glyphicon names | Per-icon override: CSS class names (HTML-escaped, never raw markup). Glyphicon names map to built-in SVGs. |
| `tooltips` | `object` | English strings | Per-button title text. |

**Unsupported legacy options** (accepted but ignored): `timeZone`, `locale`, `extraFormats`, `useStrict`, `keepInvalid`, `calendarWeeks`, `dayViewHeaderFormat`, `widgetPositioning`, `ignoreReadonly`, `disabledTimeIntervals`, `disabledHours`, `enabledHours`, `viewDate`.

---

## Methods

Call on the instance (`picker.method()`, `$('#p').data('DateTimePicker').method()`) or through jQuery (`$('#p').datetimepicker('method', ...)`). Setters return the instance.

- `date([value])`: get the value (`Date` or `null`), or set it from a `Date`, string, timestamp, or `null`.
- `show()`, `hide()`, `toggle()`, `clear()`
- `enable()`, `disable()`
- `destroy()`: removes the widget, unbinds its listeners, and removes instance data.
- `minDate([v])`, `maxDate([v])`, `defaultDate([v])`, `format([v])`, `viewMode([v])`
- `disabledDates([v])`, `enabledDates([v])`, `daysOfWeekDisabled([v])`
- `options([obj])`: get a copy of the options, or merge new ones.

---

## Events

Each event is triggered on the picker element twice: once through jQuery (if present) and once as a DOM `CustomEvent`. They reach different listeners, so a handler never sees both.

| Event | jQuery (`e.*`) | DOM (`e.detail.*`) | When |
|---|---|---|---|
| `dp.change` | `date`, `oldDate` | `date`, `oldDate` | The value actually changed (once per change). |
| `dp.show` / `dp.hide` | `date` | `date` | Popup opened / closed. |
| `dp.error` | `date` (the rejected input) | `date` | Unparseable or disabled value. |

`date` / `oldDate` are native `Date` objects or `null`.

```javascript
$('#myPicker').on('dp.change', e => console.log(e.date));
document.getElementById('myPicker').addEventListener('dp.change', e => console.log(e.detail.date));
```

---

## Testing

- Demo: `DatePicker.Web/index.html`.
- Playground: `DatePicker.Web/playground.html`. Set any option and the picker rebuilds with it. It shows the live value, `date()`, a `dp.change` counter, an event log, buttons for every instance method, and the matching init code. The setup is saved in the URL hash, so you can bookmark or share a link to a failing case.
- Tests: `DatePicker.Web/test.html` renders PASS/FAIL lines and exposes `window.testResults` / `window.testsDone`. All 67 pass.
