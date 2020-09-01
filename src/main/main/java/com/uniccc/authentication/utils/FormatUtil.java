package com.uniccc.authentication.utils;


import lombok.extern.slf4j.Slf4j;


@Slf4j
public class FormatUtil {

    public static String getString(Object object){
        if(object == null){
            return null;
        }
        return object.toString();
    }

    public static Long getLong(Object object){
        if(object == null){
            return null;
        }
        Long l = null;
        try{
            l = Long.parseLong(getString(object));
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return l;
    }


    public static Integer getInteger(Object object){
        if(object == null){
            return null;
        }
        return formatInteger(getString(object));
    }

    private static Integer formatInteger(String str){
        Integer i = null;
        try {
            i = Integer.parseInt(str);
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return i;
    }

}
