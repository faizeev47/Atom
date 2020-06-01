package com.example.quiztaker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.LinkedList;

public class Dialogs {
    public static class BasicMissileDialog extends DialogFragment {
        private static final String LOG_TAG = BasicMissileDialog.class.getSimpleName() + "Logging";

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("I can fire missiles!")
                    .setPositiveButton("Fire", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(LOG_TAG, "Missile fired!");
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(LOG_TAG, "Cancelled!");
                        }
                    });
            return builder.create();
        }
    }

    public static class ListMissileDialog extends DialogFragment {
        private static final String LOG_TAG = ListMissileDialog.class.getSimpleName() + "Logging: ";

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Pick your missile: ")
                    .setItems(R.array.missiles_array, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(LOG_TAG, getResources().getStringArray(R.array.missiles_array)[which] + " clicked");
                        }
                    });
            return builder.create();
        }
    }

    public static class SingleSelectableMissileDialog extends DialogFragment {
        private static final String LOG_TAG = SingleSelectableMissileDialog.class.getSimpleName() + "Logging: ";

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Pick your missile: ")
                    .setSingleChoiceItems(R.array.missiles_array, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(LOG_TAG, getResources().getStringArray(R.array.missiles_array)[which]);
                        }
                    });
            return builder.create();
        }
    }

    public static class MultiSelectableMissileDialog extends DialogFragment {
        private static final String LOG_TAG = MultiSelectableMissileDialog.class.getSimpleName() + "Logging: ";

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final String[] itemsArray = getResources().getStringArray(R.array.missiles_array);
            final boolean[] checkedItems = new boolean[itemsArray.length];

            builder.setTitle("Pick your missile: ")
                    .setMultiChoiceItems(R.array.missiles_array, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            checkedItems[which] = isChecked;
                            Log.d(LOG_TAG, itemsArray[which] + (isChecked ? " checked" : " unchecked"));
                        }
                    });

            return builder.create();
        }
    }

    public static class CustomMissileDialog extends DialogFragment {
        private static final String LOG_TAG = CustomMissileDialog.class.getSimpleName() + "Logging:";



        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(inflater.inflate(R.layout.dialog_missiles, null))
                .setPositiveButton("+ive", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "+ive clicked");
                    }
                })
                .setNegativeButton("-ive", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "-ive clicked");
                    }
                });
            return builder.create();
        }
    }

    public static class CustomQuizDialog extends DialogFragment {
        private static final String LOG_TAG = CustomMissileDialog.class.getSimpleName() + "Logging:";

        private final int[][] STATES = new int[][] {
                new int[] { android.R.attr.state_checked},
                new int[] {-android.R.attr.state_checked}
        };

        private static final String EXTRA_BUNDLE_ = BuildConfig.APPLICATION_ID + "EXTRA_BUNDLE_";
        private static final String EXTRA_QUESTION = BuildConfig.APPLICATION_ID + "EXTRA_QUESTION";
        private static final String EXTRA_OPTIONS = BuildConfig.APPLICATION_ID + "EXTRA_OPTIONS";
        private static final String EXTRA_ANSWER = BuildConfig.APPLICATION_ID + "EXTRA_ANSWER";

        private static final String ARG_QUESTION = BuildConfig.APPLICATION_ID + "ARG_QUESTION";
        private static final String ARG_OPTIONS = BuildConfig.APPLICATION_ID + "ARG_OPTIONS";
        private static final String ARG_ANSWER = BuildConfig.APPLICATION_ID + "ARG_ANSWER";

        private RadioGroup mRadioGroup;
        private TextView mChoice;
        private Button mProceedButton;
        private Button mAnswerButton;

        private FragmentManager mFragmentManager;
        private Bundle mQuestionAnswers;
        private LinkedList<Integer> mQuestionsToAsk;

        private String mQuestion;
        private String[] mOptions;
        private int mAnswer;
        private int mUserAnswer;

        public CustomQuizDialog() {
            super();
        }

        public CustomQuizDialog(Bundle questionAnswers, LinkedList<Integer> questionsToAsk, FragmentManager fragmentManager) {
            super();
            mQuestionAnswers = questionAnswers;
            mQuestionsToAsk = questionsToAsk;
            mFragmentManager = fragmentManager;
        }


        public static CustomQuizDialog newInstance(Bundle questionAnswers, LinkedList<Integer> questionsToAsk, FragmentManager fragmentManager) {
            CustomQuizDialog dialogFragment = new CustomQuizDialog(questionAnswers, questionsToAsk, fragmentManager);

            Bundle questionBundle = questionAnswers.getBundle(EXTRA_BUNDLE_ + questionsToAsk.removeFirst());
            Bundle args = new Bundle();
            args.putString(ARG_QUESTION, questionBundle.getString(EXTRA_QUESTION));
            args.putStringArray(ARG_OPTIONS, questionBundle.getStringArray(EXTRA_OPTIONS));
            args.putInt(ARG_ANSWER, questionBundle.getInt(EXTRA_ANSWER));
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        @Override
        public void onDismiss(@NonNull DialogInterface dialog) {
            super.onDismiss(dialog);
            if (mQuestionsToAsk.size() > 0) {
                Dialogs.CustomQuizDialog customQuizDialog = Dialogs.CustomQuizDialog.newInstance(mQuestionAnswers,
                        mQuestionsToAsk,
                        mFragmentManager);
                customQuizDialog.show(mFragmentManager, "Fragment");
            }
        }

        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            if (getArguments() != null) {
                mQuestion = getArguments().getString(ARG_QUESTION);
                mOptions = getArguments().getStringArray(ARG_OPTIONS);
                mAnswer = getArguments().getInt(ARG_ANSWER);
            }
            setCancelable(false);

            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View rootView = inflater.inflate(R.layout.dialog_quiz, null);
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

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(rootView);
            return builder.create();
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

        private void check_answer(View view) {
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

        private void proceed(View view) {
            dismiss();
        }
    }


}
