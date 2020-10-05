package com.example.peta;

public class Profile {
    public String name;
    public String status;
    public String image;
    public String phone;

    public Profile() {
    }

    public Profile(String name, String status, String image, String phone) {
        this.name = name;
        this.status = status;
        this.image = image;
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String nama) {
        this.name = nama;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }


}
