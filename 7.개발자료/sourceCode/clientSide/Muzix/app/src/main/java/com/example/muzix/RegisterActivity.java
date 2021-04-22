package com.example.muzix;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/*@class RegisterActivity
* you can See Views when you do Regist*/
public class RegisterActivity extends AppCompatActivity {
    private ImageButton btn_return;
    private Button btn_register,btn_duplicate_chk;
    private EditText edt_id,edt_nickname,edt_pwd,edt_pwd_chk;
    private boolean isChecked = false; // for duplicate check
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_layout);

        btn_return = (ImageButton)findViewById(R.id.btn_return);
        btn_register = (Button)findViewById(R.id.btn_register);
        btn_duplicate_chk = (Button)findViewById(R.id.btn_duplicate_chk);
        edt_id = (EditText)findViewById(R.id.edt_id);
        edt_nickname = (EditText)findViewById(R.id.edt_nickname);
        edt_pwd = (EditText)findViewById(R.id.edt_pwd);
        edt_pwd_chk = (EditText)findViewById(R.id.edt_pwd_chk);

        /*Regist Click listener by Lamdba expression */
        btn_return.setOnClickListener(v->doReturn());
        btn_register.setOnClickListener(v->confirmRegister());
        btn_duplicate_chk.setOnClickListener(v->checkIsDuplicate());

        edt_id.addTextChangedListener(new TextWatcher() {
            //if Text is Changed you need to recheck Duplicate
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                isChecked = false;
                btn_duplicate_chk.setText("중복 확인");
                btn_duplicate_chk.setFocusable(true);
                btn_duplicate_chk.setClickable(true);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    /*do Confirm Regist Local Account*/
    private void confirmRegister(){
        String tmp_id = edt_id.getText().toString();
        String tmp_pwd = edt_pwd.getText().toString();
        String tmp_pwd_chk = edt_pwd_chk.getText().toString();
        String tmp_nickname = edt_nickname.getText().toString();


        /*============================================================================*/
        /*Error check when you try to regist your account*/
        //For Focus when you got a error from app
        InputMethodManager imm =
                (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        if(tmp_id.equals("")|| tmp_id==null) {
            errorHandle("ID를 입력하지 않았습니다.");
            edt_id.post(new Runnable() {
                @Override
                public void run() {
                    edt_id.setFocusableInTouchMode(true);
                    edt_id.requestFocus();
                    imm.showSoftInput(edt_id,0);
                }
            });
        }
        else if(!isChecked)
            errorHandle("ID 중복확인을 하십시오.");
        else if(tmp_nickname.equals("")|| tmp_nickname==null) {
            errorHandle("닉네임을 설정해주세요.");
            edt_nickname.post(new Runnable() {
                @Override
                public void run() {
                    edt_nickname.setFocusableInTouchMode(true);
                    edt_nickname.requestFocus();
                    imm.showSoftInput(edt_nickname,0);
                }
            });
        }
        else if(tmp_pwd.equals("")|| tmp_pwd==null) {
            errorHandle("비밀번호를 입력하지 않았습니다.");
            edt_pwd.post(new Runnable() {
                @Override
                public void run() {
                    edt_pwd.setFocusableInTouchMode(true);
                    edt_pwd.requestFocus();
                    imm.showSoftInput(edt_pwd,0);
                }
            });
        }
        else if(tmp_pwd_chk.equals("")|| tmp_pwd_chk==null) {
            errorHandle("비밀번호를 입력하지 않았습니다.");
            edt_pwd_chk.post(new Runnable() {
                @Override
                public void run() {
                    edt_pwd_chk.setFocusableInTouchMode(true);
                    edt_pwd_chk.requestFocus();
                    imm.showSoftInput(edt_pwd_chk,0);
                }
            });
        }
        else{
            if(tmp_pwd.equals(tmp_pwd_chk))
                new JSPServerConnector(1).execute(getString(R.string.insert_member),"member_id",tmp_id,"nickname",tmp_nickname,"password",tmp_pwd);
            else
                errorHandle("비밀번호가 일치하지 않습니다.");
        }

    }

    /*check ID duplicate*/
    private void checkIsDuplicate(){
        String tmp_id = edt_id.getText().toString();

        /* execute if edt_id is not empty*/
        if(!(tmp_id.equals("") || tmp_id ==null))
            new JSPServerConnector(0).execute(getString(R.string.select_member),"member_id",tmp_id);
        else {
            errorHandle("ID를 입력하세요.");
        }
    }


    private void doReturn(){
        finish();
    }

    public void errorHandle(String msg){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(RegisterActivity.this);
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

    /*=====================Connect to JSPServer===========================================*/
    class JSPServerConnector extends AsyncTask<String, Void,JSONObject> {

        private int type; //0번이면 -> duplicate check, 1번이면-> insert

        public JSPServerConnector(int type){
            this.type = type;
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            HttpURLConnection con = null;
            try {
                URL myurl = new URL(strings[0]);
                con = (HttpURLConnection) myurl.openConnection();
                con.setDefaultUseCaches(false);
                con.setDoInput(true);                         // 서버에서 읽기 모드 지정
                con.setDoOutput(true);                       // 서버로 쓰기 모드 지정
                con.setRequestMethod("POST");
                con.setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
                StringBuffer buffer = new StringBuffer();

                for(int i =1; i<strings.length;i+=2){
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
                    Log.e("TAG-JSPServer-error", "Type : "+type+" Connection Error!");
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
            super.onPostExecute(jsonObject);
            JSONObject json_data = jsonObject;
            try {
                if (type == 0) {
                    if (json_data.getString("result").equals("success")){
                        isChecked =false;
                        errorHandle("ID가 존재합니다.");
                    }
                    else{
                        isChecked = true;
                        btn_duplicate_chk.setText("확인 완료");
                        btn_duplicate_chk.setFocusable(false);
                        btn_duplicate_chk.setClickable(false);
                    }
                }
                else if(type ==1){
                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(RegisterActivity.this);
                    builder.setTitle("가입 성공");
                    builder.setMessage("회원 가입을 완료하였습니다.");
                    builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            RegisterActivity.this.finish();
                            dialogInterface.dismiss();
                        }
                    });
                    builder.show();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
