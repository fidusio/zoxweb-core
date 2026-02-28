/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used for string utilities shared between the client and the server.
 */
public final class SharedStringUtil {

    public static class MatchToken {
        MatchToken(CharSequence t) {
            token = t;
            index = -1;
            count = 0;
            referenceIndex = -1;
        }

        CharSequence token;
        int index;
        int count;
        int referenceIndex;

        public String toString() {
            return SharedUtil.toCanonicalID(':', token, referenceIndex, index, count);
        }

        public int getIndex() {
            return index;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * The constructor is declared private to prevent instantiation.
     */
    private SharedStringUtil() {

    }

    public static boolean isIncrementOk(String[] args, int index) {
        return isIncrementOk(args, index, 1);
    }

    public static boolean isIncrementOk(String[] args, int index, int increment) {
        return index + increment <= args.length;
    }

    /**
     * Inner class is declared private which is accessible only within the class. This class
     * declares and initializes variables token of type CharSequence and integers index,
     * count, and referenceIndex. CharSequence is a readable sequence of char values.
     * The class contains a method called toString which returns a canonical string.
     *
     */


    /**
     * Finds and returns the next token match within the string sequence.
     *
     * @param token
     * @param fromIndex
     * @param tokens
     * @return
     */
    private static MatchToken nextMatch(CharSequence token, int fromIndex, MatchToken... tokens) {
        MatchToken ret = null;

        for (int i = fromIndex; i < token.length(); i++) {
            for (MatchToken mt : tokens) {
                boolean match = false;

                int j = 0;

                for (; j < mt.token.length() && (i + j) < token.length(); j++) {
                    if (token.charAt(i + j) != mt.token.charAt(j)) {
                        break;
                    }
                }

                if (j == mt.token.length()) {
                    match = true;
                }

                if (match) {
                    mt.index = i;
                    mt.count++;
                    ret = mt;
                    break;
                }
            }

            if (ret != null) {
                break;
            }
        }

        return ret;
    }


    public static String getTokenByIndex(String path, String sep, int index, boolean ignoreCase) {
        String params[] = SharedStringUtil.parseStringLenient(path, sep);
        if (index < params.length) {
            return ignoreCase ? params[index].toLowerCase() : params[index];
        }

        return null;
    }

    public static int indexOf(String str, String strLookingFor, int startIndex, boolean ignoreCase) {
        if (ignoreCase) {
            str = toLowerCase(str);
            strLookingFor = toLowerCase(strLookingFor);
        }

        return str.indexOf(strLookingFor, startIndex);
    }

    public static boolean contains(String str, String lookingFor) {
        return contains(str, lookingFor, true);
    }

    public static boolean contains(String str, String lookingFor, boolean ignoreCase) {
        if (str != null && lookingFor != null) {
            return indexOf(str, lookingFor, 0, ignoreCase) > -1;
        }

        return false;
    }


    public static boolean contains(String str, GetName lookingFor, boolean ignoreCase) {
        if (str != null && lookingFor != null && lookingFor.getName() != null) {
            return indexOf(str, lookingFor.getName(), 0, ignoreCase) > -1;
        }

        return false;
    }

    public static boolean contains(String str, GetValue<String> lookingFor, boolean ignoreCase) {
        if (str != null && lookingFor != null && lookingFor.getValue() != null) {
            return indexOf(str, lookingFor.getValue(), 0, ignoreCase) > -1;
        }

        return false;
    }

    public static boolean equals(String str1, String str2, boolean ignoreCase) {
        if (str1 == str2) {
            return true;
        }

        if (str1 != null && str2 != null) {
            if (ignoreCase) {
                str1 = toLowerCase(str1);
                str2 = toLowerCase(str2);
            }

            return str1.equals(str2);
        }

        return false;
    }

    public static String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            // Remove the surrounding quotes
            value = value.substring(1, value.length() - 1);
            // Unescape any escaped quotes
            //value = value.replace("\\\"", "\"");
        }
        return value;
    }

    public static String removeCharFromEnd(char toRemove, String str) {
        int indexToRemove = -1;
        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != toRemove)
                break;
            indexToRemove = i;
        }
        if (indexToRemove != -1)
            return str.substring(0, indexToRemove);
        return str;
    }

    /**
     * Reverses the given token(s).
     *
     * @param reverse
     * @param tokens
     * @return
     */
    private static MatchToken[] reverseTokens(boolean reverse, MatchToken... tokens) {
        if (reverse) {
            if (tokens.length > 1) {
                MatchToken[] ret = new MatchToken[tokens.length];

                for (int i = 0; i < ret.length; i++) {
                    ret[i] = tokens[tokens.length - 1 - i];
                }

                return ret;
            }
        }

        return tokens;
    }

    /**
     * Parses the token and return matching region between startDelimiter and endDelimiter.
     * If the token contains sub groups the outer group will return.
     * Ex: token = {{hello}}there{stranger},  if sd={ and ed=} the result is 2 matches {{hello}},{stranger}
     *
     * @param token
     * @param sd    startDelimiter
     * @param ed    endDelimiter
     * @return List of CharSequence
     * @throws NullPointerException if any of the parameters is null
     */
    public static List<CharSequence> parseGroup(CharSequence token, CharSequence sd, CharSequence ed, boolean includeDelimiters)
            throws NullPointerException {

        List<CharSequence> ret = new ArrayList<CharSequence>();
        MatchToken startToken = new MatchToken(sd);
        MatchToken endToken = new MatchToken(ed);
        MatchToken currentMatch = nextMatch(token, 0, startToken, endToken);
        if (currentMatch == null)
            return ret;
        currentMatch.referenceIndex = currentMatch.index;
        boolean sameToken = sd.equals(ed);

        MatchToken tokenList[] = {
                startToken,
                endToken
        };
        do {
            tokenList = reverseTokens(sameToken, tokenList);
            currentMatch = nextMatch(token, currentMatch.index + currentMatch.token.length(), tokenList);

            if (currentMatch != null && startToken.count == endToken.count) {
                endToken.referenceIndex = endToken.index + endToken.token.length();

                if (includeDelimiters) {
                    ret.add(token.subSequence(startToken.referenceIndex, endToken.referenceIndex));
                } else {
                    ret.add(token.subSequence(startToken.referenceIndex + sd.length(), endToken.referenceIndex - ed.length()));
                }

                tokenList = reverseTokens(sameToken, tokenList);
                currentMatch = nextMatch(token, currentMatch.index + currentMatch.token.length(), tokenList);

                if (currentMatch != null) {
                    currentMatch.referenceIndex = currentMatch.index;
                }
            }

        } while (currentMatch != null && (endToken.referenceIndex != token.length()));

        return ret;
    }

    /**
     * Returns empty string if str is null, if not it will trim str from starting and ending whitespaces.
     *
     * @param str
     * @return empty string if str is null, if not it will trim str from starting and ending whitespaces.
     */
//    public static String trimOrEmpty(String str) {
//        if (str != null) {
//            str = str.trim();
//        } else {
//            str = "";
//        }
//
//        return str;
//    }

    /**
     * Returns a substring to the right of the specified index if both val and token are not null,
     * otherwise will return empty string.
     *
     * @param val
     * @param token
     * @return a substring to the right of the specified index if both val and token are not null,
     */
    public static String valueAfterRightToken(String val, String token) {
        if (val != null && token != null) {
            int lastIndex = val.lastIndexOf(token);

            if (lastIndex != -1) {
                return val.substring(lastIndex + token.length());
            }
        }

        return val;
    }

    /**
     * Returns a substring starting at the beginning of the string and ending to the left of the specified index
     * if both val and token are not null, otherwise will return empty string.
     *
     * @param val
     * @param token
     * @return a substring starting at the beginning of the string and ending to the left of the specified index
     */
    public static String valueBeforeRightToken(String val, String token) {
        if (val != null && token != null) {
            int lastIndex = val.lastIndexOf(token);

            if (lastIndex != -1) {
                return val.substring(0, lastIndex);
            }
        }

        return val;
    }

    public static String[] parseToken(String token, int matchCount, boolean ignoreCase, String... toMatches) {
        List<String> matches = new ArrayList<>();
        boolean moreToMatch;
        int lastIndex = 0;
        String tokenToProcess = ignoreCase ? token.toUpperCase() : token;
        boolean found;
        do {
            found = false;
            moreToMatch = false;
            for (String toMatch : toMatches) {

                int matchIndex = tokenToProcess.indexOf(ignoreCase ? toMatch.toUpperCase() : toMatch, lastIndex);
                if (matchIndex != -1) {
                    // we have a match
                    String toAdd = token.substring(lastIndex, matchIndex);
                    if (SUS.isNotEmpty(toAdd))
                        matches.add(toAdd);
                    found = true;
                    lastIndex = matchIndex + toMatch.length();
                    // break loop
                    break;
                }
            }
            if (matchCount > 0 && matches.size() < matchCount) {
                moreToMatch = true;
            } else if (matchCount < 1 && found) {
                moreToMatch = true;
            }

        } while (moreToMatch && found);

        String toAdd = token.substring(lastIndex);
        if (SUS.isNotEmpty(toAdd))
            matches.add(toAdd);


        return matches.toArray(new String[0]);
    }

    /**
     * Extracts the value between the prefix and postfix string.
     *
     * @param str
     * @param prefix
     * @param postfix
     * @param ignoreCase
     * @return the value between the prefix and postfix string.
     */
    public static StringToken valueBetween(String str, String prefix, String postfix, boolean ignoreCase) {
        if (str != null) {
            String val = str;

            if (ignoreCase) {
                val = str.toLowerCase();
                prefix = prefix.toLowerCase();
                postfix = postfix.toLowerCase();
            }

            int preIndex = val.indexOf(prefix);

            if (preIndex == -1) {
                return null;
            }

            int postIndex = val.indexOf(postfix, preIndex + prefix.length());

            if (postIndex == -1) {
                return null;
            }

            return new StringToken(str.substring(preIndex + prefix.length(), postIndex), preIndex + prefix.length(), postIndex);

        }

        return null;
    }

    /**
     * Returns a substring to the right of the specified index if both val and token are not null,
     * otherwise will return empty string.
     *
     * @param val
     * @param token
     * @return a substring to the right of the specified index if both val and token are not null,
     */
    public static String valueAfterLeftToken(String val, String token) {
        if (val != null && token != null) {
            int lastIndex = val.indexOf(token);

            if (lastIndex != -1) {
                return val.substring(lastIndex + token.length());
            }
        }

        return val;
    }

    /**
     * Returns a substring starting at the beginning of the string and ending to the left of the specified index
     * if both val and token are not null, otherwise will return empty string.
     *
     * @param val
     * @param token
     * @return a substring starting at the beginning of the string and ending to the left of the specified index
     */
    public static String valueBeforeLeftToken(String val, String token) {
        if (val != null && token != null) {
            int lastIndex = val.indexOf(token);

            if (lastIndex != -1) {
                return val.substring(0, lastIndex);
            }
        }

        return val;
    }

    /**
     * Checks if str is not null then return uppercase version, otherwise return null.
     *
     * @param str
     * @return if str is not null then return uppercase version, otherwise return null.
     */
    public static String toUpperCase(String str) {
        if (str != null) {
            return str.toUpperCase();
        }

        return null;
    }


    /**
     * Covert a string to bytes array using UTF-8 format
     *
     * @param str to be converted
     * @return byte array
     */
    public static byte[] toBytes(String str) {
        return getBytes(str);
    }

    /**
     * Return the byte array by converting the string to byte using UTF-8.
     *
     * @param str
     * @return the byte array by converting the string to byte using UTF-8.
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public static byte[] getBytes(String str)
            throws NullPointerException, IllegalArgumentException {
        SUS.checkIfNulls("Null String", str);

        try {
            return str.getBytes(Const.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Returns the bytes array by converting the strings to bytes using UTF-8.
     *
     * @param strs
     * @return the bytes array by converting the strings to bytes using UTF-8.
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public static byte[][] getBytesArray(String... strs)
            throws NullPointerException, IllegalArgumentException {

        SUS.checkIfNulls("Null String", (Object[]) strs);

        byte[][] ret = new byte[strs.length][];

        for (int i = 0; i < strs.length; i++) {
            ret[i] = getBytes(strs[i]);
        }

        return ret;
    }

    /**
     * Return the String based on the byte array  using encoding UTF-8.
     *
     * @param array
     * @return the String based on the byte array  using encoding UTF-8.
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public static String toString(byte[] array)
            throws NullPointerException, IllegalArgumentException {
        return toString(array, 0, array.length);
    }

    public static String toString(char[] array) {
        return new String(array);
    }


    /**
     * Return the String based on the byte array  using encoding UTF-8.
     *
     * @param array
     * @param offset
     * @param length
     * @return the String based on the byte array  using encoding UTF-8.
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public static String toString(byte[] array, int offset, int length)
            throws NullPointerException, IllegalArgumentException {

        SUS.checkIfNull("Null String", array);

        try {
            return new String(array, offset, length, Const.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static String toString(byte b) {
        int ret = b;
        if (ret < 0)
            ret += 256;
        return Integer.toString(ret);
    }

    /**
     * Checks if str is not null then return lowercase version, otherwise return null.
     *
     * @param str
     * @return if str is not null then return lowercase version, otherwise return null.
     */
    public static String toLowerCase(String str) {
        if (str != null) {
            return str.toLowerCase();
        }

        return null;
    }

    /**
     * Trims the str and checks if its length = > minLength.
     *
     * @param str
     * @param minLength
     * @return true if str match the min length criteria
     */
    public static boolean isMinimumLengthMet(String str, int minLength) {
        str = SUS.trimOrNull(str);

        if (str == null) {
            if (minLength == 0) {
                return true;
            } else {
                return false;
            }
        }

        return (str.length() >= minLength);
    }

    /**
     * Filters the given string based on given filters and returns the filtered string.
     *
     * @param str
     * @param filters
     * @return the given string based on given filters and returns the filtered string.
     */
    public static String filterString(String str, CharSequence... filters) {
        if (str != null) {
            for (CharSequence cs : filters) {
                str = str.replace(cs, "");
            }
        }

        return str;
    }

    /**
     * Parses specified string based on regex and filter.
     *
     * @param str
     * @param regex
     * @param filters
     * @return parsed strings
     */
    public static String[] parseString(String str, String regex, CharSequence... filters) {
        return parseString(str, regex, false, filters);
    }

    /**
     * Parses specified string based on regex and filter.
     *
     * @param str     to parse
     * @param regex   reg expression
     * @param noEmpty if true empty string are filtered
     * @param filters filter to the str
     * @return parsed strings
     */
    public static String[] parseString(String str, String regex, boolean noEmpty, CharSequence... filters) {
        if (filters != null) {
            str = filterString(str, filters);
        }
        String[] results = str.split(regex);
        if (!noEmpty)
            return results;

        List<String> ret = new ArrayList<String>();
        for (String tmp : results) {
            if (!SUS.isEmpty(tmp))
                ret.add(tmp);
        }
        return ret.toArray(new String[0]);
    }

    public static String[] parseStringLenient(String str, String regex, CharSequence... filters) {
        if (str != null)
            return parseString(str, regex, filters);

        return Const.EMPTY_STRING_ARRAY;
    }


    public static List<GetNameValue<String>> parseStrings(char sep, String... tokens) {
        List<GetNameValue<String>> ret = new ArrayList<GetNameValue<String>>();
        String stringSep = "" + sep;
        for (String token : tokens) {
            String[] toks = token.split(stringSep);
            if (toks.length == 2) {
                ret.add(new NVPair(toks[0], toks[1]));
            }
        }
        return ret;
    }


    /**
     * Returns a string array based on the strings str and sep.
     *
     * @param str
     * @param sep
     * @return {name, value}
     */
    public static String[] parseNameValue(String str, String sep) {
        int index = str.indexOf(sep);

        if (index != -1) {
            return new String[]{str.substring(0, index), str.substring(index + sep.length(), str.length())};
        }

        return new String[]{str};
    }

    public static boolean isComment(String line) {
        return isComment(line, Const.COMMENT_TAGS);
    }


    public static boolean isComment(String line, String... startTokenMarkers) {

        line = SUS.trimOrNull(line);
        if (line != null) {
            for (String token : startTokenMarkers) {
                if (line.startsWith(token))
                    return true;
            }
        }
        return false;
    }

    public static int indexOf(String str, String... tags) {
        int firstIndex = -1;

        for (String tag : tags) {
            int index = str.indexOf(tag);
            if (index != -1) {
                if (firstIndex == -1) {
                    firstIndex = index;
                } else if (index < firstIndex) {
                    firstIndex = index;
                }
            }
        }

        return firstIndex;
    }


    public static String concat(String sep, int length, String... tokens) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(sep);
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    /**
     * Concatenates s1 + sep + s2 = total, sep will not be added if s1 ends with sep or s2 starts with sep.
     *
     * @param s1
     * @param s2
     * @param sep
     * @return s1 + sep + s2
     */
    public static String concat(String s1, String s2, String sep) {
        s1 = SUS.trimOrEmpty(s1);
        s2 = SUS.trimOrEmpty(s2);
        sep = SUS.trimOrEmpty(sep);

        if ((s1.endsWith(sep) && !s2.startsWith(sep)) || (!s1.endsWith(sep) && s2.startsWith(sep))) {
            return s1 + s2;
        } else if (s1.endsWith(sep) && s2.startsWith(sep)) {
            return s1 + s2.substring(sep.length());
        }

        if (s2.isEmpty()) {
            return s1;
        }

        return s1 + sep + s2;
    }

    /**
     * Format a list of NPairs.
     *
     * @param list the list of name-value pairs
     * @param <V> the value type
     * @return formatted string like n1=v1&amp;n2=v2...
     */
    public static <V> String format(ArrayValues<GetNameValue<V>> list) {
        return format(list, "=", false, "&");
    }

    /**
     * This method will convert an ArrayList of NVPair into a single string
     *
     * @param list         all the NVPair
     * @param nameValueSep the name value separator
     * @param quotedValue  if the value should be quoted
     * @param nvPairSep    the separator between the nvpairs
     * @return the formated string
     */
    public static <V> String format(ArrayValues<GetNameValue<V>> list, String nameValueSep, boolean quotedValue, String nvPairSep) {
        StringBuilder sb = new StringBuilder();
        GetNameValue<V>[] all = list.values();

        for (int i = 0; i < all.length; i++) {
            GetNameValue<V> nvp = all[i];
            sb.append(format(nvp, nameValueSep, quotedValue));

            if (i + 1 < all.length) {
                sb.append(nvPairSep);
            }
        }

        return sb.toString();
    }

    /**
     * @param pair
     * @param nameValueSep
     * @param quotedValue
     * @return formatted string
     */
    public static <V> String format(GetNameValue<V> pair, String nameValueSep, boolean quotedValue) {
        return format(pair.getName(), pair.getValue(), nameValueSep, quotedValue);
    }

    /**
     * @param name
     * @param value
     * @param nameValueSep
     * @param quotedValue
     * @return formatted string
     */
    public static <V> String format(String name, V value, String nameValueSep, boolean quotedValue) {
        StringBuilder sb = new StringBuilder();

        if (name != null) {
            sb.append(name);
            sb.append(nameValueSep);
        }

        if (value != null) {
            if (quotedValue) {
                sb.append('\"');
            }

            sb.append(value);

            if (quotedValue) {
                sb.append('\"');
            }
        }

        return sb.toString();
    }

    public static String formatStringValues(String sep, String... values) {
        SUS.checkIfNulls("Null Parameter", sep, values);
        StringBuilder ret = new StringBuilder();

        for (String value : values) {
            value = SUS.trimOrNull(value);

            if (value != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }

                ret.append(value);
            }
        }

        return ret.toString();
    }


    /**
     * This method add chars between the text characters
     *
     * @param text
     * @param chars
     * @return
     */
    public static String spaceChars(String text, String chars) {
        StringBuilder ret = new StringBuilder();
        char[] stringChars = text.toCharArray();
        for (int i = 0; i < stringChars.length; i++) {
            ret.append(stringChars[i]);
            if (i + 1 < stringChars.length)
                ret.append(chars);
        }
        return ret.toString();
    }


    public static String repeatSequence(String sc, int count) {
        return repeatSequence(sc, count, null);
    }

    public static String repeatSequence(String sc, int count, String sep) {
        if (count < 1) {
            return sc;
        }
        if (sep != null && sep.length() == 0)
            sep = null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(sc);
            if (sep != null && i + 1 < count)
                sb.append(sep);
        }
        return sb.toString();
    }

    public static String formatStringToByteArray(String str, boolean hex) {
        byte data[] = getBytes(str);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (byte b : data) {
            if (sb.length() > 1)
                sb.append(',');
            if (hex)
                byteToHex(sb, "0x", b);
            else
                sb.append(b);
        }
        sb.append("}");

        return sb.toString();
    }

    public static String formatStringValues(String sep, GetName... gns) {
        SUS.checkIfNulls("Null Parameter", sep, gns);
        StringBuilder ret = new StringBuilder();

        for (GetName gn : gns) {
            String value = SUS.trimOrNull(gn.getName());
            if (value != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }

                ret.append(value);
            }
        }

        return ret.toString();
    }

    public static String formatStringValues(String sep, GetValue<?>... values) {
        SUS.checkIfNulls("Null Parameter", sep, values);
        StringBuilder ret = new StringBuilder();

        for (GetValue<?> gnv : values) {
            String value = SUS.trimOrNull(gnv.getValue() != null ? "" + gnv.getValue() : null);

            if (value != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }

                ret.append(value);
            }
        }

        return ret.toString();
    }

    public static <V> String format(String name, V value) {
        return format(name, value, "=", false);
    }

    public static String tag(String str) {
        return tag(Const.TAG_ENVELOPE, str, Const.TAG_ENVELOPE);
    }

    public static String tag(String leftRightTag, String str) {
        return tag(leftRightTag, str, leftRightTag);
    }

    public static String tag(String leftTag, String str, String rightTag) {
        return (leftTag != null ? leftTag : "") + str + (rightTag != null ? rightTag : "");
    }

    /**
     * Converts a hex string into a byte array
     *
     * @param str
     * @return a hex string into a byte array
     * @throws IllegalArgumentException
     * @throws NullPointerException
     */
    public static byte[] hexToBytes(String str)
            throws IllegalArgumentException, NullPointerException {
        str = str.toUpperCase().trim();

        if (str.startsWith("0X")) {
            str = SharedStringUtil.valueAfterLeftToken(str, "0X");
        }

        int len = str.length();

        if (len % 2 != 0) {
            throw new IllegalArgumentException("Not a valid hex format " + str);
        }

        byte[] byteRet = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            // take the first byte shift to the left a nible
            // then or it with right char value
            byteRet[i / 2] = (byte) ((hexToInt(str.charAt(i)) << 4) | hexToInt(str.charAt(i + 1)));
        }

        return byteRet;

    }

    /**
     * Convert a hex char 0-F to a integer value 0-15.
     *
     * @param c to be converted
     * @return value 0-15
     * @throws IllegalArgumentException if c is different than 0,1,2,3,4,5,6,7,8,9,a,b,c,d,e,f
     */
    public static int hexToInt(char c)
            throws IllegalArgumentException {

//        c = Character.toUpperCase(c);
//        for (int j = 0; j < Const.HEX_TOKENS.length; j++) {
//            if (c == Const.HEX_TOKENS[j]) {
//                return j;
//            }
//        }

        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;


        throw new IllegalArgumentException("Invalid character not a hex type 0-F:" + c);
    }


    public  static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++)
            if (!Character.isDigit(s.charAt(i))) return false;
        return !s.isEmpty();
    }

    /**
     * Replaces the textMarker inside text with value.
     * ex: text="Hello my name is $TEXTMARKER$", textMarker="$TEXTMARKER$", value="Earl", result="Hello my name is Earl".
     *
     * @param text
     * @param textMarker
     * @param value
     * @return text embedded with value
     */
    public static String embedText(String text, String textMarker, String value) {

        SUS.checkIfNulls("Invalid text", text);

        if (textMarker != null) {
            if (value == null) {
                value = "";
            }

            String textMarkerUpper = textMarker.toUpperCase();
            text = text.replace(textMarker, textMarkerUpper);

            return text.replace(textMarkerUpper, value);
        }

        return null;
    }

    public static byte[] embedTextAsBytes(String text, String textMarker, String value) {
        String embedded = embedText(text, textMarker, value);
        return getBytes(embedded);
    }

    /**
     * Replaces the textMarker inside text with value.
     * ex: text="Hello my name is $TEXTMARKER$", textMarker="$TEXTMARKER$", value="Earl", result="Hello my name is Earl".
     *
     * @param text
     * @param textMarker
     * @param value
     * @return text embedded with value
     */
    @SuppressWarnings("unchecked")
    public static String embedText(String text, Enum<?> textMarker, String value) {
        SUS.checkIfNulls("Invalid text", text);

        if (textMarker != null) {
            if (value == null) {
                value = "";
            }

            String textMarkerToken = null;

            if (textMarker instanceof GetValue) {
                textMarkerToken = ((GetValue<String>) textMarker).getValue();
            }

            if (textMarkerToken == null && textMarker instanceof GetName) {
                textMarkerToken = ((GetName) textMarker).getName();
            }

            if (textMarkerToken == null) {
                textMarkerToken = textMarker.name();
            }

            String textMarkerUpper = textMarkerToken.toUpperCase();
            text = text.replace(textMarkerToken, textMarkerUpper);

            return text.replace(textMarkerUpper, value);
        }

        return null;
    }

    /**
     * Returns the string after the last occurrence of delimiter if no match is found return token as is.
     *
     * @param token
     * @param delimiter
     * @return the string after the last occurrence of delimiter if no match is found return token as is.
     */
    public static String getTokenAfterDelimiter(String token, char delimiter) {
        int index = token.lastIndexOf(delimiter);

        if (index == -1) {
            return token;
        }

        return token.substring(index + 1);
    }

    /**
     * Checks if token occurs at least once in text.
     *
     * @param text
     * @param token
     * @return true if token occurs at least once in text.
     */
    public static boolean hasToken(String text, String token) {
        return !(text.indexOf(token) == -1);
    }

    /**
     * @param preToken
     * @param buffer
     * @param offset
     * @param len
     * @param postToken
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(String preToken, byte[] buffer, int offset, int len, String postToken) {
        StringBuilder sb = null;

        if (buffer != null) {
            //sb = new StringBuilder(buffer.length);
            for (int i = offset; i < offset + len; i++) {
                if (postToken != null && sb != null) {
                    sb.append(postToken);
                }
                if (sb == null) {
                    sb = new StringBuilder();
//                    // try to predict the size
//                    int size = buffer.length * (2);
//                    size += preToken != null && preToken.length() > 0 ? (preToken.length() * buffer.length) : 0;
//                    size += postToken != null && postToken.length() > 0 && (buffer.length - 1) > 0 ? postToken.length() * (buffer.length - 1) : 0;
//                    sb = new StringBuilder(size);
                }
                sb = byteToHex(sb, preToken, buffer[i]);
            }
        }

        return sb != null ? sb.toString() : null;
    }

    /**
     * Converts a byte buffer into a hexadecimal string representation.
     *
     * @param buffer
     * @param postToken
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(byte[] buffer, String postToken) {
        return bytesToHex(null, buffer, 0, buffer.length, postToken);
    }

    /**
     * Converts a byte buffer into a hexadecimal string representation.
     *
     * @param preToken
     * @param buffer
     * @param postToken
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(String preToken, byte[] buffer, String postToken) {
        return bytesToHex(preToken, buffer, 0, buffer.length, postToken);
    }

    /**
     * Converts a byte buffer into a hexadecimal string representation
     *
     * @param preToken
     * @param buffer
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(String preToken, byte[] buffer) {
        return bytesToHex(preToken, buffer, 0, buffer.length, null);
    }

    /**
     * Converts a byte buffer into a hexadecimal string representation, based on the offset and len.
     *
     * @param buffer
     * @param offset
     * @param len
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(byte[] buffer, int offset, int len) {
        return bytesToHex(null, buffer, offset, len, null);
    }

    /**
     * Converts a byte to hex StringBuilder.
     *
     * @param sb       if null this method will create a new string builder , if not null will use the one being passed
     * @param preToken to be added before the converted byte if null it will be skipped
     * @param b        byte to convert
     * @return stringbuilder of hex that represent the buffer content
     */
    public static StringBuilder byteToHex(StringBuilder sb, String preToken, byte b) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (preToken != null) {
            sb.append(preToken);
        }

        sb.append(Const.HEX_TOKENS[(b >> 4) & (0X0F)]);
        sb.append(Const.HEX_TOKENS[b & (0X0F)]);

        return sb;
    }

    /**
     * Converts a byte buffer into a hexadecimal string representation.
     *
     * @param buffer
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(byte[] buffer) {
        return bytesToHex(null, buffer, 0, buffer.length, null);
    }

    /**
     * Converts a string into a hexadecimal string representation.
     *
     * @param preToken
     * @param str
     * @return string of hex that represent the buffer content
     */
    public static String stringToHex(String preToken, String str) {
        return bytesToHex(preToken, getBytes(str));
    }

    /**
     * Truncates a string based on the specified length.
     *
     * @param str
     * @param length
     * @return Truncates a string based on the specified length.
     */
    public static String truncate(String str, int length) {
        if (str == null) {
            return null;
        }

        if (str.length() > length) {
            return str.substring(0, length);
        }

        return str;
    }

    /**
     * Compresses the given string given the maximum length, separator, and separator length.
     *
     * @param sep
     * @param str
     * @param maxLength
     * @param sepLength
     * @return compresses the given string given the maximum length, separator, and separator length.
     */
    public static String toShortHand(char sep, String str, int maxLength, int sepLength) {
        if (str != null) {
            if (str.length() < maxLength) {
                return str;
            } else {
                int midpoint = maxLength / 2 - sepLength / 2;
                StringBuilder sb = new StringBuilder();
                sb.append(str.substring(0, midpoint));

                for (int i = 0; i < sepLength; i++) {
                    sb.append(sep);
                }

                sb.append(str.substring(str.length() - midpoint, str.length()));

                return sb.toString();
            }
        }

        return null;
    }

    public static MatchToken matchToken(String str, String token, boolean ignoreCase) {
        MatchToken ret = null;

        int firstIndex = indexOf(str, token, 0, ignoreCase);
        int counter = 0;

        if (firstIndex != -1) {
            ret = new MatchToken(token);
            ret.index = firstIndex;
            ret.referenceIndex = firstIndex;

            int currentIndex = firstIndex;

            while (currentIndex != -1) {
                counter++;
                ret.referenceIndex = currentIndex;
                currentIndex = indexOf(str, token, currentIndex + token.length(), ignoreCase);
            }

            ret.count = counter;
        }

        return ret;
    }

}