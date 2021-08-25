/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

import org.zoxweb.shared.db.QueryMarker;

import java.util.Date;

/**
 * Contains constants and enums.
 *
 * @author mzebib
 */
public class Const {

  public static final String LOGGER_NAME = "zoxweb-core";

  public static final String UTF8 = "UTF-8";

  public enum JavaClassVersion {
    //VER_1_0("1.0", 45, 3),
    VER_1_1("1.1", 45, 3),
    VER_1_2("1.2", 46, 0),
    VER_1_3("1.3", 47, 0),
    VER_1_4("1.4", 48, 0),
    VER_1_5("1.5", 49, 0),
    VER_1_6("1.6", 50, 0),
    VER_1_7("1.7", 51, 0),
    VER_1_8("1.8", 52, 0),
    VER_1_9("1.9", 53, 0),
    VER_11("11", 55, 0),
    VER_UNKNOWN("UNKNOWN", 0, 0);

    private final String version;
    private final int major;
    private final int minor;

    JavaClassVersion(String version, int major, int minor) {
      this.version = version;
      this.major = major;
      this.minor = minor;
    }


    public String toString() {
      return version + "," + major + "." + minor;
    }

    public static JavaClassVersion lookup(int major, int minor) {
      for (JavaClassVersion ver : values()) {
        if (ver.major == major && ver.minor == minor) {
          return ver;
        }
      }

      return VER_UNKNOWN;
    }

    public static JavaClassVersion lookup(String version) {
      if (!SharedStringUtil.isEmpty(version)) {
        for (JavaClassVersion ver : values()) {
          if (ver.version.equalsIgnoreCase(version)) {
            return ver;
          }
        }
      }

      return VER_UNKNOWN;
    }
  }

  public enum Bool
      implements GetName, GetValue<Boolean> {

    TRUE("true", true),
    FALSE("false", false),
    ON("on", true),
    OFF("off", false),
    ENABLE("enable", true),
    ENABLED("enabled", true),
    DISABLE("disable", false),
    DISABLED("disabled", false),
    ACTIVE("active", true),
    INACTIVE("inactive", false),
    ACTIVATED("activated", true),
    DEACTIVATED("deactivated", false),
    HIGH("high", true),
    LOW("low", false),
    YES("yes", true),
    NO("no", false),

    ;
    private final boolean value;
    private final String name;

    Bool(String name, boolean value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public Boolean getValue() {
      return value;
    }

    @Override
    public String getName() {
      return name;
    }

    public static Bool parse(String str) {
      return (Bool) SharedUtil.lookupEnum(str, Bool.values());
    }

    public static boolean lookupValue(String str) {
      Bool ret = parse(str);

      if (ret == null) {
        throw new IllegalArgumentException("Invalid Bool token " + str);
      }

      return ret.value;
    }
  }

  public enum SourceOrigin {
    LOCAL,
    REMOTE,
    UNKNOWN,
  }

  public enum Status {
    ACTIVE,
    EXPIRED,
    INACTIVE,
    INVALID,
    PENDING,
    SUSPENDED,
  }

  public enum ResourceType {
    FILE,
    FOLDER,
    FORM,
    TEMP_FILE,
    NVENTITY
  }

  public enum ParamSource
  {
    /**
     * In the path
     */
    PATH,
    /**
     * In the payload od the request
     */
    PAYLOAD,
    /**
     * Query type
     */
    QUERY
  }

  public enum Unit
      implements GetName {

    EM("em"),
    PIXEL("px"),
    PERCENT("%"),

    ;

    private String name;

    Unit(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    public static Unit parseUnit(String str) {
      if (!SharedStringUtil.isEmpty(str)) {
        str = str.toLowerCase();

        for (Unit unit : Unit.values()) {
          if (str.endsWith(unit.getName())) {
            return unit;
          }
        }
      }

      return null;
    }
  }

  public enum DeviceType
      implements GetName {

    ANDROID("Android"),
    IPAD("iPad"),
    IPHONE("iPhone");

    private final String name;

    DeviceType(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    public static DeviceType lookup(String toMatch) {
      for (DeviceType md : DeviceType.values()) {
        if (SharedStringUtil.contains(toMatch, md.getName(), true)) {
          return md;
        }
      }

      return null;
    }

    public static boolean isMobileDevice(String toMatch) {
      return (lookup(toMatch) != null);
    }
  }

  /**
   * Number of bits inside a byte
   */
  //public static final int BITS_IN_BYTE = 8;

  /**
   * This enum represents size in bytes of default memory constants.
   *
   * @author mnael
   */
  public enum SizeInBytes
      implements GetName {

    // Byte
    B("B", 1),

    // Kilo bytes
    K("KB", 1024),

    // Mega bytes
    M("MB", K.LENGTH * 1024),

    // Giga bytes
    G("GB", M.LENGTH * 1024),

    // Tera bytes
    T("TB", G.LENGTH * 1024),

    // Peta bytes
    P("PB", T.LENGTH * 1024);

    /**
     * Returns the size of bytes.
     */
    public final long LENGTH;


    private String name;

    SizeInBytes(String name, long value) {
      this.name = name;
      LENGTH = value;
    }

    /**
     * This method returns a long value of the parsed string.
     *
     * @return the converted string value to long
     */
    public static long parse(String str) {
      str = str.toUpperCase();
      long multiplier = 1;
      SizeInBytes[] values = SizeInBytes.values();

      for (int i = values.length - 1; i >= 0; i--) {
        SizeInBytes bs = values[i];
        if (str.endsWith(bs.getName())) {
          multiplier = bs.LENGTH;
          str = str.substring(0, str.length() - bs.getName().length());
          break;
        } else if (str.endsWith(bs.name())) {
          multiplier = bs.LENGTH;
          str = str.substring(0, str.length() - bs.name().length());
          break;
        }
      }

      return Long.parseLong(str) * multiplier;
    }


    public long sizeInBytes(long size) {
      return LENGTH * size;
    }

    @Override
    public String getName() {
      return name;
    }

    public long convertBytes(long sizeInBytes)
    {
      return sizeInBytes/LENGTH;
    }

    public double convertBytesDouble(long sizeInBytes)
    {
      return (double)sizeInBytes/(double)LENGTH;
    }

    public static String toString(long bytes)
    {
      SizeInBytes[] sibs = values();
      for (int i = sibs.length -1; i >= 0; i--)
      {
        if (sibs[i].convertBytes(bytes) > 0)
        {
          String result = "" + sibs[i].convertBytesDouble(bytes);
          int index = result.indexOf(".");
          if(result.length() > index + 2)
          {
            result = result.substring(0, index+3);
          }


          return  result + "/" + sibs[i].getName();
        }
      }
      return "0";
    }
  }

  /**
   * This enum represents types of unit of time.
   *
   * @author mzebib
   */
  public enum TimeUnitType {
    NANOS,
    MILLIS,
    SECOND,
    MINUTES,
    HOUR,
    DAY,
  }

  /**
   * This enum is used to describe the string to be expected or converted to
   */
  public enum StringType {
    UPPER,// to upper case
    LOWER,// to lower case
    AS_IS,// as is
  }




  public enum FilenameSep {
    SLASH('/'),
    BACKSLASH('\\'),
    COLON(':'),
    SEMICOLON(';');

    public final char sep;

    FilenameSep(char s) {
      this.sep = s;
    }

    public String toString() {
      return "" + sep;
    }
  }

  /**
   * This enum represents units of time in milliseconds.
   *
   * @author mzebib
   */
  public enum TimeInMillis {
    // One millisecond
    MILLI(1, "millis", "milli"),

    // One second in milliseconds
    SECOND(MILLI.MILLIS * 1000, "seconds", "second", "secs", "sec", "s"),

    // One minute in milliseconds
    MINUTE(SECOND.MILLIS * 60, "minutes", "minute", "mins", "min", "m"),

    // One hour in milliseconds
    HOUR(MINUTE.MILLIS * 60, "hours", "hour", "h"),

    // One day in milliseconds
    DAY(HOUR.MILLIS * 24, "days", "day"),

    // One week in milliseconds
    WEEK(DAY.MILLIS * 7, "weeks", "week");

    public final long MILLIS;
    private final String[] tokens;

    TimeInMillis(long duration, String... tokens) {
      this.MILLIS = duration;
      this.tokens = tokens;
    }

    /**
     * Converts string to time in milliseconds.
     *
     * @return the time string value in millis
     */
    public static long toMillis(String time)
        throws NullPointerException, IllegalArgumentException {
      time = SharedStringUtil.toLowerCase(time).trim();
      try
      {
        return Long.parseLong(time);
      }
      catch(NumberFormatException e)
      {

      }
      String[] hhmmss = time.split(":");
      if (hhmmss.length > 0 && hhmmss.length <= 3) {
        int millis = 0;
        int ss = 0;
        int mm = 0;
        int hh = 0;
        try {
          for (int i = 0; i < hhmmss.length; i++) {
            int index = hhmmss.length - (i + 1);
            String tok = hhmmss[index];
            switch (i) {
              case 0:
                String[] millisToken = tok.split("\\.");
                if (millisToken.length == 2) {
                  millis = Integer.parseInt(millisToken[1]);
                  if (millis < 0 || millis > 999) {
                    throw new IllegalArgumentException("invalid millis value " + millis);
                  }
                  tok = millisToken[0];
                }

                ss = Integer.parseInt(tok);
                if (ss < 0 || ss > 59) {
                  throw new IllegalArgumentException("invalid second value " + ss);
                }
                break;
              case 1:
                mm = Integer.parseInt(tok);
                if (mm < 0 || mm > 59) {
                  throw new IllegalArgumentException("invalid minute value " + mm);
                }
                break;
              case 2:
                hh = Integer.parseInt(tok);
                if (hh < 0) {
                  throw new IllegalArgumentException("invalid hour value " + hh);
                }
                break;
            }
          }
          return hh * HOUR.MILLIS + mm * MINUTE.MILLIS + ss * SECOND.MILLIS + millis;
        } catch (NumberFormatException e) {
          //e.printStackTrace();
        }
      }

      TimeInMillis timeMatch = null;
      String tokenMatch = null;

      for (TimeInMillis tim : TimeInMillis.values()) {
        for (String tok : tim.tokens) {
          if (time.endsWith(tok)) {
            tokenMatch = tok;
            timeMatch = tim;
            break;
          }

          if (timeMatch != null) {
            break;
          }
        }
      }

      if (timeMatch == null) {
        throw new IllegalArgumentException("Invalid time token " + time);
      }

      String[] valueMatch = time.split(tokenMatch);

      if (valueMatch.length != 1) {
        throw new IllegalArgumentException("Invalid time token " + time);
      }

      long multiplier = Long.parseLong(valueMatch[0].trim());

      return timeMatch.MILLIS * multiplier;
    }

    /**
     * Converts the time to nano second value (10 power -9 second)
     *
     * @return the time string value in nanos
     */
    public static long toNanos(String time)
        throws NullPointerException, IllegalArgumentException {
      return toMillis(time) * 1000000;
    }

    public static String nanosToString(long nanos) {
      long rest = nanos / 1000000;

      if (rest > 0) {
        return toString(rest);
      }

      return nanos + " nanos";
    }

    public  long convertTo(Date date)
    {
      return convertTo(date.getTime());
    }


    public long convertTo(long timeInMillis)
    {
      return timeInMillis/MILLIS;
    }
    /**
     * Converts the time to micro second value (10 power -6 second)
     *
     * @return the time string value in micros
     */
    public static long toMicros(String time)
        throws NullPointerException, IllegalArgumentException {
      return toMillis(time) * 1000;
    }

    public static String toString(long millis) {
      boolean pastTime = false;
      if(millis < 0 )
      {
        millis = - millis;
        pastTime = true;
      }
      if (millis <= WEEK.MILLIS) {
        long mil = millis % SECOND.MILLIS;
        millis /= SECOND.MILLIS;
        long sec = millis % 60;
        millis /= 60;
        long min = millis % 60;
        millis /= 60;
        long hour = millis;
        millis /= 24;

        long day = millis;

        //hour += day*24;

        StringBuilder sb = new StringBuilder();
        if (day > 0) {
          sb.append(day + "d ");
          hour = hour % 24;
        }
        sb.append((hour <= 9 ? "0" : "") + hour);
        sb.append(':');
        sb.append((min <= 9 ? "0" : "") + min);
        sb.append(':');
        sb.append((sec <= 9 ? "0" : "") + sec);

        //sb.append(String.format("%02d:%02d:%02d", hour, min, sec));
        if (mil > 0) {
          sb.append('.');
          if (mil < 99) {
            sb.append(0);
          }
          if (mil < 9) {
            sb.append(0);
          }

          sb.append(mil);
        }

        return pastTime ? "Neg(-) " + sb.toString() : sb.toString();
      }

      throw new IllegalArgumentException("Out of range");
    }
  }

  public enum TimeZoneOffset
      implements GetName {

    UTC_LESS_1200("UTC-12:00", '-', 12, 0),
    UTC_LESS_1100("UTC-11:00", '-', 11, 0),
    UTC_LESS_1000("UTC-10:00", '-', 10, 0),
    UTC_LESS_0930("UTC-9:30", '-', 9, 30),
    UTC_LESS_0900("UTC-9:00", '-', 9, 0),
    UTC_LESS_0800("UTC-8:00", '-', 8, 0),
    UTC_LESS_0700("UTC-7:00", '-', 7, 0),
    UTC_LESS_0600("UTC-6:00", '-', 6, 0),
    UTC_LESS_0500("UTC-5:00", '-', 5, 0),
    UTC_LESS_0430("UTC-4:30", '-', 4, 30),
    UTC_LESS_0400("UTC-4:00", '-', 4, 0),
    UTC_LESS_0330("UTC-3:30", '-', 3, 30),
    UTC_LESS_0300("UTC-3:00", '-', 3, 0),
    UTC_LESS_0200("UTC-2:00", '-', 2, 0),
    UTC_LESS_0100("UTC-1:00", '-', 1, 0),
    UTC("UTC�00:00", '+', 0, 0),
    UTC_PLUS_0100("UTC+1:00", '+', 1, 0),
    UTC_PLUS_0200("UTC+2:00", '+', 2, 0),
    UTC_PLUS_0300("UTC+3:00", '+', 3, 0),
    UTC_PLUS_0330("UTC+3:30", '+', 3, 30),
    UTC_PLUS_0400("UTC+4:00", '+', 4, 0),
    UTC_PLUS_0430("UTC+4:30", '+', 4, 30),
    UTC_PLUS_0500("UTC+5:00", '+', 5, 0),
    UTC_PLUS_0530("UTC+5:30", '+', 5, 30),
    UTC_PLUS_0545("UTC+5:45", '+', 5, 45),
    UTC_PLUS_0600("UTC+6:00", '+', 6, 0),
    UTC_PLUS_0630("UTC+6:30", '+', 6, 30),
    UTC_PLUS_0700("UTC+7:00", '+', 7, 0),
    UTC_PLUS_0800("UTC+8:00", '+', 8, 0),
    UTC_PLUS_0845("UTC+8:45", '+', 8, 45),
    UTC_PLUS_0900("UTC+9:00", '+', 9, 0),
    UTC_PLUS_0930("UTC+9:30", '+', 9, 30),
    UTC_PLUS_1000("UTC+10:00", '+', 10, 0),
    UTC_PLUS_1030("UTC+10:30", '+', 10, 30),
    UTC_PLUS_1100("UTC+11:00", '+', 11, 0),
    UTC_PLUS_1130("UTC+11:30", '+', 11, 30),
    UTC_PLUS_1200("UTC+12:00", '+', 12, 0),
    UTC_PLUS_1245("UTC+12:45", '+', 12, 45),
    UTC_PLUS_1300("UTC+13:00", '+', 13, 0),
    UTC_PLUS_1400("UTC+14:00", '+', 14, 0);

    private String name;
    private char sign;
    private int hours;
    private int minutes;

    TimeZoneOffset(String name, char sign, int hours, int minutes) {
      this.name = name;
      this.sign = sign;
      this.hours = hours;
      this.minutes = minutes;
    }

    @Override
    public String getName() {
      return name;
    }

    public char getSign() {
      return sign;
    }

    public int getHours() {
      return hours;
    }

    public int getMinutes() {
      return minutes;
    }

    public int getOffsetInMinutes(TimeZoneOffset timeZone) {
      return timeZone.getHours() * 60 + timeZone.getMinutes();
    }

    public int getOffsetInMillis(TimeZoneOffset timeZone) {
      return getOffsetInMinutes(timeZone) * 1000;
    }
  }

  public enum ScheduleType {
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    DAY,
    NIGHT,
    CUSTOM;
  }

  /**
   * Represents data type in bytes.
   *
   * @author mzebib
   */
  public enum TypeInBytes {
    BYTE(1),
    SHORT(2),
    INT(4),
    LONG(8);

    /**
     * Returns the byte size of the data type.
     */
    public final int SIZE;

    TypeInBytes(int s) {
      SIZE = s;
    }


    public static byte[] bytesToBytes(byte b) {
      byte[] ret = new byte[1];
      ret[0] = b;
      return ret;
    }

    public static byte[] shortToBytes(short val) {
      byte[] ret = new byte[SHORT.SIZE];

      for (int i = 0; i < ret.length; i++) {
        ret[i] = (byte) (val >> (8 * (ret.length - 1 - i)));
      }

      return ret;
    }

    public static byte[] intToBytes(int val) {
      byte[] ret = new byte[INT.SIZE];

      for (int i = 0; i < ret.length; i++) {
        ret[i] = (byte) (val >> (8 * (ret.length - 1 - i)));
      }

      return ret;
    }

    public static byte[] longToBytes(long val) {

      byte[] ret = new byte[LONG.SIZE];

      for (int i = 0; i < ret.length; i++) {
        ret[i] = (byte) (val >> (8 * (ret.length - 1 - i)));
      }

      return ret;
    }


    public int sizeInBits(int length) {
      return SIZE * length * Byte.SIZE;
    }


    public int sizeInBits() {
      return SIZE * Byte.SIZE;
    }
  }

  /**
   * Document types
   *
   * @author mzebib
   */
  public enum DocumentType
      implements GetName {
    FILE("File"),
    FOLDER("Folder"),
    FORM("Form");


    private String name;

    DocumentType(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

  }


  public enum GNVType
      implements GetName {
    NVBLOB("byte[]"),
    NVBOOLEAN("boolean"),
    NVINT("int"),
    NVLONG("long"),
    NVFLOAT("float"),
    NVDOUBLE("double"),
//		NVString("String"),
//    NVDATE("date"),

    ;

    private final String name;

    GNVType(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static String toName(GetNameValue<?> gnv, char sep) {
      if (gnv instanceof NVBoolean) {
        return NVBOOLEAN.getName() + sep + gnv.getName();
      } else if (gnv instanceof NVInt) {
        return NVINT.getName() + sep + gnv.getName();
      } else if (gnv instanceof NVLong) {
        return NVLONG.getName() + sep + gnv.getName();
      } else if (gnv instanceof NVFloat) {
        return NVFLOAT.getName() + sep + gnv.getName();
      } else if (gnv instanceof NVDouble) {
        return NVDOUBLE.getName() + sep + gnv.getName();
      } else if (gnv instanceof NVBlob) {
        return NVBLOB.getName() + sep + gnv.getName();
      }
      return gnv.getName();
    }


    public static GNVTypeName toGNVTypeName(String name, char sep) {
      String[] tokens = name.split("\\" + sep);
      if (tokens.length > 1) {
        GNVType type = SharedUtil.lookupEnum(tokens[0], GNVType.values());
        if (type != null) {
          return new GNVTypeName(type, tokens[1]);
        }
      }
      return null;
    }

    public static GNVType toGNVType(Number number) {

      if (number != null) {
        if (number instanceof Float) {
          return NVFLOAT;
        }
        if (number instanceof Double) {
          return NVDOUBLE;
        }

        if (number instanceof Long) {
          return NVLONG;
        }

        if (number instanceof Integer) {
          return NVINT;
        }

        // double or float
        String numeric = number.toString();
        if (numeric.indexOf('.') == -1) {
          try {
            Integer.parseInt(numeric);
            return NVINT;
          } catch (NumberFormatException e) {
          }

          try {
            Long.parseLong(numeric);
            return NVLONG;
          } catch (NumberFormatException e) {
          }
        }
        else
        {
          try {
            float f = Float.parseFloat(numeric);
            String sf = Float.toString(f);
            if (sf.equals(numeric))
            return NVFLOAT;
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }

          try {
            Double.parseDouble(numeric);
            return NVDOUBLE;
          } catch (NumberFormatException e) {
          }
        }
        // long or int
      }

      return null;

    }

    public static GNVType toGNVType(NVConfig nvc) {
      if (byte[].class.equals(nvc.getMetaType())) {
        return NVBLOB;
      }
      if (!nvc.isArray()) {
        if (Long.class.equals(nvc.getMetaType())) {
          return NVLONG;
        }

        if (Integer.class.equals(nvc.getMetaType())) {
          return NVINT;
        }

        if (Float.class.equals(nvc.getMetaType())) {
          return NVFLOAT;
        }

        if (Double.class.equals(nvc.getMetaType())) {
          return NVDOUBLE;
        }

      }

      return null;

    }

  }

  public enum DayOfWeek
      implements GetNameValue<Integer> {

    SUNDAY("Sunday", 0),
    MONDAY("Monday", 1),
    TUESDAY("Tuesday", 2),
    WEDNESDAY("Wednesday", 3),
    THURSDAY("Thursday", 4),
    FRIDAY("Friday", 5),
    SATURDAY("Saturday", 6);

    private Integer value;
    private String name;

    DayOfWeek(String name, Integer value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Integer getValue() {
      return value;
    }

    public static DayOfWeek lookup(String str) {
      return SharedUtil.lookupEnum(str, DayOfWeek.values());
    }

    public static DayOfWeek lookup(int val) {
      return DayOfWeek.values()[val];
    }
  }

  /**
   * Months of Gregorian calendar.
   */
  public enum Month
      implements GetName {

    JANUARY("01-Jan"),
    FEBRUARY("02-Feb"),
    MARCH("03-Mar"),
    APRIL("04-Apr"),
    MAY("05-May"),
    JUNE("06-Jun"),
    JULY("07-Jul"),
    AUGUST("08-Aug"),
    SEPTEMBER("09-Sep"),
    OCTOBER("10-Oct"),
    NOVEMBER("11-Nov"),
    DECEMBER("12-Dec");

    private String name;

    Month(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }

  /**
   * This enum represents days in a month and includes the maximum number of possible days in a
   * month.
   */
  public enum DaysInMonth
      implements GetValue<Integer> {

    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    ELEVEN(11),
    TWELVE(12),
    THIRTEEN(13),
    FOURTEEN(14),
    FIFTEEN(15),
    SIXTEEN(16),
    SEVENTEEN(17),
    EIGHTEEN(18),
    NINETEEN(19),
    TWENTY(20),
    TWENTY_ONE(21),
    TWENTY_TWO(22),
    TWENTY_THREE(23),
    TWENTY_FOUR(24),
    TWENTY_FIVE(25),
    TWENTY_SIX(26),
    TWENTY_SEVEN(27),
    TWENTY_EIGHT(28),
    TWENTY_NINE(29),
    THIRTY(30),
    THIRTY_ONE(31),

    ;

    private Integer value;

    DaysInMonth(Integer value) {
      this.value = value;
    }

    @Override
    public Integer getValue() {
      return value;
    }

  }

  public enum DayPeriod
      implements GetName {

    AM("AM"),
    PM("PM");


    private String name;

    DayPeriod(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

  }

  public enum Bidi {
    LTR,
    RTL,
  }

  /**
   * Logical operators
   */
  public enum LogicalOperator
      implements GetValue<String>, QueryMarker {

    AND("AND"),
    OR("OR"),

    ;

    private String value;

    LogicalOperator(String value) {
      this.value = value;
    }

    @Override
    public String getValue() {
      return value;
    }

    public String toString() {
      return getValue();
    }

  }

  /**
   * Relational operators
   */
  public enum RelationalOperator
      implements GetValue<String> {

    EQUAL("="),
    NOT_EQUAL("!="),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),

    ;

    String value;

    RelationalOperator(String value) {
      this.value = value;
    }

    @Override
    public String getValue() {
      return value;
    }
  }

  /**
   * Utility Date pattern that can be used on the client and server side.
   */
  public enum DateTimePattern
      implements GetValue<String> {

    GMT_ZZZ("+00:00"),
    YEAR_MONTH_TZ("yyyy-MM ZZZ"),
    YEAR_MONTH_DAY_TZ("yyyy-MM-dd ZZZ"),
    YEAR_MONTH_DAY_HOURS_MINUTES_SECONDS_TZ("yyyy-MM-dd hh:mm:ss ZZZ"),
    HOURS_MINUTES_SECONDS_TZ("hh:mm:ss ZZZ"),

    ;

    private String value;

    DateTimePattern(String value) {
      this.value = value;
    }

    @Override
    public String getValue() {
      return value;
    }
  }

  /**
   * This enum contains NVPair display property as either default (depends on context), name, or
   * value.
   */
  public enum NVDisplayProp {
    DEFAULT,
    NAME,
    NAME_VALUE,
    VALUE
  }

  /**
   * Scan statuses
   */
  public enum ScanStatus {
    OK, // ok no infection
    INFECTED, // infected
    FAILED, // failed not known

  }

  /**
   * Return types
   */
  public enum ReturnType {
    NVENTITY,
    // this is the old way
    NVENTITY_LIST,
    VOID,
    BOOLEAN,
    STRING,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE,
    MAP,
    DYNAMIC_ENUM_MAP,
    DYNAMIC_ENUM_MAP_LIST,
    // this to replace NENTITY_LIST,
    NVENTITY_ARRAY,
    NVGENERIC_MAP,
  }

  /**
   * Mapping types
   */
  public final static DynamicEnumMap ASSOCIATION_TYPE =
      new DynamicEnumMap("AssociationType",
          new NVPair("ACCESS_CODE_TO_SYSTEM", "Access code of a remote system"),
          new NVPair("API_KEY_TO_CONFIG", "APIKey to APIConfig")
      );


  /**
   * This character array contains the numbers and letter representations for hexadecimal values.
   * This array is used to convert between string characters and hexadecimal byte and vice versa.
   */
  public final static char[] HEX_TOKENS =
      {
          '0',
          '1',
          '2',
          '3',
          '4',
          '5',
          '6',
          '7',
          '8',
          '9',
          'A',
          'B',
          'C',
          'D',
          'E',
          'F'
      };

  /**
   * This array contains a list of char values.
   */
  public static final CharSequence[] HTML_FILTERS =
      {
          "<pre>",
          "<PRE>",
          "</pre>",
          "</PRE>",
          "amp;",
          "AMP;"
      };

  /**
   * Used to wrap primitive types declared as the data type itself or as a class of the data type.
   *
   * @return wrapped class type
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> wrap(Class<T> c) {
    if (c.isPrimitive()) {
      for (Class<?>[] temp : PRIMITIVES_TO_WRAPPERS) {
        if (temp[0] == c) {
          return (Class<T>) temp[1];
        }
      }
    } else if (c.isArray()) {
      for (Class<?>[] temp : PRIMITIVES_ARRAY_TO_WRAPPERS) {
        if (temp[0] == c) {
          return (Class<T>) temp[1];
        }
      }
    }

    return c;
  }

  /**
   * This array contains the type and the class of the primitive type.
   */
  private final static Class<?>[][] PRIMITIVES_TO_WRAPPERS =
      {
          {boolean.class, Boolean.class},
          {byte.class, Byte.class},
          {char.class, Character.class},
          {double.class, Double.class},
          {float.class, Float.class},
          {int.class, Integer.class},
          {long.class, Long.class},
          {short.class, Short.class},
          {void.class, Void.class}
      };

  /**
   * This array contains the type and the class of the primitive array type.
   */
  private final static Class<?>[][] PRIMITIVES_ARRAY_TO_WRAPPERS =
      {
          {double[].class, Double[].class},
          {float[].class, Float[].class},
          {int[].class, Integer[].class},
          {long[].class, Long[].class},
      };


}
