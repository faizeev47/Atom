package com.example.quiztaker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName() + "Logging: ";

    private static final int ANSWER_REQUEST = 1;

    private static final String EXTRA_QUESTION = BuildConfig.APPLICATION_ID + "EXTRA_QUESTION";
    private static final String EXTRA_OPTIONS = BuildConfig.APPLICATION_ID + "EXTRA_OPTIONS";
    private static final String EXTRA_ANSWER = BuildConfig.APPLICATION_ID + "EXTRA_ANSWER";
    private static final String EXTRA_IS_FINAL = BuildConfig.APPLICATION_ID + "EXTRA_IS_FINAL";
    private static final String EXTRA_BUNDLE_ = BuildConfig.APPLICATION_ID + "EXTRA_BUNDLE_";


    private Bundle mQuestionAnswers;
    private LinkedList<Integer> mQuestionsToAsk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        StringBuilder buffer = new StringBuilder();
        buffer.append(getString(R.string.page1_questions));
        String[] questionsStr = buffer.toString().split("#");

        mQuestionAnswers = new Bundle();
        mQuestionsToAsk = new LinkedList<>();

        int questionNo = 1;

        for(String questionPackage: questionsStr) {
            String[] parts = questionPackage.split(">");
            if (parts.length == 4) {
                Bundle newQuestion = new Bundle();
                newQuestion.putString(EXTRA_QUESTION, parts[1]);
                newQuestion.putInt(EXTRA_ANSWER, Integer.parseInt(parts[3].trim()) - 1 );
                newQuestion.putStringArray(EXTRA_OPTIONS, parts[2].split(":"));
                mQuestionAnswers.putBundle(EXTRA_BUNDLE_ + questionNo, newQuestion);
                mQuestionsToAsk.add(questionNo++);
            }
        }
    }

    public void open_basic_dialog(View view) {
        Dialogs.BasicMissileDialog dialogFragment = new Dialogs.BasicMissileDialog();
        dialogFragment.show(getSupportFragmentManager(), "Fragment Tag");
    }

    public void open_list_dialog(View view) {
        Dialogs.ListMissileDialog dialogFragment = new Dialogs.ListMissileDialog();
        dialogFragment.show(getSupportFragmentManager(), "Fragment Tag");
    }

    public void open_selectable_list_dialog(View view) {
        Dialogs.SingleSelectableMissileDialog dialogFragment = new Dialogs.SingleSelectableMissileDialog();
        dialogFragment.show(getSupportFragmentManager(), "Fragment Tag");
    }

    public void open_multi_selectable_list_dialog(View view) {
        Dialogs.MultiSelectableMissileDialog dialogFragment = new Dialogs.MultiSelectableMissileDialog();
        dialogFragment.show(getSupportFragmentManager(), "Fragmen tag");
    }

    public void open_custom_dialog(View view) {
        Dialogs.CustomMissileDialog dialogFragment = new Dialogs.CustomMissileDialog();
        dialogFragment.show(getSupportFragmentManager(), "Fragment Tag");
    }

    private void launchQuiz() {
        int remainingQuestions = mQuestionsToAsk.size();
        if (remainingQuestions > 0) {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtras(mQuestionAnswers.getBundle(EXTRA_BUNDLE_ + mQuestionsToAsk.getFirst()));
            if (remainingQuestions == 1) {
                intent.putExtra(EXTRA_IS_FINAL, 1);
            }
            startActivityForResult(intent, ANSWER_REQUEST);
        }
    }

    public void open_quiz_dialog(View view) {
        launchQuiz();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ANSWER_REQUEST) {
            if (resultCode == RESULT_OK) {
                mQuestionsToAsk.removeFirst();
                launchQuiz();
            }
        }
    }

    public void open_quiz_fragment(View view) {
        String question = "Which of the following was used in WWII?";
        String[] options = getResources().getStringArray(R.array.missiles_array);
        int answer = 0;
        final FragmentManager fragmentManager = getSupportFragmentManager();

        QuizFragment quizFragment = QuizFragment.newInstance(question, options, answer);
        Dialog dialog = quizFragment.getDialog();
        if (dialog != null) {
            Log.d(LOG_TAG, "okay");
        }
        else {
            Log.e(LOG_TAG, "null");
        }
        quizFragment.show(fragmentManager, "fragment");

    }

    public void open_quiz_dialogfragment(View view) {
        String question = "Which of the following was used in WWII?";
        String[] options = getResources().getStringArray(R.array.missiles_array);
        int answer = 1;

        Bundle currentQuestion = new Bundle();
        currentQuestion.putString(EXTRA_QUESTION, question);
        currentQuestion.putStringArray(EXTRA_OPTIONS, options);
        currentQuestion.putInt(EXTRA_ANSWER, answer);

        Bundle nextQuestion = new Bundle();
        nextQuestion.putString(EXTRA_QUESTION, "Which of the following was used in the Hiroshima incident?");
        nextQuestion.putStringArray(EXTRA_OPTIONS, options);
        nextQuestion.putInt(EXTRA_ANSWER, 0);

        Bundle questionAnswers = new Bundle();
        LinkedList<Integer> questionsToAsk = new LinkedList<>();

        int questionNumber = 1;
        questionsToAsk.add(1);
        questionAnswers.putBundle(EXTRA_BUNDLE_ + questionNumber, currentQuestion);
        questionNumber = 2;
        questionsToAsk.add(2);
        questionAnswers.putBundle(EXTRA_BUNDLE_+ questionNumber, nextQuestion);


        Dialogs.CustomQuizDialog customQuizDialog = Dialogs.CustomQuizDialog.newInstance(questionAnswers,
                questionsToAsk,
                getSupportFragmentManager());
        customQuizDialog.show(getSupportFragmentManager(), "Fragment");
    }

}
