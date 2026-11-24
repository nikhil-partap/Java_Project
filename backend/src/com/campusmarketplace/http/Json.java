package com.campusmarketplace.http;

import com.campusmarketplace.exception.ValidationException;
import com.campusmarketplace.model.BaseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny hand-written JSON library (no external jars allowed).
 *
 *  - parse(text): RECURSIVE DESCENT parser. JSON object -> LinkedHashMap, array -> ArrayList,
 *    string -> String, number -> Double (WRAPPER CLASS), true/false -> Boolean, null -> null.
 *  - write(value): turns Maps / Lists / entities back into JSON text using a STRINGBUILDER
 *    (much faster than "+" on Strings, which creates a new String every time).
 */
public final class Json {

    private final String text; // the JSON being parsed
    private int pos;           // current reading position

    private Json(String text) {
        this.text = text;
    }

    // ================================================================= PARSER

    public static Object parse(String text) throws ValidationException {
        Json parser = new Json(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("Unexpected extra characters");
        }
        return value;
    }

    private Object readValue() throws ValidationException {
        if (pos >= text.length()) {
            throw error("Unexpected end of input");
        }
        char c = text.charAt(pos);
        if (c == '{') return readObject();
        if (c == '[') return readArray();
        if (c == '"') return readString();
        if (c == '-' || Character.isDigit(c)) return readNumber();
        if (text.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
        if (text.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
        if (text.startsWith("null", pos)) { pos += 4; return null; }
        throw error("Unexpected character '" + c + "'");
    }

    private Map<String, Object> readObject() throws ValidationException {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++; // skip {
        skipWhitespace();
        if (peek() == '}') { pos++; return map; }
        while (true) {
            skipWhitespace();
            if (peek() != '"') throw error("Expected a string key");
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            map.put(key, readValue()); // RECURSION: the value may itself be an object/array
            skipWhitespace();
            char c = next();
            if (c == '}') return map;
            if (c != ',') throw error("Expected ',' or '}'");
        }
    }

    private List<Object> readArray() throws ValidationException {
        List<Object> list = new ArrayList<>();
        pos++; // skip [
        skipWhitespace();
        if (peek() == ']') { pos++; return list; }
        while (true) {
            skipWhitespace();
            list.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') return list;
            if (c != ',') throw error("Expected ',' or ']'");
        }
    }

    private String readString() throws ValidationException {
        pos++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= text.length()) throw error("Unterminated string");
            char c = text.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            char esc = next();
            switch (esc) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (pos + 4 > text.length()) throw error("Bad \\u escape");
                    try {
                        // WRAPPER PARSING: hex digits -> int -> char (emoji = two chars, a surrogate pair)
                        sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException e) {
                        throw error("Bad \\u escape");
                    }
                    pos += 4;
                }
                default -> throw error("Bad escape \\" + esc);
            }
        }
    }

    private Double readNumber() throws ValidationException {
        int start = pos;
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        String number = text.substring(start, pos);
        try {
            return Double.parseDouble(number); // WRAPPER CLASS parsing
        } catch (NumberFormatException e) {
            throw error("Bad number '" + number + "'");
        }
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private char peek() throws ValidationException {
        if (pos >= text.length()) throw error("Unexpected end of input");
        return text.charAt(pos);
    }

    private char next() throws ValidationException {
        char c = peek();
        pos++;
        return c;
    }

    private void expect(char expected) throws ValidationException {
        if (next() != expected) throw error("Expected '" + expected + "'");
    }

    private ValidationException error(String message) {
        return new ValidationException(String.format("Invalid JSON at position %d: %s", pos, message));
    }

    // ================================================================= WRITER

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    private static void writeValue(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(s, sb);
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Number n) {
            writeNumber(n.doubleValue(), sb);
        } else if (value instanceof BaseEntity entity) {
            writeValue(entity.toMap(), sb); // POLYMORPHISM: User/Listing/Purchase each give their own map
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                writeValue(e.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof Collection<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(',');
                first = false;
                writeValue(item, sb);
            }
            sb.append(']');
        } else {
            writeString(value.toString(), sb);
        }
    }

    /** 2400.0 is written as 2400 so the frontend sees a "normal" number. */
    private static void writeNumber(double d, StringBuilder sb) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            sb.append("null");
        } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            sb.append((long) d);
        } else {
            sb.append(d);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c)); // other control characters
                    } else {
                        sb.append(c); // unicode is kept as-is; the response is sent as UTF-8
                    }
                }
            }
        }
        sb.append('"');
    }
}
