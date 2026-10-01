/*!
 * Modern DateTimePicker & DatePicker Component
 * Drop-in replacement for Bootstrap Datetime Picker (Bootstrap.v3.Datetimepicker.CSS)
 * Fully compatible with Bootstrap 5, Bootstrap 4, and standalone environments.
 * No dependencies; jQuery is optional. Dates are always native Date objects.
 * Licensed under MIT
 */
(function (root, factory) {
    'use strict';
    if (typeof define === 'function' && define.amd) {
        define(['jquery'], function (jQuery) {
            return factory(jQuery || root.jQuery);
        });
    } else if (typeof exports === 'object') {
        var jq;
        try { jq = require('jquery'); } catch (e) { jq = (typeof window !== 'undefined' ? window.jQuery : null); }
        module.exports = factory(jq);
    } else {
        var picker = factory(root.jQuery || root.$);
        root.DateTimePicker = picker;
        root.DatePicker = picker;
    }
}(typeof window !== 'undefined' ? window : this, function ($) {
    'use strict';

    // Built-in inline SVGs for zero-dependency icon rendering (works seamlessly with Bootstrap 5)
    var SVG_ICONS = {
        previous: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0z"/></svg>',
        next: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z"/></svg>',
        up: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M7.646 4.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1-.708.708L8 5.707l-5.646 5.647a.5.5 0 0 1-.708-.708l6-6z"/></svg>',
        down: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z"/></svg>',
        time: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M8 3.5a.5.5 0 0 0-1 0V9a.5.5 0 0 0 .252.434l3.5 2a.5.5 0 0 0 .496-.868L8 8.71V3.5z"/><path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm7-8A7 7 0 1 1 1 8a7 7 0 0 1 14 0z"/></svg>',
        date: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M3.5 0a.5.5 0 0 1 .5.5V1h8V.5a.5.5 0 0 1 1 0V1h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h1V.5a.5.5 0 0 1 .5-.5zM1 4v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V4H1z"/></svg>',
        today: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"/><path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3.5a.5.5 0 0 1-.5-.5v-4A.5.5 0 0 1 8 4z"/></svg>',
        clear: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/><path fill-rule="evenodd" d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/></svg>',
        close: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8 2.146 2.854Z"/></svg>'
    };

    var MONTH_NAMES = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];
    var MONTH_NAMES_SHORT = [
        'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
        'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
    ];
    var DAY_NAMES_SHORT = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];

    function padZero(n, len) {
        var str = String(n);
        while (str.length < (len || 2)) {
            str = '0' + str;
        }
        return str;
    }

    function copyDate(d) {
        return d ? new Date(d.getTime()) : null;
    }

    function escapeHtml(s) {
        return String(s == null ? "" : s).replace(/[&<>"']/g, function (c) {
            return { "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;" }[c];
        });
    }

    function escapeRegex(s) {
        return s.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
    }

    function expandShortcuts(fmt) {
        if (!fmt) return 'YYYY-MM-DD';
        return fmt.replace(/\bLTS\b/g, 'h:mm:ss A')
                  .replace(/\bLT\b/g, 'h:mm A')
                  .replace(/\bL\b/g, 'MM/DD/YYYY');
    }

    /**
     * Robust date parser supporting custom Moment-style format strings,
     * ISO strings, US and International date and time formats.
     */
    function parseCustomFormat(str, fmt, baseDate) {
        if (!str || !fmt) return null;
        var format = expandShortcuts(fmt);

        var tokenPatterns = {
            'YYYY': '(\\d{4})',
            'YY': '(\\d{2})',
            'MMMM': '([a-zA-Z]+)',
            'MMM': '([a-zA-Z]+)',
            'MM': '(\\d{1,2})',
            'M': '(\\d{1,2})',
            'DD': '(\\d{1,2})',
            'D': '(\\d{1,2})',
            'HH': '(\\d{1,2})',
            'H': '(\\d{1,2})',
            'hh': '(\\d{1,2})',
            'h': '(\\d{1,2})',
            'mm': '(\\d{1,2})',
            'm': '(\\d{1,2})',
            'ss': '(\\d{1,2})',
            's': '(\\d{1,2})',
            'A': '(AM|PM)',
            'a': '(am|pm)'
        };

        var tokenRegex = /(\[[^\]]*\])|(YYYY|YY|MMMM|MMM|MM|M|DD|D|HH|H|hh|h|mm|m|ss|s|A|a)/g;
        var match;
        var lastIndex = 0;
        var regexStr = '^\\s*';
        var tokens = [];

        while ((match = tokenRegex.exec(format)) !== null) {
            var literal = format.slice(lastIndex, match.index);
            if (literal) regexStr += escapeRegex(literal).replace(/\s+/g, '\\s+');
            if (match[1]) {
                regexStr += escapeRegex(match[1].slice(1, -1));
            } else if (match[2]) {
                regexStr += tokenPatterns[match[2]];
                tokens.push(match[2]);
            }
            lastIndex = tokenRegex.lastIndex;
        }
        var tail = format.slice(lastIndex);
        if (tail) regexStr += escapeRegex(tail).replace(/\s+/g, '\\s+');
        regexStr += '\\s*$';

        var m;
        try {
            m = str.match(new RegExp(regexStr, 'i'));
        } catch (e) {
            return null;
        }
        if (!m) return null;

        var base = baseDate || new Date();
        var year = null, month = null, day = null;
        var hour = 0, minute = 0, second = 0, ampm = null;

        for (var i = 0; i < tokens.length; i++) {
            var tok = tokens[i];
            var val = m[i + 1];
            if (!val) continue;

            if (tok === 'YYYY') {
                year = parseInt(val, 10);
            } else if (tok === 'YY') {
                var yy = parseInt(val, 10);
                year = yy < 69 ? 2000 + yy : 1900 + yy; // moment's pivot
            } else if (tok === 'MMMM') {
                var lVal = val.toLowerCase();
                for (var mi = 0; mi < 12; mi++) {
                    if (MONTH_NAMES[mi].toLowerCase() === lVal) { month = mi; break; }
                }
            } else if (tok === 'MMM') {
                var sVal = val.toLowerCase();
                for (var si = 0; si < 12; si++) {
                    if (MONTH_NAMES_SHORT[si].toLowerCase() === sVal) { month = si; break; }
                }
            } else if (tok === 'MM' || tok === 'M') {
                month = parseInt(val, 10) - 1;
            } else if (tok === 'DD' || tok === 'D') {
                day = parseInt(val, 10);
            } else if (tok === 'HH' || tok === 'H') {
                hour = parseInt(val, 10);
            } else if (tok === 'hh' || tok === 'h') {
                hour = parseInt(val, 10);
            } else if (tok === 'mm' || tok === 'm') {
                minute = parseInt(val, 10);
            } else if (tok === 'ss' || tok === 's') {
                second = parseInt(val, 10);
            } else if (tok === 'A' || tok === 'a') {
                ampm = val.toUpperCase();
            }
        }

        if (ampm === 'PM' && hour < 12) hour += 12;
        if (ampm === 'AM' && hour === 12) hour = 0;

        if (year === null && month === null && day === null) {
            year = base.getFullYear();
            month = base.getMonth();
            day = base.getDate();
        } else {
            if (year === null) year = base.getFullYear();
            if (month === null) month = 0;
            if (day === null) day = 1;
        }

        var res = new Date(year, month, day, hour, minute, second);
        if (res.getFullYear() !== year || res.getMonth() !== month || res.getDate() !== day) {
            return null; // invalid calendar date like 31-Feb
        }
        return res;
    }

    function parseStandardDateString(str, baseDate) {
        if (!str) return null;
        var trimmed = str.trim();
        if (!trimmed) return null;

        // Try YYYY-MM-DD or YYYY/MM/DD
        var mIso = trimmed.match(/^(\d{4})[-\/.](\d{1,2})[-\/.](\d{1,2})(?:[ T](\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?/);
        if (mIso) {
            var y = parseInt(mIso[1], 10);
            var m = parseInt(mIso[2], 10) - 1;
            var d = parseInt(mIso[3], 10);
            var hr = mIso[4] ? parseInt(mIso[4], 10) : 0;
            var mn = mIso[5] ? parseInt(mIso[5], 10) : 0;
            var sc = mIso[6] ? parseInt(mIso[6], 10) : 0;
            var resIso = new Date(y, m, d, hr, mn, sc);
            if (resIso.getFullYear() === y && resIso.getMonth() === m && resIso.getDate() === d) {
                return resIso;
            }
        }

        // Try Time only: HH:mm or hh:mm A
        var mTime = trimmed.match(/^(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?(?:\s*(AM|PM))?$/i);
        if (mTime) {
            var base = baseDate || new Date();
            var th = parseInt(mTime[1], 10);
            var tm = parseInt(mTime[2], 10);
            var ts = mTime[3] ? parseInt(mTime[3], 10) : 0;
            if (mTime[4]) {
                var ap = mTime[4].toUpperCase();
                if (ap === 'PM' && th < 12) th += 12;
                if (ap === 'AM' && th === 12) th = 0;
            }
            return new Date(base.getFullYear(), base.getMonth(), base.getDate(), th, tm, ts);
        }

        // No Date.parse fallback: it accepts junk like "1" as a real date.
        return null;
    }

    function toNativeDate(val, format, baseDate) {
        if (!val) return null;
        if (val instanceof Date) {
            return isNaN(val.getTime()) ? null : new Date(val.getTime());
        }
        if (typeof val === 'number') {
            var nd = new Date(val);
            return isNaN(nd.getTime()) ? null : nd;
        }
        if (typeof val === 'string') {
            var str = val.trim();
            if (!str) return null;

            if (format) {
                var cDate = parseCustomFormat(str, format, baseDate);
                if (cDate) return cDate;
            }

            // ISO / plain-time fallback
            return parseStandardDateString(str, baseDate);
        }
        return null;
    }

    function formatDate(d, fmt) {
        if (!d || isNaN(d.getTime())) return '';

        var format = expandShortcuts(fmt || 'YYYY-MM-DD');
        var year = d.getFullYear();
        var month = d.getMonth();
        var date = d.getDate();
        var hours24 = d.getHours();
        var hours12 = hours24 % 12 || 12;
        var minutes = d.getMinutes();
        var seconds = d.getSeconds();
        var ampm = hours24 >= 12 ? 'PM' : 'AM';

        var map = {
            'YYYY': String(year),
            'YY': String(year).slice(-2),
            'MMMM': MONTH_NAMES[month],
            'MMM': MONTH_NAMES_SHORT[month],
            'MM': padZero(month + 1),
            'M': String(month + 1),
            'DD': padZero(date),
            'D': String(date),
            'HH': padZero(hours24),
            'H': String(hours24),
            'hh': padZero(hours12),
            'h': String(hours12),
            'mm': padZero(minutes),
            'm': String(minutes),
            'ss': padZero(seconds),
            's': String(seconds),
            'A': ampm,
            'a': ampm.toLowerCase()
        };

        var tokenRegex = /(\[[^\]]*\])|(YYYY|YY|MMMM|MMM|MM|M|DD|D|HH|H|hh|h|mm|m|ss|s|A|a)/g;
        return format.replace(tokenRegex, function (match, escaped, token) {
            if (escaped) return escaped.slice(1, -1);
            return map[token] !== undefined ? map[token] : match;
        });
    }

    function isSameDay(d1, d2) {
        if (!d1 || !d2) return false;
        return d1.getFullYear() === d2.getFullYear() &&
               d1.getMonth() === d2.getMonth() &&
               d1.getDate() === d2.getDate();
    }

    // Icons are class names only (never raw HTML): the option string is escaped into a class attribute.
    // Legacy glyphicon names don't exist in Bootstrap 5, so they map to the built-in SVGs.
    function renderIcon(iconKey, customIcon) {
        if (!customIcon || String(customIcon).indexOf('glyphicon') > -1) {
            return SVG_ICONS[iconKey] || '';
        }
        return '<i class="' + escapeHtml(customIcon) + '"></i>';
    }

    // Legacy options not listed here (timeZone, extraFormats, useStrict, keepInvalid, calendarWeeks,
    // widgetPositioning, ignoreReadonly, disabledTimeIntervals, disabledHours, enabledHours, viewDate,
    // locale, dayViewHeaderFormat) are not supported and are ignored.
    var DEFAULTS = {
        format: false,
        stepping: 1,
        minDate: false,
        maxDate: false,
        useCurrent: true,
        collapse: true,
        defaultDate: false,
        disabledDates: false,
        enabledDates: false,
        icons: {
            time: 'glyphicon glyphicon-time',
            date: 'glyphicon glyphicon-calendar',
            up: 'glyphicon glyphicon-chevron-up',
            down: 'glyphicon glyphicon-chevron-down',
            previous: 'glyphicon glyphicon-chevron-left',
            next: 'glyphicon glyphicon-chevron-right',
            today: 'glyphicon glyphicon-screenshot',
            clear: 'glyphicon glyphicon-trash',
            close: 'glyphicon glyphicon-remove'
        },
        tooltips: {
            today: 'Go to today',
            clear: 'Clear selection',
            close: 'Close the picker',
            selectMonth: 'Select Month',
            prevMonth: 'Previous Month',
            nextMonth: 'Next Month',
            selectYear: 'Select Year',
            prevYear: 'Previous Year',
            nextYear: 'Next Year',
            selectDecade: 'Select Decade',
            prevDecade: 'Previous Decade',
            nextDecade: 'Next Decade',
            prevCentury: 'Previous Century',
            nextCentury: 'Next Century',
            pickHour: 'Pick Hour',
            incrementHour: 'Increment Hour',
            decrementHour: 'Decrement Hour',
            pickMinute: 'Pick Minute',
            incrementMinute: 'Increment Minute',
            decrementMinute: 'Decrement Minute',
            pickSecond: 'Pick Second',
            incrementSecond: 'Increment Second',
            decrementSecond: 'Decrement Second',
            togglePeriod: 'Toggle Period',
            selectTime: 'Select Time'
        },
        sideBySide: false,
        daysOfWeekDisabled: false,
        viewMode: 'days',
        toolbarPlacement: 'default',
        showTodayButton: false,
        showClear: false,
        showClose: false,
        widgetParent: null,
        keepOpen: false,
        focusOnShow: true,
        inline: false,
        datepickerInput: '.datepickerinput',
        allowInputToggle: false
    };

    /**
     * DateTimePicker constructor
     */
    function DateTimePicker(element, options) {
        var self = this;
        var el = (element && element.jquery) ? element[0] : (typeof element === 'string' ? document.querySelector(element) : element);
        if (!el) {
            throw new Error('DateTimePicker: Target element not found');
        }

        var $el = ($ && $(el)) ? $(el) : null;
        var input = el.tagName === 'INPUT' ? el : el.querySelector(options && options.datepickerInput ? options.datepickerInput : 'input');
        var component = el.classList.contains('input-group') ? el : (el.closest ? el.closest('.input-group') : null);

        var opts = Object.assign({}, DEFAULTS, options || {});
        if (options && options.icons) {
            opts.icons = Object.assign({}, DEFAULTS.icons, options.icons);
        }
        if (options && options.tooltips) {
            opts.tooltips = Object.assign({}, DEFAULTS.tooltips, options.tooltips);
        }

        // Determine format
        var format = opts.format;
        if (!format) {
            format = 'YYYY-MM-DD';
        }

        var hasDate, hasTime, hasSeconds, use24Hours;
        function detectParts() {
            var f = expandShortcuts(format).replace(/\[[^\]]*\]/g, ''); // ignore [literal] text
            hasDate = /[YMDd]/.test(f);
            hasTime = /[hHmsAa]/.test(f);
            hasSeconds = /s/.test(f);
            use24Hours = /H/.test(f) || !/[hAa]/.test(f);
        }
        detectParts();

        var currentDate = null;
        var viewDate = new Date();
        var currentViewMode = opts.viewMode || 'days';
        var timeScreen = !hasDate && hasTime; // if time only, default to time screen
        var timePickerView = 'picker'; // 'picker', 'hours', 'minutes'
        var isVisible = false;
        var widget = null;

        // Parse minDate and maxDate
        var minDate = opts.minDate ? toNativeDate(opts.minDate, format) : null;
        var maxDate = opts.maxDate ? toNativeDate(opts.maxDate, format) : null;

        function triggerEvent(name, data) {
            var eventData = Object.assign({ date: copyDate(currentDate) }, data || {});

            if ($el && typeof $el.trigger === 'function') {
                $el.trigger({
                    type: name,
                    date: eventData.date,
                    oldDate: eventData.oldDate
                });
            }
            if (el && typeof el.dispatchEvent === 'function') {
                var ev;
                try {
                    ev = new CustomEvent(name, { detail: eventData, bubbles: true });
                } catch (e) {
                    ev = document.createEvent('CustomEvent');
                    ev.initCustomEvent(name, true, true, eventData);
                }
                el.dispatchEvent(ev);
            }
        }

        function isDateDisabled(d) {
            if (!d) return false;
            if (minDate && d.getTime() < minDate.getTime()) {
                return true;
            }
            if (maxDate && d.getTime() > maxDate.getTime()) {
                return true;
            }
            if (opts.daysOfWeekDisabled && opts.daysOfWeekDisabled.indexOf(d.getDay()) > -1) {
                return true;
            }
            if (opts.disabledDates && Array.isArray(opts.disabledDates)) {
                for (var i = 0; i < opts.disabledDates.length; i++) {
                    var dd = toNativeDate(opts.disabledDates[i], format, viewDate);
                    if (dd && isSameDay(d, dd)) return true;
                }
            }
            if (opts.enabledDates && Array.isArray(opts.enabledDates)) {
                var enabled = false;
                for (var j = 0; j < opts.enabledDates.length; j++) {
                    var ed = toNativeDate(opts.enabledDates[j], format, viewDate);
                    if (ed && isSameDay(d, ed)) {
                        enabled = true;
                        break;
                    }
                }
                if (!enabled) return true;
            }
            return false;
        }

        function setValue(targetDate) {
            var oldDate = copyDate(currentDate);
            if (!targetDate) {
                if (!currentDate && (!input || !input.value)) return;
                currentDate = null;
                if (input) {
                    input.value = '';
                    triggerNativeInputEvents();
                }
                triggerEvent('dp.change', { date: null, oldDate: oldDate });
                render();
                return;
            }

            var nativeDate = toNativeDate(targetDate, format, currentDate || viewDate);
            if (!nativeDate || isNaN(nativeDate.getTime())) {
                triggerEvent('dp.error', { date: targetDate });
                return;
            }
            if (!hasTime) nativeDate.setHours(0, 0, 0, 0); // date-only: a stray time must not trip maxDate

            if (isDateDisabled(nativeDate)) {
                triggerEvent('dp.error', { date: targetDate });
                return;
            }

            var unchanged = currentDate && currentDate.getTime() === nativeDate.getTime();
            if (!unchanged) {
                currentDate = nativeDate;
                viewDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
            }

            // Always sync the text: format() changes and hand-typed "2026-9-29" need a rewrite
            // even when the instant is unchanged. dp.change only fires on a real change.
            var formatted = formatDate(currentDate, format);
            if (input && input.value !== formatted) {
                input.value = formatted;
                triggerNativeInputEvents();
            }

            if (!unchanged) triggerEvent('dp.change', { date: copyDate(currentDate), oldDate: oldDate });
            render();
        }

        function triggerNativeInputEvents() {
            if (!input) return;
            try {
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
            } catch (e) {}
        }

        // Build widget DOM
        function createWidget() {
            var div = document.createElement('div');
            div.className = 'bootstrap-datetimepicker-widget dropdown-menu' + (opts.sideBySide ? ' timepicker-sbs' : '');
            if (opts.inline) {
                div.className += ' inline-widget';
            }
            return div;
        }

        function buildDaysView() {
            var year = viewDate.getFullYear();
            var month = viewDate.getMonth();

            var firstDayIndex = new Date(year, month, 1).getDay();
            var lastDateCurrentMonth = new Date(year, month + 1, 0).getDate();
            var lastDatePrevMonth = new Date(year, month, 0).getDate();

            // Check if prev/next month disabled by minDate/maxDate
            var prevDisabled = false;
            if (minDate) {
                var prevMonthEnd = new Date(year, month, 0, 23, 59, 59, 999);
                if (prevMonthEnd.getTime() < minDate.getTime()) prevDisabled = true;
            }
            var nextDisabled = false;
            if (maxDate) {
                var nextMonthStart = new Date(year, month + 1, 1, 0, 0, 0, 0);
                if (nextMonthStart.getTime() > maxDate.getTime()) nextDisabled = true;
            }

            var html = '<div class="datepicker-days">';
            html += '<table class="table table-sm table-condensed">';
            html += '<thead><tr>';
            html += '<th class="prev' + (prevDisabled ? ' disabled' : '') + '" data-action="prevMonth" title="' + escapeHtml(opts.tooltips.prevMonth) + '">' + renderIcon('previous', opts.icons.previous) + '</th>';
            html += '<th class="picker-switch text-center" colspan="5" data-action="pickerSwitch" title="' + escapeHtml(opts.tooltips.selectMonth) + '">' + MONTH_NAMES[month] + ' ' + year + '</th>';
            html += '<th class="next' + (nextDisabled ? ' disabled' : '') + '" data-action="nextMonth" title="' + escapeHtml(opts.tooltips.nextMonth) + '">' + renderIcon('next', opts.icons.next) + '</th>';
            html += '</tr><tr>';

            for (var d = 0; d < 7; d++) {
                html += '<th class="dow text-center">' + DAY_NAMES_SHORT[d] + '</th>';
            }
            html += '</tr></thead><tbody>';

            var today = new Date();
            var dayCounter = 1;
            var nextMonthDayCounter = 1;

            for (var row = 0; row < 6; row++) {
                html += '<tr>';
                for (var col = 0; col < 7; col++) {
                    var cellIndex = row * 7 + col;
                    var dateObj, dayNum, cls = ['day'];

                    if (cellIndex < firstDayIndex) {
                        dayNum = lastDatePrevMonth - (firstDayIndex - cellIndex - 1);
                        dateObj = new Date(year, month - 1, dayNum);
                        cls.push('old');
                    } else if (dayCounter <= lastDateCurrentMonth) {
                        dayNum = dayCounter++;
                        dateObj = new Date(year, month, dayNum);
                    } else {
                        dayNum = nextMonthDayCounter++;
                        dateObj = new Date(year, month + 1, dayNum);
                        cls.push('new');
                    }

                    if (isSameDay(dateObj, today)) {
                        cls.push('today');
                    }
                    if (currentDate && isSameDay(dateObj, currentDate)) {
                        cls.push('active');
                    }

                    // Check day cell disabled
                    var cellStart = new Date(dateObj.getFullYear(), dateObj.getMonth(), dateObj.getDate(), 0, 0, 0, 0).getTime();
                    var cellEnd = new Date(dateObj.getFullYear(), dateObj.getMonth(), dateObj.getDate(), 23, 59, 59, 999).getTime();
                    var isCellDisabled = false;
                    if (minDate && cellEnd < minDate.getTime()) isCellDisabled = true;
                    if (maxDate && cellStart > maxDate.getTime()) isCellDisabled = true;
                    if (opts.daysOfWeekDisabled && opts.daysOfWeekDisabled.indexOf(dateObj.getDay()) > -1) isCellDisabled = true;

                    if (opts.disabledDates && Array.isArray(opts.disabledDates)) {
                        for (var i = 0; i < opts.disabledDates.length; i++) {
                            var dd = toNativeDate(opts.disabledDates[i], format, viewDate);
                            if (dd && isSameDay(dateObj, dd)) { isCellDisabled = true; break; }
                        }
                    }
                    if (opts.enabledDates && Array.isArray(opts.enabledDates)) {
                        var enabled = false;
                        for (var j = 0; j < opts.enabledDates.length; j++) {
                            var ed = toNativeDate(opts.enabledDates[j], format, viewDate);
                            if (ed && isSameDay(dateObj, ed)) { enabled = true; break; }
                        }
                        if (!enabled) isCellDisabled = true;
                    }

                    if (isCellDisabled) {
                        cls.push('disabled');
                    }

                    var dateAttr = dateObj.getFullYear() + '-' + padZero(dateObj.getMonth() + 1) + '-' + padZero(dateObj.getDate());
                    html += '<td class="' + cls.join(' ') + '" data-action="selectDay" data-day="' + dateAttr + '">' + dayNum + '</td>';
                }
                html += '</tr>';
                if (dayCounter > lastDateCurrentMonth && row >= 4) {
                    break;
                }
            }
            html += '</tbody></table></div>';
            return html;
        }

        function buildMonthsView() {
            var year = viewDate.getFullYear();
            var prevDisabled = minDate && (new Date(year - 1, 11, 31, 23, 59, 59, 999).getTime() < minDate.getTime());
            var nextDisabled = maxDate && (new Date(year + 1, 0, 1, 0, 0, 0, 0).getTime() > maxDate.getTime());

            var html = '<div class="datepicker-months">';
            html += '<table class="table table-sm table-condensed">';
            html += '<thead><tr>';
            html += '<th class="prev' + (prevDisabled ? ' disabled' : '') + '" data-action="prevYear" title="' + escapeHtml(opts.tooltips.prevYear) + '">' + renderIcon('previous', opts.icons.previous) + '</th>';
            html += '<th class="picker-switch text-center" colspan="5" data-action="pickerSwitch" title="' + escapeHtml(opts.tooltips.selectYear) + '">' + year + '</th>';
            html += '<th class="next' + (nextDisabled ? ' disabled' : '') + '" data-action="nextYear" title="' + escapeHtml(opts.tooltips.nextYear) + '">' + renderIcon('next', opts.icons.next) + '</th>';
            html += '</tr></thead><tbody><tr><td colspan="7">';

            for (var m = 0; m < 12; m++) {
                var cls = ['month'];
                if (currentDate && currentDate.getFullYear() === year && currentDate.getMonth() === m) {
                    cls.push('active');
                }
                var mEnd = new Date(year, m + 1, 0, 23, 59, 59, 999).getTime();
                var mStart = new Date(year, m, 1, 0, 0, 0, 0).getTime();
                if ((minDate && mEnd < minDate.getTime()) || (maxDate && mStart > maxDate.getTime())) {
                    cls.push('disabled');
                }
                html += '<span class="' + cls.join(' ') + '" data-action="selectMonth" data-month="' + m + '">' + MONTH_NAMES_SHORT[m] + '</span>';
            }

            html += '</td></tr></tbody></table></div>';
            return html;
        }

        function buildYearsView() {
            var year = viewDate.getFullYear();
            var decadeStart = year - (year % 10);
            var decadeEnd = decadeStart + 9;

            var prevDisabled = minDate && (new Date(decadeStart - 1, 11, 31, 23, 59, 59, 999).getTime() < minDate.getTime());
            var nextDisabled = maxDate && (new Date(decadeEnd + 1, 0, 1, 0, 0, 0, 0).getTime() > maxDate.getTime());

            var html = '<div class="datepicker-years">';
            html += '<table class="table table-sm table-condensed">';
            html += '<thead><tr>';
            html += '<th class="prev' + (prevDisabled ? ' disabled' : '') + '" data-action="prevDecade" title="' + escapeHtml(opts.tooltips.prevDecade) + '">' + renderIcon('previous', opts.icons.previous) + '</th>';
            html += '<th class="picker-switch text-center" colspan="5" data-action="pickerSwitch" title="' + escapeHtml(opts.tooltips.selectDecade) + '">' + decadeStart + ' - ' + decadeEnd + '</th>';
            html += '<th class="next' + (nextDisabled ? ' disabled' : '') + '" data-action="nextDecade" title="' + escapeHtml(opts.tooltips.nextDecade) + '">' + renderIcon('next', opts.icons.next) + '</th>';
            html += '</tr></thead><tbody><tr><td colspan="7">';

            for (var y = decadeStart - 1; y <= decadeEnd + 1; y++) {
                var cls = ['year'];
                if (y < decadeStart) cls.push('old');
                if (y > decadeEnd) cls.push('new');
                if (currentDate && currentDate.getFullYear() === y) cls.push('active');
                var yEnd = new Date(y, 11, 31, 23, 59, 59, 999).getTime();
                var yStart = new Date(y, 0, 1, 0, 0, 0, 0).getTime();
                if ((minDate && yEnd < minDate.getTime()) || (maxDate && yStart > maxDate.getTime())) {
                    cls.push('disabled');
                }
                html += '<span class="' + cls.join(' ') + '" data-action="selectYear" data-year="' + y + '">' + y + '</span>';
            }

            html += '</td></tr></tbody></table></div>';
            return html;
        }

        function buildDecadesView() {
            var year = viewDate.getFullYear();
            var centuryStart = year - (year % 100);
            var centuryEnd = centuryStart + 99;

            var html = '<div class="datepicker-decades">';
            html += '<table class="table table-sm table-condensed">';
            html += '<thead><tr>';
            html += '<th class="prev" data-action="prevCentury" title="' + escapeHtml(opts.tooltips.prevCentury) + '">' + renderIcon('previous', opts.icons.previous) + '</th>';
            html += '<th class="picker-switch text-center" colspan="5" disabled>' + centuryStart + ' - ' + centuryEnd + '</th>';
            html += '<th class="next" data-action="nextCentury" title="' + escapeHtml(opts.tooltips.nextCentury) + '">' + renderIcon('next', opts.icons.next) + '</th>';
            html += '</tr></thead><tbody><tr><td colspan="7">';

            for (var d = centuryStart - 10; d <= centuryEnd + 10; d += 10) {
                var cls = ['decade'];
                if (d < centuryStart) cls.push('old');
                if (d > centuryEnd) cls.push('new');
                if (currentDate && currentDate.getFullYear() >= d && currentDate.getFullYear() <= d + 9) cls.push('active');
                html += '<span class="' + cls.join(' ') + '" data-action="selectDecade" data-decade="' + d + '">' + d + '</span>';
            }

            html += '</td></tr></tbody></table></div>';
            return html;
        }

        function buildTimePicker() {
            var d = currentDate || new Date();
            var hours24 = d.getHours();
            var hours12 = hours24 % 12 || 12;
            var displayHours = use24Hours ? padZero(hours24) : padZero(hours12);
            var displayMinutes = padZero(d.getMinutes());
            var displaySeconds = padZero(d.getSeconds());
            var ampm = hours24 >= 12 ? 'PM' : 'AM';

            if (timePickerView === 'hours') {
                var hHtml = '<div class="timepicker"><div class="timepicker-hours"><table class="table table-sm text-center mb-0">';
                hHtml += '<thead><tr><th colspan="4" class="text-center"><a href="#" class="btn btn-sm btn-link text-decoration-none text-body fw-bold" data-action="closeTimeSubView">Back to Time</a></th></tr></thead>';
                hHtml += '<tbody>';
                var hourCount = use24Hours ? 24 : 12;
                var cols = 4;
                for (var r = 0; r < hourCount / cols; r++) {
                    hHtml += '<tr>';
                    for (var c = 0; c < cols; c++) {
                        var hVal = r * cols + c;
                        if (!use24Hours) {
                            hVal = (hVal === 0 ? 12 : hVal);
                        }
                        var hActive = (use24Hours ? hours24 === hVal : hours12 === hVal);
                        hHtml += '<td><span class="hour btn btn-sm ' + (hActive ? 'btn-primary' : 'btn-light border') + ' m-1" data-action="selectHour" data-hour="' + hVal + '">' + padZero(hVal) + '</span></td>';
                    }
                    hHtml += '</tr>';
                }
                hHtml += '</tbody></table></div></div>';
                return hHtml;
            }

            if (timePickerView === 'minutes') {
                var mHtml = '<div class="timepicker"><div class="timepicker-minutes"><table class="table table-sm text-center mb-0">';
                mHtml += '<thead><tr><th colspan="4" class="text-center"><a href="#" class="btn btn-sm btn-link text-decoration-none text-body fw-bold" data-action="closeTimeSubView">Back to Time</a></th></tr></thead>';
                mHtml += '<tbody>';
                var minStep = opts.stepping > 1 ? opts.stepping : 5; // legacy: stepping 1 shows 5-minute grid
                for (var minVal = 0; minVal < 60; minVal += minStep) {
                    if ((minVal / minStep) % 4 === 0) mHtml += (minVal ? '</tr>' : '') + '<tr>';
                    var mActive = (d.getMinutes() === minVal);
                    mHtml += '<td><span class="minute btn btn-sm ' + (mActive ? 'btn-primary' : 'btn-light border') + ' m-1" data-action="selectMinute" data-minute="' + minVal + '">' + padZero(minVal) + '</span></td>';
                }
                mHtml += '</tr>';
                mHtml += '</tbody></table></div></div>';
                return mHtml;
            }

            var html = '<div class="timepicker"><div class="timepicker-picker"><table class="table table-sm text-center mb-0">';
            html += '<tbody><tr>';

            // Increment row
            html += '<td><a href="#" class="btn btn-sm btn-link text-body" data-action="incrementHours" title="' + escapeHtml(opts.tooltips.incrementHour) + '">' + renderIcon('up', opts.icons.up) + '</a></td>';
            html += '<td class="separator"></td>';
            html += '<td><a href="#" class="btn btn-sm btn-link text-body" data-action="incrementMinutes" title="' + escapeHtml(opts.tooltips.incrementMinute) + '">' + renderIcon('up', opts.icons.up) + '</a></td>';
            if (hasSeconds) {
                html += '<td class="separator"></td>';
                html += '<td><a href="#" class="btn btn-sm btn-link text-body" data-action="incrementSeconds" title="' + escapeHtml(opts.tooltips.incrementSecond) + '">' + renderIcon('up', opts.icons.up) + '</a></td>';
            }
            if (!use24Hours) {
                html += '<td class="separator"></td><td></td>';
            }
            html += '</tr><tr>';

            // Value row
            html += '<td><span class="timepicker-hour fs-5 fw-bold" role="button" data-action="showHours" title="' + escapeHtml(opts.tooltips.pickHour) + '">' + displayHours + '</span></td>';
            html += '<td class="separator fw-bold">:</td>';
            html += '<td><span class="timepicker-minute fs-5 fw-bold" role="button" data-action="showMinutes" title="' + escapeHtml(opts.tooltips.pickMinute) + '">' + displayMinutes + '</span></td>';
            if (hasSeconds) {
                html += '<td class="separator fw-bold">:</td>';
                html += '<td><span class="timepicker-second fs-5 fw-bold">' + displaySeconds + '</span></td>';
            }
            if (!use24Hours) {
                html += '<td class="separator"></td>';
                html += '<td><button type="button" class="btn btn-sm btn-primary" data-action="togglePeriod" title="' + escapeHtml(opts.tooltips.togglePeriod) + '">' + ampm + '</button></td>';
            }
            html += '</tr><tr>';

            // Decrement row
            html += '<td><a href="#" class="btn btn-sm btn-link text-body" data-action="decrementHours" title="' + escapeHtml(opts.tooltips.decrementHour) + '">' + renderIcon('down', opts.icons.down) + '</a></td>';
            html += '<td class="separator"></td>';
            html += '<td><a href="#" class="btn btn-sm btn-link text-body" data-action="decrementMinutes" title="' + escapeHtml(opts.tooltips.decrementMinute) + '">' + renderIcon('down', opts.icons.down) + '</a></td>';
            if (hasSeconds) {
                html += '<td class="separator"></td>';
                html += '<td><a href="#" class="btn btn-sm btn-link text-body" data-action="decrementSeconds" title="' + escapeHtml(opts.tooltips.decrementSecond) + '">' + renderIcon('down', opts.icons.down) + '</a></td>';
            }
            if (!use24Hours) {
                html += '<td class="separator"></td><td></td>';
            }
            html += '</tr></tbody></table></div></div>';
            return html;
        }

        function buildToolbar() {
            var items = [];
            if (hasDate && hasTime && !opts.sideBySide) {
                var toggleTitle = timeScreen ? opts.tooltips.selectMonth : opts.tooltips.selectTime;
                var toggleIcon = timeScreen ? renderIcon('date', opts.icons.date) : renderIcon('time', opts.icons.time);
                items.push('<a href="#" class="btn btn-sm btn-light border mx-1" data-action="togglePicker" title="' + escapeHtml(toggleTitle) + '">' + toggleIcon + '</a>');
            }
            if (opts.showTodayButton) {
                items.push('<a href="#" class="btn btn-sm btn-light border mx-1" data-action="today" title="' + escapeHtml(opts.tooltips.today) + '">' + renderIcon('today', opts.icons.today) + '</a>');
            }
            if (opts.showClear) {
                items.push('<a href="#" class="btn btn-sm btn-light border mx-1" data-action="clear" title="' + escapeHtml(opts.tooltips.clear) + '">' + renderIcon('clear', opts.icons.clear) + '</a>');
            }
            if (opts.showClose) {
                items.push('<a href="#" class="btn btn-sm btn-light border mx-1" data-action="close" title="' + escapeHtml(opts.tooltips.close) + '">' + renderIcon('close', opts.icons.close) + '</a>');
            }

            if (items.length === 0) return '';
            return '<div class="picker-toolbar text-center py-1 mt-1 border-top">' + items.join('') + '</div>';
        }

        function render() {
            if (!widget) return;

            var html = '';
            var toolbarHtml = buildToolbar();

            if (opts.toolbarPlacement === 'top') {
                html += toolbarHtml;
            }

            if (opts.sideBySide && hasDate && hasTime) {
                html += '<div class="row g-0"><div class="col-auto datepicker">';
                if (currentViewMode === 'days') html += buildDaysView();
                else if (currentViewMode === 'months') html += buildMonthsView();
                else if (currentViewMode === 'years') html += buildYearsView();
                else if (currentViewMode === 'decades') html += buildDecadesView();
                html += '</div><div class="col-auto border-start ps-2">';
                html += buildTimePicker();
                html += '</div></div>';
            } else if (hasDate && (!hasTime || !timeScreen)) {
                html += '<div class="datepicker">';
                if (currentViewMode === 'days') html += buildDaysView();
                else if (currentViewMode === 'months') html += buildMonthsView();
                else if (currentViewMode === 'years') html += buildYearsView();
                else if (currentViewMode === 'decades') html += buildDecadesView();
                html += '</div>';
            } else if (hasTime) {
                html += buildTimePicker();
            }

            if (opts.toolbarPlacement !== 'top') {
                html += toolbarHtml;
            }

            widget.innerHTML = html;
        }

        function placeWidget() {
            if (!widget || opts.inline) return;

            var ref = component || input || el;
            var rect = ref.getBoundingClientRect();

            var top = rect.bottom + window.pageYOffset + 4;
            var left = rect.left + window.pageXOffset;

            // Handle bottom viewport collision
            var vpHeight = window.innerHeight;
            if (rect.bottom + widget.offsetHeight + 10 > vpHeight && rect.top - widget.offsetHeight > 10) {
                top = rect.top + window.pageYOffset - widget.offsetHeight - 4;
                widget.classList.add('top');
                widget.classList.remove('bottom');
            } else {
                widget.classList.add('bottom');
                widget.classList.remove('top');
            }

            // Handle right viewport collision
            var vpWidth = window.innerWidth;
            if (rect.left + widget.offsetWidth > vpWidth) {
                left = Math.max(10, rect.right + window.pageXOffset - widget.offsetWidth);
            }

            widget.style.position = 'absolute';
            widget.style.top = top + 'px';
            widget.style.left = left + 'px';
            widget.style.zIndex = '1060';
        }

        function show() {
            if (isVisible) return;
            if (input && input.disabled) return;

            if (!widget) {
                widget = createWidget();
                var parent = opts.widgetParent ? (typeof opts.widgetParent === 'string' ? document.querySelector(opts.widgetParent) : opts.widgetParent) : document.body;
                if (opts.inline) {
                    el.appendChild(widget);
                } else {
                    parent.appendChild(widget);
                }
                attachWidgetEvents();
            }

            if (opts.useCurrent && !currentDate && (!input || !input.value)) {
                setValue(new Date());
            }

            currentViewMode = opts.viewMode || 'days';
            timePickerView = 'picker';
            render();

            if (!opts.inline) {
                widget.style.display = 'block';
                placeWidget();
                window.addEventListener('resize', placeWidget);
                window.addEventListener('scroll', placeWidget, true);
                document.addEventListener('mousedown', onDocumentMouseDown);
            }

            isVisible = true;
            triggerEvent('dp.show');

            if (opts.focusOnShow && input && !opts.inline) {
                input.focus();
            }
        }

        function hide() {
            if (!isVisible || opts.inline) return;
            if (widget) {
                widget.style.display = 'none';
            }
            isVisible = false;
            timePickerView = 'picker';
            window.removeEventListener('resize', placeWidget);
            window.removeEventListener('scroll', placeWidget, true);
            document.removeEventListener('mousedown', onDocumentMouseDown);
            triggerEvent('dp.hide');
        }

        function toggle() {
            if (isVisible) hide();
            else show();
        }

        function onDocumentMouseDown(e) {
            if (!widget || !isVisible || opts.inline) return;
            if (widget.contains(e.target) || el.contains(e.target)) return;
            if (component && component.contains(e.target)) return;
            hide();
        }

        function handleAction(action, target) {
            var cur = currentDate ? new Date(currentDate.getTime()) : new Date();

            switch (action) {
                case 'nextMonth':
                    viewDate = new Date(viewDate.getFullYear(), viewDate.getMonth() + 1, 1);
                    render();
                    break;
                case 'prevMonth':
                    viewDate = new Date(viewDate.getFullYear(), viewDate.getMonth() - 1, 1);
                    render();
                    break;
                case 'nextYear':
                    viewDate = new Date(viewDate.getFullYear() + 1, viewDate.getMonth(), 1);
                    render();
                    break;
                case 'prevYear':
                    viewDate = new Date(viewDate.getFullYear() - 1, viewDate.getMonth(), 1);
                    render();
                    break;
                case 'nextDecade':
                    viewDate = new Date(viewDate.getFullYear() + 10, viewDate.getMonth(), 1);
                    render();
                    break;
                case 'prevDecade':
                    viewDate = new Date(viewDate.getFullYear() - 10, viewDate.getMonth(), 1);
                    render();
                    break;
                case 'nextCentury':
                    viewDate = new Date(viewDate.getFullYear() + 100, viewDate.getMonth(), 1);
                    render();
                    break;
                case 'prevCentury':
                    viewDate = new Date(viewDate.getFullYear() - 100, viewDate.getMonth(), 1);
                    render();
                    break;
                case 'pickerSwitch':
                    if (currentViewMode === 'days') currentViewMode = 'months';
                    else if (currentViewMode === 'months') currentViewMode = 'years';
                    else if (currentViewMode === 'years') currentViewMode = 'decades';
                    render();
                    break;
                case 'selectDay':
                    var dayStr = target.getAttribute('data-day');
                    if (dayStr) {
                        var parts = dayStr.split('-');
                        var y = parseInt(parts[0], 10);
                        var m = parseInt(parts[1], 10) - 1;
                        var d = parseInt(parts[2], 10);
                        var hrs = currentDate ? currentDate.getHours() : 0;
                        var mins = currentDate ? currentDate.getMinutes() : 0;
                        var secs = currentDate ? currentDate.getSeconds() : 0;
                        var selectedDate = new Date(y, m, d, hrs, mins, secs);
                        // The day cell is enabled by day range; pull the carried-over time inside min/max.
                        if (minDate && selectedDate < minDate) selectedDate = new Date(minDate.getTime());
                        if (maxDate && selectedDate > maxDate) selectedDate = new Date(maxDate.getTime());

                        setValue(selectedDate);

                        if (!hasTime && !opts.keepOpen && !opts.inline) {
                            hide();
                        } else if (hasTime && opts.collapse && !timeScreen && !opts.sideBySide) {
                            timeScreen = true;
                            render();
                        }
                    }
                    break;
                case 'selectMonth':
                    var sm = parseInt(target.getAttribute('data-month'), 10);
                    viewDate = new Date(viewDate.getFullYear(), sm, 1);
                    currentViewMode = 'days';
                    render();
                    break;
                case 'selectYear':
                    var sy = parseInt(target.getAttribute('data-year'), 10);
                    viewDate = new Date(sy, viewDate.getMonth(), 1);
                    currentViewMode = 'months';
                    render();
                    break;
                case 'selectDecade':
                    var sdec = parseInt(target.getAttribute('data-decade'), 10);
                    viewDate = new Date(sdec, viewDate.getMonth(), 1);
                    currentViewMode = 'years';
                    render();
                    break;
                case 'showHours':
                    timePickerView = 'hours';
                    render();
                    break;
                case 'showMinutes':
                    timePickerView = 'minutes';
                    render();
                    break;
                case 'closeTimeSubView':
                    timePickerView = 'picker';
                    render();
                    break;
                case 'selectHour':
                    var sh = parseInt(target.getAttribute('data-hour'), 10);
                    if (!use24Hours) {
                        var isPM = cur.getHours() >= 12;
                        if (isPM && sh < 12) sh += 12;
                        if (!isPM && sh === 12) sh = 0;
                    }
                    cur.setHours(sh);
                    setValue(cur);
                    timePickerView = 'picker';
                    render();
                    break;
                case 'selectMinute':
                    var smin = parseInt(target.getAttribute('data-minute'), 10);
                    cur.setMinutes(smin);
                    setValue(cur);
                    timePickerView = 'picker';
                    render();
                    break;
                case 'incrementHours':
                    cur.setHours(cur.getHours() + 1);
                    setValue(cur);
                    break;
                case 'decrementHours':
                    cur.setHours(cur.getHours() - 1);
                    setValue(cur);
                    break;
                case 'incrementMinutes':
                    cur.setMinutes(cur.getMinutes() + (opts.stepping || 1));
                    setValue(cur);
                    break;
                case 'decrementMinutes':
                    cur.setMinutes(cur.getMinutes() - (opts.stepping || 1));
                    setValue(cur);
                    break;
                case 'incrementSeconds':
                    cur.setSeconds(cur.getSeconds() + 1);
                    setValue(cur);
                    break;
                case 'decrementSeconds':
                    cur.setSeconds(cur.getSeconds() - 1);
                    setValue(cur);
                    break;
                case 'togglePeriod':
                    cur.setHours((cur.getHours() + 12) % 24);
                    setValue(cur);
                    break;
                case 'togglePicker':
                    timeScreen = !timeScreen;
                    timePickerView = 'picker';
                    render();
                    break;
                case 'today':
                    setValue(new Date());
                    if (!hasTime && !opts.keepOpen && !opts.inline) {
                        hide();
                    }
                    break;
                case 'clear':
                    setValue(null);
                    break;
                case 'close':
                    hide();
                    break;
            }
        }

        function attachWidgetEvents() {
            if (!widget) return;
            widget.addEventListener('click', function (e) {
                e.preventDefault();
                var target = e.target.closest('[data-action]');
                if (!target || target.classList.contains('disabled')) return;
                var action = target.getAttribute('data-action');
                handleAction(action, target);
            });
        }

        // [target, type, handler] triples, so destroy() can unbind them.
        var boundListeners = [];
        function listen(target, type, handler) {
            target.addEventListener(type, handler);
            boundListeners.push([target, type, handler]);
        }

        function onInputChange() {
            if (currentDate && input.value === formatDate(currentDate, format)) return; // our own write echoing back
            if (input.value) {
                var parsed = toNativeDate(input.value, format, currentDate || viewDate);
                if (parsed) setValue(parsed);
                else triggerEvent('dp.error', { date: input.value });
            } else {
                setValue(null);
            }
        }

        // Moves the selection by whole days or months (time of day kept). Goes through setValue,
        // so disabled targets fire dp.error and leave the value alone.
        function stepDate(days, months) {
            var d = currentDate ? new Date(currentDate.getTime()) : new Date();
            if (months) {
                var day = d.getDate();
                d.setDate(1);
                d.setMonth(d.getMonth() + months);
                d.setDate(Math.min(day, new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate())); // Jan 31 + 1 month = Feb 28
            }
            if (days) d.setDate(d.getDate() + days);
            setValue(d);
        }

        function onInputKeydown(e) {
            if (e.key === 'Escape') {
                hide();
                return;
            }
            if (e.key === 'Enter') {
                if (!opts.keepOpen) hide();
                return;
            }
            if (!isVisible || !hasDate || timeScreen) return; // arrows only drive the open day grid

            var step = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [-7, 0], ArrowDown: [7, 0], PageUp: [0, -1], PageDown: [0, 1] }[e.key];
            if (!step) return;
            e.preventDefault();
            stepDate(step[0], step[1]);
        }

        function onAddonClick(e) {
            e.preventDefault();
            toggle();
        }

        function attachElementEvents() {
            if (input) {
                // Legacy: the input itself opens the picker only when it is the picker element,
                // or allowInputToggle is set; otherwise only the addon opens it.
                if (el === input || opts.allowInputToggle) {
                    listen(input, 'focus', show);
                    listen(input, 'click', show);
                }
                listen(input, 'change', onInputChange);
                listen(input, 'keydown', onInputKeydown);
            }

            if (component) {
                var addons = component.querySelectorAll('.input-group-addon, .input-group-text, [data-toggle="datetimepicker"]');
                for (var ai = 0; ai < addons.length; ai++) {
                    listen(addons[ai], 'click', onAddonClick);
                }
            }
        }

        // Public API
        self.show = function () { show(); return self; };
        self.hide = function () { hide(); return self; };
        self.toggle = function () { toggle(); return self; };
        self.clear = function () { setValue(null); return self; };

        self.date = function (newDate) {
            if (arguments.length === 0) return copyDate(currentDate);
            setValue(newDate);
            return self;
        };

        self.minDate = function (val) {
            if (arguments.length === 0) return minDate;
            minDate = val ? toNativeDate(val, format, viewDate) : null;
            render();
            return self;
        };

        self.maxDate = function (val) {
            if (arguments.length === 0) return maxDate;
            maxDate = val ? toNativeDate(val, format, viewDate) : null;
            render();
            return self;
        };

        self.defaultDate = function (val) {
            if (arguments.length === 0) return opts.defaultDate;
            opts.defaultDate = val;
            if (!currentDate && val) setValue(val);
            return self;
        };

        self.format = function (val) {
            if (arguments.length === 0) return format;
            format = val;
            detectParts();
            if (currentDate) setValue(currentDate);
            render();
            return self;
        };

        self.viewMode = function (val) {
            if (arguments.length === 0) return currentViewMode;
            if (['days', 'months', 'years', 'decades'].indexOf(val) > -1) {
                currentViewMode = val;
                render();
            }
            return self;
        };

        self.disabledDates = function (val) {
            if (arguments.length === 0) return opts.disabledDates;
            opts.disabledDates = val;
            render();
            return self;
        };

        self.enabledDates = function (val) {
            if (arguments.length === 0) return opts.enabledDates;
            opts.enabledDates = val;
            render();
            return self;
        };

        self.daysOfWeekDisabled = function (val) {
            if (arguments.length === 0) return opts.daysOfWeekDisabled;
            opts.daysOfWeekDisabled = val;
            render();
            return self;
        };

        self.options = function (newOpts) {
            if (arguments.length === 0) return Object.assign({}, opts);
            Object.assign(opts, newOpts);
            if (newOpts.format) self.format(newOpts.format);
            if (newOpts.minDate !== undefined) self.minDate(newOpts.minDate);
            if (newOpts.maxDate !== undefined) self.maxDate(newOpts.maxDate);
            render();
            return self;
        };

        self.disable = function () {
            hide();
            if (input) input.disabled = true;
            return self;
        };

        self.enable = function () {
            if (input) input.disabled = false;
            return self;
        };

        self.destroy = function () {
            hide();
            boundListeners.forEach(function (l) { l[0].removeEventListener(l[1], l[2]); });
            boundListeners = [];
            if (widget && widget.parentNode) {
                widget.parentNode.removeChild(widget);
            }
            widget = null;
            if ($el) {
                $el.removeData('DateTimePicker');
                $el.removeData('datepicker');
            }
            if (el._dateTimePicker) {
                delete el._dateTimePicker;
            }
        };

        // Initialize value
        attachElementEvents();

        if (opts.defaultDate) {
            setValue(opts.defaultDate);
        } else if (input && input.value) {
            var initDate = toNativeDate(input.value, format, viewDate);
            if (initDate) {
                currentDate = initDate;
                viewDate = new Date(initDate.getFullYear(), initDate.getMonth(), 1);
            }
        }

        if (opts.inline) {
            show();
        }

        el._dateTimePicker = self;
        if ($el) {
            $el.data('DateTimePicker', self);
            $el.data('datepicker', self);
        }

        return self;
    }

    // jQuery Plugin registration
    if ($ && $.fn) {
        $.fn.datetimepicker = function (options) {
            var args = Array.prototype.slice.call(arguments, 1);
            var isMethod = typeof options === 'string';
            var result = this;

            this.each(function () {
                var $this = $(this);
                var instance = $this.data('DateTimePicker') || this._dateTimePicker;

                if (!instance) {
                    if (isMethod) {
                        throw new Error('datetimepicker("' + options + '") called on element before initialization');
                    }
                    instance = new DateTimePicker(this, options);
                    $this.data('DateTimePicker', instance);
                    $this.data('datepicker', instance);
                } else if (isMethod) {
                    if (typeof instance[options] !== 'function') {
                        throw new Error('datetimepicker: Unknown method "' + options + '"');
                    }
                    var ret = instance[options].apply(instance, args);
                    if (ret !== instance && ret !== undefined) {
                        result = ret;
                        return false; // break loop
                    }
                }
            });

            return result;
        };

        $.fn.datepicker = $.fn.datetimepicker;
        $.fn.datetimepicker.defaults = DEFAULTS;
        $.fn.datepicker.defaults = DEFAULTS;
    }

    return DateTimePicker;
}));
