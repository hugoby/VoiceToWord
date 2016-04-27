package com.example.huayu.voicetowordtest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

/**
 * @auther 申泽邦
 *
 * @date 2015/8/27
 *
 * 语音识别模块
 *
 * */

public class MainActivity extends ActionBarActivity {

    // 语音识别对象
    private SpeechRecognizer mAsr;

    // 云端语法文件
    private String mCloudGrammar = null;

    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    // 缓存
    private SharedPreferences mSharedPreferences;

    private static final String KEY_GRAMMAR_ABNF_ID = "grammar_abnf_id";
    private static final String GRAMMAR_TYPE_ABNF = "abnf";
    private static final String GRAMMAR_TYPE_BNF = "bnf";

    // 语法、词典临时变量
    String mContent;
    // 函数调用返回值
    int ret = 0;

    // 语音听写UI
    private RecognizerDialog mDialog;

    ImageView microphone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        microphone=(ImageView)findViewById(R.id.microphone);
        VoiceToWord();
        microphone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListen();
            }
        });
    }



    private void VoiceToWord(){
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=55cd69af");
        mDialog = new RecognizerDialog(this, mInitListener);
        mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mDialog.setParameter(SpeechConstant.ACCENT, "mandarin");

        mDialog.setParameter("asr_sch", "1");
        mDialog.setParameter("nlp_version", "2.0");

        mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);

        mCloudGrammar = FucUtil.readFile(this, "contral_sample.abnf", "utf-8");

        mSharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        mContent = new String(mCloudGrammar);

        //指定引擎类型
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        mAsr.setParameter(SpeechConstant.TEXT_ENCODING,"utf-8");
        ret = mAsr.buildGrammar(GRAMMAR_TYPE_ABNF, mContent, mCloudGrammarListener);
        if(ret != ErrorCode.SUCCESS){
            Log.e("tag", "语法构建失败,错误码：" + ret);
        }
    }

    private void startListen(){
        if (!setParam()){
            showTip("请先构建语法");
            return;
        }
        mDialog.setListener(mRecognizerDialogListener);
        mDialog.show();
        showTip("开始说话");
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.e("InitListener", "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.e("onInitError","初始化失败,错误码："+code);
            }
        }
    };

    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            if (null != results) {
                Log.e("recognizer result：", results.getResultString());
                String text ;
                text = JsonParser.parseGrammarResult(results.getResultString());
                // 显示
                Log.e("text",text);
                showTip(text);
            } else {
                Log.d("onResult", "recognizer result : null");
            }
        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }

    };

    /**
     * 参数设置
     * @param
     * @return
     */
    public boolean setParam(){
        boolean result = false;
        //设置识别引擎
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        //设置返回结果为json格式
        mAsr.setParameter(SpeechConstant.RESULT_TYPE, "json");

        String grammarId = mSharedPreferences.getString(KEY_GRAMMAR_ABNF_ID, null);
        if(TextUtils.isEmpty(grammarId))
        {
            result =  false;
        }else {
            //设置云端识别使用的语法id
            mAsr.setParameter(SpeechConstant.CLOUD_GRAMMAR, grammarId);
            result =  true;
        }

        return result;
    }

    /**
     * 云端构建语法监听器。
     */
    private GrammarListener mCloudGrammarListener = new GrammarListener() {
        @Override
        public void onBuildFinish(String grammarId, SpeechError error) {
            if(error == null){
                String grammarID = new String(grammarId);
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                if(!TextUtils.isEmpty(grammarId))
                    editor.putString(KEY_GRAMMAR_ABNF_ID, grammarID);
                editor.commit();
                Log.e("GrammarListener","语法构建成功：" + grammarId);
            }else{
                Log.e("GrammarListener","语法构建失败,错误码：" + error.getErrorCode());
            }
        }
    };

    private void showTip(final String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}
