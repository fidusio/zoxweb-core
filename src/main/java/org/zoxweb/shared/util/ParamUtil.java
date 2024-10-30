package org.zoxweb.shared.util;





import org.zoxweb.shared.data.ParamInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParamUtil {
    private ParamUtil(){}

//    public static class ParamType
//        implements GetName
//    {
//        private String name;
//    }

    public static class ParamInfoList
    {
        Map<String, ParamInfo> byName = new LinkedHashMap<String, ParamInfo>();
        Map<String, ParamInfo> byParam = new LinkedHashMap<String, ParamInfo>();


        public ParamInfoList add(String name, ParamInfo.ValueType valueType, String param, boolean mandatory, boolean caseSensitive)
        {

            ParamInfo toAdd = new ParamInfo();
            toAdd.setName(name);
            toAdd.setValueType(valueType);
            toAdd.setParam(param);
            toAdd.setMandatory(mandatory);
            toAdd.setCaseSensitive(caseSensitive);


            return add(toAdd);
        }


        public ParamInfoList add(ParamInfo pi)
        {
            byName.put(pi.getName().toLowerCase(), pi);
            if(pi.getParam() != null)
            {
                byParam.put(pi.getParam().toLowerCase(), pi);
            }
            return this;
        }
        public ParamInfo lookup(String paramName)
        {
            ParamInfo ret =  byName.get(paramName.toLowerCase());
            if(ret == null)
                ret = byParam.get(paramName.toLowerCase());

            return ret;
        }


        public String toString()
        {
            return byName+"\n" + byParam;
        }

    }

    public static class ParamMap
    {
        private Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();
        private boolean ignoreCase = false;
        private final int counter;
        private ParamMap(boolean ignoreCase, Map<String, List<String>> map, int  length)
        {
            this.map = map;
            this.ignoreCase = ignoreCase;
            this.counter = length;
        }

        public int intValue(int index)
        {
            return intValue(""+index);
        }

        public int intValue(String name)
        {
            return intValue(name, null);
        }

        public int intValue(String name, Integer defaultValue)
        {
           List<String> ret = lookup(name);
           if(ret == null)
           {
               if(defaultValue == null)
                throw new IllegalArgumentException("Parameter " + name + " not found");
               return defaultValue;
           }

           if (ret.size() == 1)
           {
               return Integer.parseInt(ret.get(0));
           }

           throw new IllegalArgumentException(name + " value not found or no valid");
        }

        public int smartIntValue(String name, Integer defaultValue)
        {
            List<String> ret = lookup(name);
            if(ret == null)
            {
                if(defaultValue == null)
                    throw new IllegalArgumentException("Parameter " + name + " not found");
                return defaultValue;
            }

            if (ret.size() == 1)
            {
                return SharedUtil.parseInt(ret.get(0));
            }

            throw new IllegalArgumentException(name + " value not found or no valid");
        }

        public long smartLongValue(String name, Long defaultValue)
        {
            List<String> ret = lookup(name);
            if(ret == null)
            {
                if(defaultValue == null)
                    throw new IllegalArgumentException("Parameter " + name + " not found");
                return defaultValue;
            }

            if (ret.size() == 1)
            {
                return SharedUtil.parseLong(ret.get(0));
            }

            throw new IllegalArgumentException(name + " value not found or no valid");
        }


        public float floatValue(int index)
        {
            return floatValue(""+index);
        }

        public float floatValue(String name)
        {
            return floatValue(name, null);
        }

        public float floatValue(String name, Float defaultValue)
        {
            List<String> ret = lookup(name);
            if(ret == null)
            {
                if(defaultValue == null)
                    throw new IllegalArgumentException("Parameter " + name + " not found");
                return defaultValue;
            }

            if (ret.size() == 1)
            {
                return Float.parseFloat(ret.get(0));
            }

            throw new IllegalArgumentException(name + " value not found or no valid");
        }



        public int hexValue(int index)
        {
            return hexValue(""+index);
        }

        public int hexValue(String name)
        {
            return hexValue(name, null);
        }

        public int hexValue(String name, Integer defaultValue)
        {
            List<String> ret = lookup(name);
            if(ret == null)
            {
                if(defaultValue == null)
                    throw new IllegalArgumentException("Parameter " + name + " not found");
                return defaultValue;
            }

            if (ret.size() == 1)
            {
                return Integer.parseInt(ret.get(0), 16);
            }

            throw new IllegalArgumentException(name + " value not found or no valid");
        }

        /**
         * @return the number of parameters without name
         */
        public int namelessCount()
        {
            return counter;
        }

        public long longValue(int index)
        {
            return intValue(""+index);
        }

        public long longValue(String name)
        {
            return longValue(name, null);
        }
        public long longValue(String name, Long defaultValue)
        {
            List<String> ret = lookup(name);
            if(ret == null)
            {
                if(defaultValue == null)
                    throw new IllegalArgumentException("Parameter " + name + " not found");
                return defaultValue;
            }
            if (ret.size() == 1)
            {
                return Long.parseLong(ret.get(0));
            }

            throw new IllegalArgumentException(name + " value not found or no valid");
        }


        public double doubleValue(int index)
        {
            return intValue(""+index);
        }

        public double doubleValue(String name)
        {
            return doubleValue(name, null);
        }
        public double doubleValue(String name, Double defaultValue)
        {
            List<String> ret = lookup(name);
            if(ret == null)
            {
                if(defaultValue == null)
                    throw new IllegalArgumentException("Parameter " + name + " not found");
                return defaultValue;
            }

            if (ret.size() == 1)
            {
                return Double.parseDouble(ret.get(0));
            }

            throw new IllegalArgumentException(name + " value not found or no valid");
        }

        public <E extends Enum<?>> E enumValue(String name, Enum ...enums)
        {
            List<String> ret = lookup(name);
            if (ret.size() == 1)
            {
                return SharedUtil.lookupEnum(ret.get(0), enums);
            }
           return null;
        }

        public boolean parameterExists(String name)
        {
            List<String> ret = lookup(name);
            return (ret != null && ret.size() == 1);
        }

        public boolean nameExists(String name)
        {
            return lookup(name) != null;
        }



        public boolean booleanValue(String name)
        {
            return booleanValue(name, true);
        }

        public boolean booleanValue(String name, boolean notFoundOk)
        {
            List<String> ret = lookup(name);
            if(ret == null)
            {
                if(!notFoundOk)
                    throw new IllegalArgumentException("Parameter " + name + " not found");
                return false;
            }

            if (ret.size() == 1)
            {
                return Const.Bool.lookupValue(ret.get(0));

            }

            throw new IllegalArgumentException(name + " value not found or no valid");
        }

        public String stringValue(int index)
        {
            return stringValue(""+index);
        }
        public String stringValue(String name)
        {
            return stringValue(name, null, false);
        }

        public String stringValue(String name, boolean nullOk)
        {
            return stringValue(name, null, nullOk);
        }

        public String stringValue(String name, String defaultValue)
        {
            return stringValue(name, defaultValue, true);
        }

        public String stringValue(String name, String defaultValue, boolean nullOk)
        {
            List<String> ret = lookup(name);
            if(ret == null || ret.isEmpty())
            {
                if(defaultValue == null && !nullOk)
                    throw new IllegalArgumentException("Parameter " + name + " not found");
                return defaultValue;
            }

            if (ret.size() == 1)
            {
                return ret.get(0);
            }

            throw new IllegalArgumentException(name + " value not found or no valid");
        }


        public List<String> lookup(int index)
        {
            return lookup(""+index);
        }

        public List<String> lookup(String name)
        {
            if(ignoreCase)
            {
                name = name.toLowerCase();
            }
            
            return map.get(name);
             
        }

        public NVPair asNVPair(String name)
        {
            List<String> ret = map.get(name);
            if (ret != null && ret.size() > 0)
                return new NVPair(name, ret.get(0));

            return null;
        }

        public String[] namelessValues(String ...toAppend)
        {
            List<String> ret = new ArrayList<String>();
            SharedUtil.addTo(ret, toAppend);
            for (int i=0 ; i < namelessCount(); i++){
                ret.add(stringValue((i)));
            }
            return ret.toArray(new String[0]);
        }

        @Override
        public String toString() {
            return "ParamMap{" +
                    "ignoreCase=" + ignoreCase +
                    ", map=" + map +

                    '}';
        }
    }



    public static ParamMap parse(String nvTag, String ...args)
    {
        return parse(nvTag, true, 0, args);
    }

    public static ParamMap parse(String nvTag, int index, String ...args)
    {
        return parse(nvTag, true, index, args);
    }

    public static ParamMap parse(String nvTag, boolean ignoreCase, String ...args)
    {
        return parse(nvTag,ignoreCase, 0, args);
    }


    public static ParamMap parse(String nvTag, boolean ignoreCase, int index, String ...args)
    {
        Map<String, List<String>> retMap = new LinkedHashMap<String, List<String>>();


        int counter = 0;
        for(; index < args.length; index++)
        {
            if (!SUS.isEmpty(args[index]))
            {
                String name = null;
                String value = null;
                if(args[index].startsWith(nvTag))
                {
                    name = args[index++];
                    if (index < args.length)
                    {
                        value = args[index];
                    }
                }
                else if(args[index].indexOf('=') != -1)
                {
                    // we have name=value
                    NVPair nvp = SharedUtil.toNVPair(args[index]);
                    name = nvp.getName();
                    value = nvp.getValue();
                }
                else
                {
                    value = args[index];
                }

                if(name == null)
                {
                    name = "" + counter++;
                }

                if(ignoreCase)
                {
                    name = name.toLowerCase();
                }


                if(retMap.get(name) == null)
                {
                    retMap.put(name, new ArrayList<String>());
                }

                retMap.get(name).add(value);
            }

        }

        return new ParamMap(ignoreCase, retMap, counter);
    }


    public static ParamMap parse(ParamInfoList piList, String ...args)
    {
        return parse(piList, 0, args);
    }

    public static ParamMap parse(ParamInfoList piList, int index, String ...args)
    {
        Map<String, List<String>> retMap = new LinkedHashMap<String, List<String>>();


        int counter = 0;
        ParamInfo lastMulti = null;
        for(; index < args.length; index++)
        {
            if (!SUS.isEmpty(args[index]))
            {
                String name = null;
                String value = null;


                ParamInfo pi = piList.lookup(args[index]);


                if(pi==null)
                {
                    if (lastMulti != null)
                    {
                        value = args[index];
                        pi = lastMulti;
                        name = pi.getParam();
                        if (name == null)
                            name = pi.getName();
                    }
                    else
                    {
                        lastMulti = null;
                        name = "" + counter++;
                        value = args[index];
                    }
                }
                else
                {
                    lastMulti = null;
                    name = pi.getParam();
                    if (name == null)
                        name = pi.getName();

                    if (pi.getValueType() == ParamInfo.ValueType.NONE)
                    {
                        value = name;
                    }
                    else if (pi.getValueType() == ParamInfo.ValueType.MULTI)
                    {
                        lastMulti = pi;
                        value = args[++index];

                    }
                    else
                    {
                        value = args[++index];
                    }

                }

                if(lookup(name, retMap) == null)
                {
                    ArrayList<String> al = new ArrayList<String>();
                    if(pi != null)
                      put(pi, al, retMap);
                    else
                      put(name, al, retMap);

                }


                lookup(name, retMap).add(value);
                //retMap.get(name.toLowerCase()).add(value);
            }

        }

        return new ParamMap(true, retMap, counter);
    }

    private static List<String> lookup(String key, Map<String, List<String>> map)
    {
        return map.get(key.toLowerCase());
    }

    private static void put(String key, List<String> list, Map<String, List<String>> map)
    {
        if(key != null)
            map.put(key.toLowerCase(), list);
    }

    private static void put(ParamInfo key, List<String> list, Map<String, List<String>> map)
    {
        put(key.getParam(), list, map);
        put(key.getName(), list, map);
    }




}
