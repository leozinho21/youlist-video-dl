package com.thesis.home.youlist.helpers;


/**
 * Created by HOME on 17/4/2016.
 */
public class StringUtils {

    public static boolean notEmpty(String string){

        if( string == null ) return false;
        else if( !string.isEmpty()) return true;
        else return false;
    }
}
