// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

package ibsmas.vault.internal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader/writer for Vault's API, so the client needs no JSON library on Java 8.
 *
 * <p>Parses to {@code Map<String, Object>} (insertion-ordered), {@code List<Object>}, {@code String},
 * {@code Long}/{@code BigInteger}/{@code Double}, {@code Boolean}, and {@code null}. Error messages give
 * only a position, never input text, because the input may hold secrets.
 *
 * <p>Internal to the client; the package name says so. Not a general-purpose JSON library.
 */
public final class Json {

    private static final int MAX_DEPTH = 64;

    private Json() {
    }

    /**
     * Parses a complete JSON document.
     *
     * @param text JSON text
     * @return the parsed value
     * @throws IllegalArgumentException on malformed input
     */
    public static Object parse(String text) {
        Parser parser = new Parser(text);
        parser.skipWhitespace();
        Object value = parser.readValue(0);
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error();
        }
        return value;
    }

    /** Returns {@code value} as a JSON object, or null if it is not one. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    /** Returns {@code obj.get(key)} as a JSON object, or null when absent or not an object. */
    public static Map<String, Object> getObject(Map<String, Object> obj, String key) {
        return obj == null ? null : asObject(obj.get(key));
    }

    /** Returns {@code obj.get(key)} as a string, or null when absent or not a string. */
    public static String getString(Map<String, Object> obj, String key) {
        Object v = obj == null ? null : obj.get(key);
        return v instanceof String ? (String) v : null;
    }

    /**
     * Serializes maps, lists, strings, numbers, booleans, and null.
     *
     * @param value value to write
     * @return JSON text
     */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            quote(sb, (String) value);
        } else if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("JSON cannot represent NaN or infinity.");
            }
            sb.append(value);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map) {
            sb.append('{');
            Iterator<? extends Map.Entry<?, ?>> it = ((Map<?, ?>) value).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> e = it.next();
                quote(sb, String.valueOf(e.getKey()));
                sb.append(':');
                writeValue(sb, e.getValue());
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append('}');
        } else if (value instanceof Iterable) {
            sb.append('[');
            Iterator<?> it = ((Iterable<?>) value).iterator();
            while (it.hasNext()) {
                writeValue(sb, it.next());
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON type: " + value.getClass().getName());
        }
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        Object readValue(int depth) {
            if (depth > MAX_DEPTH) {
                throw error();
            }
            char c = peek();
            switch (c) {
                case '{': return readObject(depth);
                case '[': return readArray(depth);
                case '"': return readString();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return readNumber();
                    }
                    throw error();
            }
        }

        private Map<String, Object> readObject(int depth) {
            pos++; // '{'
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw error();
                }
                String key = readString();
                skipWhitespace();
                if (next() != ':') {
                    throw error();
                }
                skipWhitespace();
                map.put(key, readValue(depth + 1));
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw error();
                }
            }
        }

        private List<Object> readArray(int depth) {
            pos++; // '['
            List<Object> list = new ArrayList<Object>();
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipWhitespace();
                list.add(readValue(depth + 1));
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw error();
                }
            }
        }

        private String readString() {
            pos++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c < 0x20) {
                    throw error();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = next();
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u': sb.append(readHex4()); break;
                    default: throw error();
                }
            }
        }

        private char readHex4() {
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(next(), 16);
                if (digit < 0) {
                    throw error();
                }
                value = (value << 4) | digit;
            }
            return (char) value;
        }

        private Number readNumber() {
            int start = pos;
            if (s.charAt(pos) == '-') {
                pos++;
            }
            while (pos < s.length() && "0123456789.eE+-".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
            String token = s.substring(start, pos);
            try {
                if (token.indexOf('.') >= 0 || token.indexOf('e') >= 0 || token.indexOf('E') >= 0) {
                    return Double.valueOf(token);
                }
                try {
                    return Long.valueOf(token);
                } catch (NumberFormatException overflow) {
                    return new BigInteger(token);
                }
            } catch (NumberFormatException e) {
                pos = start;
                throw error();
            }
        }

        private void expect(String word) {
            if (!s.startsWith(word, pos)) {
                throw error();
            }
            pos += word.length();
        }

        void skipWhitespace() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                    return;
                }
                pos++;
            }
        }

        private char peek() {
            if (pos >= s.length()) {
                throw error();
            }
            return s.charAt(pos);
        }

        private char next() {
            char c = peek();
            pos++;
            return c;
        }

        IllegalArgumentException error() {
            return new IllegalArgumentException("Invalid JSON at position " + pos);
        }
    }
}
