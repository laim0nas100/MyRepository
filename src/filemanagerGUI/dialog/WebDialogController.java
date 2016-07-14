/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI.dialog;

import java.net.HttpURLConnection;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import utility.ErrorReport;
import LibraryLB.Log;
import filemanagerLogic.Enums;

/**
 * FXML Controller class
 *
 * @author lemmin
 */
public class WebDialogController extends BaseDialog{
   @FXML public WebView browser;
   


    public void afterShow(Enums.WebDialog info) {
        String path = "";
        
        try{
            path = info.address;
            Log.writeln(path);
            if(!isInternetReachable(path)){
                path = "file:///"+System.getProperty("user.dir")+"/"+info.local;
            }
            Log.writeln("Loading from local");
            browser.getEngine().load(path);
            
        }catch(Exception e){
            ErrorReport.report(e);
        }
    }
    public static boolean isInternetReachable(String path){
            try {
                //make a URL to a known source
                URL url = new URL(path);

                //open a connection to that source
                HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();

                //trying to retrieve data from the source. If there
                //is no connection, this line will fail
                Object objData = urlConnect.getContent();
            }catch (Exception e) {
                //ErrorReport.report(e);
                return false;
            }
            return true;
        }
    
}
