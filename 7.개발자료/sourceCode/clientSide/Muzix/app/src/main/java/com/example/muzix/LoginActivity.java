package com.example.muzix;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.usermgmt.LoginButton;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutionException;

/**
 * Created by arden on 2019-03-20.
 */

/*Login Activity ic created for Login View
* You can Login this app in Local Id and also KaKao Id*/

public class LoginActivity extends AppCompatActivity {

    public LoginButton btn_kakao_login;
    private SessionCallback callback; // for Kakao
    private EditText edt_id, edt_pwd;
    private Button btn_login,btn_register;
    private SharedPreferences pref; //for auto Login
    private SharedPreferences.Editor editor; // for auto Login
    private String id = null;
    private String pwd = null;
    private String nickname = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = getSharedPreferences("MuzixAutoLogin", Activity.MODE_PRIVATE);
        id = pref.getString("MUZIXID", null);
        pwd = pref.getString("MUZIXPWD", null);
        nickname = pref.getString("NICKNAME",null);
        boolean isKakao=pref.getBoolean("ISKAKAO",true);
        if(!isKakao) {
            if (id != null && pwd != null && nickname !=null) {
                try {
                    new JSPServerConnector(2).execute("member_id", id).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            try {
                new JSPServerConnector(3).execute("member_id",id).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        setContentView(R.layout.login_layout);
        /*=====================================================*/
        /*                                                     */
        /*             for kakao api login                     */
        /*                                                     */
        /*=====================================================*/
        btn_kakao_login = (LoginButton) findViewById(R.id.btn_kakao_login);
        callback = new SessionCallback();
        Session.getCurrentSession().addCallback(callback);
        /*=====================================================*/

        edt_id = (EditText)findViewById(R.id.edt_id);
        edt_pwd = (EditText)findViewById(R.id.edt_pwd);
        btn_login = (Button)findViewById(R.id.btn_login);
        btn_register = (Button)findViewById(R.id.register_btn);

        btn_login.setOnClickListener(v->doLogin());
        btn_register.setOnClickListener(v->doRegister());
    }

    public void errorHandle(String msg){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("오류");
        builder.setMessage(msg);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Session.getCurrentSession().removeCallback(callback);
    }

    /*do Login with Local id Not the KaKao*/
    private void doLogin(){
        String tmp_id = edt_id.getText().toString();
        String tmp_pwd = edt_pwd.getText().toString();

        if(tmp_id.equals("") || tmp_id == null){
            //error check
            errorHandle("ID를 입력해주세요.");
            return;
        }
        else if(tmp_pwd.equals("") || tmp_pwd == null){
            //error check
            errorHandle("비밀번호를 입력해주세요.");
            return;
        }
        new JSPServerConnector(1).execute("member_id",tmp_id);
    }

    /*do Regist Account for Local Id Not the KaKao*/
    private void doRegister(){
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private class SessionCallback implements ISessionCallback {

        @Override
        public void onSessionOpened() {
            redirectSignupActivity();  // 세션 연결성공 시 redirectSignupActivity() 호출
        }
        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if(exception != null) {
                Logger.e(exception);
            }
            setContentView(R.layout.activity_main); // 세션 연결이 실패했을때
        }                                            // 로그인화면을 다시 불러옴
    }

    protected void redirectSignupActivity() {       //세션 연결 성공 시 SignupActivity로 넘김
        final Intent intent = new Intent(this, KakaoSignupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }

    /*===========================================================*/
    /*                                                           */
    /*                   Server Connect with AsyncTask           */
    /*                                                           */
    /*===========================================================*/
    class JSPServerConnector extends AsyncTask<String,Void,JSONObject> {

        private int type = 2;

        public JSPServerConnector(int type){
            this.type = type;
        }
        @Override
        protected JSONObject doInBackground(String... strings) {
            HttpURLConnection con = null;
            try {
                URL myurl = new URL(getString(R.string.select_member));
                con = (HttpURLConnection) myurl.openConnection();
                con.setDefaultUseCaches(false);
                con.setDoInput(true);                         // 서버에서 읽기 모드 지정
                con.setDoOutput(true);                       // 서버로 쓰기 모드 지정
                con.setRequestMethod("POST");
                con.setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
                StringBuffer buffer = new StringBuffer();

                for(int i =0; i<strings.length;i+=2){
                    buffer.append(strings[i]).append("=").append(strings[i+1]);
                    if(i < strings.length-2)
                        buffer.append("&");
                }

                PrintWriter pw = new PrintWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
                pw.write(buffer.toString());
                pw.flush();

                int response = con.getResponseCode();
                if (response >=200 && response <=300) {
                    StringBuilder builder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(con.getInputStream(),"UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return new JSONObject(builder.toString());
                } else {
                    Log.e("TAG-Server-error", "Connection Error!");
                }

            } catch (Exception e) {

                e.printStackTrace();

            } finally {
                con.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            JSONObject json_data = jsonObject;
            if(type ==1) {
                String tmp_id = edt_id.getText().toString();
                String tmp_pwd = edt_pwd.getText().toString();
                try {
                    if (json_data.getString("result").equals("success")) {
                        if (json_data.getString("password").equals(tmp_pwd)) {
                            editor = pref.edit();
                            editor.putString("MUZIXID", tmp_id);
                            editor.putString("MUZIXPWD", tmp_pwd);
                            editor.putString("NICKNAME",json_data.getString("nickname"));
                            editor.putBoolean("ISKAKAO",false);
                            editor.commit();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            errorHandle("비밀번호가 틀렸습니다.");
                            return;
                        }
                    } else {
                        errorHandle("ID가 존재하지 않습니다.");
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if(type ==2){ //Auto Login
                try {
                    if (json_data.getString("result").equals("success")) {
                        if (json_data.getString("password").equals(pwd)) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            else if(type ==3){ // kakao auto Login
                try {
                    if (json_data.getString("result").equals("success")) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}