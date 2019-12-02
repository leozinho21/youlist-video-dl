package com.thesis.home.youlist.helpers;

import java.util.List;
import java.util.Set;

/**
 * Created by HOME on 12/3/2016.
 */
public class ListUtils {


    public static boolean isEmptyList(List<?> list){

        if( list == null ) return true;
        else if( list.size() ==0) return true;
        else return false;
    }

    public static String[] toArray(Set<String> set){

        if( set == null ) return null;

        String[] arr = new String[set.size()];

        int i = 0;

        for(String s : set){
            arr[i] = s ;
            i++;
        }

        return arr;
    }
}
