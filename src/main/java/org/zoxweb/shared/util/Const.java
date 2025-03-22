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
import java.util.concurrent.TimeUnit;

/**
 * Contains constants and enums.
 *
 * @author mzebib
 */
public class Const {

 // public static final String LOGGER_NAME = "zoxweb-core";
  public static final String TOKEN_TAG = "$$TAG$$";
  public static final Object[] EMPTY_ARRAY = new Object[0];
  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  //public static final String UTF8 = "UTF-8";

  public enum JavaClassVersion {
    VER_UNKNOWN("UNKNOWN", "UNKNOWN", 0, 0, false),
    //VER_1_0("1.0", 45, 3),
    VER_1_1("1.1","1.1", 45, 3, true),
    VER_1_2("1.2","1.2", 46, 0,true),
    VER_1_3("1.3","1.3", 47, 0, true),
    VER_1_4("1.4","1.4", 48, 0, true),
    VER_1_5("5","1.5", 49, 0, true),
    VER_1_6("6", "1.6", 50, 0, true),
    VER_1_7("7","1.7", 51, 0, true),
    VER_1_8("8","1.8", 52, 0, true),
    VER_1_9("9","1.9", 53, 0, false),
    VER_10("10","10", 53, 0, false),
    VER_11("11", "11", 55, 0, true),
    VER_12("12", "12", 56, 0, false),
    VER_13("13", "13", 57, 0, false),
    VER_14("14", "14", 58, 0, false),
    VER_15("15", "15", 59, 0, false),
    VER_16("16", "16", 60, 0, false),
    VER_17("17", "17", 61, 0, true),
    VER_18("18", "18", 62, 0, false),
    VER_19("19", "19", 63, 0, false),
    VER_20("20", "20", 64, 0, false),
    VER_21("21", "21", 65, 0, true),
    VER_22("22", "22", 66, 0, false),
    VER_23("23", "23", 67, 0, false),
    VER_24("24", "24", 68, 0, false),
    VER_25("25", "25", 69, 0, true),

    ;

    public final String VERSION;
    public final String ALT_VERSION;

    public final int MAJOR;
    public final int MINOR;
    public final boolean IS_LTS;

    JavaClassVersion(String version, String altVersion, int major, int minor, boolean isLTS) {
      this.VERSION = version;
      this.ALT_VERSION = altVersion;
      this.MAJOR = major;
      this.MINOR = minor;
      this.IS_LTS = isLTS;
    }

    public static JavaClassVersion lookup(String javaVersion)
    {
      if(!SUS.isEmpty(javaVersion))
      {
        JavaClassVersion[] all = JavaClassVersion.values();

        for(int i = 1; i < all.length; i++)
        {
          if(javaVersion.startsWith(all[i].ALT_VERSION))
            return all[i];
        }
      }

      return JavaClassVersion.VER_UNKNOWN;
    }


    public String toString() {
      return VERSION + "," + MAJOR + "." + MINOR;
    }

    public static JavaClassVersion lookup(int major, int minor) {
      for (JavaClassVersion ver : values()) {
        if (ver.MAJOR == major && ver.MINOR == minor) {
          return ver;
        }
      }

      return VER_UNKNOWN;
    }

//    public static JavaClassVersion lookup(String version) {
//      if (!SharedStringUtil.isEmpty(version)) {
//        for (JavaClassVersion ver : values()) {
//          if (ver.VERSION.equalsIgnoreCase(version)) {
//            return ver;
//          }
//        }
//      }
//
//      return VER_UNKNOWN;
//    }
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
    ONE("1", true),
    ZERO("0", false),


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
      return SharedUtil.lookupEnum(str, Bool.values());
    }

    public static boolean lookupValue(String str) {
      Bool ret = parse(str);

      if (ret == null) {
        throw new IllegalArgumentException("Invalid Bool token " + str);
      }

      return ret.value;
    }

    public static int toInt(String str)
    {
      return lookupValue(str) ? 1 : 0;
    }

    public static boolean lookupValue(int val)
    {
      return val != 0;
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


  public enum FunctionStatus {
    CONTINUE,
    ERROR,
    COMPLETED,
    PARTIAL
  }

  public enum ResourceType {
    FILE,
    FOLDER,
    FORM,
    TEMP_FILE,
    NV_ENTITY,
    NV_GENERIC_MAP
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
    QUERY,
    /**
     * Internal resource specific to the caller
     */
    RESOURCE,
  }

//  public enum Unit
//      implements GetName {
//
//    EM("em"),
//    PIXEL("px"),
//    PERCENT("%"),
//
//    ;
//
//    private final String name;
//
//    Unit(String name) {
//      this.name = name;
//    }
//
//    @Override
//    public String getName() {
//      return name;
//    }
//
//    public static Unit parseUnit(String str) {
//      if (!SharedStringUtil.isEmpty(str)) {
//        str = str.toLowerCase();
//
//        for (Unit unit : Unit.values()) {
//          if (str.endsWith(unit.getName())) {
//            return unit;
//          }
//        }
//      }
//
//      return null;
//    }
//  }

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
   * This enum represents size in bytes of default memory constants.
   *
   * @author mnael
   */
  public enum SizeInBytes
      implements GetName {

    // Byte
    B("B", 1),

    // Kilobytes
    K("KB", 1024),

    // Megabytes
    M("MB", K.SIZE * 1024),

    // Gigabytes
    G("GB", M.SIZE * 1024),

    // Terabytes
    T("TB", G.SIZE * 1024),

    // Petabytes
    P("PB", T.SIZE * 1024);

    /**
     * Returns the size of bytes.
     */
    public final long SIZE;


    private final String name;

    SizeInBytes(String name, long value) {
      this.name = name;
      SIZE = value;
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
          multiplier = bs.SIZE;
          str = str.substring(0, str.length() - bs.getName().length());
          break;
        } else if (str.endsWith(bs.name())) {
          multiplier = bs.SIZE;
          str = str.substring(0, str.length() - bs.name().length());
          break;
        }
      }

      return Long.parseLong(str) * multiplier;
    }


    public long sizeInBytes(long size) {
      return SIZE * size;
    }

    @Override
    public String getName() {
      return name;
    }

    public long convertBytes(long sizeInBytes)
    {
      return sizeInBytes/ SIZE;
    }

    public double convertBytesDouble(long sizeInBytes)
    {
      return (double)sizeInBytes/(double) SIZE;
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
   * This enum is used to describe the string to be expected or converted to
   */
  public enum StringType {
    UPPER,// to upper case
    LOWER,// to lower case
    AS_IS,// as is
  }

  /**
   * Define the executor pool type
   */
  public enum ExecPool
  {
    // no executor
    NO_EXEC,
    // TaskUtil.getDefaultTaskProcessor()
    DEFAULT,
    // Java executor pool service
    JAVA,
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
    MILLI(1, TimeUnit.MILLISECONDS, "millis", "milli"),

    // One second in milliseconds
    SECOND(MILLI.MILLIS * 1000, TimeUnit.SECONDS,"seconds", "second", "secs", "sec", "s"),

    // One minute in milliseconds
    MINUTE(SECOND.MILLIS * 60, TimeUnit.MINUTES,"minutes", "minute", "mins", "min", "m"),

    // One hour in milliseconds
    HOUR(MINUTE.MILLIS * 60, TimeUnit.HOURS,"hours", "hour", "h"),

    // One day in milliseconds
    DAY(HOUR.MILLIS * 24, TimeUnit.DAYS,"days", "day","d"),

    // One week in milliseconds
    WEEK(DAY.MILLIS * 7, null, "weeks", "week", "w"),
    YEAR((long) ((float)DAY.MILLIS*365.2425), null, "years", "year", "y");
    public final long MILLIS;
    public final TimeUnit UNIT;
    private final String[] tokens;

    TimeInMillis(long duration, TimeUnit unit, String... tokens) {
      this.MILLIS = duration;
      this.tokens = tokens;
      this.UNIT = unit;
    }

    public String[] getTokens()
    {
      return tokens;
    }




    /**
     * Converts string to time in milliseconds.
     *
     * @return the time string value in millis
     */
    public static long toMillisNullZero(String time)
            throws IllegalArgumentException {
      if(time == null)
        return 0;

      return toMillis(time);
    }

    public static TimeInMillis toTimeInMillis(String unit)
    {
      int index =unit.indexOf("/");
      if (index != -1)
        unit = unit.substring(index+1);

      for (TimeInMillis tis : TimeInMillis.values())
      {
        for (String  token : tis.tokens)
        {
          if(token.equalsIgnoreCase(unit))
            return tis;
        }
      }
      return null;
    }

    public static TimeInMillis convert(TimeUnit unit)
    {
      switch(unit)
      {

        case MILLISECONDS:
          return TimeInMillis.MILLI;

        case SECONDS:
          return TimeInMillis.SECOND;

        case MINUTES:
          return TimeInMillis.MINUTE;

        case HOURS:
          return TimeInMillis.HOUR;

        case DAYS:
          return TimeInMillis.DAY;

        case NANOSECONDS:
        case MICROSECONDS:
        default:
          throw new IllegalArgumentException(unit + " not supported.");
      }
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
     * Converts the time to nanosecond value (10 power -9 second)
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

    /**
     * multiply MILLIS*val
     * @param val to multiplied with
     * @return MILLIS*val
     */
    public long mult(int val)
    {
      return MILLIS*val;
    }
    public long mult(long val)
    {
      return MILLIS*val;
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



    public static String toString(long millis)
    {
      boolean pastTime = false;
      if (millis < 0) {
        millis = -millis;
        pastTime = true;
      }

      long week = millis / TimeInMillis.WEEK.MILLIS;
      long remainder = millis % TimeInMillis.WEEK.MILLIS;
      long day = remainder / TimeInMillis.DAY.MILLIS;
      remainder %= TimeInMillis.DAY.MILLIS;

      long hour = remainder / TimeInMillis.HOUR.MILLIS;
      remainder %= TimeInMillis.HOUR.MILLIS;
      long min = remainder / TimeInMillis.MINUTE.MILLIS;
      remainder %= TimeInMillis.MINUTE.MILLIS;
      long sec = remainder / TimeInMillis.SECOND.MILLIS;
      long mil = millis % SECOND.MILLIS;


      StringBuilder sb = new StringBuilder();
      if (week > 0) {
        sb.append(week);
        sb.append("W:");
      }
      if (day > 0 || week > 0)
      {
        sb.append(day);
        sb.append("D:");

      }
      sb.append((hour <= 9 ? "0" : ""));sb.append(hour);
      sb.append(':');
      sb.append((min <= 9 ? "0" : ""));sb.append(min);
      sb.append(':');
      sb.append((sec <= 9 ? "0" : ""));sb.append(sec);

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

      return pastTime ? "-" + sb: sb.toString();
    }



    /**
     * Checks if given year is a leap year.
     * A year is a leap year if it is a multiple of 400 or a multiple of 4 but not 100.
     * Example: 2015 (non-leap year) returns false and 2008 (leap year) returns true.
     *
     * @param year to be checked
     * @return true if leap year, false if not
     */
    public static boolean isLeapYear(int year)
    {
      return (year % 400 == 0) || ((year % 100) != 0 && (year % 4 == 0));
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

    private final String name;
    private final char sign;
    private final int hours;
    private final int minutes;

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
    CUSTOM
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


    private final String name;

    DocumentType(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

  }


  public enum GNVType
      implements GetName,
                 GetMetaType
  {
    NVBLOB("byte[]", byte[].class),
    NVBOOLEAN("boolean", boolean.class),
    NVINT("int", int.class),
    NVLONG("long", long.class),
    NVFLOAT("float", float.class),
    NVDOUBLE("double", double.class),
    ;

    private final String name;
    private final Class<?> clazz;

    GNVType(String name, Class<?> clazz) {
      this.name = name;
      this.clazz = clazz;
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


    public static GNVTypeName toGNVTypeName(char sep, String name) {
      String[] tokens = name.split(new StringBuilder().append('\\').append(sep).toString());
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

    @Override
    public Class<?> getMetaType() {
      return clazz;
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

    private final String value;

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
    // DO NOT CHANGE ORDER
    // It is mandatory to keep them as is
    NOT_EQUAL("!="),
    GTE(">="),
    LTE("<="),
    EQUAL("="),
    GT(">"),
    LT("<"),
    ;

    private final String value;

    RelationalOperator(String value) {
      this.value = value;
    }

    @Override
    public String getValue() {
      return value;
    }
  }

  public enum RegEx
    implements GetNameValue<String>
  {
    CONTAINS_NO_CASE("ContainsNoCase",  "(?i).*" + TOKEN_TAG + "(.*?)"),
    EMAIL("Email","^[\\w.%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

    ;


    private final NVPair nv;

    RegEx(String name, String value)
    {
      nv = new NVPair(name, value);
    }


    /**
     * @return the name of the object
     */
    @Override
    public String getName() {
      return nv.getName();
    }

    /**
     * Returns the value the actual regular expression.
     *
     * @return typed value
     */
    @Override
    public String getValue() {
      return nv.getValue();
    }

    /**
     *
     * @param token to be added to the regular expression
     * @return regular express where $$TOKEN$$ replaced with token param if the regular expression contains the $$TOKEN$$
     */
    public String toRegEx(String token, boolean literal)
    {
      if(!SUS.isEmpty(token) || literal)
      {
        token = "\\Q" + token + "\\E";
      }
      return SharedStringUtil.embedText(getValue(), TOKEN_TAG, token);
    }

//    public boolean matches(String toMatch, String token)
//    {
//      return toMatch.matches(toRegEx(token));
//    }
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
