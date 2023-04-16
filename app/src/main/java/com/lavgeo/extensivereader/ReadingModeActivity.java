package com.lavgeo.extensivereader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnTouchListener;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadingModeActivity extends Activity {


    File file_out;
    TextView readingTextView;
    ScrollView readingScrollView;
    //private float[] lastTouchDownXY = new float[2];
    int mOffset;

    int scrollPositionY;
    int prevScrollPositionY;

    SharedPreferences pref;
    int pref_int;

    String TAG = "MyTAG";

    ArrayList<String> appendedWords = new ArrayList();
    String spanish_letters = "ABCDEGHILMNÑOPQRSTUVWYabcdefghijklmnopqrstuvwxyzáéíñóúü";
    String my_punctuation = "[^!?¡¿.\n]*";



    // on creation base method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        file_out = new File(getFilesDir(), getString(R.string.file_out));
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_reading_mode);
        // TextView
        readingTextView = findViewById(R.id.textViewReading);
        // ScrollView
        readingScrollView = findViewById(R.id.scrollViewReading);
        readingTextView.setMovementMethod(new ScrollingMovementMethod());

        Intent intent = getIntent();
        String text = intent.getStringExtra("text");

        readingTextView.setText(text);

//      readingTextView.setHorizontallyScrolling(false);

        myMethods();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }


    // Метод перед приостановкой приложения (сворачивание)
    @Override
    protected void onPause() {
        super.onPause();
        writeConfig();
        Log.d(TAG, "onPause");
    }


    // Метод после возобнавления приложения
    @Override
    protected void onResume() {
        super.onResume();
        pref = getApplicationContext().getSharedPreferences("MY_PREF", Context.MODE_PRIVATE);
        pref_int = pref.getInt(getString(R.string.cur_scroll_position), 0);

        if (pref_int != 0) {
            print("try to scroll " + pref_int);
            scrollPositionY = pref_int;
            // final for runnable
            final int pref_int2 = pref_int;
            readingScrollView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    readingScrollView.scrollTo(0, pref_int2);
                }
            },200);

        }
        else {
            print("pref_int is 0 " + pref_int);
        }
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onBackPressed() {
        writeConfig();
        super.onBackPressed();
    }


    @SuppressLint("ClickableViewAccessibility")
    public void myMethods() {

        readingScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                int scrollY = readingScrollView.getScrollY();
                scrollPositionY = scrollY;
                print("scrollY: " + scrollY);
            }
        });

        readingTextView.setOnTouchListener(new OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(ReadingModeActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    print("onDoubleTap");
                    mOffset = readingTextView.getOffsetForPosition(e.getX(), e.getY());
                    //  mTxtOffset.setText("" + mOffset);
                    String word = findWordForRightHanded(readingTextView.getText().toString(), mOffset);
                    String wordInSentence = word;

                    word = word.trim();
                    String sentences;
                    sentences = getSentense3();
                    print("word is " + word);
                    //sentences = sentences.replace("\n", " ");

                    Pattern p = Pattern.compile(my_punctuation + wordInSentence + my_punctuation);
                    Matcher m = p.matcher(sentences);
                    String singleSentence2 = "";
                    if(m.find()) {
                        print("found in sentences");
                        int punc = m.end();
                        String singleSentence = m.group();
                        singleSentence = singleSentence.trim() + sentences.charAt(punc);
                        singleSentence = singleSentence.replaceAll("[\t\r\n]", "");
                        Pattern p2 = Pattern.compile("\\s+");
                        Matcher m2 = p2.matcher(singleSentence);
                        singleSentence2 = m2.replaceAll(" ");
                        print(singleSentence2);
                    }

                    print("sentences is " + singleSentence2);

                    if(isContainsSpanishLetter(word)){
                        String output = "";
                        output = word.toLowerCase() + "\t" + singleSentence2 + "\r\n";
                        append_to_file(output);
                    }

                    Toast.makeText(ReadingModeActivity.this, word, Toast.LENGTH_SHORT).show();
                    //appendedWords.add(word);
                    //write_words_to_file(appendedWords);
                    return super.onDoubleTap(e); // super.onDoubleTap(e);
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //print("on Touch");
                gestureDetector.onTouchEvent(event);
                return true; // true
            }
        });

    }


    // method to test if spanish letter in given string
    private boolean isContainsSpanishLetter(String str){
        boolean isContains = false;

        for (int i = 0; i < spanish_letters.length(); i++) {
            char ch = spanish_letters.charAt(i);
            for (int y = 0; y < str.length(); y++) {
                char chInWord = str.charAt(y);
                if(chInWord == ch) {
                    isContains = true;
                    return isContains;
                }
            }
        }
        print("isContais is " + isContains);

        return isContains;
    }


    private void append_to_file(String output){
        // File file_out = new File(context.getFilesDir(), getString(R.string.file_out));
        // If arrayList is empty, break method

        print("file out is " + getString(R.string.file_out));

        output += "\r\n";

        //FileOutputStream outputStream = null;
        FileWriter out = null;
        try {
            out = new FileWriter(file_out, true);
            // add to file
            out.append(output);
            out.close();
            print("saved to file");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void write_words_to_file(ArrayList<String> arrayList){
        // File file_out = new File(context.getFilesDir(), getString(R.string.file_out));
        // If arrayList is empty, break method


        print("file out is " + getString(R.string.file_out));
        print("array list size is " + arrayList.size());
        if (arrayList.size() == 0){
            print("array list is empty");
            return;
        }

        String output = "";
        for (int i=0; i < arrayList.size(); i++){
            String str = arrayList.get(i);
            // for last line
            if(i== arrayList.size()-1){
                output += str;
            }
            // other lines
            else {
                output += str + "\r\n";
            }
        }

        print("output string is " + output);
        FileWriter out = null;
        try {
            out = new FileWriter(file_out, true);
            // rewrite file
            //out.write(output);
            // add to file
            out.append(output);
            out.close();
            print("save words to file");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeConfig() {
        // 0 no sense to write
        if (scrollPositionY != 0){
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(getString(R.string.cur_scroll_position), scrollPositionY);
        editor.apply();
        print("wrote config file " + " " + scrollPositionY);
        }
        else {
            print("scrollPositionY is " + scrollPositionY);
        }
    }

    private String findWordForRightHanded(String str, int offset) { // when you touch ' ', this method returns left word.
        if (str.length() == offset) {
            offset--; // without this code, you will get exception when touching end of the text
        }

        if (str.charAt(offset) == ' ') {
            offset--;
        }
        int startIndex = offset;
        int endIndex = offset;

        try {
            while (str.charAt(startIndex) != ' ' && str.charAt(startIndex) != '\n') {
                startIndex--;
            }
        } catch (StringIndexOutOfBoundsException e) {
            startIndex = 0;
        }

        try {
            while (str.charAt(endIndex) != ' ' && str.charAt(endIndex) != '\n') {
                endIndex++;
            }
        } catch (StringIndexOutOfBoundsException e) {
            endIndex = str.length();
        }

        // without this code, you will get 'here!' instead of 'here'
        // if you use only english, just check whether this is alphabet,
        // but 'I' use korean, so i use below algorithm to get clean word.
        char last = str.charAt(endIndex - 1);
        if (last == ',' || last == '.' ||
                last == '!' || last == '?' ||
                last == ':' || last == ';') {
            endIndex--;
        }

        return str.substring(startIndex, endIndex);
    }

    private String getSentense(){
        String result = "";

        int count = readingTextView.getLineCount();
        for (int line = 0; line < count; line++) {
            int start = readingTextView.getLayout().getLineStart(line);
            int end = readingTextView.getLayout().getLineEnd(line);
            CharSequence substring = readingTextView.getText().subSequence(start, end);
            result += String.valueOf(substring) + "\t";
        }
        return result;
    }


    private String getSentense2(){
        String result;
        int count = readingTextView.getLineCount();

        int y = readingScrollView.getScrollY();
        print("count is " + count);
        int start = readingTextView.getLayout().getLineStart(y);
        int end = readingTextView.getLayout().getLineEnd(readingTextView.getLineCount() - 1);
        print("start is " + start + " end is " + end);

        String displayed = readingTextView.getText().toString().substring(start, end);

        result = displayed;
        return result;
    }


    private String getSentense3() {
        int start = readingTextView.getLayout().getLineStart(getFirstLineIndex());
        int end = readingTextView.getLayout().getLineEnd(getLastLineIndex());
        String result = readingTextView.getText().toString().substring(start, end);
        return result;
    }

    /**
     * Gets the first line that is visible on the screen.
     *
     * @return
     */
    public int getFirstLineIndex() {
        int scrollY = readingScrollView.getScrollY();
        Layout layout = readingTextView.getLayout();
        if (layout != null) {
            return layout.getLineForVertical(scrollY);
        }
        Log.d(TAG, "Layout is null: ");
        return -1;
    }

    /**
     * Gets the last visible line number on the screen.
     * @return last line that is visible on the screen.
     */
    public int getLastLineIndex() {
        int height = readingScrollView.getHeight();
        int scrollY = readingScrollView.getScrollY();
        Layout layout = readingTextView.getLayout();
        if (layout != null) {
            return layout.getLineForVertical(scrollY + height);
        }
        return -1;
    }

    public void print(String str) {
        Log.d("MyTAG", str);
    }


}
