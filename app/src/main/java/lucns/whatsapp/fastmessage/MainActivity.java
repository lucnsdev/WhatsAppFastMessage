package lucns.whatsapp.fastmessage;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import lucns.whatsapp.fastmessage.utils.Notify;

public class MainActivity extends Activity {

    private String mimeType, fileName;
    private TextView textFileName;
    private Button buttonSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        EditText editTextMessage = findViewById(R.id.editTextMessage);
        EditText editTextTelephone = findViewById(R.id.editTextTelephone);
        textFileName = findViewById(R.id.textFileName);

        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonSend) {
                    if (mimeType == null) {
                        String number = editTextTelephone.getText().toString();
                        if (number.length() < 8) number = "+55859" + number;
                        if (number.length() < 9) number = "+5585" + number;
                        if (number.length() < 12) number = "+55" + number;
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + number + "&text=" + editTextMessage.getText().toString()));
                        startActivity(i);
                        return;
                    }
                    Uri uri = Uri.parse("content://" + MyProvider.AUTHORITY + "/share");
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setPackage("com.whatsapp");
                    intent.putExtra(Intent.EXTRA_TEXT, editTextMessage.getText().toString());
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setType(mimeType);
                    startActivity(intent);
                } else if (v.getId() == R.id.buttonPickFile) {
                    mimeType = null;
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                    startActivityForResult(intent, 1234);
                }
            }
        };
        Button buttonPickFile = findViewById(R.id.buttonPickFile);
        buttonSend = findViewById(R.id.buttonSend);
        buttonPickFile.setOnClickListener(onClick);
        buttonSend.setOnClickListener(onClick);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonSend.setEnabled(editTextMessage.getText().length() > 0 && editTextTelephone.getText().length() > 7);
            }
        };
        editTextTelephone.addTextChangedListener(textWatcher);
        editTextMessage.addTextChangedListener(textWatcher);

        clearCache();
    }

    private void clearCache() {
        File folder = new File(getCacheDir().getPath() + "/shared_files");
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isFile()) file.delete();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) {
            Notify.showToast(R.string.canceled);
            return;
        }
        mimeType = null;
        Uri uri = data.getData();
        fileName = getFileName(uri);
        if (fileName == null) {
            textFileName.setText(R.string.no_selected_file);
            buttonSend.setText(R.string.send);
        }
        else {
            textFileName.setText(fileName);
            buttonSend.setText(R.string.share);
        }
        onFilePicked(uri);
    }

    private void onFilePicked(Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                clearCache();
                File file = copyFile(uri);
                if (file == null || !file.exists() || file.length() == 0) {
                    Notify.showToast(R.string.error_file_read);
                    return;
                }
                try {
                    mimeType = Files.probeContentType(file.toPath());
                } catch (IOException e) {
                    mimeType = "*/*";
                }
                Log.d("lucas", "path " + file.getPath());
                Log.d("lucas", "mimeType " + mimeType);


                //file.delete();
            }
        }).start();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }

        return result;
    }

    private File copyFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(getCacheDir().getPath() + "/shared_files", fileName);
            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            if (tempFile.exists() && tempFile.length() > 0) return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}