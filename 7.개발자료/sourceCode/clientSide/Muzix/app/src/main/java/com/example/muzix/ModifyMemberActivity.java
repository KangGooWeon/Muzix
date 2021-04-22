package com.example.muzix;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/*this activity is created for modify account*/
public class ModifyMemberActivity extends AppCompatActivity {
    private EditText edt_nickname,edt_pwd,edt_pwd_chk;
    private Button btn_modify,btn_unregister;
    private ImageButton btn_return;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private String modified_nickname,user_id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_layout);

        pref = this.getSharedPreferences("MuzixAutoLogin", Activity.MODE_PRIVATE);

        /*동일한 레이아웃을 사용하기에
        수정 페이지에 맞게 수정*/
        TextView title = (TextView)findViewById(R.id.register_title);
        title.setText("계정 수정");
        EditText edt_id = (EditText)findViewById(R.id.edt_id);
        edt_id.setFocusableInTouchMode(false);
        edt_id.setFocusable(false);
        edt_id.setClickable(false);
        Button duplicate_chk = (Button)findViewById(R.id.btn_duplicate_chk);
        duplicate_chk.setVisibility(View.GONE);
        btn_modify = (Button)findViewById(R.id.btn_register);
        btn_modify.setText("수정 하기");
        btn_return = (ImageButton)findViewById(R.id.btn_return);
        btn_unregister = (Button)findViewById(R.id.btn_unregister);
        btn_unregister.setVisibility(View.VISIBLE);

        edt_nickname = (EditText)findViewById(R.id.edt_nickname);
        edt_pwd = (EditText)findViewById(R.id.edt_pwd);
        edt_pwd_chk = (EditText)findViewById(R.id.edt_pwd_chk);

        user_id = pref.getString("MUZIXID",null);
        edt_id.setText(user_id);
        edt_nickname.setText(pref.getString("NICKNAME",null));
        edt_pwd.setText(pref.getString("MUZIXPWD",null));

        btn_return.setOnClickListener(v->clickReturn());
        btn_modify.setOnClickListener(v->clickModify());
        btn_unregister.setOnClickListener(v->clickUnregist());
    }

    public void errorHandle(String msg){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
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

    /*수정버튼을 클릭했을 경우*/
    private void clickModify(){
        String tmp_nickname = edt_nickname.getText().toString();
        String tmp_pwd = edt_pwd.getText().toString();
        String tmp_pwd_chk = edt_pwd_chk.getText().toString();

        //For Focus when you got a error from app
        InputMethodManager imm =
                (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        if(tmp_nickname.equals("")||tmp_nickname==null) {
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
        else if(tmp_pwd.equals("")||tmp_pwd==null){
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
        else if(tmp_pwd_chk.equals("")||tmp_pwd_chk==null){
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
            if(tmp_pwd.equals(tmp_pwd_chk)){
                try {
                    modified_nickname = tmp_nickname;
                    new JSPServerConnector(1).execute(getString(R.string.update_member),"member_id",user_id,"nickname",tmp_nickname,"password",tmp_pwd);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
                errorHandle("비밀번호가 일치하지 않습니다.");
        }
    }

    /*뒤로가기를 클릭했을 경우*/
    private void clickReturn(){
        finish();
    }

    private void clickUnregist(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("회원 탈퇴");
        builder.setMessage("회원 탈퇴를 합니다.\n 탈퇴를 하시면 저장된 정보가 사라집니다. 탈퇴하시겠습니까?");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new JSPServerConnector(2).execute(getString(R.string.delete_member),"member_id",user_id);
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    /*=====================Connect to JSPServer===========================================*/
    class JSPServerConnector extends AsyncTask<String, Void,JSONObject> {
        private int type = 1;
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
                    Log.e("TAG-JSPServer-error", " Connection Error!");
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
                if(type ==1) {
                    if (json_data.getString("result").equals("success")) {
                        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(ModifyMemberActivity.this);
                        builder.setTitle("수정 완료");
                        builder.setMessage("회원 정보 수정을 완료하였습니다.");
                        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                editor = pref.edit();
                                editor.putString("NICKNAME", modified_nickname);
                                editor.commit();
                                finish();
                                dialogInterface.dismiss();
                            }
                        });
                        builder.show();
                    } else {
                        errorHandle("수정을 실패했습니다.");
                    }
                }
                else if(type ==2){
                    if (json_data.getString("result").equals("success")){
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                        finishAffinity();
                    }
                    else{
                        errorHandle("회원 탈퇴에 실패했습니다.");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
