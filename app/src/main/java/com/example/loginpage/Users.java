package com.example.loginpage;
public class Users {
    String useremail, userpassword, userId;

    public Users(String email, String password, String id){
        this.useremail = email;
        this.userpassword = password;
        this.userId = id;
    }

    public String getUseremail() {
        return useremail;
    }

    public void setUseremail(String useremail) {
        this.useremail = useremail;
    }

    public String getUserpassword() {
        return userpassword;
    }

    public void setUserpassword(String userpassword) {
        this.userpassword = userpassword;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}