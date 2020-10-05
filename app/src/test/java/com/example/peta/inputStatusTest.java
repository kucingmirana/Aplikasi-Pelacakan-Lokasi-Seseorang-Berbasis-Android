package com.example.peta;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class inputStatusTest {
    String status = "tracked";
    boolean checknama;
    @Test
    public void input_status() {

        if (status.equals("tracked") || status.equals("tracker")){
            checknama = true;
        }

        assertTrue(checknama);
    }
}
