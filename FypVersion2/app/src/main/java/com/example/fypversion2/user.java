package com.example.fypversion2;

public class user {

    String Displayname;


    String Email;
    long createdAt;

    public user (){};
    public user(String displayname,String email,long createdAt){
        this.Displayname=displayname;
        this.Email=email;
        this.createdAt=createdAt;
    }


    public String getDisplayname() {
        return Displayname;
    }

    public String getEmail() {
        return Email;
    }

    public long getCreatedAt() {
        return createdAt;
    }

}
