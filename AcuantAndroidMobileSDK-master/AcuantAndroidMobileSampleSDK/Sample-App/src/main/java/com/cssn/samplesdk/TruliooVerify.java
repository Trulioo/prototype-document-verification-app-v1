package com.cssn.samplesdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Base64;

import com.trulioo.normalizedapi.ApiClient;
import com.trulioo.normalizedapi.ApiException;
import com.trulioo.normalizedapi.api.ConfigurationApi;
import com.trulioo.normalizedapi.api.ConnectionApi;
import com.trulioo.normalizedapi.api.VerificationsApi;
import com.trulioo.normalizedapi.model.DataFields;
import com.trulioo.normalizedapi.model.Document;
import com.trulioo.normalizedapi.model.PersonInfo;
import com.trulioo.normalizedapi.model.VerifyRequest;
import com.trulioo.normalizedapi.model.VerifyResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by conor.delahunty on 2018-03-05.
 */

public class TruliooVerify {
    private byte[] truliooBack;
    private byte[] truliooFront = new byte[0];
    private byte[] truliooSelfie;
    private ApiClient apiClient;
    private String documentType;
    private String firstName;
    private String surname;
    private String country;
    private static String username;
    private static String password;


    public TruliooVerify(){
        apiClient = new ApiClient();
        apiClient.setUsername(username);
        apiClient.setPassword(password);
    }

    public void setTruliooFront(byte[] tFront){
        truliooFront = tFront;
    }
    public void setTruliooBack(byte[] tBack){
        truliooBack = tBack;
    }
    public void setTruliooSelfie(byte[] tSelfie){
        truliooSelfie = tSelfie;
    }
    public void setDocument(String docType){
        documentType = docType;
    }
    public void setFirstName(String fName){
        firstName = fName;
    }
    public void setSurname(String sName){
        surname = sName;
    }
    public void setCountry(String countryCode){
        country = countryCode;
    }

    public void setUsername(String userName){
        username = userName;
        apiClient.setUsername(userName);
    }

    public void setPassword(String psswrd){
        password = psswrd;
        apiClient.setPassword(psswrd);
    }

    private void reconstructImg(Context context)  throws FileNotFoundException, IOException {
        File path = context.getExternalFilesDir(null);
        File file = new File(path.getParent(), "byteArray.txt");
        int length = (int) file.length();

        byte[] bytes = new byte[length];
        FileInputStream in = new FileInputStream(file);
        try {
            in.read(bytes);
        } finally {
            in.close();
        }
        byte[] imgbyt = Base64.decode(bytes, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imgbyt, 0, imgbyt.length);
        MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap,"frontImage" , "description");

    }

    public void saveImgToGallery(Bitmap img, String name, Context context){
        MediaStore.Images.Media.insertImage(context.getContentResolver(), img, name,"description");
    }

    public String verify(){
        TruliooAPIVerifyTask truliooAPITask = new TruliooAPIVerifyTask();
        VerifyResult result = new VerifyResult();

        try {
            result = truliooAPITask.execute(result).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result.getTransactionID();
    }

    public byte[] bitmapToByteArray(Bitmap bmp){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray =  stream.toByteArray();
       // byte[] b64 = Base64.encode(byteArray, Base64.DEFAULT);
        return byteArray;
    }

    private Document buildDocument(){
        String truliooFrontStr = "";
        String truliooBackStr = "";
        String truliooSelfieStr = "";

        if(truliooFront != null){
            byte[] b64 = Base64.encode(truliooFront, Base64.DEFAULT);
            truliooFrontStr = new String(b64);
        }
        else truliooFrontStr = null;

        if(truliooBack != null){
            byte[] back64 = Base64.encode(truliooBack, Base64.DEFAULT);
            truliooBackStr = new String(back64);
        }
        else truliooBackStr = null;

        if(truliooSelfie != null) {
            byte[] selfie64 = Base64.encode(truliooSelfie, Base64.DEFAULT);
            truliooSelfieStr = new String(selfie64);
        }
        else truliooSelfieStr = null;

        //DEBUG Document Build
        Document doc = new Document()
                .documentFrontImageString(truliooFrontStr)
                .documentBackImageString(truliooBackStr)
                .livePhotoString(truliooSelfieStr)
                .documentType(documentType);
        //DEBUG Document Build

        //Release Document Build. Uncomment when upgrading to "com.trulioo:normalizedapi:1.+"
        /*
        Document doc = new Document()
                .documentFrontImage(truliooFrontStr)
                .documentBackImage(truliooBackStr)
                .livePhoto(truliooSelfieStr)
                .documentType(documentType);
        */
        //Release Document Build
        return doc;
    }

    private VerifyRequest buildRequest(){
        VerifyRequest request = new VerifyRequest();
        request = request
                .acceptTruliooTermsAndConditions(Boolean.TRUE)
                .cleansedAddress(false)
                .configurationName("Identity Verification")
                .countryCode(country)
                .timeout(1000)
                .dataFields(new DataFields()
                        .personInfo(new PersonInfo()
                                .firstGivenName(firstName)
                                .firstSurName(surname))
                        .document(buildDocument())
                );
        return request;
    }

    private class TruliooAPIVerifyTask extends AsyncTask<VerifyResult, Void, VerifyResult> {

        protected VerifyResult doInBackground(VerifyResult... verifyResult) {
            VerificationsApi verificationClient = new VerificationsApi(apiClient);
            VerifyRequest request;
            VerifyResult result = verifyResult[0];
            try{
                request = buildRequest();
                System.out.println(request.toString());
                result = verificationClient.verify(request);
            } catch(ApiException e){
                System.out.println("EXCEPTIONTHROWN");
                System.out.println(e.toString());
                System.out.println(e.getCode());
                System.out.println(e.getResponseBody());
                System.out.println(e.getResponseHeaders());
            }
            return result;
        }

        protected void onPostExecute(VerifyResult result) {
            System.out.println("Verify API Call Complete");
            System.out.println(result.toString());
            //progressBar.setVisibility(View.INVISIBLE);
           // confirmDialog(result);
        }
    }


    public boolean isAuthenticated(){
        TruliooAPIAuthenticateTask truliooAPIAuthenticateTask = new TruliooAPIAuthenticateTask();
        String authString = "";
        try {
            authString = truliooAPIAuthenticateTask.execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if (("Hello " + username).equals(authString)){
                return true;
            }

        return false;
    }

    private class TruliooAPIAuthenticateTask extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... voids) {
            ConnectionApi connectionApi = new ConnectionApi(apiClient);
            String authString = "";
            try{
                authString = connectionApi.testAuthentication();
            } catch(ApiException e){
                System.out.println("EXCEPTIONTHROWN");
                System.out.println(e.toString());
                System.out.println(e.getCode());
                System.out.println(e.getResponseBody());
                System.out.println(e.getResponseHeaders());
            }
            return authString;
        }

        protected void onPostExecute(String result) {
            System.out.println("Authenticate API Call Complete");
            System.out.println(result.toString());
        }
    }

    public List<String> getCountryCodes(){
        TruliooAPICountryCodesTask truliooAPICountryCodesTask = new TruliooAPICountryCodesTask();
        List<String> countryCodeArray = new ArrayList<>();
        try {
            countryCodeArray = truliooAPICountryCodesTask.execute("Identity Verification").get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return countryCodeArray;
    }

    private class TruliooAPICountryCodesTask extends AsyncTask<String, Void, List<String>> {

        protected List<String> doInBackground(String... configName) {
            ConfigurationApi configurationApi = new ConfigurationApi(apiClient);
            List<String> countryCodes = new ArrayList<>();
            try{
                countryCodes = configurationApi.getCountryCodes("Identity Verification");
            } catch(ApiException e){
                System.out.println("EXCEPTIONTHROWN");
                System.out.println(e.toString());
                System.out.println(e.getCode());
                System.out.println(e.getResponseBody());
                System.out.println(e.getResponseHeaders());
            }
            return countryCodes;
        }

        protected void onPostExecute(List<String> result) {
            System.out.println("Country Codes API Call Complete");
            System.out.println(result.toString());
        }
    }
}