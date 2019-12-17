package main.java.server.proxies;

import main.java.adminapp.AdminUI;
import main.java.server.interfaces.AdminInterface;
import main.java.server.proxies.entityinfo.UserInfo;

public class ExternalAuthenticationProxy {


    public static boolean askAdmin(UserInfo u){
       // System.out.println("Admin: Authenticate " + u.name + " with address " + u.address + ", with ID " + u.nationalIDNo);
        return AdminInterface.adminInterface.admin.yesOrNo();
    }
}
