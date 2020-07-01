package com.example.atom;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class QuestionActivity extends AppCompatActivity {
    private static final String LOG_TAG = QuestionActivity.class.getSimpleName() + "Logging: ";

    private static final String EXTRA_QUESTION = BuildConfig.APPLICATION_ID + "EXTRA_QUESTION";
    private static final String EXTRA_OPTIONS = BuildConfig.APPLICATION_ID + "EXTRA_OPTIONS";
    private static final String EXTRA_ANSWER = BuildConfig.APPLICATION_ID + "EXTRA_ANSWER";
    private static final String EXTRA_IS_FINAL = BuildConfig.APPLICATION_ID + "EXTRA_IS_FINAL";
    private static final String EXTRA_USER_ANSWER = BuildConfig.APPLICATION_ID + "EXTRA_USER_ANSWER";

    private RadioGroup mRadioGroup;
    private Button mProceedButton;
    private TextView mChoice;

    private int mAnswer;
    private int mUserAnswer;

    private final int[][] STATES = new int[][] {
            new int[] { android.R.attr.state_checked},
            new int[] {-android.R.attr.state_checked}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        this.setFinishOnTouchOutside(false);

        mRadioGroup = findViewById(R.id.question_choices_group);
        TextView questionView = findViewById(R.id.question_text);
        mChoice = findViewById(R.id.question_result);
        mProceedButton = findViewById(R.id.question_btn_next);

        mChoice.setVisibility(View.INVISIBLE);
        mProceedButton.setEnabled(false);

        if (getIntent().hasExtra(EXTRA_IS_FINAL)) {
            mProceedButton.setText(R.string.button_end);
        }

        Bundle questionBundle = getIntent().getExtras();

        if (questionBundle == null) {
            setResult(RESULT_CANCELED);
            Log.e(LOG_TAG, "Missing bundle for question!");
            finish();
        }


        String question = questionBundle.getString(EXTRA_QUESTION);
        questionView.setText("Q. " + question);


        int answer = questionBundle.getInt(EXTRA_ANSWER);
        mAnswer = answer;

        String[] mOptions = questionBundle.getStringArray(EXTRA_OPTIONS);
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
        radioButton.setTextColor(getColor(R.color.colorPrimary));
        return radioButton;
    }

    public void check_answer(View view) {
        int[] incorrect = new int[] {
                getColor(R.color.warning),
                getColor(R.color.warning)
        };

        int[] correct = new int[] {
                getColor(R.color.success),
                getColor(R.color.success)
        };


        ((RadioButton)mRadioGroup.getChildAt(mAnswer)).setButtonTintList(new ColorStateList(STATES, correct));
        int checked = mRadioGroup.indexOfChild(findViewById(mRadioGroup.getCheckedRadioButtonId()));


        Log.d(LOG_TAG, "selected: " + checked + "\tanswer: " +  mAnswer);


        if (checked != mAnswer) {
            ((RadioButton)mRadioGroup.getChildAt(checked)).setButtonTintList(new ColorStateList(STATES, incorrect));
            mChoice.setText(R.string.response_incorrect);
            mChoice.setTextColor(getColor(R.color.warning));
        }
        else {
            mChoice.setText(R.string.response_correct);
            mChoice.setTextColor(getColor(R.color.success));
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
