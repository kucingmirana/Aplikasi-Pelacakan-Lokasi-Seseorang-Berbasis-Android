package com.example.peta;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class inputNamaTest {
    String nama = "alex";
    boolean checknama;
    InputName inputName;

    @Test
    public void input_nama() {

        if (nama.equals("alex")){
            checknama = true;
        }
        if(nama.isEmpty()){
            checknama = false;
        }

        assertTrue(checknama);
    }
}
