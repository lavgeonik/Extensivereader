package com.lavgeo.extensivereader;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    TextView textViewFileName;

    // Request code for create a document.
    private static final int CREATE_FILE = 1;
    // Request code for selecting a document.
    private static final int PICK_FILE = 2;

    private Uri uriBook;
    String file_name;
    String text;
    SharedPreferences pref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewFileName = findViewById(R.id.textViewFile);

        pref = getApplicationContext().getSharedPreferences("MY_PREF", Context.MODE_PRIVATE);
        String file_path = pref.getString(getString(R.string.cur_read_file), "choose file");
        print(file_path);
        if(!file_path.equals("choose file")){
            uriBook =  Uri.parse(file_path);
            // try

            String fileName;
            try{
                fileName = getFileName(uriBook);
            } catch (Exception e) {
                e.printStackTrace();
                fileName = "choose file";
                Toast.makeText(MainActivity.this, "can't open file", Toast.LENGTH_SHORT).show();
            }

            textViewFileName.setText(fileName);
        }

        File file_out = new File(getFilesDir(), getString(R.string.file_out));
        if(file_out.exists()){
            print("file_out exists");
            print("file_out is " + file_out.getAbsolutePath());
        }

        File root = this.getExternalFilesDir(null);
        File[] listOfFiles = root.listFiles();
        for (File file : listOfFiles)
            print("file in app folder " + file.getAbsolutePath());

    }


    private void openFile() { // Uri pickerInitialUri
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, PICK_FILE);
    }

    private void write_to_file(File file_out, String str){
        // File file_out = new File(context.getFilesDir(), getString(R.string.file_out));
        // If arrayList is empty, break method

        FileOutputStream outputStream = null;
        FileWriter out = null;
        try {

            out = new FileWriter(file_out);
            // rewrite file
            out.write(str);
            out.close();
            print("save text to file");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


        private void createFile() {
            Uri pickerInitialUri = null;

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            file_name = getFileName(uriBook);
            String words_file_name = file_name.replace(".txt", "_words.txt");
            intent.putExtra(Intent.EXTRA_TITLE, words_file_name);

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker when your app creates the document.
            // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

            startActivityForResult(intent, CREATE_FILE);
        }


    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == 2
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uriSource = null;
            if (resultData != null) {
                uriSource = resultData.getData();
                File textFileInAppFolder = new File(getFilesDir(), getFileName(uriSource));
                // example conver from file to URI Uri.fromFile(new File("/sdcard/sample.jpg"))
                print("try to copy file to app folder");
                print("uri source is " + uriSource.toString());
                print("distination file is " + textFileInAppFolder.getAbsolutePath());

                String my_text;
                try {
                    my_text = readTextFromUri(uriSource);
                } catch (IOException e) {
                    e.printStackTrace();
                    my_text = "";
                }
                print("my text is " + my_text);
                write_to_file(textFileInAppFolder, my_text);

                uriBook = Uri.fromFile(textFileInAppFolder);
                // save path to preferences
                writeConfig();
                // set file name on screen
                file_name = getFileName(uriBook);
                textViewFileName.setText(file_name);
                // Perform operations on the document using its URI.
                Log.d("myTag", uriSource.toString());
            }
        }
        if (requestCode == 1
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                String words = null;
                try {
                    words = readTextFromFile(new File(getFilesDir(), getString(R.string.file_out)));
                } catch (IOException e) {
                    print("words are" + words);
                    e.printStackTrace();
                }

                try {
                    writeTextFromUri(uri, words);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType("*/*");      //all files
        intent.setType("text/plain");   //XML file only
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), 2);  // FILE_SELECT_CODE
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }


    private void writeTextFromUri(Uri uri, String output) throws  IOException {
        OutputStream outputStream = getContentResolver().openOutputStream(uri, "wa");
        OutputStreamWriter osw = new OutputStreamWriter(outputStream);
        osw.append(output);
        osw.flush();
        osw.close();
    }

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream =
                     getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\r\n");
            }
        }
        return stringBuilder.toString();
    }

    private String readTextFromFile(File file) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream =
                     getContentResolver().openInputStream(Uri.fromFile(file));
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\r\n");
            }
        }
        return stringBuilder.toString();
    }

    private String getFileName(Uri uri){
        String result = null;
        String scheme = uri.getScheme();

        if (scheme.equals("file")) {
            result = uri.getLastPathSegment();
        }
        else if (scheme.equals("content")) {

            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        }
        print(result);
        return result;
    }

    public void onClickSaveFile(View view) {
        createFile();
    }


    public void onClickChooseFile(View view) {
        openFile();
    }


    public void onClickStartReading(View view) {
        readFile();
        Intent intent = new Intent(MainActivity.this, ReadingModeActivity.class);
        intent.putExtra("text", text);
        startActivity(intent);
    }

    private void readFile(){
        text = null;
        if (uriBook != null) {
            try {
                text = readTextFromUri(uriBook);
                // for debugging read text
                //print("text is " + text);
            } catch (IOException e) {
                Log.e("Error", e.toString());
            }
        }

        if (text != null) {
            print("read text successfully");
        } else{
            Log.e("Error", "text is null");
        }
    }

    private void writeConfig(){
        print("wrote config file " + getString(R.string.cur_read_file));

        SharedPreferences.Editor editor = pref.edit();
        String file_path = uriBook.toString();
        editor.putString(getString(R.string.cur_read_file), file_path);
        editor.putInt(getString(R.string.cur_scroll_position), 0);
        editor.apply();
        print("wrote config file " + getString(R.string.cur_read_file) + " " + file_path);
    }

    public void print(String str){
        Log.d("MyTAG", str);
    }
}
