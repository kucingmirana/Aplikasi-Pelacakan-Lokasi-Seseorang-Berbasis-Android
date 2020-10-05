package com.example.peta;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class phoneLoginActivityTest {
    @Test
    public void input_phone_number() {
        String phonenumberLng = "00000000000";
        String phonenumber= "81219457622";

        assertEquals(phonenumberLng.length(), phonenumber.length());
    }
}
