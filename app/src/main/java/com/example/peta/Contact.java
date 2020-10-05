package com.example.peta;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable {

    private String name,phone,photo,uid,friend;

    public Contact() {
    }

    public Contact(String name, String phone, String photo, String uid, String friend) {
        this.name = name;
        this.phone = phone;
        this.photo = photo;
        this.uid = uid;
        this.friend = friend;
    }

    protected Contact(Parcel in) {
        name = in.readString();
        phone = in.readString();
        photo = in.readString();
        uid = in.readString();
        friend = in.readString();
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFriend() {
        return friend;
    }

    public void setFriend(String friend) {
        this.friend = friend;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(phone);
        dest.writeString(uid);
        dest.writeString(photo);
        dest.writeString(friend);
    }
}
