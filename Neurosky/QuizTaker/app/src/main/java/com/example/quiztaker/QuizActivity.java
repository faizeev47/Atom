package com.example.quiztaker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Map;

public class QuizActivity extends AppCompatActivity {

    private static final String LOG_TAG = QuizActivity.class.getSimpleName() + "Logging: ";

    private static final String EXTRA_QUESTION = BuildConfig.APPLICATION_ID + "EXTRA_QUESTION";
    private static final String EXTRA_OPTIONS = BuildConfig.APPLICATION_ID + "EXTRA_OPTIONS";
    private static final String EXTRA_ANSWER = BuildConfig.APPLICATION_ID + "EXTRA_ANSWER";
    private static final String EXTRA_IS_FINAL = BuildConfig.APPLICATION_ID + "EXTRA_IS_FINAL";
    private static final String EXTRA_USER_ANSWER = BuildConfig.APPLICATION_ID + "EXTRA_USER_ANSWER";

    private RadioGroup mRadioGroup;
    private TextView mQuizQuestion;
    private Button mProceedButton;
    private TextView mChoice;

    private int mAnswer;
    private int mUserAnswer;
    private String[] mOptions;

    private final int[][] STATES = new int[][] {
            new int[] { android.R.attr.state_checked},
            new int[] {-android.R.attr.state_checked}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        this.setFinishOnTouchOutside(false);

        mRadioGroup = findViewById(R.id.quiza_options_group);
        mQuizQuestion = findViewById(R.id.quiza_question);
        mChoice = findViewById(R.id.quiza_choice);
        mProceedButton = findViewById(R.id.quiza_next);

        mChoice.setVisibility(View.INVISIBLE);
        mProceedButton.setEnabled(false);

        if (getIntent().hasExtra(EXTRA_IS_FINAL)) {
            mProceedButton.setText("End");
        }

        Bundle questionBundle = getIntent().getExtras();

        if (questionBundle == null) {
            setResult(RESULT_CANCELED);
            Log.e(LOG_TAG, "Missing bundle for question!");
            finish();
        }


        String question = questionBundle.getString(EXTRA_QUESTION);
        mQuizQuestion.setText("Q. " + question);


        int answer = questionBundle.getInt(EXTRA_ANSWER);
        mAnswer = answer;

        mOptions = questionBundle.getStringArray(EXTRA_OPTIONS);
        for (int i = 0, l = mOptions.length; i < l; i++) {
            mRadioGroup.addView(getRadioOption(mOptions[i], i + 1));
        }
    }

    private View getRadioOption(String option, int number) {
        RadioButton radioButton = new RadioButton(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(10, 10, 10, 10);
        radioButton.setText(option);
        radioButton.setLayoutParams(params);
        return radioButton;
    }

    public void check_answer(View view) {
        int[] incorrect = new int[] {
                Color.RED,
                Color.RED
        };

        int[] correct = new int[] {
                Color.GREEN,
                Color.GREEN
        };


        ((RadioButton)mRadioGroup.getChildAt(mAnswer)).setButtonTintList(new ColorStateList(STATES, correct));
        int checked = mRadioGroup.indexOfChild(findViewById(mRadioGroup.getCheckedRadioButtonId()));


        Log.d(LOG_TAG, "selected: " + checked + "\tanswer: " +  mAnswer);


        if (checked != mAnswer) {
            ((RadioButton)mRadioGroup.getChildAt(checked)).setButtonTintList(new ColorStateList(STATES, incorrect));
            mChoice.setText("Sorry, that was incorrect.");
            mChoice.setTextColor(Color.RED);
        }
        else {
            mChoice.setText("Great, that was correct!");
            mChoice.setTextColor(Color.GREEN);
        }
        mChoice.setVisibility(View.VISIBLE);

        mUserAnswer =  checked;

        for (int i = 0, l = mRadioGroup.getChildCount(); i < l; i++) {
            mRadioGroup.getChildAt(i).setEnabled(false);
        }

        mProceedButton.setEnabled(true);
    }

    public void proceed(View view) {
        Intent replyIntent = new Intent();
        replyIntent.putExtra(EXTRA_USER_ANSWER, mUserAnswer);
        setResult(RESULT_OK);
        finish();
    }
}
