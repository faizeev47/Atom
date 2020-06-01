package com.example.quiztaker;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

public class QuizFragment extends DialogFragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_QUESTION = BuildConfig.APPLICATION_ID + "ARG_QUESTION";
    private static final String ARG_OPTIONS = BuildConfig.APPLICATION_ID + "ARG_OPTIONS";
    private static final String ARG_ANSWER = BuildConfig.APPLICATION_ID + "ARG_ANSWER";
    private static final String LOG_TAG = QuizFragment.class.getSimpleName() + "Logging ";

    private final int[][] STATES = new int[][] {
            new int[] { android.R.attr.state_checked},
            new int[] {-android.R.attr.state_checked}
    };

    private RadioGroup mRadioGroup;
    private TextView mChoice;
    private Button mProceedButton;
    private Button mAnswerButton;

    private String mQuestion;
    private String[] mOptions;
    private int mAnswer;
    private int mUserAnswer;

    public QuizFragment() {
        // Required empty public constructor
        super();
    }

    public static QuizFragment newInstance(String question, String[] options, int answer) {
        QuizFragment fragment = new QuizFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUESTION, question);
        args.putStringArray(ARG_OPTIONS, options);
        args.putInt(ARG_ANSWER, answer);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mQuestion = getArguments().getString(ARG_QUESTION);
            mOptions = getArguments().getStringArray(ARG_OPTIONS);
            mAnswer = getArguments().getInt(ARG_ANSWER);
        }
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_quiz, container, false);

        mChoice = rootView.findViewById(R.id.quiz_choice);
        mProceedButton = rootView.findViewById(R.id.quiz_next);
        mAnswerButton = rootView.findViewById(R.id.quiz_answer);
        ((TextView)rootView.findViewById(R.id.quiz_question)).setText("Q. " + mQuestion);

        mChoice.setVisibility(View.INVISIBLE);
        mProceedButton.setEnabled(false);

        mAnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check_answer(v);
            }
        });
        mProceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                proceed(v);
            }
        });
        
        mRadioGroup = rootView.findViewById(R.id.quiz_options_group);
        for (int i = 0, l = mOptions.length; i < l; i++) {
            mRadioGroup.addView(getRadioOption(mOptions[i], i + 1));
        }
        return rootView;
    }

    private View getRadioOption(String option, int number) {
        RadioButton radioButton = new RadioButton(getContext());
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
        Log.d(LOG_TAG, "selected: " + mRadioGroup.getCheckedRadioButtonId() + "\tanswer: " + mAnswer);
        int[] incorrect = new int[] {
                Color.RED,
                Color.RED
        };

        int[] correct = new int[] {
                Color.GREEN,
                Color.GREEN
        };


        ((RadioButton)mRadioGroup.getChildAt(mAnswer)).setButtonTintList(new ColorStateList(STATES, correct));
        int checked = (int)mRadioGroup.getCheckedRadioButtonId() - 1;
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
        dismiss();
    }
}
