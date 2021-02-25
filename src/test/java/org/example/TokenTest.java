package org.example;

import org.example.utils.MD5Util;

public class TokenTest {


    public static void main(String[] args) {



        String appId = "asdf";
        String key = "12345678954556";

        long timestamp = System.currentTimeMillis();
        System.out.println(timestamp);
        String signString =  timestamp + appId + key;
        String signature = MD5Util.encode(signString);

        System.out.println(signature);






    }
}
