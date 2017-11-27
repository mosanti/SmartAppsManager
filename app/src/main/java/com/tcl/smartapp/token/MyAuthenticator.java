package com.tcl.smartapp.token;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * Created by user on 5/24/16.
 */
public class MyAuthenticator extends Authenticator {
    String userName="";
    String password="";
    public MyAuthenticator() {
        }
    public MyAuthenticator(String userName,String password){
        this.userName=userName;
        this.password=password;
        }
    protected PasswordAuthentication getPasswordAuthentication(){
        return new PasswordAuthentication(userName, password);
        }

}
