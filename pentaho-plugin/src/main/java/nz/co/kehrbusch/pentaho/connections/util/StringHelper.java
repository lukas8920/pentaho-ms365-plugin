package nz.co.kehrbusch.pentaho.connections.util;

import static org.apache.commons.lang.StringUtils.trim;

public class StringHelper {
    public static boolean isEmpty(String input){
        String tmp = trim(input);
        return tmp == null || tmp.length() == 0;
    }
}
